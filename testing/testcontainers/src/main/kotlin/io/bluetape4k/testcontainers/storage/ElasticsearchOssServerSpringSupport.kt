package io.bluetape4k.testcontainers.storage

import org.springframework.data.elasticsearch.client.ClientConfiguration

/**
 * Spring Data Elasticsearch 통합 테스트에서 사용할 [ElasticsearchOssServer] 클라이언트 설정 헬퍼입니다.
 *
 * ## 동작/계약
 * - Basic Auth(`elastic` / [ElasticsearchOssServer.DEFAULT_PASSWORD])가 포함된 [ClientConfiguration]을 생성합니다.
 * - 매 호출마다 새 [ClientConfiguration] 인스턴스를 반환하며 서버 상태는 변경하지 않습니다.
 *
 * ```kotlin
 * val server = ElasticsearchOssServer.Launcher.elasticsearchOssServer
 * val config = ElasticsearchOssServerSpringSupport.getClientConfiguration(server)
 * // 또는
 * val config = ElasticsearchOssServer.Launcher.Spring.getClientConfiguration(server)
 * ```
 */
object ElasticsearchOssServerSpringSupport {

    /**
     * Spring Data Elasticsearch용 [ClientConfiguration]을 생성합니다.
     *
     * ## 동작/계약
     * - 전달한 서버의 `url`을 대상 엔드포인트로 사용합니다.
     * - 인증 정보는 `elastic` / [ElasticsearchOssServer.DEFAULT_PASSWORD] 고정값을 사용합니다.
     * - 새 설정 객체를 반환하며 서버 상태는 변경하지 않습니다.
     *
     * @param elasticsearch [ElasticsearchOssServer] 인스턴스
     * @return Spring Data Elasticsearch에서 제공하는 [ClientConfiguration] 인스턴스
     */
    fun getClientConfiguration(elasticsearch: ElasticsearchOssServer): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearch.url)
            .withBasicAuth("elastic", ElasticsearchOssServer.DEFAULT_PASSWORD)
            .build()
    }
}

/**
 * [ElasticsearchOssServer.Launcher]에서 Spring Data Elasticsearch 헬퍼에 접근하기 위한 확장 프로퍼티입니다.
 *
 * ```kotlin
 * val config = ElasticsearchOssServer.Launcher.Spring.getClientConfiguration(server)
 * ```
 */
val ElasticsearchOssServer.Launcher.Spring: ElasticsearchOssServerSpringSupport
    get() = ElasticsearchOssServerSpringSupport
