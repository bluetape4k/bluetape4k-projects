package io.bluetape4k.http.hc5.async.methods

import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer

/**
 * Apache HttpClient5의 [SimpleResponseConsumer]를 생성합니다.
 *
 * ```
 * val client = HttpAsyncClients.custom()
 * .addInterceptorFirst(LoggingRequestInterceptor())
 * .addInterceptorFirst(LoggingResponseInterceptor())
 * .setDefaultRequestConfig(requestConfig)
 * .setIOReactorConfig(ioReactorConfig)
 * .setHttpProcessor(httpProcessor)
 * .build()
 *
 * val consumer = simpleResponseConsumerOf()
 * val request = HttpGet("https://www.google.com")
 * client.execute(request, consumer, null)
 * ```
 *
 * @return 생성된 [SimpleResponseConsumer]
 */
fun simpleResponseConsumerOf(): SimpleResponseConsumer {
    return SimpleResponseConsumer.create()
}
