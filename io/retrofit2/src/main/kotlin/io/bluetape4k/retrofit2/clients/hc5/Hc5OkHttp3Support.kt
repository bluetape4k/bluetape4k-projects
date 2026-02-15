package io.bluetape4k.retrofit2.clients.hc5

import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.okhttp3.okhttp3Response
import io.bluetape4k.http.okhttp3.toTypeString
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8String
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpVersion
import org.apache.hc.core5.http.Method

private val log by lazy { KotlinLogging.logger {} }

/**
 * [okhttp3.Request] 를 HC5의 [SimpleHttpRequest] 로 변환합니다.
 */
internal fun okhttp3.Request.toSimpleHttpRequest(): SimpleHttpRequest {
    val self = this@toSimpleHttpRequest

    val method = Method.normalizedValueOf(self.method)

    val simpleRequest = simpleHttpRequest(method) {
        setHttpHost(HttpHost(self.url.scheme, self.url.host, self.url.port))
        val encodedQuery = self.url.encodedQuery
        if (encodedQuery != null) {
            setPath(self.url.encodedPath + "?" + encodedQuery)
        } else {
            setPath(self.url.encodedPath)
        }
        setVersion(HttpVersion.HTTP_1_1)
    }

    // Add Headers
    simpleRequest.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate")

    self.headers.forEach { (name, value) ->
        log.trace { "Add header. $name=$value" }
        simpleRequest.setHeader(name, value)
    }

    self.body?.let { body: okhttp3.RequestBody ->
        if (body.contentLength() > 0) {
            val contentType = body.contentType()
                ?.let { ContentType.create(it.toTypeString(), it.charset(Charsets.UTF_8)) }
                ?: ContentType.APPLICATION_JSON

            val buffer = okio.Buffer()
            body.writeTo(buffer)
            simpleRequest.setBody(buffer.readByteArray(), contentType)
            log.trace { "Request body=${simpleRequest.bodyBytes?.toUtf8String()}" }
        }
    }

    return simpleRequest
}

/**
 * Retrofit2 연동 타입 변환을 위한 `toOkHttp3Response` 함수를 제공합니다.
 */
internal fun SimpleHttpResponse.toOkHttp3Response(
    okRequest: okhttp3.Request,
): okhttp3.Response {
    log.trace { "Convert HC5 SimpleHttpResponse to okhttp3.Response." }
    val self: SimpleHttpResponse = this@toOkHttp3Response

    return okhttp3Response {
        // okhttp3 의 protocol 중 HTTP_2 는 "http/2.0"이 아니라 "h2"로 표현되어 있어서 이런 식으로 바꿔줘야 한다.
        val protocol = self.version.format().lowercase()
        if (protocol == "http/2.0") {
            protocol(Protocol.HTTP_2)
        } else {
            protocol(Protocol.get(self.version.format().lowercase()))
        }
        request(okRequest)
        code(self.code)
        message(self.reasonPhrase)

        // Header 추가
        self.headers.forEach { header ->
            addHeader(header.name, header.value)
        }

        // Content가 압축되어 있을 경우 압축을 해제합니다. (다른 놈들은 기본 제공하는 기능인데 ...)
        val bytes = self.bodyPlainBytes()
        val mediaType = self.getOkhttp3MediaType()

        body(bytes.toResponseBody(mediaType))
    }
}

/**
 * Retrofit2 연동에서 `bodyPlainBytes` 함수를 제공합니다.
 */
internal fun SimpleHttpResponse.bodyPlainBytes(): ByteArray {
    val self: SimpleHttpResponse = this@bodyPlainBytes

    // Content가 압축되어 있을 경우 압축을 해제합니다. (다른 놈들은 기본 제공하는 기능인데 ...)
    val encodings = self.getHeader(HttpHeaders.CONTENT_ENCODING)
        ?.value
        .orEmpty()
        .split(',')
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .toSet()

    val rawBytes = self.bodyBytes ?: return emptyByteArray
    return when {
        "gzip" in encodings || "x-gzip" in encodings -> runCatching {
            log.trace { "Decompress gzip bytes." }
            Compressors.GZip.decompress(rawBytes)
        }.getOrElse {
            log.warn(it) { "Fail to decompress gzip response. fallback to raw bytes." }
            rawBytes.copyOf()
        }
        "deflate" in encodings                       -> runCatching {
            log.trace { "Decompress deflate bytes." }
            Compressors.Deflate.decompress(rawBytes)
        }.getOrElse {
            log.warn(it) { "Fail to decompress deflate response. fallback to raw bytes." }
            rawBytes.copyOf()
        }
        else                                         -> rawBytes.copyOf()
    }
}

/**
 * Retrofit2 연동에서 `getOkhttp3MediaType` 함수를 제공합니다.
 */
internal fun SimpleHttpResponse.getOkhttp3MediaType(): MediaType? {
    val self: SimpleHttpResponse = this@getOkhttp3MediaType

    val mimeType = self.contentType?.mimeType
    return mimeType?.toMediaTypeOrNull()
}
