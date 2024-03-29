package com.jeffpdavidson.kotwords.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.Apz
import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.Puzzleable
import com.jeffpdavidson.kotwords.formats.Rgz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

enum class Format(
    val extensions: List<String>,
    val readFn: suspend (ByteArray) -> Puzzleable,
) {
    APZ(listOf("apz"), { data -> Apz.fromXmlString(data.decodeToString()).toAcrostic() }),
    IPUZ(listOf("ipuz"), { data -> Ipuz(data.decodeToString()) }),
    JPZ(listOf("jpz", "xml"), { data -> JpzFile(data) }),
    PUZ(listOf("puz"), { data -> AcrossLite(data) }),
    RGZ(listOf("rg", "rgz"), { data -> Rgz.fromRgzFile(data) });

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
            require (file.name.contains('.')) {
                "Puzzle format cannot be inferred; please provide it using --format."
            }
            val extension = file.name.substringAfterLast('.')
            Format.FORMATS_BY_EXTENSION[extension] ?:
                throw IllegalArgumentException("Unsupported puzzle format: $extension")
        }()
        runBlocking {
            val data = FileSystem.SYSTEM.read(file) {
                readByteArray()
            }
            val puzzle = resolvedFormat.readFn(data).asPuzzle()
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

fun main(args: Array<String>) = KotwordsCli()
    .subcommands(DumpEntries())
    .main(args)
