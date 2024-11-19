package io.bluetape4k.http.hc5.auth

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.Credentials
import org.apache.hc.client5.http.auth.CredentialsProvider
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder
import org.apache.hc.core5.http.HttpHost

/**
 * [CredentialsProvider]를 생성합니다.
 *
 * ```
 * val credentialsProvider = credentialsProvider {
 *    add(AuthScope.ANY, UsernamePasswordCredentials("username", "password"))
 *    add(HttpHost("localhost", 8080), UsernamePasswordCredentials("username", "password"))
 * }
 * ```
 *
 * @param initializer [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
inline fun credentialsProvider(
    initializer: CredentialsProviderBuilder.() -> Unit,
): CredentialsProvider {
    return CredentialsProviderBuilder.create().apply(initializer).build()
}

/**
 * 빈 [CredentialsProvider]를 생성합니다.
 *
 * ```
 * val credentialsProvider = emptyCredentialsProvider()
 * ```
 *
 * @return [CredentialsProvider] 인스턴스
 */
fun emptyCredentialsProvider(): CredentialsProvider = credentialsProvider { }

/**
 * [AuthScope]와 [Credentials]를 추가한 [CredentialsProvider]를 생성합니다.
 *
 * ```
 * val credentialsProvider = credentialsProviderOf(
 *      AuthScope.ANY,
 *      UsernamePasswordCredentials("username", "password")
 * )
 * ```
 *
 * @param authScope [AuthScope]
 * @param credentials [Credentials]
 * @param initializer [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
fun credentialsProviderOf(
    authScope: AuthScope,
    credentials: Credentials,
    initializer: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider = credentialsProvider {
    add(authScope, credentials)
    initializer()
}

/**
 * [HttpHost]와 [Credentials]를 추가한 [CredentialsProvider]를 생성합니다.
 *
 * ```
 * val credentialsProvider = credentialsProviderOf(
 *      HttpHost("localhost", 8080),
 *      UsernamePasswordCredentials("username", "password")
 * )
 * ```
 *
 * @param httpHost [HttpHost]
 * @param credentials [Credentials]
 * @param initializer [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
fun credentialsProviderOf(
    httpHost: HttpHost,
    credentials: Credentials,
    initializer: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider = credentialsProvider {
    add(httpHost, credentials)
    initializer()
}

/**
 * [AuthScope]와 [username], [password]를 추가한 [CredentialsProvider]를 생성합니다.
 *
 * ```
 * val credentialsProvider = credentialsProviderOf(
 *      AuthScope.ANY,
 *      "username",
 *      "password".toCharArray()
 * )
 * ```
 *
 * @param authScope [AuthScope]
 * @param username 사용자 이름
 * @param password 비밀번호
 * @param initializer [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
fun credentialsProviderOf(
    authScope: AuthScope,
    username: String,
    password: CharArray,
    initializer: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider = credentialsProvider {
    add(authScope, username, password)
    initializer()
}

/**
 * [HttpHost]와 [username], [password]를 추가한 [CredentialsProvider]를 생성합니다.
 *
 * ```
 * val credentialsProvider = credentialsProviderOf(
 *      HttpHost("localhost", 8080),
 *      "username",
 *      "password".toCharArray()
 * ) {
 *      setCredentials(AuthScope.ANY, UsernamePasswordCredentials("username", "password"))
 * }
 * ```
 *
 * @param httpHost [HttpHost]
 * @param username 사용자 이름
 * @param password 비밀번호
 * @param initializer [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
fun credentialsProviderOf(
    httpHost: HttpHost,
    username: String,
    password: CharArray,
    initializer: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider = credentialsProvider {
    add(httpHost, username, password)
    initializer()
}
