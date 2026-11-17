package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Deterministic 3D perspective architectural renderer for an empty white-painted room.
 *
 * Preserves 100% of the source tile bitmap's pixels, color, and pattern via exact 4-point
 * projective homography ([Matrix.setPolyToPoly]) onto a true metric floor grid.
 */
object RoomPerspectiveRenderer {

    private const val CAMERA_Y_METERS = 1.50f
    private const val CAMERA_PITCH_DEG = 13.5f
    private const val Z_NEAR_METERS = 1.70f
    private const val BASEBOARD_HEIGHT_METERS = 0.085f
    private const val COS_45 = 0.70710677f

    private class Camera3D(
        private val viewWidth: Float,
        private val viewHeight: Float,
    ) {
        private val sinP = sin(Math.toRadians(CAMERA_PITCH_DEG.toDouble())).toFloat()
        private val cosP = cos(Math.toRadians(CAMERA_PITCH_DEG.toDouble())).toFloat()
        private val focalPx = min(viewWidth * 0.78f, viewHeight * 1.04f)
        private val cx = viewWidth * 0.5f
        private val cy = viewHeight * 0.5f

        fun project(x: Float, y: Float, z: Float): PointF? {
            val dy = y - CAMERA_Y_METERS
            val zc = -dy * sinP + z * cosP
            if (zc <= 0.12f) return null
            val yc = dy * cosP + z * sinP
            val sx = cx + (x / zc) * focalPx
            val sy = cy - (yc / zc) * focalPx
            return PointF(sx, sy)
        }
    }

    fun renderToBitmap(
        widthPx: Int,
        heightPx: Int,
        tileBitmap: Bitmap,
        tileSpec: TileSpec,
        roomSpec: RoomSpec,
        includeSpecCaption: Boolean = false,
    ): Bitmap {
        val out = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        drawRoom(
            canvas = canvas,
            viewWidth = widthPx.toFloat(),
            viewHeight = heightPx.toFloat(),
            tileBitmap = tileBitmap,
            tileSpec = tileSpec,
            roomSpec = roomSpec,
        )
        if (includeSpecCaption) {
            drawExportCaption(canvas, widthPx.toFloat(), heightPx.toFloat(), tileSpec, roomSpec)
        }
        return out
    }

