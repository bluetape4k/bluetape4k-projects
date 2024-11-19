package io.bluetape4k.nats.client

import io.nats.client.JetStreamApiException

const val JET_STREAM_NOT_FOUND = 10059

val JetStreamApiException.isNotFound: Boolean get() = apiErrorCode == JET_STREAM_NOT_FOUND
