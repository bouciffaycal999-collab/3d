package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.GroutTone
import com.example.domain.LayingPattern
import com.example.domain.RoomPerspectiveRenderer
import com.example.domain.RoomSpec
import com.example.domain.SampleTileGenerator
import com.example.domain.SampleTileOption
import com.example.domain.TilePlacementStats
import com.example.domain.TileSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class NormalizedCropRect(
    val left: Float = 0.1f,
    val top: Float = 0.1f,
    val right: Float = 0.9f,
    val bottom: Float = 0.9f,
) {
    fun clamped(): NormalizedCropRect {
        val l = left.coerceIn(0f, 0.94f)
        val t = top.coerceIn(0f, 0.94f)
        val r = right.coerceIn(l + 0.06f, 1f)
        val b = bottom.coerceIn(t + 0.06f, 1f)
        return NormalizedCropRect(l, t, r, b)
    }
}

data class TileVisualizerUiState(
    val samples: List<SampleTileOption>,
    val activeSampleId: String?,
    val activeTileName: String,
    val sourceBitmap: Bitmap,
    val croppedTileBitmap: Bitmap,
    val cropRect: NormalizedCropRect = NormalizedCropRect(0f, 0f, 1f, 1f),
    val pendingCropBitmap: Bitmap? = null,
    val pendingCropRect: NormalizedCropRect = NormalizedCropRect(0.12f, 0.12f, 0.88f, 0.88f),
    val isCropperOpen: Boolean = false,
    val isFullscreenOpen: Boolean = false,
    val isLoadingImage: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val tileSpec: TileSpec = TileSpec(),
    val roomSpec: RoomSpec = RoomSpec(),
    val stats: TilePlacementStats = TilePlacementStats.calculate(TileSpec(), RoomSpec()),
)

class TileVisualizerViewModel : ViewModel() {

    private val builtInSamples: List<SampleTileOption> = SampleTileGenerator.createBuiltInSamples()

    private val _uiState = MutableStateFlow(
        run {
            val first = builtInSamples.first()
            val initialTileSpec = TileSpec(
                widthCm = first.defaultWidthCm,
                heightCm = first.defaultHeightCm,
            )
            val initialRoomSpec = RoomSpec()
            TileVisualizerUiState(
                samples = builtInSamples,
                activeSampleId = first.id,
                activeTileName = first.name,
                sourceBitmap = first.bitmap,
                croppedTileBitmap = first.bitmap,
                tileSpec = initialTileSpec,
                roomSpec = initialRoomSpec,
                stats = TilePlacementStats.calculate(initialTileSpec, initialRoomSpec),
            )
        },
    )
    val uiState: StateFlow<TileVisualizerUiState> = _uiState.asStateFlow()

    fun selectSample(sample: SampleTileOption) {
        _uiState.update { state ->
            val updatedTile = state.tileSpec.copy(
                widthCm = sample.defaultWidthCm,
                heightCm = sample.defaultHeightCm,
            )
            state.copy(
                activeSampleId = sample.id,
                activeTileName = sample.name,
                sourceBitmap = sample.bitmap,
                croppedTileBitmap = sample.bitmap,
                cropRect = NormalizedCropRect(0f, 0f, 1f, 1f),
                tileSpec = updatedTile,
                stats = TilePlacementStats.calculate(updatedTile, state.roomSpec),
                errorMessage = null,
            )
        }
    }

