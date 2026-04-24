package io.bluetape4k.aws.kotlin.sesv2.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SesV2DestinationTest {

    companion object : KLogging()

    @Test
    fun `destinationOf vararg로 수신자를 설정한다`() {
        val dest = destinationOf("user1@example.com", "user2@example.com")

        dest.toAddresses.shouldNotBeNull()
        dest.toAddresses!! shouldContain "user1@example.com"
        dest.toAddresses!! shouldContain "user2@example.com"
    }

    @Test
    fun `destinationOf는 TO, CC, BCC 주소를 설정한다`() {
        val dest = destinationOf(
            toAddresses = listOf("to@example.com"),
            ccAddresses = listOf("cc@example.com"),
            bccAddresses = listOf("bcc@example.com")
        )

        dest.toAddresses!! shouldContain "to@example.com"
        dest.ccAddresses!! shouldContain "cc@example.com"
        dest.bccAddresses!! shouldContain "bcc@example.com"
    }

    @Test
    fun `destinationOf vararg 빈 목록은 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            destinationOf() // empty vararg
        }
    }

    @Test
    fun `destinationOf 모든 주소가 null 또는 빈 목록이면 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            destinationOf(
                toAddresses = null,
                ccAddresses = null,
                bccAddresses = null
            )
        }
    }

    @Test
    fun `destinationOf 단일 TO 주소를 설정할 수 있다`() {
        val dest = destinationOf("user@example.com")
        dest.toAddresses!!.size shouldBeEqualTo 1
    }
}
