package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Files

class WallStreetJournalTest {
    @Test
    fun basicPuzzle() {
        val path = FileSystems.getDefault().getPath("/Users/Jeff/Desktop/test-nocircles.puz")
        Files.write(path, WallStreetJournal(WallStreetJournalTest::class.readUtf8Resource("wsj/test-nocircles.json"),
                includeDateInTitle = false)
                .asCrossword().toAcrossLiteBinary())
        Assertions.assertArrayEquals(
                WallStreetJournalTest::class.readBinaryResource("puz/test-nocircles.puz"),
                WallStreetJournal(WallStreetJournalTest::class.readUtf8Resource("wsj/test-nocircles.json"),
                        includeDateInTitle = false)
                        .asCrossword().toAcrossLiteBinary())
    }
}