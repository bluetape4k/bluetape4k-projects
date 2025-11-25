package io.bluetape4k.csv

import java.io.Closeable

/**
 * CSV 포맷 형태의 파일 정보를 [Record] 로 읽어드리는 Reader 입니다.
 */
interface RecordWriter: Closeable {

    fun writeHeaders(headers: Iterable<String>)

    fun writeHeaders(vararg headers: String) {
        writeHeaders(headers.toList())
    }

    fun writeRow(rows: Iterable<*>)

    /**
     * 하나의 엔티티를 여러 컬럼의 정보로 매핑하여 하나의 Record로 저장합니다.
     * @param entity T
     * @param transform Function1<T, Iterable<*>>
     */
    fun <T> writeRow(entity: T, transform: (T) -> Iterable<*>) {
        writeRow(transform(entity))
    }

    /**
     * 복수개의 정보를 저장소에 씁니다.
     * @param rows 저장할 레코드들
     */
    fun writeAll(rows: Sequence<Iterable<*>>)

    /**
     * 복수개의 정보를 저장소에 씁니다.
     *
     * @param entities 저장할 엔티티들
     * @param transform 엔티티를 레코드로 변환하는 함수
     */
    fun <T> writeAll(entities: Sequence<T>, transform: (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }
}
