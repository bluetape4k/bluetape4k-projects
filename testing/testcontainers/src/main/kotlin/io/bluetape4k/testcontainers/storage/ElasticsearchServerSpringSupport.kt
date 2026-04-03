package io.bluetape4k.testcontainers.storage

import org.springframework.data.elasticsearch.client.ClientConfiguration

/**
 * Spring Data Elasticsearch 통합 테스트에서 사용할 [ElasticsearchServer] 클라이언트 설정 헬퍼입니다.
 *
 * ## 동작/계약
 * - SSL과 Basic Auth(`elastic` / [ElasticsearchServer.password])를 조합한 [ClientConfiguration]을 생성합니다.
 * - 매 호출마다 새 [ClientConfiguration] 인스턴스를 반환하며 서버 상태는 변경하지 않습니다.
 *
 * ```kotlin
 * val server = ElasticsearchServer.Launcher.elasticsearch
 * val config = ElasticsearchServerSpringSupport.getClientConfiguration(server)
 * // 또는
 * val config = ElasticsearchServer.Launcher.Spring.getClientConfiguration(server)
 * ```
 */
object ElasticsearchServerSpringSupport {

    /**
     * Spring Data Elasticsearch 를 사용할 때 사용할 [ClientConfiguration]을 생성합니다.
     *
     * ## 동작/계약
     * - 전달한 서버의 `url`을 대상 엔드포인트로 사용합니다.
     * - SSL 컨텍스트(`createSslContextFromCa()`)와 Basic Auth(`elastic` / [ElasticsearchServer.password])를 설정합니다.
     * - 새 설정 객체를 반환하며 서버 상태는 변경하지 않습니다.
     *
     * @param elasticsearch [ElasticsearchServer] 인스턴스
     * @return Spring Data Elasticsearch에서 제공하는 [ClientConfiguration] 인스턴스
     */
    fun getClientConfiguration(elasticsearch: ElasticsearchServer): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearch.url)
            .usingSsl(elasticsearch.createSslContextFromCa())
            .withBasicAuth("elastic", elasticsearch.password)
            .build()
    }
}

/**
 * [ElasticsearchServer.Launcher]에서 Spring Data Elasticsearch 헬퍼에 접근하기 위한 확장 프로퍼티입니다.
 *
 * ```kotlin
 * val config = ElasticsearchServer.Launcher.Spring.getClientConfiguration(server)
 * ```
 */
val ElasticsearchServer.Launcher.Spring: ElasticsearchServerSpringSupport
    get() = ElasticsearchServerSpringSupport
