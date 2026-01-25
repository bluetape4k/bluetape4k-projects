package io.bluetape4k.workshop.shared

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body

fun WebClient.httpGet(
    uri: String,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    get()
        .uri(uri)
        .accept(accept)
        .retrieve()

fun WebClient.httpHead(
    uri: String,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    head()
        .uri(uri)
        .accept(accept)
        .retrieve()

fun WebClient.httpPost(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
        }
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> WebClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> WebClient.httpPost(
    uri: String,
    publisher: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

fun WebClient.httpPut(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
        }
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> WebClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> WebClient.httpPut(
    uri: String,
    publisher: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

fun WebClient.httpPatch(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
        }
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> WebClient.httpPatch(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> WebClient.httpPatch(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .accept(accept)
        .retrieve()

fun WebClient.httpDelete(
    uri: String,
    accept: MediaType = MediaType.ALL,
): WebClient.ResponseSpec =
    delete()
        .uri(uri)
        .accept(accept)
        .retrieve()
