package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.parseArray
import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

class JSONArrayExtensionsTest: AbstractFastjson2Test() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `read value as list for json array`() {
        val users = List(2) { newUser() }
        val jsonArrayString = users.toJsonString()

        val parsedArray: JSONArray = jsonArrayString.parseArray()

        val user = parsedArray.readValueOrNull<User>(0)
        user shouldBeEqualTo users[0]
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read value as list`() {
        val users = List(2) { newUser() }
        val jsonArrayString = users.toJsonString()

        val parsedUsers = jsonArrayString.readValueAsList<User>()
        parsedUsers shouldBeEqualTo users
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert to json object and read value as list`() {
        val users = List(2) { newUser() }
        val jsonArrayString = users.toJsonString()

        val parsedArray: JSONArray = jsonArrayString.parseArray()

        val parsedUsers = parsedArray.readList<User>()
        parsedUsers shouldBeEqualTo users
    }
}
