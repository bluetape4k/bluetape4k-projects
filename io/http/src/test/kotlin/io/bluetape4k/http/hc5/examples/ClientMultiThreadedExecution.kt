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

    // Create an HttpClient with the PoolingHttpClientConnectionManager.
    // This connection manager must be used if more than one thread will
    // be using the HttpClient.
    private val cm = httpClientConnectionManager {
        setMaxConnTotal(100)
    }

    @Test
    fun `execute get in multi threading`() {
        val httpclient = httpClientOf(cm)
        httpclient.use {
            MultithreadingTester()
                .numThreads(6)
                .roundsPerThread(2)
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

            // Multi threading 환경에서 사용 시에는 response.entity 를 읽어서 원하는 수형으로 변환해서 반환해야 합니다.
            // 아니면 response 를 바로 반환하면, input stream 이 공유 되어버립니다.
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
