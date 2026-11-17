package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.FullscreenRoomDialog
import com.example.ui.components.RoomViewportCard
import com.example.ui.components.TileControlDeck
import com.example.ui.components.TileCropperDialog

@Composable
fun TileVisualizerScreen(
    viewModel: TileVisualizerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoPicked(context, uri)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val launchPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val shareRoomImage = {
        viewModel.exportAndShareRoom(context) { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.share_chooser_title),
                ),
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.studio_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        bottomBar = {
            BottomActionBar(
                isExporting = state.isExporting,
                onFullscreenClick = viewModel::openFullscreen,
                onShareClick = shareRoomImage,
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val isWideLayout = maxWidth >= 760.dp

            if (isWideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        RoomViewportCard(
                            tileBitmap = state.croppedTileBitmap,
                            tileSpec = state.tileSpec,
                            roomSpec = state.roomSpec,
                            stats = state.stats,
                            onOpenFullscreen = viewModel::openFullscreen,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                    ) {
                        TileControlDeck(
                            activeTileName = state.activeTileName,
                            activeSampleId = state.activeSampleId,
                            croppedTileBitmap = state.croppedTileBitmap,
                            samples = state.samples,
                            tileSpec = state.tileSpec,
                            roomSpec = state.roomSpec,
                            stats = state.stats,
                            onPickPhotoClick = launchPicker,
                            onRecropClick = viewModel::openRecrop,
                            onSelectSample = viewModel::selectSample,
                            onTileSizeChange = viewModel::setTileDimensions,
                            onPatternChange = viewModel::setLayingPattern,
                            onVaryRotationChange = viewModel::setVaryQuarterTurn,
                            onGroutWidthChange = viewModel::setGroutWidthMm,
                            onGroutToneChange = viewModel::setGroutTone,
                            onRoomSizeChange = viewModel::setRoomSize,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RoomViewportCard(
                        tileBitmap = state.croppedTileBitmap,
                        tileSpec = state.tileSpec,
                        roomSpec = state.roomSpec,
                        stats = state.stats,
                        onOpenFullscreen = viewModel::openFullscreen,
                        modifier = Modifier.widthIn(max = 600.dp),
                    )

                    TileControlDeck(
                        activeTileName = state.activeTileName,
                        activeSampleId = state.activeSampleId,
                        croppedTileBitmap = state.croppedTileBitmap,
                        samples = state.samples,
                        tileSpec = state.tileSpec,
                        roomSpec = state.roomSpec,
                        stats = state.stats,
                        onPickPhotoClick = launchPicker,
                        onRecropClick = viewModel::openRecrop,
                        onSelectSample = viewModel::selectSample,
                        onTileSizeChange = viewModel::setTileDimensions,
                        onPatternChange = viewModel::setLayingPattern,
                        onVaryRotationChange = viewModel::setVaryQuarterTurn,
                        onGroutWidthChange = viewModel::setGroutWidthMm,
                        onGroutToneChange = viewModel::setGroutTone,
                        onRoomSizeChange = viewModel::setRoomSize,
                        modifier = Modifier.widthIn(max = 600.dp),
                    )
                }
            }

            if (state.isLoadingImage) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 6.dp,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(20.dp))
                    }
                }
            }
        }
    }

    val cropBitmap = state.pendingCropBitmap
    if (state.isCropperOpen && cropBitmap != null) {
        TileCropperDialog(
            sourceBitmap = cropBitmap,
            initialRect = state.pendingCropRect,
            tileWidthCm = state.tileSpec.widthCm,
            tileHeightCm = state.tileSpec.heightCm,
            onRotate90 = viewModel::rotatePendingCrop90,
            onConfirmCrop = viewModel::applyCrop,
            onDismiss = viewModel::dismissCropper,
        )
    }

    if (state.isFullscreenOpen) {
        FullscreenRoomDialog(
            tileBitmap = state.croppedTileBitmap,
            tileSpec = state.tileSpec,
            roomSpec = state.roomSpec,
            stats = state.stats,
            onDismiss = viewModel::closeFullscreen,
        )
    }
}

@Composable
private fun BottomActionBar(
    isExporting: Boolean,
    onFullscreenClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onFullscreenClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fullscreen_action_button"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.fullscreen_button))
                }

                Button(
                    onClick = onShareClick,
                    enabled = !isExporting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_result_button"),
                    shape = MaterialTheme.shapes.small,
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.cd_share),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share_result_button))
                }
            }
        }
    }
}