    fun onPhotoPicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImage = true, errorMessage = null) }
            val loaded = withContext(Dispatchers.IO) {
                decodeScaledBitmap(context, uri, maxDim = 2048)
            }
            if (loaded == null) {
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        errorMessage = context.getString(com.example.R.string.error_load_image),
                    )
                }
                return@launch
            }
            val initialRect = defaultCenteredAspectRect(
                bmpW = loaded.width,
                bmpH = loaded.height,
                targetAspect = _uiState.value.tileSpec.widthCm / _uiState.value.tileSpec.heightCm,
            )
            _uiState.update { state ->
                state.copy(
                    isLoadingImage = false,
                    pendingCropBitmap = loaded,
                    pendingCropRect = initialRect,
                    isCropperOpen = true,
                )
            }
        }
    }

    fun openRecrop() {
        _uiState.update { state ->
            val rect = if (state.activeSampleId == null) {
                state.cropRect
            } else {
                NormalizedCropRect(0.08f, 0.08f, 0.92f, 0.92f)
            }
            state.copy(
                pendingCropBitmap = state.sourceBitmap,
                pendingCropRect = rect,
                isCropperOpen = true,
            )
        }
    }

    fun rotatePendingCrop90() {
        val current = _uiState.value.pendingCropBitmap ?: return
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                val matrix = Matrix().apply { postRotate(90f) }
                Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
            }
            _uiState.update { state ->
                state.copy(
                    pendingCropBitmap = rotated,
                    pendingCropRect = defaultCenteredAspectRect(
                        bmpW = rotated.width,
                        bmpH = rotated.height,
                        targetAspect = state.tileSpec.widthCm / state.tileSpec.heightCm,
                    ),
                )
            }
        }
    }

    fun applyCrop(rect: NormalizedCropRect) {
        val src = _uiState.value.pendingCropBitmap ?: return
        val cleanRect = rect.clamped()
        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                val x = (cleanRect.left * src.width).roundToInt().coerceIn(0, src.width - 2)
                val y = (cleanRect.top * src.height).roundToInt().coerceIn(0, src.height - 2)
                val w = ((cleanRect.right - cleanRect.left) * src.width).roundToInt().coerceIn(2, src.width - x)
                val h = ((cleanRect.bottom - cleanRect.top) * src.height).roundToInt().coerceIn(2, src.height - y)
                val slice = Bitmap.createBitmap(src, x, y, w, h)
                // Downscale excessively huge crops (>1024px) cleanly for smooth 60fps rendering while keeping full crisp detail
                val maxEdge = max(slice.width, slice.height)
                if (maxEdge > 1024) {
                    val scale = 1024f / maxEdge
                    Bitmap.createScaledBitmap(
                        slice,
                        (slice.width * scale).roundToInt().coerceAtLeast(2),
                        (slice.height * scale).roundToInt().coerceAtLeast(2),
                        true,
                    )
                } else {
                    slice
                }
            }
            _uiState.update { state ->
                state.copy(
                    activeSampleId = null,
                    activeTileName = "Uploaded Tile",
                    sourceBitmap = src,
                    croppedTileBitmap = cropped,
                    cropRect = cleanRect,
                    pendingCropBitmap = null,
                    isCropperOpen = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun dismissCropper() {
        _uiState.update { it.copy(isCropperOpen = false, pendingCropBitmap = null) }
    }

    fun setTileDimensions(widthCm: Float, heightCm: Float) {
        val safeW = widthCm.coerceIn(5f, 300f)
        val safeH = heightCm.coerceIn(5f, 300f)
        _uiState.update { state ->
            val updated = state.tileSpec.copy(widthCm = safeW, heightCm = safeH)
            state.copy(
                tileSpec = updated,
                stats = TilePlacementStats.calculate(updated, state.roomSpec),
            )
        }
    }

    fun setLayingPattern(pattern: LayingPattern) {
        _uiState.update { state ->
            state.copy(tileSpec = state.tileSpec.copy(pattern = pattern))
        }
    }

    fun setVaryQuarterTurn(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(tileSpec = state.tileSpec.copy(varyQuarterTurn = enabled))
        }
    }

    fun setGroutWidthMm(mm: Float) {
        _uiState.update { state ->
            state.copy(tileSpec = state.tileSpec.copy(groutWidthMm = mm.coerceIn(0f, 15f)))
        }
    }

    fun setGroutTone(tone: GroutTone) {
        _uiState.update { state ->
            state.copy(tileSpec = state.tileSpec.copy(groutTone = tone))
        }
    }

    fun setRoomSize(widthMeters: Float, depthMeters: Float) {
        val safeW = widthMeters.coerceIn(2.0f, 12.0f)
        val safeD = depthMeters.coerceIn(2.5f, 14.0f)
        _uiState.update { state ->
            val updatedRoom = state.roomSpec.copy(widthMeters = safeW, depthMeters = safeD)
            state.copy(
                roomSpec = updatedRoom,
                stats = TilePlacementStats.calculate(state.tileSpec, updatedRoom),
            )
        }
    }

    fun openFullscreen() {
        _uiState.update { it.copy(isFullscreenOpen = true) }
    }

    fun closeFullscreen() {
        _uiState.update { it.copy(isFullscreenOpen = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun exportAndShareRoom(context: Context, onReady: (Uri) -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    val bmp = RoomPerspectiveRenderer.renderToBitmap(
                        widthPx = 1600,
                        heightPx = 1200,
                        tileBitmap = snapshot.croppedTileBitmap,
                        tileSpec = snapshot.tileSpec,
                        roomSpec = snapshot.roomSpec,
                        includeSpecCaption = true,
                    )
                    val dir = File(context.cacheDir, "shared_renders").apply { mkdirs() }
                    val file = File(dir, "tilecraft_empty_room_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 94, out)
                    }
                    bmp.recycle()
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.getOrNull()
            }
            _uiState.update { it.copy(isExporting = false) }
            if (uri != null) {
                onReady(uri)
            }
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h)
                    if (longest > maxDim) {
                        val scale = maxDim.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1),
                        )
                    }
                }
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
                var sampleSize = 1
                while (max(boundsOpts.outWidth, boundsOpts.outHeight) / sampleSize > maxDim * 2) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
        }.getOrNull()
    }

    private fun defaultCenteredAspectRect(
        bmpW: Int,
        bmpH: Int,
        targetAspect: Float,
    ): NormalizedCropRect {
        val imgAspect = bmpW.toFloat() / bmpH.coerceAtLeast(1).toFloat()
        val safeTarget = targetAspect.coerceIn(0.25f, 4.0f)
        // Normalized box whose pixel aspect equals safeTarget and covers ~68% of shorter axis
        val normAspect = safeTarget / imgAspect
        var w = 0.68f
        var h = w / normAspect
        if (h > 0.72f) {
            h = 0.72f
            w = h * normAspect
        }
        val left = ((1f - w) * 0.5f).coerceIn(0.04f, 0.85f)
        val top = ((1f - h) * 0.5f).coerceIn(0.04f, 0.85f)
        return NormalizedCropRect(left, top, (left + w).coerceAtMost(0.96f), (top + h).coerceAtMost(0.96f))
    }
}
