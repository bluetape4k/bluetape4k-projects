package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.cookie.CookieSpecFactory
import org.apache.hc.client5.http.impl.CookieSpecSupport
import org.apache.hc.client5.http.psl.PublicSuffixMatcher
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader
import org.apache.hc.core5.http.config.Lookup

/**
 * 제공된 PublicSuffixMatcher로 기본 쿠키 스펙 레지스트리를 생성합니다.
 *
 * ```kotlin
 * val registry = defaultRegistryOf()
 * val httpClient = httpClient {
 *     setDefaultCookieSpecRegistry(registry)
 * }
 * ```
 *
 * @param publicSuffixMatcher 사용할 [PublicSuffixMatcher] (기본값: [PublicSuffixMatcherLoader.getDefault])
 * @return [CookieSpecFactory] 조회용 [Lookup]
 */
fun defaultRegistryOf(
    publicSuffixMatcher: PublicSuffixMatcher = PublicSuffixMatcherLoader.getDefault(),
): Lookup<CookieSpecFactory> =
    CookieSpecSupport.createDefault(publicSuffixMatcher)
