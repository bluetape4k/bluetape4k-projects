package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.parseArray
import com.alibaba.fastjson2.parseObject
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import java.io.Serializable

class JSONExtensionsTest: AbstractFastjson2Test() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `json into 01`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        val parsedUser = jsonString.readValueOrNull<User>()
        parsedUser.shouldNotBeNull() shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json into 02`() {
        val user = newUser()
        val json = user.toJSONString()
        log.debug { "json: $json" }

        val users = """{"user":$json}""".readValueOrNull<Map<String, User>>()
        users.shouldNotBeNull()
        users.size shouldBeEqualTo 1

        val parsedUser = users["user"]!!
        parsedUser shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `stream into 01`() {
        val user = newUser()
        val jsonString = user.toJSONString()
        val input = jsonString.byteInputStream()

        val parsedUser = input.readValueOrNull<User>()
        parsedUser shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object 01`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        val parsed = jsonString.readValueOrNull<User>()
        parsed shouldBeEqualTo user
        parsed.toJSONString() shouldBeEqualTo jsonString
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object 02`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        val parsed = jsonString.parseObject<User>()
        parsed shouldBeEqualTo user
        parsed.toJSONString() shouldBeEqualTo jsonString
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse object 03`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        // JSONObject
        val parsed = jsonString.parseObject()
        parsed shouldBeEqualTo mapOf(
            "id" to user.id,
            "name" to user.name,
        )
    }

    /**
     * 배열과 같은 경우, 전체 JSON 을 한번에 변환하는게 아니라, 하나씩 consume 한다.
     * Sequence 와 유사
     */
    @RepeatedTest(REPEAT_SIZE)
    fun `parse object 04`() = runTest {
        val users = List(5) { newUser() }
        val inputStream = users.toJSONString().byteInputStream()

        inputStream.parseObject<User> {
            when (it.id) {
                users[0].id -> it.name shouldBeEqualTo users[0].name
                users[1].id -> it.name shouldBeEqualTo users[1].name
            }
        }

        inputStream.reset()

        inputStream.parseObject<User>(Charsets.UTF_8) {
            when (it.id) {
                users[0].id -> it.name shouldBeEqualTo users[0].name
                users[1].id -> it.name shouldBeEqualTo users[1].name
            }
        }

        inputStream.reset()

        inputStream.parseObject<User>(Charsets.UTF_8, delimiter = '\n') {
            when (it.id) {
                users[0].id -> it.name shouldBeEqualTo users[0].name
                users[1].id -> it.name shouldBeEqualTo users[1].name
            }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse array 01`() {
        // JSONArray
        val list = listOf<Any>(
            faker.random().nextInt(),
            faker.random().nextDouble().toString(),    // dobule 은 string 으로 변환해야 비교가 된다.
            faker.random().nextBoolean(),
            faker.random().nextLong()
        )
        val json = list.toJSONString()
        val data: JSONArray = json.parseArray()

        data.forEachIndexed { index, item ->
            item shouldBeEqualTo list[index]
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse array 02`() {
        // JSONArray
        val user = newUser()
        val list = listOf(user)
        val parsed = list.toJSONString().readValueAsList<User>()

        parsed.size shouldBeEqualTo 1
        parsed[0] shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse default marker`() {
        val m0 = """{}""".readValueOrNull<Meta>()
        m0 shouldBeEqualTo Meta()

        val m1 = """{"id":2}""".readValueOrNull<Meta>()
        m1 shouldBeEqualTo Meta(id = 2)

        val m2 = """{"tag":"json"}""".readValueOrNull<Meta>()
        m2 shouldBeEqualTo Meta(tag = "json")

        val m3 = """{"id":3,"tag":"json"}""".readValueOrNull<Meta>()
        m3 shouldBeEqualTo Meta(id = 3, tag = "json")
    }

    data class Meta(
        val id: Int = 1,
        val tag: String = "json",
    ): Serializable

}
