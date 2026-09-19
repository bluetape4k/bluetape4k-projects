package io.bluetape4k.http.hc5.http

import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.cookie.CookieSpecFactory
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader
import org.apache.hc.core5.http.config.Lookup
import org.junit.jupiter.api.Test

class CookieSpecSupportTest {

    companion object: KLogging()

    @Test
    fun `defaultRegistryOf - 기본 PublicSuffixMatcher 로 레지스트리 생성`() {
        val registry: Lookup<CookieSpecFactory> = defaultRegistryOf()
        log.debug { "registry=$registry" }
        registry.shouldNotBeNull()
    }

    @Test
    fun `defaultRegistryOf - 커스텀 PublicSuffixMatcher 로 레지스트리 생성`() {
        val matcher = PublicSuffixMatcherLoader.getDefault()
        val registry: Lookup<CookieSpecFactory> = defaultRegistryOf(matcher)

        log.debug { "registry=$registry" }
        registry.shouldNotBeNull()
    }
}
