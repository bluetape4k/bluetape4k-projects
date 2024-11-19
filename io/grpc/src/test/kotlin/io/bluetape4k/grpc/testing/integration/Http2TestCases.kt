package io.bluetape4k.grpc.testing.integration

import io.bluetape4k.support.requireNotBlank

enum class Http2TestCases(val description: String) {

    RST_AFTER_HEADER("server resets stream after sending header"),
    RST_AFTER_DATA("server resets stream after sending data"),
    RST_DURING_DATA("server resets stream in the middle of sending data"),
    GOAWAY("server sends goaway after first request and asserts second request uses new connection"),
    PING("server sends pings during request and verifies client response"),
    MAX_STREAMS("server verifies that the client respects MAX_STREAMS setting");

    companion object {
        @JvmStatic
        fun fromString(s: String): Http2TestCases {
            s.requireNotBlank("s")
            return try {
                Http2TestCases.valueOf(s.lowercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid test cases: $s")
            }
        }
    }
}
