package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body

fun WebClient.httpGet(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    get()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun WebClient.httpHead(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    head()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun WebClient.httpPost(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }
        .retrieve()

inline fun <reified T: Any> WebClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .apply { accept?.let { accept(it) } }
        .retrieve()

inline fun <reified T: Any> WebClient.httpPost(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun WebClient.httpPut(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }
        .retrieve()

inline fun <reified T: Any> WebClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .apply { accept?.let { accept(it) } }
        .retrieve()

inline fun <reified T: Any> WebClient.httpPut(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun WebClient.httpPatch(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { bodyValue(it) }
            accept?.let { accept(it) }
        }
        .retrieve()

fun WebClient.httpDelete(
    uri: String,
    accept: MediaType? = null,
): WebClient.ResponseSpec =
    delete()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()
