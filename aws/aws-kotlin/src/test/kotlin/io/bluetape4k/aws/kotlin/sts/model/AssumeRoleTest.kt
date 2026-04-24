package io.bluetape4k.aws.kotlin.sts.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class AssumeRoleTest {

    companion object : KLogging()

    @Test
    fun `assumeRoleRequest DSL 블록으로 AssumeRoleRequest를 생성한다`() {
        val req = assumeRoleRequest {
            roleArn = "arn:aws:iam::123456789012:role/MyRole"
            roleSessionName = "my-session"
        }

        req.roleArn shouldBeEqualTo "arn:aws:iam::123456789012:role/MyRole"
        req.roleSessionName shouldBeEqualTo "my-session"
    }

    @Test
    fun `assumeRoleRequestOf는 roleArn과 sessionName으로 요청을 생성한다`() {
        val req = assumeRoleRequestOf(
            roleArn = "arn:aws:iam::123456789012:role/TestRole",
            sessionName = "test-session"
        )

        req.roleArn shouldBeEqualTo "arn:aws:iam::123456789012:role/TestRole"
        req.roleSessionName shouldBeEqualTo "test-session"
    }

    @Test
    fun `assumeRoleRequestOf는 builder 블록으로 추가 설정이 가능하다`() {
        val req = assumeRoleRequestOf(
            roleArn = "arn:aws:iam::123456789012:role/TestRole",
            sessionName = "test-session"
        ) {
            durationSeconds = 3600
        }

        req.shouldNotBeNull()
        req.durationSeconds shouldBeEqualTo 3600
    }

    @Test
    fun `assumeRoleRequestOf 인스턴스는 null이 아니다`() {
        val req = assumeRoleRequestOf(
            roleArn = "arn:aws:iam::123456789012:role/TestRole",
            sessionName = "test-session"
        )
        req.shouldNotBeNull()
    }
}
