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

    fun createSquareList(sideLength: Int): List<Square> {
        val squareList = mutableListOf<Square>()
        (0..(sideLength / 2)).forEach { i ->
            val max = sideLength - i
            (i until max).forEach {
                squareList.add(Square(it, i, if (it == max - 1) null else Puzzle.BorderDirection.BOTTOM))
            }
            (i + 1 until max).forEach {
                squareList.add(Square(max - 1, it, if (it == max - 1) null else Puzzle.BorderDirection.LEFT))
            }
            (max - 2 downTo i).forEach {
                squareList.add(Square(it, max - 1, if (it == i) null else Puzzle.BorderDirection.TOP))
            }
            (max - 2 downTo i + 1).forEach {
                squareList.add(Square(i, it, if (it == i + 1) null else Puzzle.BorderDirection.RIGHT))
            }
        }
        return squareList
    }
}