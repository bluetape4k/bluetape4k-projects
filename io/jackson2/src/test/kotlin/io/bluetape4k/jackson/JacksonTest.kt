package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.uuid.JsonUuidModule
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNotNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JacksonTest {

    companion object: KLogging()

    @Test
    fun `classpath에 있는 모듈을 자동으로 등록하기`() {
        val mapper = Jackson.defaultJsonMapper

        mapper.registeredModuleIds.forEach { moduleId ->
            println(moduleId)
        }
        mapper.registeredModuleIds.size shouldBeGreaterThan 0

        val modules = ObjectMapper.findModules()
        mapper.registeredModuleIds shouldContainAll modules.map { it.typeId.toString() }

        // classpath 에 있는 JsonUuidModule 을 자동으로 등록했다
        mapper.registeredModuleIds shouldContain JsonUuidModule::class.qualifiedName
    }

    @Test
    fun `defaultJsonMapper는 lazy singleton이다`() {
        val mapper1 = Jackson.defaultJsonMapper
        val mapper2 = Jackson.defaultJsonMapper
        (mapper1 === mapper2).shouldBeTrue()
    }

    @Test
    fun `createDefaultJsonMapper는 호출 시마다 새 인스턴스를 반환한다`() {
        val mapper1 = Jackson.createDefaultJsonMapper()
        val mapper2 = Jackson.createDefaultJsonMapper()
        (mapper1 !== mapper2).shouldBeTrue()
    }

    @Test
    fun `createTypedJsonMapper - 허용 패키지 지정 시 정상 생성`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.")
        mapper.shouldNotBeNull()
        // 생성된 매퍼로 간단한 직렬화 확인
        val json = mapper.writeValueAsString(mapOf("key" to "value"))
        json.shouldBeNotNull()
        (json.contains("key")).shouldBeTrue()
    }

    @Test
    fun `createTypedJsonMapper - 빈 패키지 목록이면 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            Jackson.createTypedJsonMapper()
        }
    }

    @Test
    fun `createTypedJsonMapper - 여러 패키지 허용 가능`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.", "com.example.")
        mapper.shouldNotBeNull()
    }

    @Test
    fun `registeredModuleIdList - 등록된 모듈 ID를 List로 반환`() {
        val ids = Jackson.defaultJsonMapper.registeredModuleIdList()
        ids.shouldNotBeNull()
        (ids.isNotEmpty()).shouldBeTrue()
    }
}
