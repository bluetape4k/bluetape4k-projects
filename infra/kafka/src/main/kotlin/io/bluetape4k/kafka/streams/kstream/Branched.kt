@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.KStream

/**
 * Kafka Streams에서 브랜치(분기) 작업을 수행할 때 사용할 [Branched] 인스턴스를 생성합니다.
 *
 * 브랜치 이름을 지정하여 브랜치 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val branched = branchedOf<String, String>("valid-branch")
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param name 브랜치 이름
 * @return [Branched] 인스턴스
 */
fun <K, V> branchedOf(name: String): Branched<K, V> = Branched.`as`(name)

/**
 * Kafka Streams에서 브랜치(분기) 작업을 수행할 때 사용할 [Branched] 인스턴스를 생성합니다.
 *
 * 브랜치 처리 함수를 지정하여 브랜치 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val branched = branchedOf<String, String>(
 *     chain = { stream -> stream.filter { _, value -> value.startsWith("A") } },
 *     name = "starts-with-a"
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param chain 브랜치 처리 함수
 * @param name 브랜치 이름 (선택적)
 * @return [Branched] 인스턴스
 */
@JvmName("branchedWithFunction")
fun <K, V> branchedOf(
    chain: (KStream<K, V>) -> KStream<K, V>,
    name: String? = null,
): Branched<K, V> = Branched.withFunction(chain, name)

/**
 * Kafka Streams에서 브랜치(분기) 작업을 수행할 때 사용할 [Branched] 인스턴스를 생성합니다.
 *
 * 브랜치 소비 함수를 지정하여 브랜치 작업을 구성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val branched = branchedOf<String, String>(
 *     chain = { stream -> stream.to("output-topic") },
 *     name = "output-branch"
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param chain 브랜치 소비 함수
 * @param name 브랜치 이름 (선택적)
 * @return [Branched] 인스턴스
 */
@JvmName("branchedWithConsumer")
fun <K, V> branchedOf(
    chain: (KStream<K, V>) -> Unit,
    name: String? = null,
): Branched<K, V> = Branched.withConsumer(chain, name)
