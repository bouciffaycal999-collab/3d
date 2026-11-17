package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.domain.LayingPattern
import com.example.domain.RoomPerspectiveRenderer
import com.example.domain.RoomSpec
import com.example.domain.SampleTileGenerator
import com.example.domain.TilePlacementStats
import com.example.domain.TileSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TileCraft", appName)
    }

    @Test
    fun `metric tile count calculation for 45x45cm in 4x5m room`() {
        val tile = TileSpec(widthCm = 45f, heightCm = 45f)
        val room = RoomSpec(widthMeters = 4.0f, depthMeters = 5.0f)
        val stats = TilePlacementStats.calculate(tile, room)
        assertEquals(20.0f, stats.roomAreaM2, 0.01f)
        assertEquals(99, stats.wholeAndCutCount)
    }

    @Test
    fun `built-in samples generate valid non-empty bitmaps`() {
        val samples = SampleTileGenerator.createBuiltInSamples()
        assertEquals(3, samples.size)
        samples.forEach { sample ->
            assertEquals(512, sample.bitmap.width)
            assertEquals(512, sample.bitmap.height)
            assertTrue(Color.alpha(sample.bitmap.getPixel(256, 256)) == 255)
        }
    }

    @Test
    fun `perspective renderer renders exact tile pixels onto empty white room`() {
        val tileColor = Color.rgb(180, 120, 90)
        val tileBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(tileColor)
        }
        val room = RoomSpec(widthMeters = 4.0f, depthMeters = 5.0f)

        LayingPattern.entries.forEach { pattern ->
            val rendered = RoomPerspectiveRenderer.renderToBitmap(
                widthPx = 400,
                heightPx = 300,
                tileBitmap = tileBmp,
                tileSpec = TileSpec(widthCm = 45f, heightCm = 45f, pattern = pattern),
                roomSpec = room,
                includeSpecCaption = true,
            )
            assertEquals(400, rendered.width)
            assertEquals(300, rendered.height)

            // Back white wall pixel should be bright neutral white/off-white
            val backWallPixel = rendered.getPixel(200, 90)
            assertTrue(Color.red(backWallPixel) > 220)
            assertTrue(Color.blue(backWallPixel) > 210)

            // Floor pixel should reflect the exact terracotta tile hue (R > B), not plain white wall
            val floorPixel = rendered.getPixel(200, 220)
            assertEquals(255, Color.alpha(floorPixel))
            assertNotEquals(backWallPixel, floorPixel)
            assertTrue(Color.red(floorPixel) > Color.blue(floorPixel) + 40)

            rendered.recycle()
        }
        tileBmp.recycle()
    }
}
