package io.bluetape4k.io

import java.net.URL
import java.net.URLConnection

/**
 * URL의 컨텐츠를 다운로드 받아 ByteArray로 읽어옵니다.
 *
 * ```
 * val url = URL("https://www.google.com")
 * val bytes = url.toByteArray()
 * ```
 *
 * @receiver URL 읽을 URL
 * @return ByteArray 읽은 바이트 배열
 */
fun URL.toByteArray(): ByteArray = openStream().use { it.toByteArray() }

/**
 * [URLConnection]의 컨텐츠 정보를 다운로드 받아 ByteArray로 읽어옵니다.
 *
 * ```
 * val url = URL("https://www.google.com")
 * val connection = url.openConnection()
 * val bytes = connection.toByteArray()
 * ```
 *
 * @receiver URLConnection 읽을 URLConnection
 * @return ByteArray 읽은 바이트 배열
 */
fun URLConnection.toByteArray(): ByteArray = getInputStream().use { it.toByteArray() }
