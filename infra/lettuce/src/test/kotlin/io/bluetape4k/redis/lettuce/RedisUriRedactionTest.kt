package io.bluetape4k.redis.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.net.URI

class RedisUriRedactionTest {

    @Test
    fun `정상 URI는 호스트 포트와 안전한 옵션을 보존한다`() {
        "redis://localhost:6379/0?database=1".redactUriCredentials() shouldBeEqualTo
            "redis://localhost:6379/0?database=1"
    }

    @Test
    fun `authority 자격증명과 percent encoded password를 숨긴다`() {
        val redacted = "redis://cache-user:p%40ssword@localhost:6379/0".redactUriCredentials()

        redacted shouldBeEqualTo "redis://<redacted>@localhost:6379/0"
        redacted shouldNotContain "p%40ssword"
    }

    @Test
    fun `query와 semicolon 옵션의 credential을 반복해서 숨긴다`() {
        val uri = "redis://localhost:6379/0;password=path-secret;tls=true" +
                "?user=cache-user&password=query%2Dsecret&token=token-secret&password=second-secret"

        val redacted = uri.redactUriCredentials()

        redacted shouldBeEqualTo "redis://localhost:6379/0;password=<redacted>;tls=true" +
                "?user=<redacted>&password=<redacted>&token=<redacted>&password=<redacted>"
        redacted shouldNotContain "path-secret"
        redacted shouldNotContain "query%2Dsecret"
        redacted shouldNotContain "token-secret"
        redacted shouldNotContain "second-secret"
    }

    @Test
    fun `URI 확장은 동일한 공용 redaction 정책을 사용한다`() {
        URI("redis://cache-user:secret@localhost:6379/0?api_key=key-secret").toRedactedLogString() shouldBeEqualTo
            "redis://<redacted>@localhost:6379/0?api_key=<redacted>"
    }

    @Test
    fun `malformed와 ambiguous URI는 fail closed 결과를 반환한다`() {
        "redis://localhost:6379/%ZZ?password=secret".redactUriCredentials() shouldBeEqualTo "<redacted-uri>"
        "redis://cache-user:secret@localhost:6379@evil".redactUriCredentials() shouldBeEqualTo "<redacted-uri>"
    }
}
