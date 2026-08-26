package io.bluetape4k.concurrent.virtualthread.api

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test

/**
 * Java 21 호환 API가 core와 패키지를 공유하지 않는지 검증합니다.
 */
class VirtualThreadApiPackageBoundaryTest {

    @Test
    fun `API 타입은 virtualthread api 패키지에만 존재해야 한다`() {
        val apiPackage = "io.bluetape4k.concurrent.virtualthread.api"

        VirtualThreads::class.java.packageName shouldBeEqualTo apiPackage
        VirtualThreadRuntime::class.java.packageName shouldBeEqualTo apiPackage
        StructuredTaskScopes::class.java.packageName shouldBeEqualTo apiPackage
        TaskContext::class.java.packageName shouldBeEqualTo apiPackage
    }

    @Test
    fun `이전 split package의 API class resource는 없어야 한다`() {
        val legacyResource =
            "io/bluetape4k/concurrent/virtualthread/VirtualThreads.class"

        Thread.currentThread().contextClassLoader.getResource(legacyResource).shouldBeNull()
    }
}
