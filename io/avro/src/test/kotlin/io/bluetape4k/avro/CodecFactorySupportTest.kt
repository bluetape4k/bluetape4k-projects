package io.bluetape4k.avro

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * [CodecFactorySupport]에 정의된 코덱 팩토리 상수 및 [codecFactoryOf] 함수를 검증합니다.
 */
class CodecFactorySupportTest: AbstractAvroTest() {

    companion object: KLogging()

    @Test
    fun `기본 코덱 팩토리 인스턴스가 정상 생성된다`() {
        DEFAULT_CODEC_FACTORY.shouldNotBeNull()
        FAST_CODEC_FACTORY.shouldNotBeNull()
        ARCHIVE_CODEC_FACTORY.shouldNotBeNull()
        NULL_CODEC_FACTORY.shouldNotBeNull()
        DEFLATE_CODEC_FACTORY.shouldNotBeNull()
        SNAPPY_CODEC_FACTORY.shouldNotBeNull()
    }

    @Test
    fun `코덱 팩토리는 lazy 로 동일 인스턴스를 반환한다`() {
        val first = DEFAULT_CODEC_FACTORY
        val second = DEFAULT_CODEC_FACTORY
        (first === second) shouldBeEqualTo true
    }

    @ParameterizedTest(name = "codecFactoryOf({0})")
    @ValueSource(strings = ["null", "none", "deflate", "snappy", "zstd", "zstandard", "zstd-fast", "bzip2", "xz"])
    fun `codecFactoryOf 로 지원하는 코덱을 문자열로 생성할 수 있다`(codecName: String) {
        val codec = codecFactoryOf(codecName)
        codec.shouldNotBeNull()
        log.trace { "codec=$codec for name=$codecName" }
    }

    @ParameterizedTest(name = "codecFactoryOf({0}) - case insensitive")
    @ValueSource(strings = ["NULL", "Deflate", "SNAPPY", "Zstd", "ZSTANDARD", "BZIP2", "XZ"])
    fun `codecFactoryOf 는 대소문자를 무시한다`(codecName: String) {
        val codec = codecFactoryOf(codecName)
        codec.shouldNotBeNull()
    }

    @Test
    fun `지원하지 않는 코덱 이름은 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> {
            codecFactoryOf("unknown")
        }
    }

    @Test
    fun `공백이 포함된 코덱 이름도 처리할 수 있다`() {
        val codec = codecFactoryOf("  snappy  ")
        codec.shouldNotBeNull()
    }
}
