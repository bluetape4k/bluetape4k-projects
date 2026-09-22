package io.bluetape4k.qdrant.client.grpc

import io.qdrant.client.grpc.Collections

inline fun vectorParams(
    builder: Collections.VectorParams.Builder.() -> Unit
): Collections.VectorParams =
    Collections.VectorParams.newBuilder().apply(builder).build()
