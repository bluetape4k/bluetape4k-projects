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
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.Serializable

class JSONExtensionsTest : AbstractFastjson2Test() {
    companion object : KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `JSON 문자열을 User 객체로 역직렬화`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        val parsedUser = jsonString.readValueOrNull<User>()
        parsedUser.shouldNotBeNull() shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `JSON 문자열에서 User가 포함된 Map 역직렬화`() {
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
    fun `JSON 스트림에서 User 객체 역직렬화`() {
        val user = newUser()
        val jsonString = user.toJSONString()
        val input = jsonString.byteInputStream()

        val parsedUser = input.readValueOrNull<User>()
        parsedUser shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `JSON 문자열 파싱 후 재직렬화 결과 동일`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        val parsed = jsonString.readValueOrNull<User>()
        parsed shouldBeEqualTo user
        parsed.toJSONString() shouldBeEqualTo jsonString
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parseObject 확장 함수로 User 역직렬화`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        val parsed = jsonString.parseObject<User>()
        parsed shouldBeEqualTo user
        parsed.toJSONString() shouldBeEqualTo jsonString
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parseObject 로 JSONObject 파싱`() {
        val user = newUser()
        val jsonString = user.toJSONString()

        // JSONObject
        val parsed = jsonString.parseObject()
        parsed shouldBeEqualTo
            mapOf(
                "id" to user.id,
                "name" to user.name
            )
    }

    /**
     * 배열과 같은 경우, 전체 JSON 을 한번에 변환하는게 아니라, 하나씩 consume 한다.
     * Sequence 와 유사
     */
    @RepeatedTest(REPEAT_SIZE)
    fun `InputStream 에서 스트리밍 방식으로 User 파싱`() =
        runTest {
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
    fun `JSON 배열 문자열을 JSONArray 로 파싱`() {
        // JSONArray
        val list =
            listOf<Any>(
                faker.random().nextInt(),
                faker.random().nextDouble().toString(), // dobule 은 string 으로 변환해야 비교가 된다.
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
    fun `JSON 배열 문자열을 User 리스트로 역직렬화`() {
        // JSONArray
        val user = newUser()
        val list = listOf(user)
        val parsed = list.toJSONString().readValueAsList<User>()

        parsed.size shouldBeEqualTo 1
        parsed[0] shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `기본값이 있는 Meta 객체 JSON 역직렬화`() {
        val m0 = """{}""".readValueOrNull<Meta>()
        m0 shouldBeEqualTo Meta()

        val m1 = """{"id":2}""".readValueOrNull<Meta>()
        m1 shouldBeEqualTo Meta(id = 2)

        val m2 = """{"tag":"json"}""".readValueOrNull<Meta>()
        m2 shouldBeEqualTo Meta(tag = "json")

        val m3 = """{"id":3,"tag":"json"}""".readValueOrNull<Meta>()
        m3 shouldBeEqualTo Meta(id = 3, tag = "json")
    }

    @Test
    fun `null 문자열 readValueOrNull 시 null 반환`() {
        val result = (null as String?).readValueOrNull<User>()
        result.shouldBeNull()
    }

    @Test
    fun `null 문자열 readValueAsList 시 빈 리스트 반환`() {
        val result = (null as String?).readValueAsList<User>()
        result.shouldBeEmpty()
    }

    @Test
    fun `빈 문자열 readValueAsList 시 빈 리스트 반환`() {
        val result = "".readValueAsList<User>()
        result.shouldBeEmpty()
    }

    @Test
    fun `toJsonString null 수신자 시 null 문자열 반환`() {
        // fastjson2는 null 객체를 "null" 문자열로 직렬화함
        val result = (null as Any?).toJsonString()
        result shouldBeEqualTo "null"
    }

    @Test
    fun `InputStream readValueOrNull null 시 null 반환`() {
        val result = (null as java.io.InputStream?).readValueOrNull<User>()
        result.shouldBeNull()
    }

    data class Meta(
        val id: Int = 1,
        val tag: String = "json",
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
