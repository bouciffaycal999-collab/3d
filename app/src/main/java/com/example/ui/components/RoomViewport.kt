package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.RoomPerspectiveRenderer
import com.example.domain.RoomSpec
import com.example.domain.TilePlacementStats
import com.example.domain.TileSpec
import com.example.domain.formatDim
import android.graphics.Bitmap

@Composable
fun RoomViewportCard(
    tileBitmap: Bitmap,
    tileSpec: TileSpec,
    roomSpec: RoomSpec,
    stats: TilePlacementStats,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cdPreview = stringResource(R.string.cd_room_preview)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.large)
            .semantics { contentDescription = cdPreview }
            .testTag("room_viewport_card"),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                RoomPerspectiveRenderer.drawRoom(
                    canvas = drawContext.canvas.nativeCanvas,
                    viewWidth = size.width,
                    viewHeight = size.height,
                    tileBitmap = tileBitmap,
                    tileSpec = tileSpec,
                    roomSpec = roomSpec,
                )
            }

            // Fullscreen expand button
            FilledTonalIconButton(
                onClick = onOpenFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("fullscreen_icon_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xCC1A1C1E),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Fullscreen,
                    contentDescription = stringResource(R.string.cd_fullscreen),
                )
            }

            // Compact live metric spec badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xCC1A1C1E),
            ) {
                Text(
                    text = "${tileSpec.widthCm.formatDim()}×${tileSpec.heightCm.formatDim()} cm  •  " +
                        "${tileSpec.pattern.label}  •  ${stats.wholeAndCutCount} tiles",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun FullscreenRoomDialog(
    tileBitmap: Bitmap,
    tileSpec: TileSpec,
    roomSpec: RoomSpec,
    stats: TilePlacementStats,
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

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101114))
                .onSizeChanged { containerSize = it },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                            val maxPanX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                            val maxPanY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                            scale = newScale
                            offset = if (newScale <= 1.02f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                    y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY),
                                )
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                ) {
                    RoomPerspectiveRenderer.drawRoom(
                        canvas = drawContext.canvas.nativeCanvas,
                        viewWidth = size.width,
                        viewHeight = size.height,
                        tileBitmap = tileBitmap,
                        tileSpec = tileSpec,
                        roomSpec = roomSpec,
                    )
                }
            }

            // Top overlay controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xCC1E2024),
                ) {
                    Text(
                        text = "${tileSpec.widthCm.formatDim()}×${tileSpec.heightCm.formatDim()} cm  •  " +
                            "Room ${roomSpec.widthMeters.formatDim()}×${roomSpec.depthMeters.formatDim()} m  •  " +
                            "${stats.wholeAndCutCount} tiles",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scale > 1.05f) {
                        FilledTonalIconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xCC1E2024),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = stringResource(R.string.reset_crop),
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_fullscreen_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xCC1E2024),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close_fullscreen),
                        )
                    }
                }
            }

            // Bottom zoom hint
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                shape = CircleShape,
                color = Color(0xB31E2024),
            ) {
                Text(
                    text = stringResource(R.string.zoom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
