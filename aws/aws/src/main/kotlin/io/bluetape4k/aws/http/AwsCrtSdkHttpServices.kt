package io.bluetape4k.aws.http

/**
 * AWS SDK 비동기 HTTP 서비스 구현을 CRT로 고정하는 초기화 오브젝트입니다.
 *
 * ## 동작/계약
 * - 클래스 로딩 시 JVM 시스템 프로퍼티 `software.amazon.awssdk.http.coroutines.service.impl`를 설정한다.
 * - 설정값은 `software.amazon.awssdk.http.crt.AwsCrtSdkHttpService`로 고정된다.
 *
 * ```kotlin
 * AwsCrtSdkHttpServices
 * val serviceImpl = System.getProperty("software.amazon.awssdk.http.coroutines.service.impl")
 * // serviceImpl == "software.amazon.awssdk.http.crt.AwsCrtSdkHttpService"
 * ```
 */
object AwsCrtSdkHttpServices {
    init {
        System.setProperty(
            "software.amazon.awssdk.http.coroutines.service.impl",
            "software.amazon.awssdk.http.crt.AwsCrtSdkHttpService"
        )
    }
}
