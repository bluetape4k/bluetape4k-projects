package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClientConnectionManager
import io.bluetape4k.http.hc5.classic.httpClientOf
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.junit.jupiter.api.Test

class ClientMultiThreadedExecution: AbstractHc5Test() {

    companion object: KLogging()

    // PoolingHttpClientConnectionManager를 사용하는 HttpClient를 생성합니다.
    // 여러 스레드에서 HttpClient를 사용할 때는 이 연결 관리자가 필요합니다.
    private val cm = httpClientConnectionManager {
        setMaxConnTotal(100)
    }

    @Test
    fun `execute get in multi threading`() {
        val httpclient = httpClientOf(cm)
        httpclient.use {
            MultithreadingTester()
                .workers(6)
                .rounds(2)
                .add {
                    executeHttpGet(httpclient, urisToGet[0], 0)
                }
                .add {
                    executeHttpGet(httpclient, urisToGet[1], 1)
                }
                .add {
                    executeHttpGet(httpclient, urisToGet[2], 2)
                }
                .run()
        }
    }

    private fun executeHttpGet(httpclient: CloseableHttpClient, uriToGet: String, id: Int) {
        try {
            val httpget = HttpGet(uriToGet)
            log.debug { "get execute to get $uriToGet[$id]" }

            // 멀티스레드 환경에서는 response.entity를 읽어 원하는 형식으로 변환해 반환해야 합니다.
            // response를 그대로 반환하면 입력 스트림이 공유될 수 있습니다.
            httpclient.execute(httpget) { response ->
                val entity = response.entity
                if (entity != null) {
                    val bytes = EntityUtils.toByteArray(entity)
                    log.debug { "$id - ${bytes.size} bytes read" }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Fail to get url. $uriToGet[$id]" }
        }
    }
}
