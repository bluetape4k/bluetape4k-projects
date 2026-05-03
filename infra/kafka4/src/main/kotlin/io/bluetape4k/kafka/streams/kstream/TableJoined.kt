@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.streams.kstream.TableJoined
import org.apache.kafka.streams.processor.StreamPartitioner

/**
 * Kafka Streams에서 KTable-KTable 조인을 수행할 때 사용할 [TableJoined] 인스턴스를 생성합니다.
 *
 * 양쪽 테이블의 파티셔너를 지정하여 테이블 조인을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val leftPartitioner = StreamPartitioner<String, Void> { topic, key, value, numPartitions ->
 *     key.hashCode() % numPartitions
 * }
 * val rightPartitioner = StreamPartitioner<Int, Void> { topic, key, value, numPartitions ->
 *     key % numPartitions
 * }
 * val tableJoined = tableJoinedOf(leftPartitioner, rightPartitioner)
 * val joinedTable = leftTable.join(rightTable, joiner, tableJoined)
 * ```
 *
 * @param K 왼쪽 테이블의 키 타입
 * @param K0 오른쪽 테이블의 키 타입
 * @param partitioner 왼쪽 테이블의 파티셔너
 * @param otherPartitioner 오른쪽 테이블의 파티셔너
 * @return [TableJoined] 인스턴스
 */
fun <K, K0> tableJoinedOf(
    partitioner: StreamPartitioner<K, Void>,
    otherPartitioner: StreamPartitioner<K0, Void>,
): TableJoined<K, K0> = TableJoined.with(partitioner, otherPartitioner)

/**
 * Kafka Streams에서 KTable-KTable 조인을 수행할 때 사용할 [TableJoined] 인스턴스를 생성합니다.
 *
 * 작업 이름을 지정하여 테이블 조인을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val tableJoined = tableJoinedOf<String, Int>("table-join")
 * val joinedTable = leftTable.join(rightTable, joiner, tableJoined)
 * ```
 *
 * @param K 왼쪽 테이블의 키 타입
 * @param K0 오른쪽 테이블의 키 타입
 * @param name 테이블 조인 작업의 이름
 * @return [TableJoined] 인스턴스
 */
fun <K, K0> tableJoinedOf(name: String): TableJoined<K, K0> = TableJoined.`as`(name)
