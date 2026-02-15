package io.bluetape4k.http.hc5

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.logging.KLogging

abstract class AbstractHc5Test: AbstractHttpTest() {

    companion object: KLogging()

    protected val urisToGet: List<String>
        get() = listOf(
            "$httpbinBaseUrl/get",
            "$httpbinBaseUrl/ip",
            "$httpbinBaseUrl/headers"
        )
}
