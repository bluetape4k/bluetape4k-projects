package io.bluetape4k.http.hc5.auth

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.HttpHost
import org.junit.jupiter.api.Test

class CredentialsProviderBuilderTest {

    companion object : KLogging()

    // HC5 5.x has no AuthScope.ANY — use all-null/wildcard constructor instead
    private val anyScope = AuthScope(null, null, -1, null, null)

    private fun httpContext() = HttpClientContext.create()

    @Test
    fun `credentialsProvider DSL creates provider`() {
        val provider = credentialsProvider {
            add(anyScope, UsernamePasswordCredentials("user", "pass".toCharArray()))
        }

        provider.shouldNotBeNull()
    }

    @Test
    fun `emptyCredentialsProvider creates provider with no credentials`() {
        val provider = emptyCredentialsProvider()

        provider.shouldNotBeNull()
        val credentials = provider.getCredentials(anyScope, httpContext())
        credentials.shouldBeNull()
    }

    @Test
    fun `credentialsProviderOf with AuthScope and Credentials returns correct credentials`() {
        val authScope = AuthScope(null, null, -1, null, null)
        val credentials = UsernamePasswordCredentials("admin", "secret".toCharArray())

        val provider = credentialsProviderOf(authScope, credentials)

        provider.shouldNotBeNull()
        val retrieved = provider.getCredentials(authScope, httpContext())
        retrieved.shouldNotBeNull()
        val upCreds = retrieved as UsernamePasswordCredentials
        upCreds.userName shouldBeEqualTo "admin"
    }

    @Test
    fun `credentialsProviderOf with specific AuthScope retrieves credentials`() {
        val authScope = AuthScope("http", "localhost", 8080, null, null)
        val credentials = UsernamePasswordCredentials("testuser", "testpass".toCharArray())

        val provider = credentialsProviderOf(authScope, credentials)

        provider.shouldNotBeNull()
        // Query with same scope should match
        val retrieved = provider.getCredentials(authScope, httpContext())
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
    fun `credentialsProviderOf with HttpHost retrieves credentials by host scope`() {
        val host = HttpHost("example.com", 80)
        val credentials = UsernamePasswordCredentials("hostuser", "hostpass".toCharArray())

        val provider = credentialsProviderOf(host, credentials)

        provider.shouldNotBeNull()
        val hostScope = AuthScope(host)
        val retrieved = provider.getCredentials(hostScope, httpContext())
        retrieved.shouldNotBeNull()
        val upCreds = retrieved as UsernamePasswordCredentials
        upCreds.userName shouldBeEqualTo "hostuser"
    }

    @Test
    fun `credentialsProviderOf with AuthScope username and password creates provider`() {
        val authScope = AuthScope(null, null, -1, null, null)
        val username = "userA"
        val password = "passwordA".toCharArray()

        val provider = credentialsProviderOf(authScope, username, password)

        provider.shouldNotBeNull()
        val retrieved = provider.getCredentials(authScope, httpContext())
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
    fun `credentialsProvider DSL with multiple hosts`() {
        val host1 = HttpHost("host1.example.com", 80)
        val host2 = HttpHost("host2.example.com", 443)

        val provider = credentialsProvider {
            add(host1, UsernamePasswordCredentials("user1", "pass1".toCharArray()))
            add(host2, UsernamePasswordCredentials("user2", "pass2".toCharArray()))
        }

        provider.shouldNotBeNull()
    }

    @Test
    fun `credentialsProvider DSL retrieves correct credentials per host`() {
        val host1 = HttpHost("http", "host1.example.com", 80)
        val creds1 = UsernamePasswordCredentials("user1", "pass1".toCharArray())

        val provider = credentialsProvider {
            add(host1, creds1)
        }

        provider.shouldNotBeNull()
        val scope1 = AuthScope(host1)
        val retrieved = provider.getCredentials(scope1, httpContext())
        retrieved.shouldNotBeNull()
        (retrieved as UsernamePasswordCredentials).userName shouldBeEqualTo "user1"
    }
}
