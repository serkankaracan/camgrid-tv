package io.github.serkankaracan.camgridtv.ui.wall

import kotlin.math.abs
import kotlin.math.min

/**
 * Calculates a camera wall without depending on Android or Compose APIs.
 *
 * Candidates are scored using the area of a 16:9 video fitted inside each grid cell. This keeps the
 * decision deterministic and makes the result straightforward to unit test. The normalized
 * placement values can be multiplied by Compose layout constraints to position each tile.
 */
class GridLayoutCalculator(
    private val availableWidth: Double = DEFAULT_AVAILABLE_WIDTH,
    private val availableHeight: Double = DEFAULT_AVAILABLE_HEIGHT,
    private val videoAspectRatio: Double = DEFAULT_VIDEO_ASPECT_RATIO,
) {
    init {
        require(availableWidth.isFinite() && availableWidth > 0.0) {
            "availableWidth must be finite and greater than zero"
        }
        require(availableHeight.isFinite() && availableHeight > 0.0) {
            "availableHeight must be finite and greater than zero"
        }
        require(videoAspectRatio.isFinite() && videoAspectRatio > 0.0) {
            "videoAspectRatio must be finite and greater than zero"
        }
    }

    fun calculate(cameraCount: Int): GridLayout {
        require(cameraCount >= 0) { "cameraCount must not be negative" }
        if (cameraCount == 0) return GridLayout.EMPTY

        var best: Candidate? = null
        for (rows in 1..cameraCount) {
            val columns = ceilingDivide(cameraCount, rows)

            // A candidate must not contain a completely unused trailing row. Such a grid is
            // always dominated by the equivalent grid with that row removed.
            if (ceilingDivide(cameraCount, columns) != rows) continue

            val candidate = candidate(rows = rows, columns = columns, cameraCount = cameraCount)
            if (best == null || candidate.isBetterThan(best)) {
                best = candidate
            }
        }

        return requireNotNull(best).toLayout(cameraCount)
    }

    private fun candidate(rows: Int, columns: Int, cameraCount: Int): Candidate {
        val cellWidth = availableWidth / columns
        val cellHeight = availableHeight / rows
        val videoWidth = min(cellWidth, cellHeight * videoAspectRatio)
        val videoHeight = videoWidth / videoAspectRatio
        val emptyCells = (rows.toLong() * columns - cameraCount).toInt()

        return Candidate(
            rows = rows,
            columns = columns,
            emptyCells = emptyCells,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
        )
    }

    private fun Candidate.isBetterThan(other: Candidate): Boolean {
        val areaComparison = compareWithTolerance(videoArea, other.videoArea)
        if (areaComparison != 0) return areaComparison > 0

        if (emptyCells != other.emptyCells) return emptyCells < other.emptyCells

        val orientationPenalty = if (columns >= rows) 0 else 1
        val otherOrientationPenalty = if (other.columns >= other.rows) 0 else 1
        if (orientationPenalty != otherOrientationPenalty) {
            return orientationPenalty < otherOrientationPenalty
        }

        // A landscape TV gets the row-major variant when transposed candidates are otherwise
        // equivalent (for example 2x1 versus 1x2).
        if (rows != other.rows) return rows < other.rows
        return columns < other.columns
    }

    private fun Candidate.toLayout(cameraCount: Int): GridLayout {
        val widthFraction = 1.0 / columns
        val heightFraction = 1.0 / rows
        val placements = ArrayList<GridCellPlacement>(cameraCount)

        for (itemIndex in 0 until cameraCount) {
            val row = itemIndex / columns
            val firstItemInRow = row * columns
            val rowItemCount = min(columns, cameraCount - firstItemInRow)
            val column = itemIndex - firstItemInRow
            val leadingOffsetColumns = (columns - rowItemCount) / 2.0

            placements +=
                GridCellPlacement(
                    itemIndex = itemIndex,
                    row = row,
                    column = column,
                    rowItemCount = rowItemCount,
                    leadingOffsetColumns = leadingOffsetColumns,
                    leftFraction = (leadingOffsetColumns + column) / columns,
                    topFraction = row.toDouble() / rows,
                    widthFraction = widthFraction,
                    heightFraction = heightFraction,
                )
        }

        return GridLayout(
            cameraCount = cameraCount,
            rows = rows,
            columns = columns,
            placements = placements,
            videoTileWidth = videoWidth,
            videoTileHeight = videoHeight,
        )
    }

    private data class Candidate(
        val rows: Int,
        val columns: Int,
        val emptyCells: Int,
        val videoWidth: Double,
        val videoHeight: Double,
    ) {
        val videoArea: Double
            get() = videoWidth * videoHeight
    }

    companion object {
        const val DEFAULT_AVAILABLE_WIDTH: Double = 16.0
        const val DEFAULT_AVAILABLE_HEIGHT: Double = 9.0
        const val DEFAULT_VIDEO_ASPECT_RATIO: Double = 16.0 / 9.0

        /** Convenience entry point for callers that use the standard 16:9 TV viewport. */
        fun calculate(cameraCount: Int): GridLayout = GridLayoutCalculator().calculate(cameraCount)

        private fun ceilingDivide(dividend: Int, divisor: Int): Int =
            ((dividend.toLong() + divisor - 1L) / divisor).toInt()

        private fun compareWithTolerance(left: Double, right: Double): Int {
            val tolerance = AREA_COMPARISON_EPSILON * maxOf(1.0, abs(left), abs(right))
            return when {
                left > right + tolerance -> 1
                right > left + tolerance -> -1
                else -> 0
            }
        }

        private const val AREA_COMPARISON_EPSILON = 1e-12
    }
}

/** The selected dimensions and row-major tile placements for one camera wall. */
data class GridLayout(
    val cameraCount: Int,
    val rows: Int,
    val columns: Int,
    val placements: List<GridCellPlacement>,
    val videoTileWidth: Double,
    val videoTileHeight: Double,
) {
    val rowCount: Int
        get() = rows

    val columnCount: Int
        get() = columns

    val cells: List<GridCellPlacement>
        get() = placements

    val capacity: Int
        get() = rows * columns

    val emptyCellCount: Int
        get() = capacity - cameraCount

    val videoTileArea: Double
        get() = videoTileWidth * videoTileHeight

    val lastRowItemCount: Int
        get() = placements.lastOrNull()?.rowItemCount ?: 0

    companion object {
        val EMPTY =
            GridLayout(
                cameraCount = 0,
                rows = 0,
                columns = 0,
                placements = emptyList(),
                videoTileWidth = 0.0,
                videoTileHeight = 0.0,
            )
    }
}

/**
 * Position of one item in a [GridLayout].
 *
 * [column] is the zero-based ordinal inside its row. [columnPosition] includes the fractional
 * leading offset used to center an incomplete final row. The normalized bounds are convenient for a
 * custom Compose `Layout`; a composition made from `Row`s can instead use [rowItemCount] and center
 * each row directly.
 */
data class GridCellPlacement(
    val itemIndex: Int,
    val row: Int,
    val column: Int,
    val rowItemCount: Int,
    val leadingOffsetColumns: Double,
    val leftFraction: Double,
    val topFraction: Double,
    val widthFraction: Double,
    val heightFraction: Double,
) {
    val index: Int
        get() = itemIndex

    val columnPosition: Double
        get() = leadingOffsetColumns + column

    val rightFraction: Double
        get() = leftFraction + widthFraction

    val bottomFraction: Double
        get() = topFraction + heightFraction
}
