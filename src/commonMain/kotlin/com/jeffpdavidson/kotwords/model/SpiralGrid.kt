package com.jeffpdavidson.kotwords.model

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object SpiralGrid {
    data class Square(val x: Int, val y: Int, val borderDirection: Puzzle.BorderDirection?)

    fun getSideLength(letterCount: Int): Int {
        val sideLengthDouble = sqrt(letterCount.toDouble())
        return ceil(sideLengthDouble).roundToInt()
    }

    fun createSquareList(width: Int, height: Int): List<Square> {
        val totalSize = width * height
        // Create the ordered list of squares by weaving through the grid, rotating clockwise whenever we hit the edge
        // or a point that's already in the grid.
        val usedPoints = mutableSetOf<Pair<Int, Int>>()
        val squareList = mutableListOf<Square>()
        var direction = Direction.EAST
        var currentPoint = 0 to 0
        while (usedPoints.size < totalSize) {
            var nextPoint = direction.move(currentPoint)
            var includeBorder = true
            if (nextPoint in usedPoints || nextPoint.first !in 0 until width || nextPoint.second !in 0 until height) {
                direction = direction.rotate()
                nextPoint = direction.move(currentPoint)
                includeBorder = false
            }
            val borderDirection = if (includeBorder) direction.borderDirection else null
            squareList.add(Square(currentPoint.first, currentPoint.second, borderDirection))
            usedPoints.add(currentPoint)
            currentPoint = nextPoint
        }
        return squareList
    }

    private enum class Direction(
        private val offset: Pair<Int, Int>,
        val borderDirection: Puzzle.BorderDirection,
    ) {
        EAST(1 to 0, Puzzle.BorderDirection.BOTTOM),
        SOUTH(0 to 1, Puzzle.BorderDirection.LEFT),
        WEST(-1 to 0, Puzzle.BorderDirection.TOP),
        NORTH(0 to -1, Puzzle.BorderDirection.RIGHT);

        fun move(point: Pair<Int, Int>): Pair<Int, Int> = point.first + offset.first to point.second + offset.second

        fun rotate(): Direction = when (this) {
            EAST -> SOUTH
            SOUTH -> WEST
            WEST -> NORTH
            NORTH -> EAST
        }
    }
}