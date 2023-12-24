@file:JsModule("pdfjs-dist/legacy/build/pdf.min.js")
@file:JsNonModule

package com.jeffpdavidson.kotwords.js

import org.khronos.webgl.Uint8Array
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.js.Promise

internal external class GlobalWorkerOptions {
    companion object {
        var workerSrc: String
    }
}

internal external fun getDocument(src: Uint8Array): PDFDocumentLoadingTask

internal external interface PDFDocumentLoadingTask {
    val promise: Promise<PDFDocumentProxy>
}

internal external interface PDFDocumentProxy {
    val numPages: Int
    fun getPage(page: Int): Promise<PDFPageProxy>
}

internal external interface PDFPageProxy {
    fun render(params: PDFRenderParams): PDFDocumentLoadingTask
    fun getViewport(params: GetViewportParameters): PDFPageViewport
}

internal external interface PDFRenderParams {
    var canvasContext: CanvasRenderingContext2D
    var viewport: PDFPageViewport
}

internal external interface PDFPageViewport {
    val width: Int
    val height: Int
}

internal external interface GetViewportParameters {
    var scale: Float
}