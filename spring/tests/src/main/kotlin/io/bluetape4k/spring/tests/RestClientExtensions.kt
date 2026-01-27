package io.bluetape4k.spring.tests

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

fun RestClient.httpGet(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    get()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun RestClient.httpHead(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    head()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun RestClient.httpPost(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
            accept?.let { accept(it) }
        }
        .retrieve()

inline fun <reified T: Any> RestClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .apply { accept?.let { accept(it) } }
        .retrieve()

inline fun <reified T: Any> RestClient.httpPost(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun RestClient.httpPut(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
            accept?.let { accept(it) }
        }
        .retrieve()

inline fun <reified T: Any> RestClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .apply { accept?.let { accept(it) } }
        .retrieve()

inline fun <reified T: Any> RestClient.httpPut(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .apply { accept?.let { accept(it) } }
        .retrieve()

fun RestClient.httpPatch(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
            accept?.let { accept(it) }
        }
        .retrieve()

fun RestClient.httpDelete(
    uri: String,
    accept: MediaType? = null,
): RestClient.ResponseSpec =
    delete()
        .uri(uri)
        .apply { accept?.let { accept(it) } }
        .retrieve()
