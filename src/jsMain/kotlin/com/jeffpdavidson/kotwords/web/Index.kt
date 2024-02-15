package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.h5
import kotlinx.html.hr
import kotlinx.html.img
import kotlinx.html.p

@JsExport
object Index {
    private val GENERATORS = listOf(
        "Acrostic",
        "Around the Bend",
        "Coded",
        "Crossword",
        "Crosswordle",
        "Eight Tracks",
        "Hearts and Arrows",
        "Helter Skelter",
        "Jelly Roll",
        "Labyrinth",
        "Marching Bands",
        "Patchwork",
        "Rows Garden",
        "Snake Charmer",
        "Spell Weaving",
        "Spiral",
        "Twists and Turns",
        "Two-Tone"
    )

    fun render() {
        Html.renderPage {
            append {
                p {
                    +"Generate digital, solvable versions of variety puzzles for use in "
                    a {
                        href = "https://mrichards42.github.io/xword/"
                        target = "_blank"
                        +"XWord"
                    }
                    +", the "
                    a {
                        href = "https://www.crosswordnexus.com/solve/"
                        target = "_blank"
                        +"Crossword Nexus solver"
                    }
                    +", "
                    a {
                        href = "https://squares.io"
                        target = "_blank"
                        +"squares.io"
                    }
                    +", and "
                    a {
                        href = "https://www.crosswordsolver.info/"
                        target = "_blank"
                        +"Crossword Solver"
                    }
                    +"."
                }
                GENERATORS.chunked(4).forEach { generatorRow ->
                    div(classes = "row row-cols-1 row-cols-md-4") {
                        generatorRow.forEach { generator ->
                            val id = generator.lowercase().replace(' ', '-')
                            div(classes = "col mb-4") {
                                div(classes = "card text-center") {
                                    div(classes = "card-body") {
                                        img(classes = "mx-auto icon") {
                                            src = "icons/$id.png"
                                            alt = generator
                                        }
                                        h5(classes = "card-title mt-2 mb-0") {
                                            a(classes = "stretched-link") {
                                                href = "$id.html"
                                                +generator
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                hr {}
                p {
                    +"Please use the "
                    a {
                        href = "https://github.com/jpd236/kotwords/issues"
                        target = "_blank"
                        +"issue tracker"
                    }
                    +" to report any problems. Source code available on "
                    a {
                        href = "https://github.com/jpd236/kotwords"
                        target = "_blank"
                        +"GitHub"
                    }
                    +"."
                }
            }
        }
    }
}