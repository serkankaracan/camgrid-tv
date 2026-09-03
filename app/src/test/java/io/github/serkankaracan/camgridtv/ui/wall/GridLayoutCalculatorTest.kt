package io.github.serkankaracan.camgridtv.ui.wall

import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GridLayoutCalculatorTest {
    private val calculator = GridLayoutCalculator()

    @Test
    fun `camera counts zero through twelve use the expected grids`() {
        val expected =
            mapOf(
                0 to (0 to 0),
                1 to (1 to 1),
                2 to (1 to 2),
                3 to (2 to 2),
                4 to (2 to 2),
                5 to (2 to 3),
                6 to (2 to 3),
                7 to (3 to 3),
                8 to (3 to 3),
                9 to (3 to 3),
                10 to (3 to 4),
                11 to (3 to 4),
                12 to (3 to 4),
            )

        expected.forEach { (cameraCount, dimensions) ->
            val layout = calculator.calculate(cameraCount)

            assertEquals("rows for $cameraCount cameras", dimensions.first, layout.rows)
            assertEquals("columns for $cameraCount cameras", dimensions.second, layout.columns)
            assertEquals(cameraCount, layout.placements.size)
        }
    }

    @Test
    fun `incomplete final rows are centered`() {
        listOf(3, 5, 7, 8, 10, 11).forEach { cameraCount ->
            val layout = calculator.calculate(cameraCount)
            val finalRow = layout.placements.filter { it.row == layout.rows - 1 }

            val leftMargin = finalRow.first().leftFraction
            val rightMargin = 1.0 - finalRow.last().rightFraction
            assertEquals("outer margins for $cameraCount cameras", leftMargin, rightMargin, EPSILON)
            assertEquals(
                (layout.columns - finalRow.size) / 2.0,
                finalRow.first().leadingOffsetColumns,
                EPSILON,
            )
        }
    }

    @Test
    fun `three camera layout places its final tile on the horizontal center`() {
        val layout = calculator.calculate(3)
        val finalTile = layout.placements.single { it.row == 1 }

        assertEquals(0, finalTile.column)
        assertEquals(0.5, finalTile.leadingOffsetColumns, EPSILON)
        assertEquals(0.25, finalTile.leftFraction, EPSILON)
        assertEquals(0.75, finalTile.rightFraction, EPSILON)
    }

    @Test
    fun `full final rows have no centering offset`() {
        listOf(1, 2, 4, 6, 9, 12).forEach { cameraCount ->
            val layout = calculator.calculate(cameraCount)
            val finalRow = layout.placements.filter { it.row == layout.rows - 1 }

            assertEquals(layout.columns, finalRow.size)
            finalRow.forEach { placement ->
                assertEquals(0.0, placement.leadingOffsetColumns, EPSILON)
            }
        }
    }

    @Test
    fun `placements are row major unique and stay inside normalized bounds`() {
        for (cameraCount in 1..64) {
            val layout = calculator.calculate(cameraCount)

            assertTrue(layout.capacity >= cameraCount)
            assertEquals(cameraCount, layout.placements.map { it.itemIndex }.distinct().size)
            layout.placements.forEachIndexed { itemIndex, placement ->
                assertEquals(itemIndex, placement.itemIndex)
                assertTrue(placement.row in 0 until layout.rows)
                assertTrue(placement.column in 0 until placement.rowItemCount)
                assertTrue(placement.leftFraction >= -EPSILON)
                assertTrue(placement.topFraction >= -EPSILON)
                assertTrue(placement.rightFraction <= 1.0 + EPSILON)
                assertTrue(placement.bottomFraction <= 1.0 + EPSILON)
            }
        }
    }

    @Test
    fun `chosen grid maximizes fitted sixteen by nine video area`() {
        for (cameraCount in 1..40) {
            val layout = calculator.calculate(cameraCount)
            val maximumArea = maximumCandidateArea(cameraCount)

            assertEquals(
                "video area for $cameraCount cameras",
                maximumArea,
                layout.videoTileArea,
                EPSILON,
            )
            assertEquals(
                GridLayoutCalculator.DEFAULT_VIDEO_ASPECT_RATIO,
                layout.videoTileWidth / layout.videoTileHeight,
                EPSILON,
            )
        }
    }

    @Test
    fun `area ties prefer fewer empty cells`() {
        val fiveCameraLayout = calculator.calculate(5)
        val tenCameraLayout = calculator.calculate(10)

        assertEquals(1, fiveCameraLayout.emptyCellCount)
        assertEquals(3, fiveCameraLayout.columns)
        assertEquals(2, fiveCameraLayout.rows)
        assertEquals(2, tenCameraLayout.emptyCellCount)
        assertEquals(4, tenCameraLayout.columns)
        assertEquals(3, tenCameraLayout.rows)
    }

    @Test
    fun `landscape orientation wins otherwise equivalent transpose tie`() {
        val twoCameraLayout = calculator.calculate(2)
        val sixCameraLayout = calculator.calculate(6)

        assertEquals(1, twoCameraLayout.rows)
        assertEquals(2, twoCameraLayout.columns)
        assertEquals(2, sixCameraLayout.rows)
        assertEquals(3, sixCameraLayout.columns)
    }

    @Test
    fun `calculator supports counts greater than nine`() {
        val layouts = listOf(10, 11, 12, 13, 17, 25, 100).associateWith(calculator::calculate)

        assertEquals(3 to 4, layouts.getValue(10).rows to layouts.getValue(10).columns)
        assertEquals(4 to 4, layouts.getValue(13).rows to layouts.getValue(13).columns)
        assertEquals(4 to 5, layouts.getValue(17).rows to layouts.getValue(17).columns)
        assertEquals(5 to 5, layouts.getValue(25).rows to layouts.getValue(25).columns)
        layouts.forEach { (cameraCount, layout) ->
            assertEquals(cameraCount, layout.placements.size)
            assertTrue(layout.capacity >= cameraCount)
        }
    }

    @Test
    fun `portrait viewport selects the larger fitted video area`() {
        val portraitCalculator = GridLayoutCalculator(availableWidth = 9.0, availableHeight = 16.0)

        val layout = portraitCalculator.calculate(2)

        assertEquals(2, layout.rows)
        assertEquals(1, layout.columns)
    }

    @Test
    fun `zero cameras returns an empty layout`() {
        val layout = GridLayoutCalculator.calculate(0)

        assertEquals(GridLayout.EMPTY, layout)
        assertEquals(0, layout.capacity)
        assertEquals(0, layout.lastRowItemCount)
    }

    @Test
    fun `negative camera count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { calculator.calculate(-1) }
    }

    @Test
    fun `non-positive or non-finite geometry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GridLayoutCalculator(availableWidth = 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GridLayoutCalculator(availableHeight = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GridLayoutCalculator(videoAspectRatio = Double.POSITIVE_INFINITY)
        }
    }

    private fun maximumCandidateArea(cameraCount: Int): Double {
        var maximum = 0.0
        for (rows in 1..cameraCount) {
            for (columns in 1..cameraCount) {
                if (rows * columns < cameraCount) continue

                val cellWidth = GridLayoutCalculator.DEFAULT_AVAILABLE_WIDTH / columns
                val cellHeight = GridLayoutCalculator.DEFAULT_AVAILABLE_HEIGHT / rows
                val videoWidth =
                    min(
                        cellWidth,
                        cellHeight * GridLayoutCalculator.DEFAULT_VIDEO_ASPECT_RATIO,
                    )
                val videoHeight = videoWidth / GridLayoutCalculator.DEFAULT_VIDEO_ASPECT_RATIO
                maximum = maxOf(maximum, videoWidth * videoHeight)
            }
        }
        return maximum
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
