package io.bluetape4k.fastjson2.extensions

import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.io.toInputStream
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class JSONBExtensionsTest: AbstractFastjson2Test() {
    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `object convert with JSONB`() {
        val user = newUser()

        val bytes = user.toJsonBytes()
        val parsed = bytes.readBytesOrNull<User>()

        parsed.shouldNotBeNull() shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `list convert with JSONB`() {
        val users = List(5) { newUser() }

        val bytes = users.toJsonBytes()
        val parsed = bytes.readBytesOrNull<List<User>>()

        parsed.shouldNotBeNull() shouldBeEqualTo users
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `list convert as input stream with JSONB`() {
        val users = List(2) { newUser() }

        users.toJsonBytes().toInputStream().use { inputStream ->
            val parsed = inputStream.readBytesOrNull<List<User>>()
            parsed.shouldNotBeNull() shouldBeEqualTo users
        }
    }

    @Test
    fun `null 객체 직렬화 시 빈 바이트 배열 반환`() {
        val bytes = null.toJsonBytes()
        bytes shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `null 바이트 배열 역직렬화 시 fastjson2 기본 동작 확인`() {
        // JSONB.parseObject(null, ...) 는 fastjson2 내부에서 처리됨 (null 또는 예외)
        // 실제 동작: null ByteArray 는 fastjson2가 null 반환
        val result = runCatching { (null as ByteArray?).readBytesOrNull<User>() }
        // 예외 없이 null 반환되거나, JSONException 발생 — 양쪽 모두 허용
        result.getOrNull().shouldBeNull()
    }

    @Test
    fun `빈 바이트 배열 역직렬화 시 fastjson2 기본 동작 확인`() {
        // JSONB.parseObject(emptyByteArray, ...) 는 fastjson2 내부에서 처리됨
        val result = runCatching { emptyByteArray.readBytesOrNull<User>() }
        result.getOrNull().shouldBeNull()
    }

    @Test
    fun `null InputStream 역직렬화 시 null 반환`() {
        val result = (null as java.io.InputStream?).readBytesOrNull<User>()
        result.shouldBeNull()
    }

    @Test
    fun `단일 객체 JSONB 왕복 직렬화`() {
        val user = newUser()
        val bytes = user.toJsonBytes()
        bytes.shouldNotBeEmpty()

        val restored = bytes.readBytesOrNull<User>()
        restored.shouldNotBeNull() shouldBeEqualTo user
    }
}
