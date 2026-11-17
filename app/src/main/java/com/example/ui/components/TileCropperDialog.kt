package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.formatDim
import com.example.ui.NormalizedCropRect
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class DragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, MOVE
}

@Composable
fun TileCropperDialog(
    sourceBitmap: Bitmap,
    initialRect: NormalizedCropRect,
    tileWidthCm: Float,
    tileHeightCm: Float,
    onRotate90: () -> Unit,
    onConfirmCrop: (NormalizedCropRect) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(onBack = onDismiss)

        var cropRect by remember(sourceBitmap, initialRect) { mutableStateOf(initialRect.clamped()) }
        var lockRatio by remember { mutableStateOf(true) }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }
        var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

        val targetPixelAspect = (tileWidthCm / tileHeightCm).coerceIn(0.25f, 4.0f)

        // Snap rectangle to tile aspect ratio whenever Lock Ratio is enabled
        LaunchedEffect(lockRatio, sourceBitmap) {
            if (lockRatio) {
                cropRect = enforceAspect(cropRect, sourceBitmap.width, sourceBitmap.height, targetPixelAspect)
            }
        }

        // Live preview bitmap slice
        val previewBitmap = remember(sourceBitmap, cropRect) {
            val x = (cropRect.left * sourceBitmap.width).roundToInt().coerceIn(0, sourceBitmap.width - 2)
            val y = (cropRect.top * sourceBitmap.height).roundToInt().coerceIn(0, sourceBitmap.height - 2)
            val w = ((cropRect.right - cropRect.left) * sourceBitmap.width).roundToInt()
                .coerceIn(2, sourceBitmap.width - x)
            val h = ((cropRect.bottom - cropRect.top) * sourceBitmap.height).roundToInt()
                .coerceIn(2, sourceBitmap.height - y)
            runCatching { Bitmap.createBitmap(sourceBitmap, x, y, w, h) }.getOrNull()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF141518))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cropper_cancel_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = Color.White,
                    )
                }

                Text(
                    text = stringResource(R.string.cropper_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRotate90,
                        modifier = Modifier.testTag("cropper_rotate_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RotateRight,
                            contentDescription = stringResource(R.string.cd_rotate_image),
                            tint = Color.White,
                        )
                    }
                    TextButton(
                        onClick = {
                            val reset = NormalizedCropRect(0.1f, 0.1f, 0.9f, 0.9f)
                            cropRect = if (lockRatio) {
                                enforceAspect(reset, sourceBitmap.width, sourceBitmap.height, targetPixelAspect)
                            } else {
                                reset
                            }
                        },
                        modifier = Modifier.testTag("cropper_reset_button"),
                    ) {
                        Text(stringResource(R.string.reset_crop), color = Color(0xFFE59476))
                    }
                }
            }

            Text(
                text = stringResource(R.string.cropper_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8BCC4),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )

            // Interactive Photo + Crop Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onSizeChanged { canvasSize = it },
                contentAlignment = Alignment.Center,
            ) {
                val imageBounds = remember(canvasSize, sourceBitmap) {
                    computeFitRect(
                        containerW = canvasSize.width.toFloat(),
                        containerH = canvasSize.height.toFloat(),
                        bmpW = sourceBitmap.width.toFloat(),
                        bmpH = sourceBitmap.height.toFloat(),
                    )
                }

                val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageBounds, lockRatio, sourceBitmap) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    if (imageBounds.width <= 0f || imageBounds.height <= 0f) return@detectDragGestures
                                    val cLeft = imageBounds.left + cropRect.left * imageBounds.width
                                    val cTop = imageBounds.top + cropRect.top * imageBounds.height
                                    val cRight = imageBounds.left + cropRect.right * imageBounds.width
                                    val cBottom = imageBounds.top + cropRect.bottom * imageBounds.height
                                    val hitRadius = 64f
                                    activeHandle = when {
                                        hypot(start.x - cLeft, start.y - cTop) <= hitRadius -> DragHandle.TOP_LEFT
                                        hypot(start.x - cRight, start.y - cTop) <= hitRadius -> DragHandle.TOP_RIGHT
                                        hypot(start.x - cRight, start.y - cBottom) <= hitRadius -> DragHandle.BOTTOM_RIGHT
                                        hypot(start.x - cLeft, start.y - cBottom) <= hitRadius -> DragHandle.BOTTOM_LEFT
                                        start.x in cLeft..cRight && start.y in cTop..cBottom -> DragHandle.MOVE
                                        else -> DragHandle.NONE
                                    }
                                },
                                onDragEnd = { activeHandle = DragHandle.NONE },
                                onDragCancel = { activeHandle = DragHandle.NONE },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (imageBounds.width <= 1f || imageBounds.height <= 1f) return@detectDragGestures
                                    val dx = dragAmount.x / imageBounds.width
                                    val dy = dragAmount.y / imageBounds.height
                                    cropRect = updateCropRect(
                                        current = cropRect,
                                        handle = activeHandle,
                                        dx = dx,
                                        dy = dy,
                                        lockRatio = lockRatio,
                                        bmpW = sourceBitmap.width,
                                        bmpH = sourceBitmap.height,
                                        targetAspect = targetPixelAspect,
                                    )
                                },
                            )
                        },
                ) {
                    if (imageBounds.width <= 0f || imageBounds.height <= 0f) return@Canvas

                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(imageBounds.left.roundToInt(), imageBounds.top.roundToInt()),
                        dstSize = IntSize(imageBounds.width.roundToInt(), imageBounds.height.roundToInt()),
                    )

                    val cLeft = imageBounds.left + cropRect.left * imageBounds.width
                    val cTop = imageBounds.top + cropRect.top * imageBounds.height
                    val cRight = imageBounds.left + cropRect.right * imageBounds.width
                    val cBottom = imageBounds.top + cropRect.bottom * imageBounds.height
                    val cW = cRight - cLeft
                    val cH = cBottom - cTop

                    // Dimmed mask outside crop box
                    val fullPath = Path().apply { addRect(imageBounds) }
                    val holePath = Path().apply { addRect(Rect(cLeft, cTop, cRight, cBottom)) }
                    val maskPath = Path.combine(PathOperation.Difference, fullPath, holePath)
                    drawPath(maskPath, color = Color(0xA6000000))

                    // Crisp white border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cLeft, cTop),
                        size = Size(cW, cH),
                        style = Stroke(width = 2.5f),
                    )

                    // Rule-of-thirds + symmetry guide lines
                    val gridColor = Color.White.copy(alpha = 0.35f)
                    for (i in 1..2) {
                        val gx = cLeft + cW * (i / 3f)
                        val gy = cTop + cH * (i / 3f)
                        drawLine(gridColor, Offset(gx, cTop), Offset(gx, cBottom), strokeWidth = 1f)
                        drawLine(gridColor, Offset(cLeft, gy), Offset(cRight, gy), strokeWidth = 1f)
                    }

                    // Bold corner L-handles
                    val arm = min(28f, min(cW, cH) * 0.3f)
                    val handleColor = Color(0xFFE59476)
                    val hw = 6f
                    // TL
                    drawLine(handleColor, Offset(cLeft, cTop), Offset(cLeft + arm, cTop), hw)
                    drawLine(handleColor, Offset(cLeft, cTop), Offset(cLeft, cTop + arm), hw)
                    // TR
                    drawLine(handleColor, Offset(cRight, cTop), Offset(cRight - arm, cTop), hw)
                    drawLine(handleColor, Offset(cRight, cTop), Offset(cRight, cTop + arm), hw)
                    // BR
                    drawLine(handleColor, Offset(cRight, cBottom), Offset(cRight - arm, cBottom), hw)
                    drawLine(handleColor, Offset(cRight, cBottom), Offset(cRight, cBottom - arm), hw)
                    // BL
                    drawLine(handleColor, Offset(cLeft, cBottom), Offset(cLeft + arm, cBottom), hw)
                    drawLine(handleColor, Offset(cLeft, cBottom), Offset(cLeft, cBottom - arm), hw)
                }
            }

            // Bottom Controls & Confirm Bar
            Surface(
                color = Color(0xFF1E2025),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = lockRatio,
                            onClick = { lockRatio = !lockRatio },
                            label = {
                                Text(
                                    text = if (lockRatio) {
                                        stringResource(
                                            R.string.lock_aspect_ratio,
                                            tileWidthCm.formatDim(),
                                            tileHeightCm.formatDim(),
                                        )
                                    } else {
                                        stringResource(R.string.free_crop)
                                    },
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (lockRatio) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6B2D15),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color(0xFFE59476),
                                labelColor = Color(0xFFD2D5DC),
                            ),
                        )

                        if (previewBitmap != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${previewBitmap.width}×${previewBitmap.height}px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7AAB0),
                                )
                                Spacer(Modifier.width(8.dp))
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_tile_thumbnail),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.5.dp, Color(0xFFE59476), RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onConfirmCrop(cropRect) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_crop_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.apply_tile),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

