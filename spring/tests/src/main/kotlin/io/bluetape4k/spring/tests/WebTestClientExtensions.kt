package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body

fun WebTestClient.httpGet(
    uri: String,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    get()
        .uri(uri)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpHead(
    uri: String,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    head()
        .uri(uri)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpPost(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .body(publisher)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPost(
    uri: String,
    flow: Flow<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    post()
        .uri(uri)
        .body(flow)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpPut(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .body(publisher)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

inline fun <reified T: Any> WebTestClient.httpPut(
    uri: String,
    flow: Flow<T>,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    put()
        .uri(uri)
        .body(flow)
        .exchange()
        .expectStatus().isEqualTo(httpStatus)


fun WebTestClient.httpPatch(
    uri: String,
    value: Any? = null,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .expectStatus().isEqualTo(httpStatus)

fun WebTestClient.httpDelete(
    uri: String,
    httpStatus: HttpStatus = HttpStatus.OK,
): WebTestClient.ResponseSpec =
    delete()
        .uri(uri)
        .exchange()
        .expectStatus().isEqualTo(httpStatus) 
