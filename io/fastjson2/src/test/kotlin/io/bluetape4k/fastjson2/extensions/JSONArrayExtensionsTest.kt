package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.parseArray
import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class JSONArrayExtensionsTest : AbstractFastjson2Test() {
    companion object : KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `read value as list for json array`() {
        val users = List(5) { newUser() }
        val jsonArrayString = users.toJsonString()

        val parsedArray: JSONArray = jsonArrayString.parseArray()

        val user = parsedArray.readValueOrNull<User>(0)
        user shouldBeEqualTo users[0]
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read value as list`() {
        val users = List(5) { newUser() }
        val jsonArrayString = users.toJsonString()

        val parsedUsers = jsonArrayString.readValueAsList<User>()
        parsedUsers shouldBeEqualTo users
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert to json object and read value as list`() {
        val users = List(5) { newUser() }
        val jsonArrayString = users.toJsonString()

        val parsedArray: JSONArray = jsonArrayString.parseArray()

        val parsedUsers = parsedArray.readList<User>()
        parsedUsers shouldBeEqualTo users
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `JSONArray를 배열로 변환`() {
        val users = List(3) { newUser() }
        val parsedArray: JSONArray = users.toJsonString()!!.parseArray()

        val arr = parsedArray.readArray<User>()
        arr shouldHaveSize 3
        arr[0] shouldBeEqualTo users[0]
        arr[2] shouldBeEqualTo users[2]
    }

    @Test
    fun `빈 JSONArray readList 시 빈 리스트 반환`() {
        val empty = JSONArray()
        val result = empty.readList<User>()
        result.shouldBeEmpty()
    }

    @Test
    fun `빈 JSONArray readArray 시 빈 배열 반환`() {
        val empty = JSONArray()
        val result = empty.readArray<User>()
        result.shouldNotBeNull()
        result.size shouldBeEqualTo 0
    }

    @Test
    fun `null 문자열 readValueAsList 시 빈 리스트 반환`() {
        val result = (null as String?).readValueAsList<User>()
        result.shouldBeEmpty()
    }

    @Test
    fun `JSONArray 특정 인덱스 readValueOrNull 타입 불일치 시 JSONException 발생`() {
        val array = JSONArray.of("hello", "world")
        // String 값을 User(복합 타입) 로 변환 시 fastjson2가 JSONException 발생
        org.junit.jupiter.api.assertThrows<com.alibaba.fastjson2.JSONException> {
            array.readValueOrNull<User>(0)
        }
    }
}