private fun computeFitRect(
    containerW: Float,
    containerH: Float,
    bmpW: Float,
    bmpH: Float,
): Rect {
    if (containerW <= 0f || containerH <= 0f || bmpW <= 0f || bmpH <= 0f) return Rect.Zero
    val scale = min(containerW / bmpW, containerH / bmpH)
    val drawW = bmpW * scale
    val drawH = bmpH * scale
    val left = (containerW - drawW) * 0.5f
    val top = (containerH - drawH) * 0.5f
    return Rect(left, top, left + drawW, top + drawH)
}

private fun enforceAspect(
    rect: NormalizedCropRect,
    bmpW: Int,
    bmpH: Int,
    targetAspect: Float,
): NormalizedCropRect {
    val imgAspect = bmpW.toFloat() / bmpH.coerceAtLeast(1).toFloat()
    val normAspect = (targetAspect / imgAspect).coerceIn(0.15f, 6f)
    val cx = (rect.left + rect.right) * 0.5f
    val cy = (rect.top + rect.bottom) * 0.5f
    var w = (rect.right - rect.left).coerceAtLeast(0.1f)
    var h = w / normAspect
    if (h > 0.92f) {
        h = 0.92f
        w = h * normAspect
    }
    if (w > 0.92f) {
        w = 0.92f
        h = w / normAspect
    }
    val l = (cx - w * 0.5f).coerceIn(0f, 1f - w)
    val t = (cy - h * 0.5f).coerceIn(0f, 1f - h)
    return NormalizedCropRect(l, t, l + w, t + h).clamped()
}

