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
 * @param builder [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
inline fun credentialsProvider(
    @BuilderInference builder: CredentialsProviderBuilder.() -> Unit,
): CredentialsProvider =
    CredentialsProviderBuilder.create().apply(builder).build()

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
 * @param builder [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
inline fun credentialsProviderOf(
    authScope: AuthScope,
    credentials: Credentials,
    @BuilderInference builder: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider =
    credentialsProvider {
        add(authScope, credentials)
        builder()
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
 * @param builder [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
inline fun credentialsProviderOf(
    httpHost: HttpHost,
    credentials: Credentials,
    @BuilderInference builder: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider =
    credentialsProvider {
        add(httpHost, credentials)
        builder()
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
 * @param builder [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
inline fun credentialsProviderOf(
    authScope: AuthScope,
    username: String,
    password: CharArray,
    @BuilderInference builder: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider =
    credentialsProvider {
        add(authScope, username, password)
        builder()
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
 * @param builder [CredentialsProviderBuilder] 초기화 람다
 * @return [CredentialsProvider] 인스턴스
 */
inline fun credentialsProviderOf(
    httpHost: HttpHost,
    username: String,
    password: CharArray,
    @BuilderInference builder: CredentialsProviderBuilder.() -> Unit = {},
): CredentialsProvider =
    credentialsProvider {
        add(httpHost, username, password)
        builder()
    }
