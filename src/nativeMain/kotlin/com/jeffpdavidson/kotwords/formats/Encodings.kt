package com.jeffpdavidson.kotwords.formats

import com.fleeksoft.ksoup.parser.Parser
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import okio.Buffer
import okio.deflate

internal actual object Encodings {
    // TODO: Explore using platform.iconv* for CP1252 conversion.
    actual fun decodeCp1252(bytes: ByteArray): String {
        return commonDecodeCp1252(bytes)
    }

    actual fun encodeCp1252(string: String): ByteArray {
        return commonEncodeCp1252(string)
    }

    actual fun decodeUrl(url: String): String {
        return UrlEncoderUtil.decode(url)
    }

    actual fun decodeHtmlEntities(string: String): String {
        return Parser.unescapeEntities(string, inAttribute = false)
    }

    actual fun unescape(string: String): String {
        return commonUnescape(string)
    }

    actual fun deflate(bytes: ByteArray): ByteArray {
        val source = Buffer()
        source.write(bytes)

        val sink = Buffer()
        val deflaterSink = sink.deflate()
        deflaterSink.write(source, source.size)
        deflaterSink.close()
        return sink.readByteArray()
    }
}