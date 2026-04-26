package io.bluetape4k.elasticsearch

import java.time.Duration

/**
 * Elasticsearch 클라이언트 기본 설정값 상수 모음.
 */
object ElasticsearchDefaults {

    /** 기본 호스트 */
    const val DEFAULT_HOST: String = "localhost"

    /** 기본 포트 */
    const val DEFAULT_PORT: Int = 9200

    /** 기본 스킴 */
    const val DEFAULT_SCHEME: String = "http"

    /** 기본 사용자명 */
    const val DEFAULT_USERNAME: String = "elastic"

    /**
     * Bulk API 기본 청크 크기.
     * ES 권장: 요청당 5~15MB 이내 → 일반 문서 기준 500건
     */
    const val DEFAULT_BULK_CHUNK_SIZE: Int = 500

    /**
     * Search API 기본 배치 크기 (search_after 페이지 크기).
     * 일반 페이지 사이즈 기준 100건
     */
    const val DEFAULT_SEARCH_BATCH_SIZE: Int = 100

    /**
     * BulkIngester 기본 최대 작업 수.
     * ES 권장 bulk 사이즈 기준 1000건
     */
    const val DEFAULT_BULK_INGESTER_MAX_OPERATIONS: Int = 1000

    /**
     * BulkIngester 기본 flush 주기.
     */
    val DEFAULT_BULK_INGESTER_FLUSH_INTERVAL: Duration = Duration.ofSeconds(5)
}
