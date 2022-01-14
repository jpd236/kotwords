package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.js.Interop
import kotlinx.browser.document
import okio.ByteString.Companion.toByteString
import okio.Closeable
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal actual class ParsedImage private constructor(
    private val canvas: HTMLCanvasElement,
    private val xOffset: Int,
    private val yOffset: Int,
    actual val width: Int,
    actual val height: Int,
) : Closeable {
    private val context = canvas.getContext("2d") as CanvasRenderingContext2D

    actual fun containsVisiblePixels(): Boolean {
        val imageData =
            context.getImageData(xOffset.toDouble(), yOffset.toDouble(), width.toDouble(), height.toDouble()).data
        return (3 until imageData.length step 4).any {
            imageData[it] != 0.toByte()
        }
    }

    actual fun crop(width: Int, height: Int, x: Int, y: Int): ParsedImage {
        return ParsedImage(canvas, xOffset + x, yOffset + y, width, height)
    }

    actual suspend fun toPngBytes(): ByteArray {
        val imageData =
            context.getImageData(xOffset.toDouble(), yOffset.toDouble(), width.toDouble(), height.toDouble())
        val croppedCanvas = document.createElement("canvas") as HTMLCanvasElement
        croppedCanvas.width = width
        croppedCanvas.height = height
        val croppedContext = croppedCanvas.getContext("2d") as CanvasRenderingContext2D
        croppedContext.putImageData(imageData, 0.0, 0.0)
        val blob = suspendCoroutine<Blob> { continuation ->
            croppedCanvas.toBlob({ blob ->
                continuation.resume(blob!!)
            }, "image/png")
        }
        croppedCanvas.remove()
        return Interop.readBlob(blob)
    }

    override fun close() {
        canvas.remove()
    }

    actual companion object {
        actual suspend fun parse(format: ParsedImageFormat, data: ByteArray): ParsedImage {
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            val context = canvas.getContext("2d") as CanvasRenderingContext2D
            renderToCanvas(format, data, canvas, context)
            return ParsedImage(canvas, xOffset = 0, yOffset = 0, width = canvas.width, height = canvas.height)
        }

        internal suspend fun renderToCanvas(
            format: ParsedImageFormat, data: ByteArray, canvas: HTMLCanvasElement, context: CanvasRenderingContext2D
        ) {
            return suspendCoroutine { continuation ->
                val image = Image()
                image.onload = {
                    canvas.width = image.naturalWidth
                    canvas.height = image.naturalHeight
                    context.drawImage(image, 0.0, 0.0)
                    continuation.resume(Unit)
                }
                val formatStr = when (format) {
                    ParsedImageFormat.GIF -> "gif"
                    ParsedImageFormat.JPG -> "jpeg"
                    ParsedImageFormat.PNG -> "png"
                }
                image.src = "data:image/$formatStr;base64,${data.toByteString().base64()}"
            }
        }
    }
}