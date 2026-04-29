package io.bluetape4k.images

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ImageFormatExtensionsTest {

    @Test
    fun `isWritableByImageIO - 기본 포맷은 true 반환`() {
        ImageFormat.GIF.isWritableByImageIO().shouldBeTrue()
        ImageFormat.JPG.isWritableByImageIO().shouldBeTrue()
        ImageFormat.PNG.isWritableByImageIO().shouldBeTrue()
        ImageFormat.WEBP.isWritableByImageIO().shouldBeTrue()
        ImageFormat.TIFF.isWritableByImageIO().shouldBeTrue()
    }

    @Test
    fun `isWritableByImageIO - incubating 포맷은 false 반환`() {
        ImageFormat.SVG.isWritableByImageIO().shouldBeFalse()
        ImageFormat.AVIF.isWritableByImageIO().shouldBeFalse()
        ImageFormat.HEIC.isWritableByImageIO().shouldBeFalse()
    }

    @Test
    fun `requireWritable - 쓰기 가능 포맷은 예외 없음`() {
        ImageFormat.PNG.requireWritable()
        ImageFormat.TIFF.requireWritable()
    }

    @Test
    fun `requireWritable - SVG 포맷은 예외 발생`() {
        try {
            ImageFormat.SVG.requireWritable()
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            e.message.shouldNotBeNull()
        }
    }

    @Test
    fun `requireWritable - AVIF 포맷은 예외 발생`() {
        try {
            ImageFormat.AVIF.requireWritable()
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            e.message.shouldNotBeNull()
        }
    }

    @Test
    fun `parse - TIFF 파싱`() {
        val result = ImageFormat.parse("TIFF")
        result.shouldNotBeNull()
        result shouldBe ImageFormat.TIFF
    }

    @Test
    fun `parse - SVG 파싱`() {
        val result = ImageFormat.parse("svg")
        result.shouldNotBeNull()
        result shouldBe ImageFormat.SVG
    }

    @Test
    fun `parse - AVIF 파싱`() {
        val result = ImageFormat.parse("avif")
        result.shouldNotBeNull()
        result shouldBe ImageFormat.AVIF
    }

    @Test
    fun `parse - HEIC 파싱`() {
        val result = ImageFormat.parse("heic")
        result.shouldNotBeNull()
        result shouldBe ImageFormat.HEIC
    }

    @Test
    fun `parse - 빈 문자열 null 반환`() {
        ImageFormat.parse("").shouldBeNull()
        ImageFormat.parse("   ").shouldBeNull()
    }
}
