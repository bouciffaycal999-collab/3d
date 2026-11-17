package com.example.domain

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor

enum class LayingPattern(val label: String) {
    STRAIGHT("Straight"),
    OFFSET_HALF("Offset 50%"),
    DIAGONAL("Diagonal 45°"),
}

enum class GroutTone(val displayName: String, val colorInt: Int) {
    WARM_WHITE("Warm White", AndroidColor.rgb(236, 233, 226)),
    LIGHT_GREY("Light Grey", AndroidColor.rgb(198, 198, 194)),
    MEDIUM_GREY("Cement Grey", AndroidColor.rgb(142, 143, 140)),
    SAND("Warm Sand", AndroidColor.rgb(206, 191, 168)),
    ANTHRACITE("Anthracite", AndroidColor.rgb(66, 68, 72)),
}

data class TileSizePreset(
    val widthCm: Float,
    val heightCm: Float,
) {
    val label: String
        get() = "${widthCm.formatDim()} × ${heightCm.formatDim()}"
}

data class RoomSizePreset(
    val widthMeters: Float,
    val depthMeters: Float,
    val label: String,
)

data class TileSpec(
    val widthCm: Float = 45f,
    val heightCm: Float = 45f,
    val groutWidthMm: Float = 2.0f,
    val groutTone: GroutTone = GroutTone.LIGHT_GREY,
    val pattern: LayingPattern = LayingPattern.STRAIGHT,
    val varyQuarterTurn: Boolean = false,
)

data class RoomSpec(
    val widthMeters: Float = 4.0f,
    val depthMeters: Float = 5.0f,
    val ceilingMeters: Float = 2.75f,
)

data class TilePlacementStats(
    val tilesAcross: Float,
    val tilesDeep: Float,
    val wholeAndCutCount: Int,
    val roomAreaM2: Float,
) {
    companion object {
        fun calculate(tile: TileSpec, room: RoomSpec): TilePlacementStats {
            val tileWm = (tile.widthCm / 100f).coerceAtLeast(0.05f)
            val tileHm = (tile.heightCm / 100f).coerceAtLeast(0.05f)
            val across = room.widthMeters / tileWm
            val deep = room.depthMeters / tileHm
            val roomArea = room.widthMeters * room.depthMeters
            val count = kotlin.math.ceil((roomArea / (tileWm * tileHm)).toDouble()).toInt()
            return TilePlacementStats(
                tilesAcross = across,
                tilesDeep = deep,
                wholeAndCutCount = count,
                roomAreaM2 = roomArea,
            )
        }
    }
}

data class SampleTileOption(
    val id: String,
    val name: String,
    val defaultWidthCm: Float,
    val defaultHeightCm: Float,
    val bitmap: Bitmap,
)

fun Float.formatDim(): String {
    return if (this % 1f == 0f) this.toInt().toString() else String.format(java.util.Locale.US, "%.1f", this)
}

val DefaultTileSizePresets = listOf(
    TileSizePreset(45f, 45f),
    TileSizePreset(60f, 60f),
    TileSizePreset(30f, 60f),
    TileSizePreset(60f, 120f),
    TileSizePreset(30f, 30f),
)

val DefaultRoomSizePresets = listOf(
    RoomSizePreset(3.5f, 4.5f, "3.5 × 4.5 m"),
    RoomSizePreset(4.0f, 5.0f, "4.0 × 5.0 m"),
    RoomSizePreset(5.0f, 6.0f, "5.0 × 6.0 m"),
)

val DefaultGroutWidthsMm = listOf(1.5f, 2.0f, 3.0f, 5.0f)
