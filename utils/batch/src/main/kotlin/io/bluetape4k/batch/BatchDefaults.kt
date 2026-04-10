package io.bluetape4k.batch

import kotlin.time.Duration.Companion.seconds

/**
 * bluetape4k-batch 공용 디폴트 상수.
 *
 * 청크 크기, 페이지 크기, 커밋 타임아웃 등 배치 처리에서 공통으로 사용하는
 * 기본값을 중앙 관리합니다.
 *
 * ```kotlin
 * val reader = MyBatchReader(pageSize = BatchDefaults.READER_PAGE_SIZE)
 * val runner = ChunkBatchRunner(chunkSize = BatchDefaults.CHUNK_SIZE)
 * ```
 */
object BatchDefaults {
    /** 청크 단위 처리 기본 크기 */
    const val CHUNK_SIZE: Int = 100

    /** 페이지 기반 리더의 기본 페이지 크기 */
    const val READER_PAGE_SIZE: Int = 1_000

    /** 청크 커밋 타임아웃 기본값. 0 이하면 타임아웃 미적용 */
    val COMMIT_TIMEOUT = 30.seconds
}
