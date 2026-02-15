package io.bluetape4k.retrofit2.services

import java.io.Serializable

/** `httpbin.org/get` 혹은 `/headers` 등이 반환하는 공통 응답입니다. */
data class HttpbinGetResponse(
    val args: Map<String, String>,
    val headers: Map<String, String>,
    val origin: String,
    val url: String,
): Serializable

/** `httpbin.org/post` 와 유사하게 본문/폼/파일 정보를 함께 받는 응답 구조입니다. */
data class HttpbinPostResponse(
    val args: Map<String, String>,
    val data: String,
    val files: Map<String, String>,
    val form: Map<String, String>,
    val headers: Map<String, String>,
    val json: Map<String, Any>?,
    val origin: String,
    val url: String,
): Serializable

/** `/anything`, `/put`, `/patch` 처럼 메소드와 본문 정보를 포함하는 범용 응답입니다. */
data class HttpbinAnythingResponse(
    val args: Map<String, String>,
    val data: String,
    val files: Map<String, String>,
    val form: Map<String, String>,
    val headers: Map<String, String>,
    val json: Map<String, Any>?,
    val method: String,
    val origin: String,
    val url: String,
): Serializable

/** `/cookies` 계열 엔드포인트가 반환하는 쿠키 맵입니다. */
data class HttpBinCookiesResponse(
    val cookies: Map<String, String>,
): Serializable

/** `/uuid`와 같이 단일 식별자를 받는 응답입니다. */
data class HttpbinUuidResponse(
    val uuid: String,
): Serializable
