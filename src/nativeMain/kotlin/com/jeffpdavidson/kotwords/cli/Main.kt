package com.jeffpdavidson.kotwords.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.Apz
import com.jeffpdavidson.kotwords.formats.BostonGlobe
import com.jeffpdavidson.kotwords.formats.Cnn
import com.jeffpdavidson.kotwords.formats.Crosshare
import com.jeffpdavidson.kotwords.formats.Crosswordr
import com.jeffpdavidson.kotwords.formats.Guardian
import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.NewYorkTimes
import com.jeffpdavidson.kotwords.formats.PuzzleMe
import com.jeffpdavidson.kotwords.formats.Puzzleable
import com.jeffpdavidson.kotwords.formats.Pzzl
import com.jeffpdavidson.kotwords.formats.Rgz
import com.jeffpdavidson.kotwords.formats.UclickJpz
import com.jeffpdavidson.kotwords.formats.UclickJson
import com.jeffpdavidson.kotwords.formats.UclickXml
import com.jeffpdavidson.kotwords.formats.WallStreetJournal
import com.jeffpdavidson.kotwords.formats.WallStreetJournalAcrostic
import com.jeffpdavidson.kotwords.formats.XWordInfo
import com.jeffpdavidson.kotwords.formats.XWordInfoAcrostic
import korlibs.time.Date
import korlibs.time.DateTime
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath

enum class Format(
    val extensions: List<String>,
    val readFn: suspend (ByteArray, Date, author: String, copyright: String) -> Puzzleable,
    val writeFn: (suspend (Puzzleable) -> ByteArray)? = null,
) {
    // Input/output formats
    IPUZ(listOf("ipuz"), { data, _, _, _ -> Ipuz(data.decodeToString()) }, { it.asIpuzFile() }),
    JPZ(listOf("jpz", "xml"), { data, _, _, _ -> JpzFile(data) }, { it.asJpzFile() }),
    PUZ(listOf("puz"), { data, _, _, _ -> AcrossLite(data) }, { it.asAcrossLiteBinary() }),

    // Input-only formats
    APZ(listOf("apz"), { data, _, _, _ -> Apz.fromXmlString(data.decodeToString()).toAcrostic() }),
    RGZ(listOf("rg", "rgz"), { data, _, _, _ -> Rgz.fromRgzFile(data) }),
    BOSTON_GLOBE_HTML(listOf(), { data, _, _, _ -> BostonGlobe(data.decodeToString()) }),
    CNN_JSON(listOf(), { data, _, _, _ -> Cnn(data.decodeToString()) }),
    CROSSHARE_JSON(listOf(), { data, _, _, _ -> Crosshare(data.decodeToString()) }),
    CROSSWORDR_JSON(listOf(), { data, _, _, _ -> Crosswordr(data.decodeToString()) }),
    GUARDIAN_JSON(listOf(), { data, _, _, copyright -> Guardian(data.decodeToString(), copyright) }),
    NEW_YORK_TIMES_HTML(listOf(), { data, _, _, _ -> NewYorkTimes.fromHtml(data.decodeToString()) }),
    PUZZLE_ME_JSON(listOf(), { data, _, _, _ -> PuzzleMe(data.decodeToString()) }),
    PZZL_TEXT(listOf(), { data, _, _, _ -> Pzzl(data.decodeToString()) }),
    UCLICK_JPZ(listOf(), { data, date, _, _ -> UclickJpz(data.decodeToString(), date) }),
    UCLICK_JSON(listOf(), { data, _, _, copyright -> UclickJson(data.decodeToString(), copyright) }),
    UCLICK_XML(listOf(), { data, date, _, _ -> UclickXml(data.decodeToString(), date) }),
    WALL_STREET_JOURNAL_JSON(listOf(), { data, _, _, _ -> WallStreetJournal(data.decodeToString()) }),
    WALL_STREET_JOURNAL_ACROSTIC_JSON(
        listOf(),
        { data, _, _, _ -> WallStreetJournalAcrostic(data.decodeToString()) }
    ),
    XWORD_INFO_JSON(listOf(), { data, _, _, _ -> XWordInfo(data.decodeToString()) }),
    XWORD_INFO_ACROSTIC_JSON(listOf(), { data, _, author, _ -> XWordInfoAcrostic(data.decodeToString(), author) });

    companion object {
        val FORMATS_BY_EXTENSION = entries.flatMap { format ->
            format.extensions.map { extension ->
                extension to format
            }
        }.toMap()
    }
}

class KotwordsCli : CliktCommand() {
    override fun run() = Unit
}

class DumpEntries : CliktCommand(help = "Dump information about a puzzle") {
    val format by option(help = "Puzzle file format. By default, use the file's extension.").enum<Format>()
    val file by option(help = "Puzzle file path").convert {
        val path = it.toPath()
        if (!FileSystem.SYSTEM.exists(path)) {
            fail("File not found: $path")
        } else {
            path
        }
    }.required()

    override fun run() {
        val resolvedFormat: Format = format ?: {
            require(file.name.contains('.')) {
                "Puzzle format cannot be inferred; please provide it using --format."
            }
            val extension = file.name.substringAfterLast('.')
            Format.FORMATS_BY_EXTENSION[extension]
                ?: throw IllegalArgumentException("Unsupported puzzle format: $extension")
        }()
        runBlocking {
            val data = FileSystem.SYSTEM.read(file) {
                readByteArray()
            }
            // Since we're just dumping the grid, the date/author/copyright don't matter.
            val puzzle = resolvedFormat.readFn(data, DateTime.nowLocal().local.date, "", "").asPuzzle()
            puzzle.words.forEach { word ->
                println(
                    word.cells.joinToString("") { coordinate ->
                        puzzle.grid[coordinate.y][coordinate.x].solution
                    }
                )
            }
        }
    }
}

class Convert : CliktCommand(help = "Convert a puzzle between formats") {
    val inputFormat by option(help = "Input puzzle format").choice(Format.entries.map { format ->
        format.name to format
    }.toMap()).required()
    val inputFile by option(help = "Input puzzle file path").convert {
        val path = it.toPath()
        if (!FileSystem.SYSTEM.exists(path)) {
            fail("File not found: $path")
        } else {
            path
        }
    }.required()
    val outputFormat by option(help = "Output puzzle format").choice(Format.entries.filter { format ->
        format.writeFn != null
    }.map { format ->
        format.name to format
    }.toMap()).required()
    val outputFile by option(help = "Output puzzle file path").convert { it.toPath() }.required()
    val date by option(
        help = "For Uclick JPZ/XML inputs, the date of the puzzle in YYYY-MM-DD format. Defaults to today."
    ).convert {
        DateTime.parse(it).local.date
    }.default(DateTime.nowLocal().local.date)
    val author by option(help = "For XWordInfo Acrostic inputs, the author of the puzzle. Defaults to blank.")
        .default("")
    val copyright by option(help = "For Uclick JSON inputs, the copyright of the puzzle. Defaults to blank.")
        .default("")

    override fun run() {
        runBlocking {
            val inputData = FileSystem.SYSTEM.read(inputFile) {
                readByteArray()
            }
            val puzzleable = inputFormat.readFn(inputData, date, author, copyright)
            val outputData = outputFormat.writeFn!!(puzzleable)
            FileSystem.SYSTEM.write(outputFile) {
                write(outputData)
            }
        }
    }
}

fun main(args: Array<String>) = KotwordsCli()
    .subcommands(DumpEntries(), Convert())
    .main(args)
