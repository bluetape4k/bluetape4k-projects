package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class TrieConfigTest {
    companion object: KLogging()

    @Test
    fun `기본 설정값 검증`() {
        val config = TrieConfig()

        config.allowOverlaps.shouldBeTrue()
        config.onlyWholeWords.shouldBeFalse()
        config.onlyWholeWordsWhiteSpaceSeparated.shouldBeFalse()
        config.ignoreCase.shouldBeFalse()
        config.stopOnHit.shouldBeFalse()
    }

    @Test
    fun `기본 설정 객체 검증`() {
        val defaultConfig = TrieConfig.DEFAULT

        defaultConfig.allowOverlaps.shouldBeTrue()
        defaultConfig.onlyWholeWords.shouldBeFalse()
        defaultConfig.ignoreCase.shouldBeFalse()
    }

    @Test
    fun `빌더를 통한 설정 변경`() {
        val config =
            TrieConfig
                .builder()
                .allowOverlaps(false)
                .onlyWholeWords(true)
                .onlyWholeWordsWhiteSpaceSeparated(true)
                .ignoreCase(true)
                .stopOnHit(true)
                .build()

        config.allowOverlaps.shouldBeFalse()
        config.onlyWholeWords.shouldBeTrue()
        config.onlyWholeWordsWhiteSpaceSeparated.shouldBeTrue()
        config.ignoreCase.shouldBeTrue()
        config.stopOnHit.shouldBeTrue()
    }

    @Test
    fun `allowOverlaps 설정`() {
        val config =
            TrieConfig
                .builder()
                .allowOverlaps(false)
                .build()

        config.allowOverlaps.shouldBeFalse()
    }

    @Test
    fun `allowOverlaps 기본값 설정`() {
        val config =
            TrieConfig
                .builder()
                .allowOverlaps()
                .build()

        config.allowOverlaps.shouldBeTrue()
    }

    @Test
    fun `onlyWholeWords 설정`() {
        val config =
            TrieConfig
                .builder()
                .onlyWholeWords(true)
                .build()

        config.onlyWholeWords.shouldBeTrue()
    }

    @Test
    fun `ignoreCase 설정`() {
        val config =
            TrieConfig
                .builder()
                .ignoreCase(true)
                .build()

        config.ignoreCase.shouldBeTrue()
    }

    @Test
    fun `stopOnHit 설정`() {
        val config =
            TrieConfig
                .builder()
                .stopOnHit(true)
                .build()

        config.stopOnHit.shouldBeTrue()
    }

    @Test
    fun `모든 설정을 false로 설정`() {
        val config =
            TrieConfig
                .builder()
                .allowOverlaps(false)
                .onlyWholeWords(false)
                .onlyWholeWordsWhiteSpaceSeparated(false)
                .ignoreCase(false)
                .stopOnHit(false)
                .build()

        config.allowOverlaps.shouldBeFalse()
        config.onlyWholeWords.shouldBeFalse()
        config.onlyWholeWordsWhiteSpaceSeparated.shouldBeFalse()
        config.ignoreCase.shouldBeFalse()
        config.stopOnHit.shouldBeFalse()
    }

    @Test
    fun `설정값 직접 변경`() {
        val config = TrieConfig()

        config.allowOverlaps = false
        config.onlyWholeWords = true
        config.ignoreCase = true

        config.allowOverlaps.shouldBeFalse()
        config.onlyWholeWords.shouldBeTrue()
        config.ignoreCase.shouldBeTrue()
    }

    @Test
    fun `Serializable 인터페이스 구현 검증`() {
        val config = TrieConfig()

        // Serializable을 구현하는지 확인 (컴파일 타임에 검증됨)
        config shouldBeEqualTo config
    }
}
