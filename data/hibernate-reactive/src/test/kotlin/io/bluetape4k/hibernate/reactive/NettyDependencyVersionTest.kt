package io.bluetape4k.hibernate.reactive

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.netty.util.Version
import org.junit.jupiter.api.Test

/**
 * Vert.x 5의 Netty 4.2 호환성을 유지하면서
 * CVE-2026-75595의 첫 번째 수정 버전 이상을 선택하는지 검증합니다.
 */
class NettyDependencyVersionTest {

    companion object {
        private const val FIRST_SAFE_NETTY_PATCH = 17
    }

    @Test
    fun `netty-handler 는 첫 번째 패치 버전 이상을 사용한다`() {
        val handlerVersion = Version.identify()["netty-handler"]?.artifactVersion()
            .shouldNotBeNull()
        val components = handlerVersion.substringBefore('-').split('.')

        val major = components.getOrNull(0)?.toIntOrNull().shouldNotBeNull()
        val minor = components.getOrNull(1)?.toIntOrNull().shouldNotBeNull()
        val patch = components.getOrNull(2)?.toIntOrNull().shouldNotBeNull()

        major shouldBeEqualTo 4
        minor shouldBeEqualTo 2
        patch shouldBeGreaterOrEqualTo FIRST_SAFE_NETTY_PATCH
    }
}
