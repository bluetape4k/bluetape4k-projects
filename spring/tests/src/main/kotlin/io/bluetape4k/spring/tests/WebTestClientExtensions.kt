package io.bluetape4k.spring.tests

import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.httpGet(uri: String, httpStatus: HttpStatus = HttpStatus.OK) =
    get()
        .uri(uri)
        .exchange()
        .apply { expectStatus().isEqualTo(httpStatus) }

fun WebTestClient.httpHead(uri: String, httpStatus: HttpStatus = HttpStatus.OK) =
    head()
        .uri(uri)
        .exchange()
        .apply { expectStatus().isEqualTo(httpStatus) }

fun WebTestClient.httpPost(uri: String, value: Any? = null, httpStatus: HttpStatus = HttpStatus.OK) =
    post()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .apply { expectStatus().isEqualTo(httpStatus) }

fun WebTestClient.httpPut(uri: String, value: Any? = null, httpStatus: HttpStatus = HttpStatus.OK) =
    put()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .apply { expectStatus().isEqualTo(httpStatus) }

fun WebTestClient.httpPatch(uri: String, value: Any? = null, httpStatus: HttpStatus = HttpStatus.OK) =
    patch()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .apply { expectStatus().isEqualTo(httpStatus) }

fun WebTestClient.httpDelete(uri: String, httpStatus: HttpStatus = HttpStatus.OK) =
    delete()
        .uri(uri)
        .exchange()
        .apply { expectStatus().isEqualTo(httpStatus) }
