package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.js.GlobalWorkerOptions
import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.js.getDocument
import com.jeffpdavidson.kotwords.js.newGetViewportParameters
import com.jeffpdavidson.kotwords.js.newPdfRenderParams
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.html.canvas
import kotlinx.html.dom.append
import kotlinx.html.id
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import kotlin.test.assertEquals
import kotlin.test.assertTrue

actual object ImageComparator {
    init {
        // TODO: Is there a better way to bundle this source?
        GlobalWorkerOptions.workerSrc = "/base/build/js/node_modules/pdfjs-dist/legacy/build/pdf.worker.min.js"
    }

    actual suspend fun assertPdfEquals(expected: ByteArray, actual: ByteArray) {
        assertImageEquals(getPdfImageData(expected), getPdfImageData(actual))
    }

    actual suspend fun assertPngEquals(expected: ByteArray, actual: ByteArray) {
        assertImageEquals(getPngImageData(expected), getPngImageData(actual))
    }

    private fun assertImageEquals(expectedImageData: ImageData, actualImageData: ImageData) {
        assertEquals(expectedImageData.width, actualImageData.width)
        assertEquals(expectedImageData.height, actualImageData.height)
        assertEquals(expectedImageData.data.length, actualImageData.data.length)
        (0 until expectedImageData.data.length).forEach { i ->
            assertEquals(expectedImageData.data[i], actualImageData.data[i])
        }
    }

    /** Render the given PDF as an image on an HTML canvas and return the corresponding [ImageData] */
    private suspend fun getPdfImageData(pdfBytes: ByteArray): ImageData = renderImage { canvasElem, context ->
        val pdf = getDocument(Uint8Array(pdfBytes.toArrayBuffer())).promise.await()
        assertEquals(1, pdf.numPages)
        val page = pdf.getPage(1).await()
        val viewport = page.getViewport(newGetViewportParameters(scale = 1.0f))
        assertTrue(viewport.height > 0 && viewport.width > 0)
        canvasElem.height = viewport.height
        canvasElem.width = viewport.width
        page.render(newPdfRenderParams(canvasContext = context, viewport = viewport)).promise.await()
    }

    private suspend fun getPngImageData(imageBytes: ByteArray): ImageData = renderImage { canvasElem, context ->
        ParsedImage.renderToCanvas(ParsedImageFormat.PNG, imageBytes, canvasElem, context)
    }

    private suspend fun renderImage(
        renderFn: suspend (HTMLCanvasElement, CanvasRenderingContext2D) -> Unit
    ): ImageData {
        val canvasId = "image-canvas"
        val canvasElem = window.document.body?.append {
            canvas {
                id = canvasId
            }
        }?.get(0) as HTMLCanvasElement
        val context = canvasElem.getContext("2d") as CanvasRenderingContext2D
        renderFn(canvasElem, context)
        val imageData = context.getImageData(0.0, 0.0, canvasElem.width.toDouble(), canvasElem.height.toDouble())
        canvasElem.remove()
        return imageData
    }
}