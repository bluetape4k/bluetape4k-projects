package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body


fun WebTestClient.httpGet(
    uri: String,
    httpStatus: HttpStatus = HttpStatus.OK,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    get()
        .uri(uri)
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpHead(
    uri: String,
    httpStatus: HttpStatus = HttpStatus.OK,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    head()
        .uri(uri)
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpPost(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
        }
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPost(
    uri: String,
    flow: Flow<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpPut(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
        }
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPut(
    uri: String,
    flow: Flow<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)


fun WebTestClient.httpPatch(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus = HttpStatus.OK,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebTestClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.run { bodyValue(this) }
        }
        .accept(accept)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpDelete(
    uri: String,
    httpStatus: HttpStatus = HttpStatus.OK,
    vararg accepts: MediaType = arrayOf(MediaType.ALL),
): WebTestClient.ResponseSpec =
    delete()
        .uri(uri)
        .accept(*accepts)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)
