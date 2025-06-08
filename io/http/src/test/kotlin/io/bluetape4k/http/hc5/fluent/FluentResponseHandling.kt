package io.bluetape4k.http.hc5.fluent

import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.io.toString
import io.bluetape4k.io.toUtf8String
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import jakarta.json.JsonException
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.ClientProtocolException
import org.apache.hc.client5.http.HttpResponseException
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpStatus
import org.junit.jupiter.api.Test
import javax.xml.parsers.ParserConfigurationException

/**
 * This example demonstrates how the HttpClient fluent API can be used to handle HTTP responses
 * without buffering content body in memory.
 */
class FluentResponseHandling: AbstractHc5Test() {

    companion object: KLogging()

    private val mapper = Jackson.defaultJsonMapper

    @Test
    fun `handle HTTP response without buffering content body in memory with Jackson`() {
        val node = requestGet("$httpbinBaseUrl/get")
            .execute()
            .handleResponse { response ->
                val status = response.code
                if (status >= HttpStatus.SC_REDIRECTION) {
                    throw HttpResponseException(status, response.reasonPhrase)
                }
                val entity = response.entity ?: throw ClientProtocolException("Response contains no content")

                try {
                    val contentType = ContentType.parseLenient(entity.contentType)
                    if (contentType.equals(ContentType.APPLICATION_JSON)) {
                        mapper.readValue<JsonNode>(entity.content)
                    } else {
                        val charset = contentType.charset ?: Charsets.UTF_8
                        mapper.readValue(entity.content.toString(charset))
                    }
                } catch (ex: ParserConfigurationException) {
                    throw IllegalStateException(ex)
                } catch (ex: JsonException) {
                    throw ClientProtocolException("Malformed JSON document", ex)
                }
            }

        log.debug { "json node=\n${node.toPrettyString()}" }
        node["headers"]["Host"].textValue() shouldBeEqualTo "${httpbinServer.host}:${httpbinServer.port}"
    }

    @Test
    fun `handle HTTP response without buffering content body in memory with Fastjson2`() {
        val node = requestGet("$httpbinBaseUrl/get")
            .execute()
            .handleResponse { response ->
                val status = response.code
                if (status >= HttpStatus.SC_REDIRECTION) {
                    throw HttpResponseException(status, response.reasonPhrase)
                }
                val entity = response.entity ?: throw ClientProtocolException("Response contains no content")

                try {
                    val contentType = ContentType.parseLenient(entity.contentType)
                    if (contentType.equals(ContentType.APPLICATION_JSON)) {
                        JSON.parseObject(entity.content.toUtf8String())
                    } else {
                        val charset = contentType.charset ?: Charsets.UTF_8
                        JSON.parseObject(entity.content.toString(charset))
                    }
                } catch (ex: ParserConfigurationException) {
                    throw IllegalStateException(ex)
                } catch (ex: JsonException) {
                    throw ClientProtocolException("Malformed JSON document", ex)
                }
            }

        log.debug { "json node=\n${node.toJSONString()}" }
    }
}
