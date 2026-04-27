package io.bluetape4k.images

import io.bluetape4k.images.avif.AvifEncodeOptions
import io.bluetape4k.images.heic.HeicReadOptions
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * AvifWriter, HeicReader incubating 인터페이스 및 옵션 data class 검증 테스트입니다.
 *
 * 참고: @IncubatingImageApi 는 BINARY 보존이므로 런타임 리플렉션으로 읽을 수 없습니다.
 * 컴파일 시 @OptIn(IncubatingImageApi::class) 없이 사용하면 경고가 발생하는지는 컴파일러가 검증합니다.
 */
class IncubatingImageApiTest {

    companion object : KLoggingChannel()

    @Test
    fun `AvifEncodeOptions 기본값 검증`() {
        val opts = AvifEncodeOptions.Default
        opts.quality shouldBeEqualTo 0.85f
        opts.lossless.shouldBeFalse()
    }

    @Test
    fun `AvifEncodeOptions 사용자 정의 값`() {
        val opts = AvifEncodeOptions(quality = 0.9f, lossless = true)
        opts.quality shouldBeEqualTo 0.9f
        opts.lossless.shouldBeTrue()
    }

    @Test
    fun `AvifEncodeOptions copy 가능`() {
        val opts = AvifEncodeOptions.Default.copy(quality = 0.5f)
        opts.quality shouldBeEqualTo 0.5f
        opts.lossless.shouldBeFalse()
    }

    @Test
    fun `HeicReadOptions 기본값 검증`() {
        val opts = HeicReadOptions.Default
        opts.pageIndex shouldBeEqualTo 0
        opts.applyOrientation.shouldBeTrue()
    }

    @Test
    fun `HeicReadOptions 사용자 정의 값`() {
        val opts = HeicReadOptions(pageIndex = 2, applyOrientation = false)
        opts.pageIndex shouldBeEqualTo 2
        opts.applyOrientation.shouldBeFalse()
    }
}
