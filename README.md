# kotwords

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/jpd236/kotwords/gradle-build.yaml?branch=master)](https://github.com/jpd236/kotwords/actions/workflows/gradle-build.yaml)
[![Maven Central](https://img.shields.io/maven-central/v/com.jeffpdavidson.kotwords/kotwords)](https://search.maven.org/artifact/com.jeffpdavidson.kotwords/kotwords)
[![javadoc](https://javadoc.io/badge2/com.jeffpdavidson.kotwords/kotwords/javadoc.svg)](https://javadoc.io/doc/com.jeffpdavidson.kotwords/kotwords)

Collection of crossword puzzle file format converters and other utilities, written in Kotlin.

The library uses Kotlin multiplatform and supports Java, Javascript, and native Mac/Windows/Linux.

This project also includes a [web interface](https://jpd236.github.io/kotwords/) for generating digital versions of
different variety puzzles and a command line tool to perform conversions and dump puzzle
information.

## Features

* Read crosswords in a variety of file formats, including formats used by online puzzle sites
* Remove a direction of clues to make puzzles more challenging
* Write crosswords in .puz, .jpz, or .ipuz format
* Generate PDFs for crosswords
* Create .jpz and .ipuz files for variety crossword puzzles

## How To Use

In your `build.gradle`:

```groovy
dependencies {
  implementation "com.jeffpdavidson.kotwords:kotwords:1.4.14"
}
```

Sample code for parsing a JPZ file and converting it to a PDF with only Down clues:

```kotlin
  val pdf = JpzFile(jpz).asPuzzle().withDownsOnly().asPdf();
```

See the [Javadoc](https://javadoc.io/doc/com.jeffpdavidson.kotwords/kotwords) for full API details.

## Command-line interface

[Release builds](https://github.com/jpd236/kotwords/releases) include Mac/Windows/Linux command-line tools.

Example to convert a PUZ file to a PDF:

```shell
kotwords-cli convert \
    --input-file=/path/to/puzzle.puz
    --input-format=PUZ
    --output-file=/path/to/puzzle.pdf
    --output-format=PDF
```

Example to print all clues and answers in a JPZ file:

```shell
kotwords-cli dump-entries
    --file=/path/to/puzzle.jpz
    --output-format="[[clue]]: [[answer]]"
```

Run the tool with no flags to see all available commands, and add --help after any command to see
the available options.

## Development

Kotwords is a standard Gradle project that can be imported into IntelliJ. Common commands include:

* Run all tests for all environments: `./gradlew check`
* Run a local instance of the web tools: `./gradlew jsBrowserDevelopmentRun`
* Publish to the local Maven repository for testing in another application: `./gradlew publishToMavenLocal`

Bug reports and contributions welcome!
