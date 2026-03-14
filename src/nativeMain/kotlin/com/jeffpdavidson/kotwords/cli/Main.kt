package com.jeffpdavidson.kotwords.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.enum
import com.jeffpdavidson.kotwords.cli.InputFile.Companion.inputFile
import com.jeffpdavidson.kotwords.cli.InputFile.PathFile
import com.jeffpdavidson.kotwords.cli.OutputFile.Companion.outputFile
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.Apz
import com.jeffpdavidson.kotwords.formats.BostonGlobe
import com.jeffpdavidson.kotwords.formats.Cnn
import com.jeffpdavidson.kotwords.formats.Crosshare
import com.jeffpdavidson.kotwords.formats.Crosswordr
import com.jeffpdavidson.kotwords.formats.Guardian
import com.jeffpdavidson.kotwords.formats.HtmlSanitizer
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
import com.jeffpdavidson.kotwords.formats.WashingtonPost
import com.jeffpdavidson.kotwords.formats.XWordInfo
import com.jeffpdavidson.kotwords.formats.XWordInfoAcrostic
import com.jeffpdavidson.kotwords.formats.Xd
import com.jeffpdavidson.kotwords.formats.pdf.FONT_FAMILY_TIMES_ROMAN
import com.jeffpdavidson.kotwords.formats.pdf.PdfFont
import com.jeffpdavidson.kotwords.formats.pdf.PdfFontFamily
import com.jeffpdavidson.kotwords.formats.pdf.PdfFontId
import com.jeffpdavidson.kotwords.formats.pdf.TtfFonts
import korlibs.time.Date
import korlibs.time.DateTime
import korlibs.time.date
import korlibs.time.nowLocal
import korlibs.time.parse
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.ByteString.Companion.decodeBase64
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

enum class FontFamilyId(val fontFamily: PdfFontFamily) {
    TIMES_ROMAN(FONT_FAMILY_TIMES_ROMAN),
    NOTO_SERIF(
        PdfFontFamily(
            baseFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Regular")) {
                decodeTtfResource(TtfFonts.NOTOSERIF_REGULAR_TTF_BASE64)
            },
            boldFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Bold")) {
                decodeTtfResource(TtfFonts.NOTOSERIF_BOLD_TTF_BASE64)
            },
            italicFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Italic")) {
                decodeTtfResource(TtfFonts.NOTOSERIF_ITALIC_TTF_BASE64)
            },
            boldItalicFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-BoldItalic")) {
                decodeTtfResource(TtfFonts.NOTOSERIF_BOLDITALIC_TTF_BASE64)
            },
        )
    ),
    NOTO_SANS(
        PdfFontFamily(
            baseFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSans-Regular")) {
                decodeTtfResource(TtfFonts.NOTOSANS_REGULAR_TTF_BASE64)
            },
            boldFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSans-Bold")) {
                decodeTtfResource(TtfFonts.NOTOSANS_BOLD_TTF_BASE64)
            },
            italicFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSans-Italic")) {
                decodeTtfResource(TtfFonts.NOTOSANS_ITALIC_TTF_BASE64)
            },
            boldItalicFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSans-BoldItalic")) {
                decodeTtfResource(TtfFonts.NOTOSANS_BOLDITALIC_TTF_BASE64)
            },
        )
    );

    companion object {
        private fun decodeTtfResource(encodedTtf: String): ByteArray {
            val compressedBytes = encodedTtf.decodeBase64() ?: throw IllegalStateException("Unable to decode TTF")
            val buffer = Buffer().write(compressedBytes)
            return GzipSource(buffer).buffer().use { it.readByteArray() }
        }
    }
}

data class PdfOptions(
    val fontFamilyId: FontFamilyId,
    val blackSquareLightnessAdjustment: Double,
)

