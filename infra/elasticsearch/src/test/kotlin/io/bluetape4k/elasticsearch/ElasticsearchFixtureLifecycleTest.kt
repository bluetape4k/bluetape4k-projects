package io.bluetape4k.elasticsearch

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/** 공유 서버 준비가 coroutine 테스트 본문보다 먼저 완료되는지 검증합니다. */
class ElasticsearchFixtureLifecycleTest: AbstractElasticsearchTest() {

    @Test
    fun `기반 fixture가 BeforeAll 초기화를 등록한다`() {
        AbstractElasticsearchTest::class.java.declaredMethods
            .any { it.isAnnotationPresent(BeforeAll::class.java) }
            .shouldBeTrue()
    }

    @Test
    fun `테스트 본문 진입 전에 공유 서버의 endpoint가 등록된다`() {
        // lazy getter를 먼저 호출하면 테스트 자체가 서버를 시작하여 회귀를 가립니다.
        System.getProperty("testcontainers.elasticsearch.url").shouldNotBeNull()
        elasticsearch.isRunning.shouldBeTrue()
    }
}
