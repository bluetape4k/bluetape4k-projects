package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeycloakServerTest: AbstractContainerTest() {

    companion object: KLogging()

    private val keycloak = KeycloakServer.Launcher.keycloak

    @Test
    fun `Keycloak 서버가 정상 실행 중이어야 한다`() {
        keycloak.isRunning.shouldBeTrue()
        (keycloak.port > 0).shouldBeTrue()
    }

    @Test
    fun `시스템 프로퍼티가 등록되어야 한다`() {
        val prefix = "testcontainers.${KeycloakServer.NAME}"

        System.getProperty("$prefix.host").shouldNotBeBlank()
        System.getProperty("$prefix.port").shouldNotBeBlank()
        System.getProperty("$prefix.url").shouldNotBeBlank()
        System.getProperty("$prefix.auth-url").shouldNotBeBlank()
        System.getProperty("$prefix.admin-username").shouldNotBeBlank()
        System.getProperty("$prefix.admin-password").shouldNotBeBlank()

        log.debug { "auth-url=${System.getProperty("$prefix.auth-url")}" }
    }

    @Test
    fun `Admin 액세스 토큰을 획득할 수 있어야 한다`() {
        val client = OkHttpClient()
        val authServerUrl = keycloak.authServerUrl

        // Admin 액세스 토큰 획득
        val tokenRequest = Request.Builder()
            .url("$authServerUrl/realms/master/protocol/openid-connect/token")
            .post(
                FormBody.Builder()
                    .add("grant_type", "password")
                    .add("username", keycloak.adminUsername)
                    .add("password", keycloak.adminPassword)
                    .add("client_id", "admin-cli")
                    .build()
            )
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        (tokenResponse.code == 200).shouldBeTrue()

        val tokenBody = tokenResponse.body.string().shouldNotBeNull()
        log.debug { "Token response: $tokenBody" }
        tokenBody shouldContain "access_token"

        // access_token 추출 (간단한 문자열 파싱)
        val accessToken = tokenBody
            .substringAfter("\"access_token\":\"")
            .substringBefore("\"")
        accessToken.shouldNotBeBlank()
        log.debug { "Access token acquired (length=${accessToken.length})" }

        // Bearer 토큰으로 Admin Realms API 호출
        val realmsRequest = Request.Builder()
            .url("$authServerUrl/admin/realms")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val realmsResponse = client.newCall(realmsRequest).execute()
        (realmsResponse.code == 200).shouldBeTrue()

        val realmsBody = realmsResponse.body.string().shouldNotBeNull()
        log.debug { "Realms response: $realmsBody" }

        // master realm 존재 확인
        realmsBody shouldContain "\"realm\":\"master\""
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { KeycloakServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { KeycloakServer(tag = " ") }
    }
}
