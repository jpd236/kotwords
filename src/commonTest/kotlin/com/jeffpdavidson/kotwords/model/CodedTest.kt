package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CodedTest {
    @Test
    fun jpzGeneration() = runTest {
        val coded = Coded(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf('A', 'B', 'C', null, 'D', 'E', 'F'),
                listOf('G', null, 'H', 'I', 'J', null, 'K'),
                listOf('L', 'M', 'N', 'O', 'P', 'Q', 'R'),
                listOf(null, null, 'S', 'T', 'U', null, null),
                listOf('V', 'W', 'X', 'Y', 'Z', 'A', 'B'),
                listOf('C', null, 'D', 'E', 'F', null, 'G'),
                listOf('F', 'E', 'D', null, 'C', 'B', 'A'),
            ),
            assignments = listOf(
                'Z', 'A', 'Y', 'B', 'X', 'C', 'W', 'D', 'V', 'E', 'U', 'F', 'T',
                'G', 'S', 'H', 'R', 'I', 'Q', 'J', 'P', 'K', 'O', 'L', 'N', 'M',
            ),
            givens = listOf('B', 'Q'),
        )

        val expected = readStringResource(CodedTest::class, "coded/coded.jpz")
        assertEquals(expected, coded.asPuzzle().asJpz().toXmlString())
    }
}