package io.bluetape4k.grpc.testing.integration

import io.bluetape4k.support.requireNotBlank

enum class TestCases(val description: String) {

    EMPTY_UNARY("empty (zero bytes) request and response"),
    CACHEABLE_UNARY("cacheable unary rpc sent using GET"),
    LARGE_UNARY("single request and (large) response"),
    CLIENT_COMPRESSED_UNARY("client compressed unary request"),
    CLIENT_COMPRESSED_UNARY_NOPROBE(
        "client compressed unary request (skip initial feature-probing request)"
    ),
    SERVER_COMPRESSED_UNARY("server compressed unary response"),
    CLIENT_STREAMING("request streaming with single response"),
    CLIENT_COMPRESSED_STREAMING("client per-message compression on stream"),
    CLIENT_COMPRESSED_STREAMING_NOPROBE(
        "client per-message compression on stream (skip initial feature-probing request)"
    ),
    SERVER_STREAMING("single request with response streaming"),
    SERVER_COMPRESSED_STREAMING("server per-message compression on stream"),
    PING_PONG("full-duplex ping-pong streaming"),
    EMPTY_STREAM("A stream that has zero-messages in both directions"),
    COMPUTE_ENGINE_CREDS("large_unary with service_account auth"),
    COMPUTE_ENGINE_CHANNEL_CREDENTIALS("large unary with compute engine channel builder"),
    SERVICE_ACCOUNT_CREDS("large_unary with compute engine auth"),
    JWT_TOKEN_CREDS("JWT-based auth"),
    OAUTH2_AUTH_TOKEN("raw oauth2 access token auth"),
    PER_RPC_CREDS("per rpc raw oauth2 access token auth"),
    GOOGLE_DEFAULT_CREDENTIALS(
        "google default credentials, i.e. GoogleManagedChannel based auth"
    ),
    CUSTOM_METADATA("unary and full duplex calls with metadata"),
    STATUS_CODE_AND_MESSAGE("request error code and message"),
    SPECIAL_STATUS_MESSAGE("special characters in status message"),
    UNIMPLEMENTED_METHOD("call an unimplemented RPC method"),
    UNIMPLEMENTED_SERVICE("call an unimplemented RPC service"),
    CANCEL_AFTER_BEGIN("cancel stream after starting it"),
    CANCEL_AFTER_FIRST_RESPONSE("cancel on first response"),
    TIMEOUT_ON_SLEEPING_SERVER("timeout before receiving a response"),
    VERY_LARGE_REQUEST("very large request"),
    PICK_FIRST_UNARY("all requests are sent to one server despite multiple servers are resolved");


    companion object {
        @JvmStatic
        fun fromString(s: String): TestCases {
            s.requireNotBlank("s")
            return TestCases.valueOf(s.lowercase())
        }
    }
}