    fun drawRoom(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        tileBitmap: Bitmap,
        tileSpec: TileSpec,
        roomSpec: RoomSpec,
    ) {
        if (viewWidth <= 1f || viewHeight <= 1f) return
        canvas.drawColor(Color.rgb(240, 238, 233))
        val cam = Camera3D(viewWidth, viewHeight)

        val halfW = roomSpec.widthMeters * 0.5f
        val zNear = Z_NEAR_METERS
        val zFar = Z_NEAR_METERS + roomSpec.depthMeters
        val ceilH = roomSpec.ceilingMeters

        // Project core room corners
        val flBackL = cam.project(-halfW, 0f, zFar) ?: return
        val flBackR = cam.project(halfW, 0f, zFar) ?: return
        val flFrontL = cam.project(-halfW, 0f, zNear) ?: return
        val flFrontR = cam.project(halfW, 0f, zNear) ?: return

        val clBackL = cam.project(-halfW, ceilH, zFar) ?: return
        val clBackR = cam.project(halfW, ceilH, zFar) ?: return
        val clFrontL = cam.project(-halfW, ceilH, zNear) ?: return
        val clFrontR = cam.project(halfW, ceilH, zNear) ?: return

        // Baseboard top edge points
        val bbBackL = cam.project(-halfW, BASEBOARD_HEIGHT_METERS, zFar) ?: return
        val bbBackR = cam.project(halfW, BASEBOARD_HEIGHT_METERS, zFar) ?: return
        val bbFrontL = cam.project(-halfW, BASEBOARD_HEIGHT_METERS, zNear) ?: return
        val bbFrontR = cam.project(halfW, BASEBOARD_HEIGHT_METERS, zNear) ?: return

        // 1. Ceiling (clean matte architectural white)
        val ceilingPath = quadPath(clFrontL, clFrontR, clBackR, clBackL)
        val ceilingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, max(clBackL.y, 1f),
                Color.rgb(250, 249, 246),
                Color.rgb(239, 236, 230),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(ceilingPath, ceilingPaint)

        // 2. Back White-Painted Wall
        val backWallPath = quadPath(clBackL, clBackR, flBackR, flBackL)
        val backWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, clBackL.y, 0f, flBackL.y,
                Color.rgb(245, 244, 240),
                Color.rgb(234, 231, 224),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(backWallPath, backWallPaint)

        // 3. Left White-Painted Wall (gentle sunlit side)
        val leftWallPath = quadPath(clFrontL, clBackL, flBackL, flFrontL)
        val leftWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, flBackL.x, 0f,
                Color.rgb(240, 238, 233),
                Color.rgb(227, 224, 217),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(leftWallPath, leftWallPaint)

        // 4. Right White-Painted Wall (subtle architectural shadow side)
        val rightWallPath = quadPath(clBackR, clFrontR, flFrontR, flBackR)
        val rightWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                flBackR.x, 0f, viewWidth, 0f,
                Color.rgb(221, 218, 210),
                Color.rgb(233, 231, 225),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(rightWallPath, rightWallPaint)

        // Subtle crisp architectural corner lines between white-painted walls
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 186, 182, 174)
            style = Paint.Style.STROKE
            strokeWidth = max(1.2f, viewWidth * 0.0016f)
        }
        canvas.drawLine(clBackL.x, clBackL.y, flBackL.x, flBackL.y, cornerPaint)
        canvas.drawLine(clBackR.x, clBackR.y, flBackR.x, flBackR.y, cornerPaint)
        canvas.drawLine(clBackL.x, clBackL.y, clBackR.x, clBackR.y, cornerPaint)
        canvas.drawLine(clFrontL.x, clFrontL.y, clBackL.x, clBackL.y, cornerPaint)
        canvas.drawLine(clFrontR.x, clFrontR.y, clBackR.x, clBackR.y, cornerPaint)

        // 5. Exact Perspective-Projected Floor Tile Grid (clipped to room floor)
        val floorPath = quadPath(flBackL, flBackR, flFrontR, flFrontL)
        canvas.save()
        canvas.clipPath(floorPath)

        // Solid grout mortar bed across floor
        val groutPaint = Paint().apply {
            color = tileSpec.groutTone.colorInt
            style = Paint.Style.FILL
        }
        canvas.drawPath(floorPath, groutPaint)

        drawExactTilesOnFloor(
            canvas = canvas,
            cam = cam,
            tileBitmap = tileBitmap,
            tileSpec = tileSpec,
            roomSpec = roomSpec,
            zNear = zNear,
            zFar = zFar,
        )

        // Subtle contact ambient occlusion at wall-floor perimeter ONLY (never alters tile pattern/center color)
        drawFloorPerimeterShadow(
            canvas = canvas,
            flBackL = flBackL,
            flBackR = flBackR,
            flFrontL = flFrontL,
            flFrontR = flFrontR,
            viewHeight = viewHeight,
        )

        canvas.restore()

        // 6. Crisp White-Painted Baseboards (Plinthes) sitting neatly on top of the cut perimeter tiles
        drawBaseboards(
            canvas = canvas,
            bbBackL = bbBackL,
            bbBackR = bbBackR,
            bbFrontL = bbFrontL,
            bbFrontR = bbFrontR,
            flBackL = flBackL,
            flBackR = flBackR,
            flFrontL = flFrontL,
            flFrontR = flFrontR,
            viewWidth = viewWidth,
        )
    }

    private fun drawExactTilesOnFloor(
        canvas: Canvas,
        cam: Camera3D,
        tileBitmap: Bitmap,
        tileSpec: TileSpec,
        roomSpec: RoomSpec,
        zNear: Float,
        zFar: Float,
    ) {
        val tileWm = (tileSpec.widthCm / 100f).coerceIn(0.05f, 3.0f)
        val tileHm = (tileSpec.heightCm / 100f).coerceIn(0.05f, 3.0f)
        val halfGrout = ((tileSpec.groutWidthMm / 1000f) * 0.5f)
            .coerceIn(0f, min(tileWm, tileHm) * 0.18f)

        val halfW = roomSpec.widthMeters * 0.5f
        val bmpW = tileBitmap.width.toFloat()
        val bmpH = tileBitmap.height.toFloat()

        // Source quad corners in tile bitmap space (TL, TR, BR, BL)
        val baseSrc = floatArrayOf(
            0f, 0f,
            bmpW, 0f,
            bmpW, bmpH,
            0f, bmpH,
        )
        val rotatedSrc = FloatArray(8)
        val dstPts = FloatArray(8)
        val matrix = Matrix()
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (tileSpec.pattern) {
            LayingPattern.STRAIGHT, LayingPattern.OFFSET_HALF -> {
                // Center columns symmetrically around X = 0 so left & right wall cuts match identically
                val minCol = -ceil((halfW / tileWm) + 1.0).toInt()
                val maxCol = ceil((halfW / tileWm) + 1.0).toInt()
                val maxRow = ceil((roomSpec.depthMeters / tileHm) + 1.0).toInt()

                for (row in 0..maxRow) {
                    val z0 = zNear + row * tileHm + halfGrout
                    val z1 = zNear + (row + 1) * tileHm - halfGrout
                    if (z0 > zFar || z1 <= zNear) continue

                    val rowOffset = if (tileSpec.pattern == LayingPattern.OFFSET_HALF && (row and 1) == 1) {
                        tileWm * 0.5f
                    } else {
                        0f
                    }

                    for (col in minCol..maxCol) {
                        val x0 = (col - 0.5f) * tileWm + rowOffset + halfGrout
                        val x1 = (col + 0.5f) * tileWm + rowOffset - halfGrout
                        if (x1 <= -halfW || x0 >= halfW) continue

                        // Far edge is screen top (z1), near edge is screen bottom (z0)
                        val pTL = cam.project(x0, 0f, z1) ?: continue
                        val pTR = cam.project(x1, 0f, z1) ?: continue
                        val pBR = cam.project(x1, 0f, z0) ?: continue
                        val pBL = cam.project(x0, 0f, z0) ?: continue

                        dstPts[0] = pTL.x; dstPts[1] = pTL.y
                        dstPts[2] = pTR.x; dstPts[3] = pTR.y
                        dstPts[4] = pBR.x; dstPts[5] = pBR.y
                        dstPts[6] = pBL.x; dstPts[7] = pBL.y

                        val turn = if (tileSpec.varyQuarterTurn) deterministicTurn(col, row) else 0
                        fillRotatedSrc(baseSrc, rotatedSrc, turn)
                        if (matrix.setPolyToPoly(rotatedSrc, 0, dstPts, 0, 4)) {
                            canvas.drawBitmap(tileBitmap, matrix, tilePaint)
                        }
                    }
                }
            }

            LayingPattern.DIAGONAL -> {
                val zCenter = (zNear + zFar) * 0.5f
                val maxSpan = roomSpec.widthMeters + roomSpec.depthMeters
                val minU = -ceil((maxSpan / tileWm) + 1.0).toInt()
                val maxU = ceil((maxSpan / tileWm) + 1.0).toInt()
                val minV = -ceil((maxSpan / tileHm) + 1.0).toInt()
                val maxV = ceil((maxSpan / tileHm) + 1.0).toInt()

                for (v in minV..maxV) {
                    val v0 = v * tileHm + halfGrout
                    val v1 = (v + 1) * tileHm - halfGrout
                    for (u in minU..maxU) {
                        val u0 = u * tileWm + halfGrout
                        val u1 = (u + 1) * tileWm - halfGrout

                        // Transform 4 corners of rotated tile into room (x, z)
                        val x0 = (u0 - v1) * COS_45
                        val z0 = zCenter + (u0 + v1) * COS_45
                        val x1 = (u1 - v1) * COS_45
                        val z1 = zCenter + (u1 + v1) * COS_45
                        val x2 = (u1 - v0) * COS_45
                        val z2 = zCenter + (u1 + v0) * COS_45
                        val x3 = (u0 - v0) * COS_45
                        val z3 = zCenter + (u0 + v0) * COS_45

                        val minTileX = min(min(x0, x1), min(x2, x3))
                        val maxTileX = max(max(x0, x1), max(x2, x3))
                        val minTileZ = min(min(z0, z1), min(z2, z3))
                        val maxTileZ = max(max(z0, z1), max(z2, z3))
                        if (maxTileX < -halfW || minTileX > halfW || maxTileZ < zNear || minTileZ > zFar) {
                            continue
                        }

                        val p0 = cam.project(x0, 0f, z0) ?: continue
                        val p1 = cam.project(x1, 0f, z1) ?: continue
                        val p2 = cam.project(x2, 0f, z2) ?: continue
                        val p3 = cam.project(x3, 0f, z3) ?: continue

                        dstPts[0] = p0.x; dstPts[1] = p0.y
                        dstPts[2] = p1.x; dstPts[3] = p1.y
                        dstPts[4] = p2.x; dstPts[5] = p2.y
                        dstPts[6] = p3.x; dstPts[7] = p3.y

                        val turn = if (tileSpec.varyQuarterTurn) deterministicTurn(u, v) else 0
                        fillRotatedSrc(baseSrc, rotatedSrc, turn)
                        if (matrix.setPolyToPoly(rotatedSrc, 0, dstPts, 0, 4)) {
                            canvas.drawBitmap(tileBitmap, matrix, tilePaint)
                        }
                    }
                }
            }
        }
    }

    private fun fillRotatedSrc(base: FloatArray, out: FloatArray, quarterTurns: Int) {
        val shift = (quarterTurns and 3) * 2
        for (i in 0 until 8) {
            out[i] = base[(i + shift) % 8]
        }
    }

    private fun deterministicTurn(col: Int, row: Int): Int {
        val h = (col * 73856093) xor (row * 19349663)
        return (h ushr 2) and 3
    }

    private fun drawFloorPerimeterShadow(
        canvas: Canvas,
        flBackL: PointF,
        flBackR: PointF,
        flFrontL: PointF,
        flFrontR: PointF,
        viewHeight: Float,
    ) {
        val shadowDepth = viewHeight * 0.032f
        val backShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, flBackL.y, 0f, flBackL.y + shadowDepth,
                Color.argb(38, 25, 22, 18),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(flBackL.x, flBackL.y, flBackR.x, flBackL.y + shadowDepth, backShadow)

        // Subtle left/right wall contact AO
        val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(26, 25, 22, 18)
            style = Paint.Style.STROKE
            strokeWidth = max(4f, shadowDepth * 0.45f)
        }
        canvas.drawLine(flFrontL.x, flFrontL.y, flBackL.x, flBackL.y, sidePaint)
        canvas.drawLine(flFrontR.x, flFrontR.y, flBackR.x, flBackR.y, sidePaint)
    }

    private fun drawBaseboards(
        canvas: Canvas,
        bbBackL: PointF,
        bbBackR: PointF,
        bbFrontL: PointF,
        bbFrontR: PointF,
        flBackL: PointF,
        flBackR: PointF,
        flFrontL: PointF,
        flFrontR: PointF,
        viewWidth: Float,
    ) {
        val backBbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(249, 248, 245)
            style = Paint.Style.FILL
        }
        val leftBbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(242, 240, 235)
            style = Paint.Style.FILL
        }
        val rightBbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(233, 230, 224)
            style = Paint.Style.FILL
        }

        canvas.drawPath(quadPath(bbBackL, bbBackR, flBackR, flBackL), backBbPaint)
        canvas.drawPath(quadPath(bbFrontL, bbBackL, flBackL, flFrontL), leftBbPaint)
        canvas.drawPath(quadPath(bbBackR, bbFrontR, flFrontR, flBackR), rightBbPaint)

        val topTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 194, 190, 182)
            style = Paint.Style.STROKE
            strokeWidth = max(1.2f, viewWidth * 0.0015f)
        }
        canvas.drawLine(bbFrontL.x, bbFrontL.y, bbBackL.x, bbBackL.y, topTrimPaint)
        canvas.drawLine(bbBackL.x, bbBackL.y, bbBackR.x, bbBackR.y, topTrimPaint)
        canvas.drawLine(bbBackR.x, bbBackR.y, bbFrontR.x, bbFrontR.y, topTrimPaint)

        val floorSeamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(110, 80, 76, 70)
            style = Paint.Style.STROKE
            strokeWidth = max(1.4f, viewWidth * 0.0018f)
        }
        canvas.drawLine(flFrontL.x, flFrontL.y, flBackL.x, flBackL.y, floorSeamPaint)
        canvas.drawLine(flBackL.x, flBackL.y, flBackR.x, flBackR.y, floorSeamPaint)
        canvas.drawLine(flBackR.x, flBackR.y, flFrontR.x, flFrontR.y, floorSeamPaint)
    }

    private fun drawExportCaption(
        canvas: Canvas,
        width: Float,
        height: Float,
        tileSpec: TileSpec,
        roomSpec: RoomSpec,
    ) {
        val barH = height * 0.065f
        val barPaint = Paint().apply {
            color = Color.argb(205, 24, 26, 30)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - barH, width, height, barPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = barH * 0.40f
            isFakeBoldText = true
        }
        val caption = "TileCraft  •  ${tileSpec.widthCm.formatDim()} × ${tileSpec.heightCm.formatDim()} cm  •  " +
            "${tileSpec.pattern.label}  •  ${tileSpec.groutWidthMm.formatDim()} mm grout  •  " +
            "Room ${roomSpec.widthMeters.formatDim()} × ${roomSpec.depthMeters.formatDim()} m"
        val baseline = height - barH * 0.34f
        canvas.drawText(caption, width * 0.03f, baseline, textPaint)
    }

    private fun quadPath(p0: PointF, p1: PointF, p2: PointF, p3: PointF): Path {
        return Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
    }
}
