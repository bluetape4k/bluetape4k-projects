package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.socket.ConnectionSocketFactory
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.core5.http.URIScheme
import org.apache.hc.core5.http.config.Registry
import org.apache.hc.core5.http.config.RegistryBuilder

/**
 * 새로운 [Registry]`<T>` 를 생성합니다.
 *
 * ```
 * val registry = registry<ConnectionSocketFactory> {
 *    register(URIScheme.HTTP.id, PlainConnectionSocketFactory.getSocketFactory())
 *    register(URIScheme.HTTPS.id, SSLConnectionSocketFactory.getSocketFactory())
 *    // ...
 * }
 * ```
 *
 * @param initializer [RegistryBuilder] 를 초기화합니다.
 * @return [Registry]`<T>` 인스턴스
 */
inline fun <T> registry(
    initializer: RegistryBuilder<T>.() -> Unit,
): Registry<T> =
    RegistryBuilder.create<T>().apply(initializer).build()

/**
 * [Registry]`<T>` 를 생성합니다.
 *
 * ```
 * val registry = registryOf(mapOf(
 *    URIScheme.HTTP.id to PlainConnectionSocketFactory.getSocketFactory(),
 *    URIScheme.HTTPS.id to SSLConnectionSocketFactory.getSocketFactory(),
 * ))
 * ```
 *
 * @param items [Registry]`<T>` 에 등록할 아이템들
 * @return [Registry]`<T>` 인스턴스
 */
fun <T> registryOf(items: Map<String, T>): Registry<T> = registry {
    items.forEach { (id, item) ->
        register(id, item)
    }
}

/**
 * 기본 [Registry]`<ConnectionSocketFactory>` 를 생성합니다.
 */
@Deprecated(message = "Deprecated ConnectionSocketFactory")
val defaultSocketFactoryRegistry: Registry<ConnectionSocketFactory> by lazy {
    RegistryBuilder.create<ConnectionSocketFactory>()
        .register(URIScheme.HTTP.id, PlainConnectionSocketFactory.getSocketFactory())
        .register(URIScheme.HTTPS.id, SSLConnectionSocketFactory.getSocketFactory())
        .build()
}

/**
 * [ConnectionSocketFactory] 를 등록한 [Registry]`<ConnectionSocketFactory>` 를 생성합니다.
 *
 * ```
 * val registry = registryOfConnectionSocketFactory()
 * ```
 *
 * @param plain [PlainConnectionSocketFactory] 를 등록합니다.
 * @param ssl [SSLConnectionSocketFactory] 를 등록합니다.
 * @return [Registry]`<ConnectionSocketFactory>` 인스턴스
 */
@Deprecated(message = "Deprecated ConnectionSocketFactory")
fun registryOfConnectionSocketFactory(
    plain: ConnectionSocketFactory = PlainConnectionSocketFactory.getSocketFactory(),
    ssl: ConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory(),
): Registry<ConnectionSocketFactory> = registry {
    register(URIScheme.HTTP.id, plain)
    register(URIScheme.HTTPS.id, ssl)
}
