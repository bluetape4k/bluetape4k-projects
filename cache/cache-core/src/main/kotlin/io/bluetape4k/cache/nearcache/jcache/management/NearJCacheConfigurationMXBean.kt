package io.bluetape4k.cache.nearcache.jcache.management

import javax.cache.management.CacheMXBean

/**
 * NearJCache 구성과 타입 해석 근거를 노출하는 management MXBean 계약입니다.
 *
 * 표준 [CacheMXBean] 속성에 더해 key/value 타입을 어느 configuration source에서
 * 해석했는지, 실제 front cache가 제공한 정확한 정보인지 함께 제공합니다.
 */
interface NearJCacheConfigurationMXBean: CacheMXBean {

    /** key/value 타입 pair를 선택한 configuration source 이름을 반환합니다. */
    fun getTypeResolutionSource(): String

    /** 타입 pair가 실제 front cache configuration에서 직접 확인됐는지 반환합니다. */
    fun isTypeResolutionExact(): Boolean
}
