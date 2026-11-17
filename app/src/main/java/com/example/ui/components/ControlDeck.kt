package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DefaultGroutWidthsMm
import com.example.domain.DefaultRoomSizePresets
import com.example.domain.DefaultTileSizePresets
import com.example.domain.GroutTone
import com.example.domain.LayingPattern
import com.example.domain.RoomSpec
import com.example.domain.SampleTileOption
import com.example.domain.TilePlacementStats
import com.example.domain.TileSpec
import com.example.domain.formatDim
import kotlin.math.abs

@Composable
fun TileControlDeck(
    activeTileName: String,
    activeSampleId: String?,
    croppedTileBitmap: Bitmap,
    samples: List<SampleTileOption>,
    tileSpec: TileSpec,
    roomSpec: RoomSpec,
    stats: TilePlacementStats,
    onPickPhotoClick: () -> Unit,
    onRecropClick: () -> Unit,
    onSelectSample: (SampleTileOption) -> Unit,
    onTileSizeChange: (Float, Float) -> Unit,
    onPatternChange: (LayingPattern) -> Unit,
    onVaryRotationChange: (Boolean) -> Unit,
    onGroutWidthChange: (Float) -> Unit,
    onGroutToneChange: (GroutTone) -> Unit,
    onRoomSizeChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Tile Photo Source & Quick Samples
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val thumbBmp = remember(croppedTileBitmap) { croppedTileBitmap.asImageBitmap() }
                Image(
                    bitmap = thumbBmp,
                    contentDescription = stringResource(R.string.cd_tile_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${tileSpec.widthCm.formatDim()} × ${tileSpec.heightCm.formatDim()} cm  •  " +
                            "${croppedTileBitmap.width}×${croppedTileBitmap.height} px",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onPickPhotoClick,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("upload_tile_button"),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.upload_tile_photo))
                        }
                        OutlinedButton(
                            onClick = onRecropClick,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("recrop_tile_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Crop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.recrop_tile))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.sample_tiles_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                samples.forEach { sample ->
                    val selected = activeSampleId == sample.id
                    val sampleThumb = remember(sample.bitmap) { sample.bitmap.asImageBitmap() }
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSample(sample) },
                        label = { Text(sample.name) },
                        leadingIcon = {
                            Image(
                                bitmap = sampleThumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                        },
                        modifier = Modifier.testTag("sample_chip_${sample.id}"),
                    )
                }
            }
        }

        // 2. Exact Tile Size (cm)
        SectionCard {
            Text(
                text = stringResource(R.string.tile_dimensions_label),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DefaultTileSizePresets.forEach { preset ->
                    val isSelected = abs(tileSpec.widthCm - preset.widthCm) < 0.1f &&
                        abs(tileSpec.heightCm - preset.heightCm) < 0.1f
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTileSizeChange(preset.widthCm, preset.heightCm) },
                        label = { Text("${preset.label} cm", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("size_preset_${preset.widthCm.toInt()}x${preset.heightCm.toInt()}"),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            TileMetricInputs(
                widthCm = tileSpec.widthCm,
                heightCm = tileSpec.heightCm,
                onApply = onTileSizeChange,
            )
        }

        // 3. Laying Pattern & Natural Variation
        SectionCard {
            Text(
                text = stringResource(R.string.laying_pattern_label),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                LayingPattern.entries.forEachIndexed { index, pattern ->
                    SegmentedButton(
                        selected = tileSpec.pattern == pattern,
                        onClick = { onPatternChange(pattern) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = LayingPattern.entries.size),
                        modifier = Modifier.testTag("pattern_button_${pattern.name.lowercase()}"),
                    ) {
                        Text(pattern.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.vary_rotation_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Switch(
                    checked = tileSpec.varyQuarterTurn,
                    onCheckedChange = onVaryRotationChange,
                    modifier = Modifier.testTag("vary_rotation_switch"),
                )
            }
        }

        // 4. Grout Joint & Empty Room Size
        SectionCard {
            Text(
                text = stringResource(R.string.grout_label),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DefaultGroutWidthsMm.forEach { mm ->
                    val selected = abs(tileSpec.groutWidthMm - mm) < 0.05f
                    FilterChip(
                        selected = selected,
                        onClick = { onGroutWidthChange(mm) },
                        label = { Text("${mm.formatDim()} mm", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("grout_mm_${mm.formatDim()}"),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GroutTone.entries.forEach { tone ->
                    val selected = tileSpec.groutTone == tone
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(tone.colorInt))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { onGroutToneChange(tone) }
                            .testTag("grout_tone_${tone.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            val checkTint = if (tone == GroutTone.ANTHRACITE) Color.White else Color(0xFF1A1C1E)
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = tone.displayName,
                                tint = checkTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.room_size_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${stats.roomAreaM2.formatDim()} m²  •  ${stats.wholeAndCutCount} tiles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DefaultRoomSizePresets.forEach { preset ->
                    val selected = abs(roomSpec.widthMeters - preset.widthMeters) < 0.05f &&
                        abs(roomSpec.depthMeters - preset.depthMeters) < 0.05f
                    FilterChip(
                        selected = selected,
                        onClick = { onRoomSizeChange(preset.widthMeters, preset.depthMeters) },
                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TileMetricInputs(
    widthCm: Float,
    heightCm: Float,
    onApply: (Float, Float) -> Unit,
) {
    var wText by remember { mutableStateOf(widthCm.formatDim()) }
    var hText by remember { mutableStateOf(heightCm.formatDim()) }

    LaunchedEffect(widthCm) {
        val parsed = wText.toFloatOrNull()
        if (parsed == null || abs(parsed - widthCm) > 0.05f) {
            wText = widthCm.formatDim()
        }
    }
    LaunchedEffect(heightCm) {
        val parsed = hText.toFloatOrNull()
        if (parsed == null || abs(parsed - heightCm) > 0.05f) {
            hText = heightCm.formatDim()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = wText,
            onValueChange = { next ->
                wText = next
                val w = next.toFloatOrNull()
                val h = hText.toFloatOrNull() ?: heightCm
                if (w != null && w in 5f..300f) {
                    onApply(w, h)
                }
            },
            label = { Text(stringResource(R.string.width_cm_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.labelSmall.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("tile_width_input"),
        )
        Text("×", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = hText,
            onValueChange = { next ->
                hText = next
                val w = wText.toFloatOrNull() ?: widthCm
                val h = next.toFloatOrNull()
                if (h != null && h in 5f..300f) {
                    onApply(w, h)
                }
            },
            label = { Text(stringResource(R.string.height_cm_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.labelSmall.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("tile_height_input"),
        )
    }
}

@Composable
private fun SectionCard(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}
