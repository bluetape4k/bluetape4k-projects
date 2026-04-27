package io.bluetape4k.http.hc5.auth

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.protocol.BasicHttpContext
import org.junit.jupiter.api.Test

class CredentialsProviderBuilderTest {

    companion object : KLogging()

    @Test
    fun `credentialsProvider DSL creates provider`() {
        val provider = credentialsProvider {
            add(AuthScope.ANY, UsernamePasswordCredentials("user", "pass".toCharArray()))
        }

        provider.shouldNotBeNull()
    }

    @Test
    fun `emptyCredentialsProvider creates provider with no credentials`() {
        val provider = emptyCredentialsProvider()

        provider.shouldNotBeNull()
        val credentials = provider.getCredentials(AuthScope.ANY, BasicHttpContext())
        credentials.shouldBeNull()
    }

    @Test
    fun `credentialsProviderOf with AuthScope and Credentials returns correct credentials`() {
        val authScope = AuthScope(null, null, -1, null, null)
        val credentials = UsernamePasswordCredentials("admin", "secret".toCharArray())

        val provider = credentialsProviderOf(authScope, credentials)

        provider.shouldNotBeNull()
        val retrieved = provider.getCredentials(authScope, BasicHttpContext())
        retrieved.shouldNotBeNull()
        val upCreds = retrieved as UsernamePasswordCredentials
        upCreds.userName shouldBeEqualTo "admin"
    }

    @Test
    fun `credentialsProviderOf with AuthScope ANY retrieves credentials`() {
        val credentials = UsernamePasswordCredentials("testuser", "testpass".toCharArray())

        val provider = credentialsProviderOf(AuthScope.ANY, credentials)

        provider.shouldNotBeNull()
        val retrieved = provider.getCredentials(AuthScope.ANY, BasicHttpContext())
        retrieved.shouldNotBeNull()
        val upCreds = retrieved as UsernamePasswordCredentials
        upCreds.userName shouldBeEqualTo "testuser"
    }

    @Test
    fun `credentialsProviderOf with HttpHost and Credentials creates provider`() {
        val host = HttpHost("localhost", 8080)
        val credentials = UsernamePasswordCredentials("hostuser", "hostpass".toCharArray())

        val provider = credentialsProviderOf(host, credentials)

        provider.shouldNotBeNull()
    }

    @Test
    fun `credentialsProviderOf with AuthScope username and password creates provider`() {
        val authScope = AuthScope.ANY
        val username = "userA"
        val password = "passwordA".toCharArray()

        val provider = credentialsProviderOf(authScope, username, password)

        provider.shouldNotBeNull()
        val retrieved = provider.getCredentials(AuthScope.ANY, BasicHttpContext())
        retrieved.shouldNotBeNull()
        val upCreds = retrieved as UsernamePasswordCredentials
        upCreds.userName shouldBeEqualTo username
    }

    @Test
    fun `credentialsProviderOf with HttpHost username and password creates provider`() {
        val host = HttpHost("example.com", 443)
        val username = "hostAdmin"
        val password = "hostSecret".toCharArray()

        val provider = credentialsProviderOf(host, username, password)

        provider.shouldNotBeNull()
    }

    @Test
    fun `credentialsProvider DSL with multiple auth scopes`() {
        val host1 = HttpHost("host1.example.com", 80)
        val host2 = HttpHost("host2.example.com", 443)

        val provider = credentialsProvider {
            add(host1, UsernamePasswordCredentials("user1", "pass1".toCharArray()))
            add(host2, UsernamePasswordCredentials("user2", "pass2".toCharArray()))
        }

        provider.shouldNotBeNull()
    }
}
