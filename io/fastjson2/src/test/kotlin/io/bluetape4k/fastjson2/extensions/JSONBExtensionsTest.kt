package io.bluetape4k.fastjson2.extensions

import io.bluetape4k.fastjson2.AbstractFastjson2Test
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.io.toInputStream
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

class JSONBExtensionsTest: AbstractFastjson2Test() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `object convert with JSONB`() {
        val user = newUser()

        val bytes = user.toJsonBytes()
        val parsed = bytes.readBytesOrNull<User>()!!

        parsed shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `list convert with JSONB`() {
        val users = List(2) { newUser() }

        val bytes = users.toJsonBytes()
        val parsed = bytes.readBytesOrNull<List<User>>()!!

        parsed shouldBeEqualTo users
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `list convert as input stream with JSONB`() {
        val users = List(2) { newUser() }

        users.toJsonBytes()!!.toInputStream().use { inputStream ->
            val parsed = inputStream.readBytesOrNull<List<User>>()!!
            parsed shouldBeEqualTo users
        }
    }
}
