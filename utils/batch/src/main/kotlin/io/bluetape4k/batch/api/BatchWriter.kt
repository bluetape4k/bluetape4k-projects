package io.bluetape4k.batch.api

/**
 * 배치 처리 결과 라이터 인터페이스.
 *
 * [write]는 청크(chunk) 단위 트랜잭션 경계 안에서 호출된다.
 * 실패 시 청크 전체가 롤백된다.
 *
 * ## 사용 순서
 * ```
 * open() → write(chunk1)* → write(chunk2)* → ... → close()
 * ```
 *
 * @param T 저장할 아이템 타입
 */
interface BatchWriter<in T : Any> {
    /**
     * 라이터를 초기화한다.
     */
    suspend fun open() {}

    /**
     * 아이템 목록을 저장한다.
     *
     * @param items 저장할 아이템 목록. 빈 리스트면 no-op.
     */
    suspend fun write(items: List<T>)

    /**
     * 라이터를 닫고 리소스를 해제한다.
     */
    suspend fun close() {}
}
