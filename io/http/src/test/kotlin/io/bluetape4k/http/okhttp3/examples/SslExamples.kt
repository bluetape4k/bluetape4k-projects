package io.bluetape4k.http.okhttp3.examples

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.http.okhttp3.okhttp3RequestOf
import io.bluetape4k.http.okhttp3.print
import io.bluetape4k.logging.info
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Disabled("보안 때문에 사용할 수 없습니다.")
@Execution(ExecutionMode.SAME_THREAD)
class SslExample: AbstractHttpTest() {

    @Test
    fun `certificate pinning`() {

        val certificatePinner = CertificatePinner.Builder()
            .add("publicobject.com", "sha256/afwiKY3RxoMmLkuRW1l7QsPZTJPwDS2pdDROQjXw8ig=")
            .build()

        val client = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()

        val request = okhttp3RequestOf("https://publicobject.com/robots.txt")

        val response = client.newCall(request).execute()

        assertResponse(response)
        response.print()

        response.handshake?.peerCertificates?.forEach { certificate ->
            log.info { CertificatePinner.pin(certificate) }
        }
    }
}
