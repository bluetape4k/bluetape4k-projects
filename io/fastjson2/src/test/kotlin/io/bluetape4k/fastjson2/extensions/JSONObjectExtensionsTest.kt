package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class JSONObjectExtensionsTest : AbstractFastjson2Test() {
    companion object : KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object`() {
        val user = newUser()
        val json = user.toJsonString()
        json.shouldNotBeNull()

        // parse as JSONObject
        val jsonObject: JSONObject = json.readAsJSONObject()

        val parsedUser = jsonObject.readValueOrNull<User>()
        parsedUser.shouldNotBeNull() shouldBeEqualTo user

        val parsedUser2 = jsonObject.readValueOrNull<User>()
        parsedUser2.shouldNotBeNull() shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object with key`() {
        val user = newUser()
        val json = """{"key": ${user.toJSONString()}}"""

        // parse as JSONObject
        val jsonObject = json.readAsJSONObject()

        val parsedUser = jsonObject.readValueOrNull<User>("key")
        parsedUser.shouldNotBeNull() shouldBeEqualTo user

        val parsedUser2 = jsonObject.readValueOrNull<User>("key")
        parsedUser2.shouldNotBeNull() shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object 02`() {
        val user = newUser()

        // JSONObject
        val jsonObject: JSONObject =
            JSONObject().apply {
                put(
                    "user",
                    JSONObject().apply {
                        put("id", user.id)
                        put("name", user.name)
                    }
                )
            }

        val users = jsonObject.readValueOrNull<Map<String, User>>()
        users.shouldNotBeNull()
        val parsedUser = users["user"]
        parsedUser shouldBeEqualTo user
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() {
        val user = newUser()
        val jsonObject = user.toJsonString()!!.readAsJSONObject()

        val result = jsonObject.readValueOrNull<User>("nonexistent")
        result.shouldBeNull()
    }

    data class Tag(
        val id: Int = 0,
        val label: String = "",
    )

    @Test
    fun `빈 JSONObject readValueOrNull 시 기본값 객체 반환`() {
        // User는 non-null name 필드가 있으므로 fastjson2가 NPE 발생
        // 대신 모든 필드에 기본값이 있는 타입으로 검증
        val jsonObject = JSONObject()
        val result = jsonObject.readValueOrNull<Tag>()
        result.shouldNotBeNull()
        result.id shouldBeEqualTo 0
        result.label shouldBeEqualTo ""
    }

    @Test
    fun `JSONObject 키-값 readValueOrNull 왕복 검증`() {
        val user = newUser()
        val jsonObject =
            JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
            }

        val result = jsonObject.readValueOrNull<User>()
        result.shouldNotBeNull() shouldBeEqualTo user
    }
}
