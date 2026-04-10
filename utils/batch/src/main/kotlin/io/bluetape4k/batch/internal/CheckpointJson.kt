package io.bluetape4k.batch.internal

/**
 * Checkpoint 객체를 직렬화 봉투([TypedCheckpoint])로 감싸 round-trip을 보장한다.
 *
 * Jackson 3는 activateDefaultTyping이 제거되어 `Any` 역직렬화 시 원래 타입을 잃는다.
 * 예: writeValueAsString(42L) → readValue("42", Any::class) → Integer(42) (Long 아님)
 * TypedCheckpoint는 className을 함께 저장하여 이 문제를 해결한다.
 */
internal data class TypedCheckpoint(val className: String, val payload: String)

/**
 * Checkpoint 객체를 문자열로 직렬화/역직렬화하는 전략 인터페이스.
 *
 * ## round-trip 보장 필수
 * [read]가 원래 타입을 완전히 복원하지 못하면 `BatchReader.restoreFrom(checkpoint as K)`에서
 * [ClassCastException]이 발생하여 **silent 재시작 실패**가 된다.
 * toString() 기반 fallback 같은 lossy 직렬화는 금지된다.
 *
 * ## 기본 구현
 * [jackson3] — `bluetape4k-jackson3` (tools.jackson) 모듈이 classpath에 있을 때만 사용 가능.
 * jackson3 없으면 사용자가 직접 구현하여 `ExposedJdbc/R2dbcBatchJobRepository` 생성자에 주입한다.
 *
 * ## InMemoryBatchJobRepository
 * 인메모리 구현은 `Any` 객체 그대로 `ConcurrentHashMap`에 저장하므로 본 인터페이스가 필요 없다.
 */
interface CheckpointJson {
    fun write(obj: Any): String
    fun read(json: String): Any

    companion object {
        /**
         * `bluetape4k-jackson3` 기반 팩토리.
         * `tools.jackson.databind.json.JsonMapper`가 classpath에 없으면
         * 즉시 [IllegalStateException]을 던진다 (Jackson 3는 `tools.jackson.*` 패키지).
         */
        fun jackson3(): CheckpointJson = try {
            Class.forName("tools.jackson.databind.json.JsonMapper")
            Jackson3CheckpointJson()
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "CheckpointJson.jackson3() requires bluetape4k-jackson3 (tools.jackson) on classpath. " +
                    "Provide a custom CheckpointJson or add the bluetape4k-jackson3 dependency.",
                e,
            )
        }
        // toString() fallback은 round-trip 불가 → 제공하지 않음
    }
}

/**
 * `bluetape4k-jackson3`의 [io.bluetape4k.jackson3.Jackson.defaultJsonMapper] 기반 구현.
 *
 * [TypedCheckpoint] 봉투로 className을 보존하여 Jackson 3의 Default Typing 제거에도
 * 타입 round-trip을 보장한다.
 *
 * - write(42L) → `{"className":"java.lang.Long","payload":"42"}`
 * - read(json) → Long(42)
 */
internal class Jackson3CheckpointJson : CheckpointJson {
    private val mapper = io.bluetape4k.jackson3.Jackson.defaultJsonMapper

    override fun write(obj: Any): String {
        val envelope = TypedCheckpoint(
            className = obj.javaClass.name,
            payload = mapper.writeValueAsString(obj),
        )
        return mapper.writeValueAsString(envelope)
    }

    override fun read(json: String): Any {
        val envelope = mapper.readValue(json, TypedCheckpoint::class.java)
        val clazz = Class.forName(envelope.className)
        return mapper.readValue(envelope.payload, clazz)
    }
}
