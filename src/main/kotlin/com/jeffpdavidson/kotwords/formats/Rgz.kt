package com.jeffpdavidson.kotwords.formats

import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.zip.ZipInputStream
import javax.xml.stream.XMLOutputFactory

private var DELIMITER = ": ?".toRegex()

class Rgz(private val data: String) {
    fun asJpz(): String {
        var title = ""
        var author = ""
        var copyright = ""
        var rows = mutableListOf<Pair<String, String>>()
        var lights = mutableListOf<Pair<String, String>>()
        var mediums = mutableListOf<Pair<String, String>>()
        var darks = mutableListOf<Pair<String, String>>()

        var line = 0
        val lines = data.lines()

        fun parseClues(): MutableList<Pair<String, String>> {
            val clueList = mutableListOf<Pair<String, String>>()
            line++
            var currentClue = ""
            var currentAnswer = ""
            while (true) {
                when {
                    lines[line].contains("^- (- )?clue".toRegex()) -> {
                        if (currentClue != "") {
                            clueList.add(currentClue to currentAnswer)
                        }
                        currentClue = lines[line].split(DELIMITER)[1]
                        line++
                        currentAnswer = convertAnswer(lines[line].split(DELIMITER)[1])
                        line++
                    }
                    lines[line].startsWith("  - clue") -> {
                        currentClue += " / " + lines[line].split(DELIMITER)[1]
                        line++
                        currentAnswer += convertAnswer(lines[line].split(DELIMITER)[1])
                        line++
                    }
                    else -> {
                        if (currentClue != "") {
                            clueList.add(currentClue to currentAnswer)
                        }
                        line--
                        return clueList
                    }
                }
            }
        }

        while (line < lines.size) {
            val parts = lines[line].split(DELIMITER)
            when (parts[0]) {
                "title" -> title = parts[1]
                "author" -> author = parts[1]
                "copyright" -> copyright = parts[1]
                "rows" -> rows = parseClues()
                "light" -> lights = parseClues()
                "medium" -> mediums = parseClues()
                "dark" -> darks = parseClues()
            }
            line++
        }

        val output = StringWriter()
        val xml = XMLOutputFactory.newInstance().createXMLStreamWriter(output)

        xml.writeStartDocument("UTF-8", "1.0")

        xml.writeStartElement("crossword-compiler-applet")
        xml.writeAttribute("xmlns", "http://crossword.info/xml/crossword-compiler")

        xml.writeStartElement("rectangular-puzzle")
        xml.writeAttribute("xmlns", "http://crossword.info/xml/rectangular-puzzle")
        xml.writeAttribute("alphabet", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

        xml.writeStartElement("metadata")

        xml.writeStartElement("title")
        xml.writeCharacters(title)
        xml.writeEndElement()

        xml.writeStartElement("creator")
        xml.writeCharacters(author)
        xml.writeEndElement()

        xml.writeStartElement("copyright")
        xml.writeCharacters(copyright)
        xml.writeEndElement()

        xml.writeEndElement() // metadata

        xml.writeStartElement("crossword")

        xml.writeStartElement("grid")
        xml.writeAttribute("width", "21")
        xml.writeAttribute("height", "12")

        xml.writeEmptyElement("grid-look")
        xml.writeAttribute("hide-lines", "true")
        xml.writeAttribute("cell-size-in-pixels", "25")

        val solutionLetters = rows.joinToString("") { it.second }
        var solutionIndex = 0

        fun writeCell(x: Int, y: Int, solution: Char? = null, backgroundColor: String = "", number: String = "") {
            xml.writeEmptyElement("cell")
            xml.writeAttribute("x", "$x")
            xml.writeAttribute("y", "$y")
            if (solution == null) {
                xml.writeAttribute("type", "block")
            } else {
                xml.writeAttribute("solution", "$solution")
            }
            if (backgroundColor != "") {
                xml.writeAttribute("background-color", backgroundColor)
            }
            if (number != "") {
                xml.writeAttribute("number", number)
            }
        }

        writeCell(1, 1)
        writeCell(2, 1)
        writeCell(3, 1)
        writeCell(4, 1, solutionLetters[solutionIndex++], "#FAAFBE", "A")
        writeCell(5, 1, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(6, 1, solutionLetters[solutionIndex++], "#FAAFBE", "M1")
        writeCell(7, 1)
        writeCell(8, 1)
        writeCell(9, 1)
        writeCell(10, 1, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(11, 1, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(12, 1, solutionLetters[solutionIndex++], "#FAAFBE", "M2")
        writeCell(13, 1)
        writeCell(14, 1)
        writeCell(15, 1)
        writeCell(16, 1, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(17, 1, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(18, 1, solutionLetters[solutionIndex++], "#FAAFBE", "M3")
        writeCell(19, 1)
        writeCell(20, 1)
        writeCell(21, 1)

        writeCell(1, 2, solutionLetters[solutionIndex++], "#FFFFFF", "B")
        writeCell(2, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(3, 2, solutionLetters[solutionIndex++], "#FFFFFF", "L1")
        writeCell(4, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(5, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(6, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(7, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(8, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(9, 2, solutionLetters[solutionIndex++], "#FFFFFF", "L2")
        writeCell(10, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(11, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(12, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(13, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(14, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(15, 2, solutionLetters[solutionIndex++], "#FFFFFF", "L3")
        writeCell(16, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(17, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(18, 2, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(19, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(20, 2, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(21, 2, solutionLetters[solutionIndex++], "#FFFFFF", "L4")

        writeCell(1, 3, solutionLetters[solutionIndex++], "#FFFFFF", "C")
        writeCell(2, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(3, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(4, 3, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(5, 3, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(6, 3, solutionLetters[solutionIndex++], "#0080FF", "D1")
        writeCell(7, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(8, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(9, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(10, 3, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(11, 3, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(12, 3, solutionLetters[solutionIndex++], "#0080FF", "D2")
        writeCell(13, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(14, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(15, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(16, 3, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(17, 3, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(18, 3, solutionLetters[solutionIndex++], "#0080FF", "D3")
        writeCell(19, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(20, 3, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(21, 3, solutionLetters[solutionIndex++], "#FFFFFF")

        writeCell(1, 4, solutionLetters[solutionIndex++], "#FAAFBE", "D")
        writeCell(2, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(3, 4, solutionLetters[solutionIndex++], "#FAAFBE", "M4")
        writeCell(4, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(5, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(6, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(7, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(8, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(9, 4, solutionLetters[solutionIndex++], "#FAAFBE", "M5")
        writeCell(10, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(11, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(12, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(13, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(14, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(15, 4, solutionLetters[solutionIndex++], "#FAAFBE", "M6")
        writeCell(16, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(17, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(18, 4, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(19, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(20, 4, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(21, 4, solutionLetters[solutionIndex++], "#FAAFBE", "M7")

        writeCell(1, 5, solutionLetters[solutionIndex++], "#FAAFBE", "E")
        writeCell(2, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(3, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(4, 5, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(5, 5, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(6, 5, solutionLetters[solutionIndex++], "#FFFFFF", "L5")
        writeCell(7, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(8, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(9, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(10, 5, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(11, 5, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(12, 5, solutionLetters[solutionIndex++], "#FFFFFF", "L6")
        writeCell(13, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(14, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(15, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(16, 5, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(17, 5, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(18, 5, solutionLetters[solutionIndex++], "#FFFFFF", "L7")
        writeCell(19, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(20, 5, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(21, 5, solutionLetters[solutionIndex++], "#FAAFBE")

        writeCell(1, 6, solutionLetters[solutionIndex++], "#0080FF", "F")
        writeCell(2, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(3, 6, solutionLetters[solutionIndex++], "#0080FF", "D4")
        writeCell(4, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(5, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(6, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(7, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(8, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(9, 6, solutionLetters[solutionIndex++], "#0080FF", "D5")
        writeCell(10, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(11, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(12, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(13, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(14, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(15, 6, solutionLetters[solutionIndex++], "#0080FF", "D6")
        writeCell(16, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(17, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(18, 6, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(19, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(20, 6, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(21, 6, solutionLetters[solutionIndex++], "#0080FF", "D7")

        writeCell(1, 7, solutionLetters[solutionIndex++], "#0080FF", "G")
        writeCell(2, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(3, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(4, 7, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(5, 7, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(6, 7, solutionLetters[solutionIndex++], "#FAAFBE", "M8")
        writeCell(7, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(8, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(9, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(10, 7, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(11, 7, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(12, 7, solutionLetters[solutionIndex++], "#FAAFBE", "M9")
        writeCell(13, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(14, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(15, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(16, 7, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(17, 7, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(18, 7, solutionLetters[solutionIndex++], "#FAAFBE", "M10")
        writeCell(19, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(20, 7, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(21, 7, solutionLetters[solutionIndex++], "#0080FF")

        writeCell(1, 8, solutionLetters[solutionIndex++], "#FFFFFF", "H")
        writeCell(2, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(3, 8, solutionLetters[solutionIndex++], "#FFFFFF", "L8")
        writeCell(4, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(5, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(6, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(7, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(8, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(9, 8, solutionLetters[solutionIndex++], "#FFFFFF", "L9")
        writeCell(10, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(11, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(12, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(13, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(14, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(15, 8, solutionLetters[solutionIndex++], "#FFFFFF", "L10")
        writeCell(16, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(17, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(18, 8, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(19, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(20, 8, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(21, 8, solutionLetters[solutionIndex++], "#FFFFFF", "L11")

        writeCell(1, 9, solutionLetters[solutionIndex++], "#FFFFFF", "I")
        writeCell(2, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(3, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(4, 9, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(5, 9, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(6, 9, solutionLetters[solutionIndex++], "#0080FF", "D8")
        writeCell(7, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(8, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(9, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(10, 9, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(11, 9, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(12, 9, solutionLetters[solutionIndex++], "#0080FF", "D9")
        writeCell(13, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(14, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(15, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(16, 9, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(17, 9, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(18, 9, solutionLetters[solutionIndex++], "#0080FF", "D10")
        writeCell(19, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(20, 9, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(21, 9, solutionLetters[solutionIndex++], "#FFFFFF")

        writeCell(1, 10, solutionLetters[solutionIndex++], "#FAAFBE", "J")
        writeCell(2, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(3, 10, solutionLetters[solutionIndex++], "#FAAFBE", "M11")
        writeCell(4, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(5, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(6, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(7, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(8, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(9, 10, solutionLetters[solutionIndex++], "#FAAFBE", "M12")
        writeCell(10, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(11, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(12, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(13, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(14, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(15, 10, solutionLetters[solutionIndex++], "#FAAFBE", "M13")
        writeCell(16, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(17, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(18, 10, solutionLetters[solutionIndex++], "#0080FF")
        writeCell(19, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(20, 10, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(21, 10, solutionLetters[solutionIndex++], "#FAAFBE", "M14")

        writeCell(1, 11, solutionLetters[solutionIndex++], "#FAAFBE", "K")
        writeCell(2, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(3, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(4, 11, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(5, 11, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(6, 11, solutionLetters[solutionIndex++], "#FFFFFF", "L12")
        writeCell(7, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(8, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(9, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(10, 11, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(11, 11, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(12, 11, solutionLetters[solutionIndex++], "#FFFFFF", "L13")
        writeCell(13, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(14, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(15, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(16, 11, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(17, 11, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(18, 11, solutionLetters[solutionIndex++], "#FFFFFF", "L14")
        writeCell(19, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(20, 11, solutionLetters[solutionIndex++], "#FAAFBE")
        writeCell(21, 11, solutionLetters[solutionIndex++], "#FAAFBE")

        writeCell(1, 12)
        writeCell(2, 12)
        writeCell(3, 12)
        writeCell(4, 12, solutionLetters[solutionIndex++], "#FFFFFF", "L")
        writeCell(5, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(6, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(7, 12)
        writeCell(8, 12)
        writeCell(9, 12)
        writeCell(10, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(11, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(12, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(13, 12)
        writeCell(14, 12)
        writeCell(15, 12)
        writeCell(16, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(17, 12, solutionLetters[solutionIndex++], "#FFFFFF")
        writeCell(18, 12, solutionLetters[solutionIndex], "#FFFFFF")
        writeCell(19, 12)
        writeCell(20, 12)
        writeCell(21, 12)

        xml.writeEndElement() // grid

        fun writeWord(id: Int, vararg ranges: Pair<String, String>) {
            xml.writeStartElement("word")
            xml.writeAttribute("id", "$id")
            if (ranges.size == 1) {
                xml.writeAttribute("x", ranges[0].first)
                xml.writeAttribute("y", ranges[0].second)
            } else {
                ranges.forEach {
                    xml.writeEmptyElement("cells")
                    xml.writeAttribute("x", it.first)
                    xml.writeAttribute("y", it.second)
                }
            }
            xml.writeEndElement()
        }

        writeWord(1, "4-6" to "1", "10-12" to "1", "16-18" to "1")
        writeWord(2, "1-21" to "2")
        writeWord(3, "1-21" to "3")
        writeWord(4, "1-21" to "4")
        writeWord(5, "1-21" to "5")
        writeWord(6, "1-21" to "6")
        writeWord(7, "1-21" to "7")
        writeWord(8, "1-21" to "8")
        writeWord(9, "1-21" to "9")
        writeWord(10, "1-21" to "10")
        writeWord(11, "1-21" to "11")
        writeWord(12, "4-6" to "12", "10-12" to "12", "16-18" to "12")

        writeWord(100, "1-3" to "2", "3-1" to "3")
        writeWord(101, "7-9" to "2", "9-7" to "3")
        writeWord(102, "13-15" to "2", "15-13" to "3")
        writeWord(103, "19-21" to "2", "21-19" to "3")

        writeWord(104, "4-6" to "5", "6-4" to "6")
        writeWord(105, "10-12" to "5", "12-10" to "6")
        writeWord(106, "16-18" to "5", "18-16" to "6")

        writeWord(107, "1-3" to "8", "3-1" to "9")
        writeWord(108, "7-9" to "8", "9-7" to "9")
        writeWord(109, "13-15" to "8", "15-13" to "9")
        writeWord(110, "19-21" to "8", "21-19" to "9")

        writeWord(111, "4-6" to "11", "6-4" to "12")
        writeWord(112, "10-12" to "11", "12-10" to "12")
        writeWord(113, "16-18" to "11", "18-16" to "12")

        writeWord(200, "4-6" to "1", "6-4" to "2")
        writeWord(201, "10-12" to "1", "12-10" to "2")
        writeWord(202, "16-18" to "1", "18-16" to "2")

        writeWord(203, "1-3" to "4", "3-1" to "5")
        writeWord(204, "7-9" to "4", "9-7" to "5")
        writeWord(205, "13-15" to "4", "15-13" to "5")
        writeWord(206, "19-21" to "4", "21-19" to "5")

        writeWord(207, "4-6" to "7", "6-4" to "8")
        writeWord(208, "10-12" to "7", "12-10" to "8")
        writeWord(209, "16-18" to "7", "18-16" to "8")

        writeWord(210, "1-3" to "10", "3-1" to "11")
        writeWord(211, "7-9" to "10", "9-7" to "11")
        writeWord(212, "13-15" to "10", "15-13" to "11")
        writeWord(213, "19-21" to "10", "21-19" to "11")

        writeWord(300, "4-6" to "3", "6-4" to "4")
        writeWord(301, "10-12" to "3", "12-10" to "4")
        writeWord(302, "16-18" to "3", "18-16" to "4")

        writeWord(303, "1-3" to "6", "3-1" to "7")
        writeWord(304, "7-9" to "6", "9-7" to "7")
        writeWord(305, "13-15" to "6", "15-13" to "7")
        writeWord(306, "19-21" to "6", "21-19" to "7")

        writeWord(307, "4-6" to "9", "6-4" to "10")
        writeWord(308, "10-12" to "9", "12-10" to "10")
        writeWord(309, "16-18" to "9", "18-16" to "10")

        xml.writeStartElement("clues")
        xml.writeAttribute("ordering", "normal")

        xml.writeStartElement("title")
        xml.writeStartElement("b")
        xml.writeCharacters("ROWS")
        xml.writeEndElement()
        xml.writeEndElement()

        rows.forEachIndexed { index, clue ->
            xml.writeStartElement("clue")
            xml.writeAttribute("word", "${index + 1}")
            xml.writeAttribute("number", "${'A' + index}")
            xml.writeCharacters(clue.first)
            xml.writeEndElement()
        }

        xml.writeEndElement() // clues

        xml.writeStartElement("clues")
        xml.writeAttribute("ordering", "normal")

        xml.writeStartElement("title")
        xml.writeStartElement("b")
        xml.writeCharacters("BLOOMS")
        xml.writeEndElement()
        xml.writeEndElement()

        lights.forEachIndexed { index, clue ->
            xml.writeStartElement("clue")
            xml.writeAttribute("word", "${index + 100}")
            xml.writeAttribute("number", "L${index + 1}")
            xml.writeCharacters(clue.first)
            xml.writeEndElement()
        }
        mediums.forEachIndexed { index, clue ->
            xml.writeStartElement("clue")
            xml.writeAttribute("word", "${index + 200}")
            xml.writeAttribute("number", "M${index + 1}")
            xml.writeCharacters(clue.first)
            xml.writeEndElement()
        }
        darks.forEachIndexed { index, clue ->
            xml.writeStartElement("clue")
            xml.writeAttribute("word", "${index + 300}")
            xml.writeAttribute("number", "D${index + 1}")
            xml.writeCharacters(clue.first)
            xml.writeEndElement()
        }

        xml.writeEndElement() // clues

        xml.writeEndElement() // crossword

        xml.writeEndElement() // rectangular-puzzle

        xml.writeEndElement() // crossword-compiler-applet

        xml.writeEndDocument()

        return output.toString()
    }

    private fun convertAnswer(answer: String): String {
        return answer.replace("[^A-Z]".toRegex(), "")
    }
}

fun main(args: Array<String>) {
    val inFile = FileSystems.getDefault().getPath("/Users/Jeff/Google Drive/Crosswords/FREE RG - Sept19.rgz")
    ZipInputStream(Files.newInputStream(inFile)).use {
        it.nextEntry
        it.bufferedReader(StandardCharsets.UTF_8).use {
            val rgInput = it.readText()
            val jpzOutput = Rgz(rgInput).asJpz()
            val outputFile = FileSystems.getDefault().getPath(inFile.toString().replace(".rgz", ".jpz"))
            Files.write(outputFile, jpzOutput.toByteArray(StandardCharsets.UTF_8))
        }
    }
}