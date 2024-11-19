package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.cookie.CookieSpecFactory
import org.apache.hc.client5.http.impl.CookieSpecSupport
import org.apache.hc.client5.http.psl.PublicSuffixMatcher
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader
import org.apache.hc.core5.http.config.Lookup

/**
 * 제공된 PublicSuffixMatcher로 기본 레지스트리를 생성합니다.
 *
 * @param publicSuffixMatcher [PublicSuffixMatcher] (default: [PublicSuffixMatcherLoader.getDefault])
 * @return [Lookup] of [CookieSpecFactory]
 */
fun defaultRegistryOf(
    publicSuffixMatcher: PublicSuffixMatcher = PublicSuffixMatcherLoader.getDefault(),
): Lookup<CookieSpecFactory> =
    CookieSpecSupport.createDefault(publicSuffixMatcher)
