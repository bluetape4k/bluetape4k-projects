package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import tools.jackson.databind.json.JsonMapper

/**
 * Jackson 3 커스텀 JSON 엔벨로프 방식으로 직렬화/역직렬화를 수행하는 Redisson [Codec] 구현체입니다.
 *
 * ## 직렬화 포맷
 * 객체를 `{"_type": "com.example.Foo", "_data": {...}}` 형태의 JSON 엔벨로프로 감싸 저장합니다.
 * `_type` 필드에 FQCN을 기록하여 역직렬화 시 타입 정보를 복원합니다.
 *
 * ## Jackson 3.x 보안 설계
 * Jackson 3.x에서는 `activateDefaultTyping`이 제거되었습니다. 이 Codec은 자체 엔벨로프 포맷을 사용하여
 * 동일한 다형 타입 지원을 구현하면서, 역직렬화 전에 `validateClassName`으로 클래스 이름을 검사합니다.
 * 이는 pre-materialization 보안 제어에 해당합니다.
 *
 * ## 보안 경고
 * - `allowedPackagePrefixes = null`이면 모든 클래스 이름을 허용합니다 (**신뢰된 내부 Redis 환경에서만 사용**).
 * - 외부에 노출된 Redis 또는 다중 테넌트 환경에서는 [allowedPackagePrefixes]를 반드시 지정하십시오:
 *   ```kotlin
 *   val codec = Jackson3Codec(allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k."))
 *   // 또는 factory 사용:
 *   val codec = RedissonCodecs.jackson3(setOf("com.mycompany.", "io.bluetape4k."))
 *   ```
 *
 * ## 제한사항
 * - 루트 타입이 `List`, `Map` 등 컬렉션인 경우 원소 타입 정보가 소실됩니다. DTO 래퍼로 감싸서 사용하십시오.
 * - 직렬화 실패 시 [fallbackCodec]으로 자동 전환합니다.
 *
 * @property mapper Jackson 3 [JsonMapper] 인스턴스 (기본값: [io.bluetape4k.jackson3.Jackson.defaultJsonMapper])
 * @property fallbackCodec 직렬화/역직렬화 실패 시 사용할 대체 Codec (기본값: [RedissonCodecs.Fory])
 * @property classLoader 역직렬화 시 클래스 로드에 사용할 [ClassLoader]
 * @property allowedPackagePrefixes 허용할 패키지 prefix 목록. null이면 모든 클래스 허용 (보안 주의)
 */
class Jackson3Codec(
    private val mapper: JsonMapper = io.bluetape4k.jackson3.Jackson.defaultJsonMapper,
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
    private val classLoader: ClassLoader? = null,
    private val allowedPackagePrefixes: Set<String>? = null,
): BaseCodec() {

    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(
        io.bluetape4k.jackson3.Jackson.defaultJsonMapper,
        RedissonCodecs.Fory,
        classLoader,
    )

    constructor(classLoader: ClassLoader, codec: Jackson3Codec): this(
        codec.mapper,
        copy(classLoader, codec.fallbackCodec),
        classLoader,
        codec.allowedPackagePrefixes,
    )

    companion object: KLogging() {
        private const val TYPE_FIELD = "_type"
        private const val DATA_FIELD = "_data"
    }

    private fun validateClassName(className: String) {
        if (allowedPackagePrefixes != null) {
            val allowed = allowedPackagePrefixes.any { className.startsWith(it) }
            if (!allowed) {
                throw SecurityException(
                    "Class '$className' is not in the allowed package list. " +
                            "Allowed prefixes: $allowedPackagePrefixes"
                )
            }
        }
    }

    private val encoder: Encoder = Encoder { graph ->
        try {
            val node = mapper.createObjectNode()
            node.put(TYPE_FIELD, graph.javaClass.name)
            node.set(DATA_FIELD, mapper.valueToTree(graph))
            val bytes = mapper.writeValueAsBytes(node)
            Unpooled.wrappedBuffer(bytes)
        } catch (e: Exception) {
            log.info(e) { "Encoding failed for Jackson3Codec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        try {
            val tree = mapper.readTree(bytes)
            val typeNode = tree.get(TYPE_FIELD)
            if (typeNode != null && typeNode.isString) {
                val className = typeNode.asString()
                validateClassName(className)
                val cl = classLoader ?: Thread.currentThread().contextClassLoader ?: javaClass.classLoader
                val clazz = Class.forName(className, false, cl)
                mapper.treeToValue(tree.get(DATA_FIELD), clazz)
            } else {
                throw IllegalStateException("Missing or non-textual '$TYPE_FIELD' field in JSON envelope")
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            log.info(e) { "Decoding failed for Jackson3Codec. Using fallbackCodec[$fallbackCodec]" }
            val fallbackBuf = Unpooled.wrappedBuffer(bytes)
            try {
                fallbackCodec.valueDecoder.decode(fallbackBuf, state)
            } finally {
                fallbackBuf.release()
            }
        }
    }

    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder

    override fun toString(): String =
        "Jackson3Codec(fallback=${fallbackCodec.javaClass.simpleName}, allowedPrefixes=$allowedPackagePrefixes)"
}
