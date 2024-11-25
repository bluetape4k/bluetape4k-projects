package io.bluetape4k.spring.tests

import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.httpGet(uri: String) =
    get()
        .uri(uri)
        .exchange()
        .expectStatus().is2xxSuccessful

fun WebTestClient.httpHead(uri: String) =
    head()
        .uri(uri)
        .exchange()
        .expectStatus().is2xxSuccessful

fun WebTestClient.httpPost(uri: String, value: Any? = null) =
    post()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .expectStatus().is2xxSuccessful

fun WebTestClient.httpPut(uri: String, value: Any? = null) =
    put()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .expectStatus().is2xxSuccessful

fun WebTestClient.httpPatch(uri: String, value: Any? = null) =
    patch()
        .uri(uri)
        .apply { value?.run { bodyValue(this) } }
        .exchange()
        .expectStatus().is2xxSuccessful

fun WebTestClient.httpDelete(uri: String) =
    delete()
        .uri(uri)
        .exchange()
        .expectStatus().is2xxSuccessful
