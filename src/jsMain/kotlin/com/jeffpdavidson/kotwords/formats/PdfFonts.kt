package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.js.Http
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PdfFonts {
    private val initMutex = Mutex()
    private var NOTO_FONT_FAMILY: PdfFontFamily? = null

    suspend fun getNotoFontFamily(): PdfFontFamily {
        initMutex.withLock {
            if (NOTO_FONT_FAMILY == null) {
                NOTO_FONT_FAMILY = PdfFontFamily(
                    PdfFont.TtfFont("NotoSerif", "normal", Http.getBinary("fonts/NotoSerif-Regular.ttf")),
                    PdfFont.TtfFont("NotoSerif", "bold", Http.getBinary("fonts/NotoSerif-Bold.ttf")),
                    PdfFont.TtfFont("NotoSerif", "italic", Http.getBinary("fonts/NotoSerif-Italic.ttf")),
                    PdfFont.TtfFont("NotoSerif", "bolditalic", Http.getBinary("fonts/NotoSerif-BoldItalic.ttf")),
                )
            }
        }
        return NOTO_FONT_FAMILY!!
    }
}