package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

class JSONObjectExtensionsTest: AbstractFastjson2Test() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object`() {
        val user = newUser()
        val json = user.toJsonString()

        // parse as JSONObject
        val jsonObject: JSONObject = json.readAsJSONObject()

        val parsedUser = jsonObject.readValueOrNull<User>()
        parsedUser shouldBeEqualTo user

        val parsedUser2 = jsonObject.readValueOrNull<User>()!!
        parsedUser2 shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object with key`() {
        val user = newUser()
        val json = """{"key": ${user.toJSONString()}}"""

        // parse as JSONObject
        val jsonObject: JSONObject = json.readAsJSONObject()

        val parsedUser = jsonObject.readValueOrNull<User>("key")
        parsedUser shouldBeEqualTo user

        val parsedUser2 = jsonObject.readValueOrNull<User>("key")!!
        parsedUser2 shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object 02`() {
        val user = newUser()
        // JSONObject
        val jsonObject: JSONObject = JSONObject().apply {
            put(
                "user",
                JSONObject().apply {
                    put("id", user.id)
                    put("name", user.name)
                }
            )
        }

        val users = jsonObject.readValueOrNull<Map<String, User>>()!!
        val parsedUser = users["user"]
        parsedUser shouldBeEqualTo user
    }
}
