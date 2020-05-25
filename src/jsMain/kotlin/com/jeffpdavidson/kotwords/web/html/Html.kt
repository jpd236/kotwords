package com.jeffpdavidson.kotwords.web.html

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.Blob
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.browser.document
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.dom.addClass

private external class navigator {
    companion object {
        fun msSaveBlob(blob: Blob, defaultName: String): Boolean
    }
}

object Html {
    @Suppress("UNCHECKED_CAST")
    fun <T : HTMLElement> getElementById(id: String): Lazy<T> {
        return lazy {
            document.getElementById(id) as T
        }
    }

    fun renderPage(renderFn: HTMLDivElement.() -> Unit) {
        val body = document.getElementById("body") as HTMLDivElement
        body.renderFn()
        val loadingContainer = document.getElementById("loading-container") as HTMLDivElement
        loadingContainer.addClass("d-none")
    }

    fun getSelectedFile(fileInput: HTMLInputElement): File {
        if (fileInput.files?.length == 0 || fileInput.files!![0] == null) {
            throw IllegalArgumentException("No file selected")
        }
        return fileInput.files!![0]!!
    }

    fun downloadBlob(fileName: String, blob: Blob) {
        val hasMsSaveBlob = js("typeof navigator.msSaveBlob === \"function\"") as Boolean
        if (hasMsSaveBlob) {
            navigator.msSaveBlob(blob, fileName)
        } else {
            val dataUrlPromise = GlobalScope.promise {
                val reader = FileReader()
                suspendCoroutine<String> { cont ->
                    reader.onload = { event ->
                        cont.resume(event.target.asDynamic().result)
                    }
                    reader.readAsDataURL(blob)
                }
            }
            dataUrlPromise.then {
                val link = document.createElement("a") as HTMLAnchorElement
                link.download = fileName
                link.href = it
                link.click()
            }
        }
    }
}