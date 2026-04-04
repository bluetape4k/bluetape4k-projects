package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.ConnectExceptionSupport
import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.HttpHostConnectException
import org.apache.hc.core5.net.NamedEndpoint
import java.io.IOException
import java.net.InetAddress

/**
 * [IOException]을 [ConnectTimeoutException]으로 변환합니다.
 *
 * ```kotlin
 * try {
 *     connectToRemote()
 * } catch (e: IOException) {
 *     val endpoint = URIAuthority("example.com", 443)
 *     throw e.toConnectTimeoutException(endpoint)
 *     // ConnectTimeoutException이 발생합니다.
 * }
 * ```
 *
 * @param namedEndpoint [NamedEndpoint]
 * @param remoteAddresses [InetAddress]
 * @return [ConnectTimeoutException]
 */
fun IOException.toConnectTimeoutException(
    namedEndpoint: NamedEndpoint,
    vararg remoteAddresses: InetAddress,
): ConnectTimeoutException =
    ConnectExceptionSupport.createConnectTimeoutException(this, namedEndpoint, *remoteAddresses)

/**
 * [IOException]을 [HttpHostConnectException]으로 변환합니다.
 *
 * ```kotlin
 * try {
 *     connectToRemote()
 * } catch (e: IOException) {
 *     val endpoint = URIAuthority("example.com", 443)
 *     throw e.toHttpHostConnectException(endpoint)
 *     // HttpHostConnectException이 발생합니다.
 * }
 * ```
 *
 * @param namedEndpoint [NamedEndpoint]
 * @param remoteAddresses [InetAddress]
 * @return [HttpHostConnectException]
 */
fun IOException.toHttpHostConnectException(
    namedEndpoint: NamedEndpoint,
    vararg remoteAddresses: InetAddress,
): HttpHostConnectException =
    ConnectExceptionSupport.createHttpHostConnectException(this, namedEndpoint, *remoteAddresses)

/**
 * [IOException]에 [NamedEndpoint]와 [InetAddress] 정보를 추가해 보강합니다.
 *
 * ```kotlin
 * try {
 *     connectToRemote()
 * } catch (e: IOException) {
 *     val endpoint = URIAuthority("example.com", 443)
 *     throw e.enhance(endpoint)
 *     // 엔드포인트 정보가 포함된 IOException이 발생합니다.
 * }
 * ```
 *
 * @param namedEndpoint [NamedEndpoint]
 * @param remoteAddresses [InetAddress]
 * @return [IOException]
 */
fun IOException.enhance(
    namedEndpoint: NamedEndpoint,
    vararg remoteAddresses: InetAddress,
): IOException =
    ConnectExceptionSupport.enhance(this, namedEndpoint, *remoteAddresses)
