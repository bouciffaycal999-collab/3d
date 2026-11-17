package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

object SampleTileGenerator {

    fun createBuiltInSamples(): List<SampleTileOption> = listOf(
        SampleTileOption(
            id = "calacatta_45",
            name = "Calacatta Gold",
            defaultWidthCm = 45f,
            defaultHeightCm = 45f,
            bitmap = createCalacattaMarbleBitmap(512),
        ),
        SampleTileOption(
            id = "geometric_45",
            name = "Medina Ceramic",
            defaultWidthCm = 45f,
            defaultHeightCm = 45f,
            bitmap = createGeometricCeramicBitmap(512),
        ),
        SampleTileOption(
            id = "travertine_60",
            name = "Warm Travertine",
            defaultWidthCm = 60f,
            defaultHeightCm = 60f,
            bitmap = createTravertineBitmap(512),
        ),
    )

    private fun createCalacattaMarbleBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(
                    Color.rgb(245, 243, 238),
                    Color.rgb(236, 233, 227),
                    Color.rgb(243, 241, 236),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Soft cloud mineral patches
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, 198, 192, 182)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size * 0.25f, size * 0.30f, size * 0.24f, cloudPaint)
        canvas.drawCircle(size * 0.72f, size * 0.68f, size * 0.28f, cloudPaint)

        // Primary diagonal grey-taupe marble vein
        val mainVein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(125, 134, 128, 120)
            style = Paint.Style.STROKE
            strokeWidth = size * 0.024f
            strokeCap = Paint.Cap.ROUND
        }
        val path1 = Path().apply {
            moveTo(0f, size * 0.78f)
            cubicTo(size * 0.26f, size * 0.64f, size * 0.42f, size * 0.44f, size * 0.68f, size * 0.28f)
            cubicTo(size * 0.80f, size * 0.20f, size * 0.90f, size * 0.12f, size.toFloat(), size * 0.06f)
        }
        canvas.drawPath(path1, mainVein)

        // Warm gold mineral accent along the main vein
        val goldVein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(115, 178, 140, 88)
            style = Paint.Style.STROKE
            strokeWidth = size * 0.009f
            strokeCap = Paint.Cap.ROUND
        }
        val pathGold = Path().apply {
            moveTo(size * 0.05f, size * 0.81f)
            cubicTo(size * 0.30f, size * 0.66f, size * 0.45f, size * 0.46f, size * 0.72f, size * 0.29f)
            lineTo(size * 0.96f, size * 0.10f)
        }
        canvas.drawPath(pathGold, goldVein)

        // Fine secondary branch veins
        val fineVein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(82, 152, 146, 138)
            style = Paint.Style.STROKE
            strokeWidth = size * 0.008f
        }
        val path2 = Path().apply {
            moveTo(size * 0.34f, size * 0.56f)
            quadTo(size * 0.52f, size * 0.62f, size * 0.78f, size * 0.86f)
        }
        canvas.drawPath(path2, fineVein)

        val path3 = Path().apply {
            moveTo(size * 0.18f, size * 0.14f)
            quadTo(size * 0.42f, size * 0.24f, size * 0.58f, size * 0.34f)
        }
        canvas.drawPath(path3, fineVein)

        return bmp
    }

    private fun createGeometricCeramicBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        // Warm off-white ceramic glaze base
        val basePaint = Paint().apply { color = Color.rgb(242, 237, 228) }
        canvas.drawRect(0f, 0f, s, s, basePaint)

        val slatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(66, 82, 92)
            style = Paint.Style.FILL
        }
        val terracottaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(168, 92, 62)
            style = Paint.Style.FILL
        }
        val sandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 189, 162)
            style = Paint.Style.FILL
        }

        val half = s * 0.5f

        // Corner quadrants forming a classic interlocking floral/star medallion when tiled
        val cornerArcRadius = s * 0.28f
        val corners = listOf(0f to 0f, s to 0f, s to s, 0f to s)
        for ((cx, cy) in corners) {
            canvas.drawCircle(cx, cy, cornerArcRadius, slatePaint)
            canvas.drawCircle(cx, cy, cornerArcRadius * 0.62f, sandPaint)
            canvas.drawCircle(cx, cy, cornerArcRadius * 0.30f, terracottaPaint)
        }

        // Center 8-point geometric star
        val starPath = Path()
        val outerR = s * 0.26f
        val innerR = s * 0.11f
        for (i in 0 until 16) {
            val angle = Math.toRadians(i * 22.5)
            val r = if (i % 2 == 0) outerR else innerR
            val px = half + (r * cos(angle)).toFloat()
            val py = half + (r * sin(angle)).toFloat()
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        canvas.drawPath(starPath, slatePaint)

        val innerDiamond = Path().apply {
            moveTo(half, half - s * 0.11f)
            lineTo(half + s * 0.11f, half)
            lineTo(half, half + s * 0.11f)
            lineTo(half - s * 0.11f, half)
            close()
        }
        canvas.drawPath(innerDiamond, terracottaPaint)

        // Crisp border hairline to show exact tile edges clearly
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 90, 82, 74)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, s - 1f, s - 1f, borderPaint)

        return bmp
    }

    private fun createTravertineBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, s,
                intArrayOf(
                    Color.rgb(228, 217, 199),
                    Color.rgb(218, 204, 184),
                    Color.rgb(225, 213, 194),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, s, s, bg)

        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val bandSpecs = listOf(
            Triple(0.10f, 0.055f, Color.argb(65, 186, 168, 143)),
            Triple(0.24f, 0.032f, Color.argb(75, 166, 146, 120)),
            Triple(0.41f, 0.068f, Color.argb(50, 240, 232, 218)),
            Triple(0.58f, 0.042f, Color.argb(72, 174, 154, 128)),
            Triple(0.76f, 0.055f, Color.argb(58, 192, 174, 150)),
            Triple(0.89f, 0.030f, Color.argb(70, 162, 142, 118)),
        )
        for ((yFrac, hFrac, col) in bandSpecs) {
            bandPaint.color = col
            canvas.drawRect(0f, s * yFrac, s, s * (yFrac + hFrac), bandPaint)
        }

        // Subtle porous stone flecks
        val fleckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 148, 130, 108)
        }
        for (i in 0 until 45) {
            val fx = ((i * 97 + 19) % 100) / 100f * s
            val fy = ((i * 53 + 7) % 100) / 100f * s
            val fw = s * (0.015f + (i % 4) * 0.008f)
            canvas.drawRect(fx, fy, fx + fw, fy + 2.5f, fleckPaint)
        }

        return bmp
    }
}