enum class Format(
    val extensions: List<String>,
    val readFn: (suspend (ByteArray, Date, author: String, copyright: String) -> Puzzleable)? = null,
    val writeFn: (suspend (Puzzleable, PdfOptions) -> ByteArray)? = null,
) {
    // Input/output formats
    IPUZ(listOf("ipuz"), { data, _, _, _ -> Ipuz(data.decodeToString()) }, { puzzle, _ -> puzzle.asIpuzFile() }),
    JPZ(listOf("jpz", "xml"), { data, _, _, _ -> JpzFile(data) }, { puzzle, _ -> puzzle.asJpzFile() }),
    PUZ(listOf("puz"), { data, _, _, _ -> AcrossLite(data) }, { puzzle, _ -> puzzle.asAcrossLiteBinary() }),

    // Output-only formats
    PDF(
        extensions = listOf(),
        readFn = null,
        writeFn = { puzzle, pdfOptions ->
            puzzle.asPdf(
                fontFamily = pdfOptions.fontFamilyId.fontFamily,
                blackSquareLightnessAdjustment = pdfOptions.blackSquareLightnessAdjustment
            )
        }
    ),

    // Input-only formats
    APZ(listOf("apz"), { data, _, _, _ -> Apz.fromXmlString(data.decodeToString()).toAcrostic() }),
    RGZ(listOf("rg", "rgz"), { data, _, _, _ -> Rgz.fromRgzFile(data) }),
    BOSTON_GLOBE_HTML(listOf(), { data, _, _, _ -> BostonGlobe(data.decodeToString()) }),
    CNN_JSON(listOf(), { data, _, _, _ -> Cnn(data.decodeToString()) }),
    CROSSHARE_JSON(listOf(), { data, _, _, _ -> Crosshare(data.decodeToString()) }),
    CROSSWORDR_JSON(listOf(), { data, _, _, _ -> Crosswordr(data.decodeToString()) }),
    GUARDIAN_JSON(listOf(), { data, _, _, copyright -> Guardian(data.decodeToString(), copyright) }),
    NEW_YORK_TIMES_HTML(listOf(), { data, _, _, _ -> NewYorkTimes.fromHtml(data.decodeToString()) }),

    // For now, set stream to empty, since this only impacts the title ("NY Times" vs "NY Times Mini Crossword").
    NEW_YORK_TIMES_JSON(listOf(), { data, _, _, _ -> NewYorkTimes.fromApiJson(data.decodeToString(), stream = "") }),
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
    WASHINGTON_POST_JSON(listOf(), { data, _, _, _ -> WashingtonPost(data.decodeToString()) }),
    XD(listOf("xd"), { data, _, _, _ -> Xd(data.decodeToString()) }),
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

class DumpEntries : CliktCommand() {
    val format by option(help = "Puzzle file format. By default, use the file's extension.").enum<Format>()
    val file by option(help = "Puzzle file path. Use '-' for stdin.").inputFile().required()
    val outputFormat by option(
        help = """
        Format to use when outputting each entry. Supports the following substitutions:

        - [[number]] - clue number${'\u0085'}
        - [[direction]] - clue direction${'\u0085'}
        - [[clue]] - clue text${'\u0085'}
        - [[answer]] - answer

        Defaults to "[[number]]-[[direction]]: [[clue]] - [[answer]]".
    """.trimIndent()
    ).default("[[number]]-[[direction]]: [[clue]] - [[answer]]")

    override fun help(context: Context): String = "Dump information about a puzzle"

    override fun run() {
        val resolvedFormat: Format = format ?: {
            val fileValue = file
            require(fileValue is PathFile && fileValue.path.name.contains('.')) {
                "Puzzle format cannot be inferred; please provide it using --format."
            }
            val extension = fileValue.path.name.substringAfterLast('.')
            Format.FORMATS_BY_EXTENSION[extension]
                ?: throw IllegalArgumentException("Unsupported puzzle format: $extension")
        }()
        runBlocking {
            val data = file.readContents()
            // Since we're just dumping the grid, the date/author/copyright don't matter.
            val readFn =
                resolvedFormat.readFn ?: throw IllegalArgumentException("File format not supported for reading")
            val puzzle = readFn(data, DateTime.nowLocal().local.date, "", "").asPuzzle()
            val wordIdToWordMap = puzzle.words.associateBy { it.id }
            puzzle.clues.forEach { clueList ->
                // Strip HTML from direction. The formatting options are rarely desirable here.
                val direction = HtmlSanitizer.substituteUnsupportedText(
                    clueList.title,
                    sanitizeCharacters = false,
                    formatHtml = false
                )
                clueList.clues.forEach { clue ->
                    val word = wordIdToWordMap[clue.wordId]?.cells ?: listOf()
                    val solution = word.joinToString("") { coordinate ->
                        puzzle.grid[coordinate.y][coordinate.x].solution
                    }
                    println(SUBSTITUTION_PATTERN.findAll(outputFormat).fold(outputFormat) { result, matchResult ->
                        val (match, key) = matchResult.groupValues
                        result.replace(
                            match, when (key) {
                                "number" -> clue.number
                                "direction" -> direction
                                "clue" -> HtmlSanitizer.substituteUnsupportedText(clue.text, sanitizeCharacters = false)
                                "answer" -> solution
                                else -> match
                            }
                        )
                    })
                }
            }
        }
    }

    companion object {
        private val SUBSTITUTION_PATTERN = "\\[\\[([a-z]+)\\]\\]".toRegex()

        private val STRIP_HTML_REPLACEMENTS = mapOf(
            "</?[a-z]+>" to "",
            "&amp;" to "&",
            "&lt;" to "<",
        ).mapKeys { it.key.toRegex(RegexOption.IGNORE_CASE) }

        private val FORMAT_HTML_REPLACEMENTS = mapOf(
            "</?b>" to "*",
            "</?i>" to "\"",
        ).mapKeys { it.key.toRegex(RegexOption.IGNORE_CASE) } + STRIP_HTML_REPLACEMENTS
    }
}

class Convert : CliktCommand() {
    val inputFormat by option(help = "Input puzzle format").choice(Format.entries.map { format ->
        format.name to format
    }.toMap()).required()
    val inputFile by option(help = "Input puzzle file path. Use '-' for stdin.").inputFile().required()
    val outputFormat by option(help = "Output puzzle format").choice(Format.entries.filter { format ->
        format.writeFn != null
    }.map { format ->
        format.name to format
    }.toMap()).required()
    val outputFile by option(help = "Output puzzle file path. Use '-' for stdout.").outputFile().required()
    val date by option(
        help = "For Uclick JPZ/XML inputs, the date of the puzzle in YYYY-MM-DD format. Defaults to today."
    ).convert {
        DateTime.parse(it).local.date
    }.default(DateTime.nowLocal().local.date)
    val author by option(help = "For XWordInfo Acrostic inputs, the author of the puzzle. Defaults to blank.")
        .default("")
    val copyright by option(help = "For Uclick JSON inputs, the copyright of the puzzle. Defaults to blank.")
        .default("")
    val fontFamily by option(help = "For PDF output, the font family to use. Defaults to NOTO_SERIF.")
        .choice(FontFamilyId.entries.map { fontFamilyId -> fontFamilyId.name to fontFamilyId }.toMap())
        .default(FontFamilyId.NOTO_SERIF)
    val blackSquareLightnessAdjustment by option(
        help = "For PDF output, percentage (from 0 to 1) indicating how much to brighten black/colored squares (i.e. " +
                "to save ink). 0 indicates no adjustment; 1 would be fully white. Defaults to 0."
    ).double().default(0.0)

    override fun help(context: Context): String = "Convert a puzzle between formats"

    override fun run() {
        runBlocking {
            val inputData = inputFile.readContents()
            val readFn = inputFormat.readFn ?: throw IllegalArgumentException("File format not supported for reading")
            val puzzleable = readFn(inputData, date, author, copyright)
            val writeFn =
                outputFormat.writeFn ?: throw IllegalArgumentException("File format not supported for writing")
            val outputData = writeFn(puzzleable, PdfOptions(fontFamily, blackSquareLightnessAdjustment))
            outputFile.writeContents(outputData)
        }
    }
}

sealed interface InputFile {
    fun readContents(): ByteArray

    data object Stdin : InputFile {
        override fun readContents(): ByteArray {
            return generateSequence { readlnOrNull() }.joinToString("\n").encodeToByteArray()
        }
    }

    data class PathFile(val path: Path) : InputFile {
        override fun readContents(): ByteArray {
            return FileSystem.SYSTEM.read(path) {
                readByteArray()
            }
        }
    }

    companion object {
        fun RawOption.inputFile(): NullableOption<InputFile, InputFile> {
            return convert(completionCandidates = CompletionCandidates.Path) { str ->
                if (str == "-") {
                    Stdin
                } else {
                    val path = str.toPath()
                    if (!FileSystem.SYSTEM.exists(path)) {
                        fail("File not found: $path")
                    }
                    PathFile(path)
                }
            }
        }
    }
}

sealed interface OutputFile {
    fun writeContents(data: ByteArray)

    data object Stdout : OutputFile {
        override fun writeContents(data: ByteArray) {
            data.decodeToString().lineSequence().forEach { line ->
                println(line)
            }
        }
    }

    data class PathFile(val path: Path) : OutputFile {
        override fun writeContents(data: ByteArray) {
            FileSystem.SYSTEM.write(path) {
                write(data)
            }
        }
    }

    companion object {
        fun RawOption.outputFile(): NullableOption<OutputFile, OutputFile> {
            return convert(completionCandidates = CompletionCandidates.Path) { str ->
                if (str == "-") {
                    Stdout
                } else {
                    OutputFile.PathFile(str.toPath())
                }
            }
        }
    }
}

fun main(args: Array<String>) = KotwordsCli()
    .subcommands(DumpEntries(), Convert())
    .main(args)
