package io.bluetape4k.batch.api

/**
 * 배치 처리용 데이터 리더 인터페이스.
 *
 * ## 체크포인트 시맨틱
 * - [checkpoint]는 **마지막으로 writer.write()가 성공한 키**를 반환해야 한다.
 * - [onChunkCommitted]는 청크 커밋 완료 후 runner가 호출한다. 이 시점에 내부 포인터를 전진시킨다.
 * - [restoreFrom]은 [open] 이후 첫 [read] 전에 호출된다. 신규 실행에서는 호출되지 않는다.
 * - runner는 아이템을 개별 추적하지 않는다 — 위치 추적 책임은 reader에 있다.
 *
 * ## 사용 순서
 * ```
 * open() → [restoreFrom(checkpoint)]? → read()* → onChunkCommitted()* → close()
 * ```
 *
 * @param T 읽어들이는 아이템 타입
 */
interface BatchReader<out T : Any> {
    /**
     * 리더를 초기화한다. 첫 [read] 전에 반드시 호출된다.
     */
    suspend fun open() {}

    /**
     * 다음 아이템을 반환한다.
     *
     * @return 아이템, 또는 EOF이면 null
     */
    suspend fun read(): T?

    /**
     * 마지막으로 성공적으로 커밋된 위치를 반환한다.
     * 재시작 시 [restoreFrom]에 전달될 값이다.
     *
     * @return 체크포인트 값, 또는 아직 커밋이 없으면 null
     */
    suspend fun checkpoint(): Any? = null

    /**
     * 저장된 체크포인트로 읽기 위치를 복원한다.
     * [open] 직후, 첫 [read] 전에 호출된다.
     * 신규 실행(checkpoint == null)에서는 호출되지 않는다.
     *
     * @param checkpoint 복원할 체크포인트 값
     */
    suspend fun restoreFrom(checkpoint: Any) {}

    /**
     * 청크 커밋 완료를 통지받는다.
     * 이 시점에 내부의 lastCommittedKey를 lastReadKey로 전진시킨다.
     */
    suspend fun onChunkCommitted() {}

    /**
     * 리더를 닫고 리소스를 해제한다.
     */
    suspend fun close() {}
}
