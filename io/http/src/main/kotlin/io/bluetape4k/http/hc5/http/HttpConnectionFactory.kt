package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.impl.io.DefaultHttpResponseParserFactory
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory
import org.apache.hc.client5.http.io.ManagedHttpClientConnection
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.config.CharCodingConfig
import org.apache.hc.core5.http.config.Http1Config
import org.apache.hc.core5.http.impl.io.DefaultHttpRequestWriterFactory
import org.apache.hc.core5.http.io.HttpConnectionFactory
import org.apache.hc.core5.http.io.HttpMessageParserFactory
import org.apache.hc.core5.http.io.HttpMessageWriterFactory

/**
 * [ManagedHttpClientConnectionFactory] 를 생성합니다.
 *
 * ```
 * val factory = managedHttpConnectionFactory {
 *     setBufferSize(8192)
 *     setFragmentSizeHint(8192)
 *     setMaxHeaderCount(200)
 *     setMaxLineLength(2000)
 * }
 * ```
 *
 * @param builder [ManagedHttpClientConnectionFactory.Builder] 초기화 람다
 * @return [HttpConnectionFactory]`<ManagedHttpClientConnection>` 인스턴스
 */
inline fun managedHttpConnectionFactory(
    @BuilderInference builder: ManagedHttpClientConnectionFactory.Builder.() -> Unit,
): HttpConnectionFactory<ManagedHttpClientConnection> =
    ManagedHttpClientConnectionFactory.builder().apply(builder).build()


/**
 * [ManagedHttpClientConnectionFactory] 를 생성합니다.
 *
 * ```
 * val http1Config = Http1Config.custom()
 *    .setBufferSize(8192)
 *    .build()
 * val charCodingConfig = CharCodingConfig.custom().build()
 *
 * val factory = managedHttpConnectionFactoryOf(http1Config, charCodingConfig) {
 *    setBufferSize(8192)
 *    setFragmentSizeHint(8192)
 * }
 * val connection = factory.create()
 * ```
 *
 * @param http1Config [Http1Config] 설정
 * @param charCodingConfig [CharCodingConfig] 설정
 * @param requestWriterFactory [HttpMessageWriterFactory] 인스턴스 (기본: [DefaultHttpRequestWriterFactory.INSTANCE])
 * @param responseParserFactory [HttpMessageParserFactory] 인스턴스 (기본: [DefaultHttpResponseParserFactory.INSTANCE])
 * @param builder [ManagedHttpClientConnectionFactory.Builder] 초기화 람다
 * @return [HttpConnectionFactory]`<ManagedHttpClientConnection>` 인스턴스
 */
inline fun managedHttpConnectionFactoryOf(
    http1Config: Http1Config = Http1Config.DEFAULT,
    charCodingConfig: CharCodingConfig = CharCodingConfig.DEFAULT,
    requestWriterFactory: HttpMessageWriterFactory<ClassicHttpRequest> = DefaultHttpRequestWriterFactory.INSTANCE,
    responseParserFactory: HttpMessageParserFactory<ClassicHttpResponse> = DefaultHttpResponseParserFactory.INSTANCE,
    @BuilderInference builder: ManagedHttpClientConnectionFactory.Builder.() -> Unit = {},
): HttpConnectionFactory<ManagedHttpClientConnection> =
    managedHttpConnectionFactory {
        http1Config(http1Config)
        charCodingConfig(charCodingConfig)
        requestWriterFactory(requestWriterFactory)
        responseParserFactory(responseParserFactory)
        builder()
    }
