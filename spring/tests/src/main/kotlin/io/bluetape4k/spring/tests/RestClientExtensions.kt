package io.bluetape4k.workshop.shared

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

fun RestClient.httpGet(
    uri: String,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    get()
        .uri(uri)
        .accept(accept)
        .retrieve()

fun RestClient.httpHead(
    uri: String,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    head()
        .uri(uri)
        .accept(accept)
        .retrieve()

fun RestClient.httpPost(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
        }
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> RestClient.httpPost(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> RestClient.httpPost(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    post()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .accept(accept)
        .retrieve()

fun RestClient.httpPut(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
        }
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> RestClient.httpPut(
    uri: String,
    publisher: Publisher<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(publisher)
        .accept(accept)
        .retrieve()

inline fun <reified T: Any> RestClient.httpPut(
    uri: String,
    flow: Flow<T>,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    put()
        .uri(uri)
        .apply { contentType?.let { contentType(it) } }
        .body(flow)
        .accept(accept)
        .retrieve()

fun RestClient.httpPatch(
    uri: String,
    value: Any? = null,
    contentType: MediaType? = null,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    patch()
        .uri(uri)
        .apply {
            contentType?.let { contentType(it) }
            value?.let { body(it) }
        }
        .accept(accept)
        .retrieve()

fun RestClient.httpDelete(
    uri: String,
    accept: MediaType = MediaType.ALL,
): RestClient.ResponseSpec =
    delete()
        .uri(uri)
        .accept(accept)
        .retrieve()
