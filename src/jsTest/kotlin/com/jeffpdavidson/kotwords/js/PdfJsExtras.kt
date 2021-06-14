package com.jeffpdavidson.kotwords.js

import org.w3c.dom.CanvasRenderingContext2D

internal fun newPdfRenderParams(
    canvasContext: CanvasRenderingContext2D,
    viewport: PDFPageViewport,
): PDFRenderParams {
    val options = js("{}").unsafeCast<PDFRenderParams>()
    options.canvasContext = canvasContext
    options.viewport = viewport
    return options
}

internal fun newGetViewportParameters(
    scale: Float
): GetViewportParameters {
    val options = js("{}").unsafeCast<GetViewportParameters>()
    options.scale = scale
    return options
}