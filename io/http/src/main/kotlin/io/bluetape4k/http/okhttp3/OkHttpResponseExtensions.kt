package io.bluetape4k.http.okhttp3

import java.io.InputStream

/** Response Body를 [InputStream]으로 변환합니다. */
fun okhttp3.Response?.bodyAsInputStream(): InputStream? = this?.body?.byteStream()

/** Response Body를 [ByteArray]로 변환합니다. */
fun okhttp3.Response?.bodyAsByteArray(): ByteArray? = this?.body?.bytes()

/** Response Body를 [String]으로 변환합니다. */
fun okhttp3.Response?.bodyAsString(): String? = this?.body?.string()

/** [okhttp3.Response]의 핵심 정보를 표준 출력으로 출력합니다. */
fun okhttp3.Response.print(no: Int = 1) {
    println("Response[$no]: ${this.code} ${this.message}")
    println("Headers[$no]: ${this.headers}")
    println("Cache Response[$no]: ${this.cacheResponse}")
    println("Network Response[$no]: ${this.networkResponse}")
}

/** [okhttp3.MediaType]을 `type/subtype` 문자열로 변환합니다. */
fun okhttp3.MediaType.toTypeString(): String = "${this.type}/${this.subtype}"