private fun updateCropRect(
    current: NormalizedCropRect,
    handle: DragHandle,
    dx: Float,
    dy: Float,
    lockRatio: Boolean,
    bmpW: Int,
    bmpH: Int,
    targetAspect: Float,
): NormalizedCropRect {
    val minSize = 0.08f
    if (handle == DragHandle.MOVE) {
        val w = current.right - current.left
        val h = current.bottom - current.top
        val newL = (current.left + dx).coerceIn(0f, 1f - w)
        val newT = (current.top + dy).coerceIn(0f, 1f - h)
        return NormalizedCropRect(newL, newT, newL + w, newT + h)
    }
    if (handle == DragHandle.NONE) return current

    var l = current.left
    var t = current.top
    var r = current.right
    var b = current.bottom

    when (handle) {
        DragHandle.TOP_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            t = (t + dy).coerceIn(0f, b - minSize)
        }
        DragHandle.TOP_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            t = (t + dy).coerceIn(0f, b - minSize)
        }
        DragHandle.BOTTOM_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            b = (b + dy).coerceIn(t + minSize, 1f)
        }
        DragHandle.BOTTOM_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            b = (b + dy).coerceIn(t + minSize, 1f)
        }
        else -> Unit
    }

    if (!lockRatio) {
        return NormalizedCropRect(l, t, r, b).clamped()
    }

    val imgAspect = bmpW.toFloat() / bmpH.coerceAtLeast(1).toFloat()
    val normAspect = (targetAspect / imgAspect).coerceIn(0.15f, 6f)
    var w = (r - l).coerceAtLeast(minSize)
    var h = w / normAspect

    when (handle) {
        DragHandle.TOP_LEFT -> {
            val maxH = current.bottom
            if (h > maxH) {
                h = maxH
                w = h * normAspect
            }
            l = (current.right - w).coerceAtLeast(0f)
            t = (current.bottom - h).coerceAtLeast(0f)
            w = current.right - l
            h = w / normAspect
            t = (current.bottom - h).coerceAtLeast(0f)
            return NormalizedCropRect(l, t, current.right, current.bottom).clamped()
        }
        DragHandle.TOP_RIGHT -> {
            val maxH = current.bottom
            if (h > maxH) {
                h = maxH
                w = h * normAspect
            }
            r = (current.left + w).coerceAtMost(1f)
            w = r - current.left
            h = w / normAspect
            t = (current.bottom - h).coerceAtLeast(0f)
            return NormalizedCropRect(current.left, t, r, current.bottom).clamped()
        }
        DragHandle.BOTTOM_RIGHT -> {
            val maxH = 1f - current.top
            if (h > maxH) {
                h = maxH
                w = h * normAspect
            }
            r = (current.left + w).coerceAtMost(1f)
            w = r - current.left
            h = w / normAspect
            b = (current.top + h).coerceAtMost(1f)
            return NormalizedCropRect(current.left, current.top, r, b).clamped()
        }
        DragHandle.BOTTOM_LEFT -> {
            val maxH = 1f - current.top
            if (h > maxH) {
                h = maxH
                w = h * normAspect
            }
            l = (current.right - w).coerceAtLeast(0f)
            w = current.right - l
            h = w / normAspect
            b = (current.top + h).coerceAtMost(1f)
            return NormalizedCropRect(l, current.top, current.right, b).clamped()
        }
        else -> return NormalizedCropRect(l, t, r, b).clamped()
    }
}
