package io.bluetape4k.qdrant.client

import io.qdrant.client.QdrantGrpcClient

inline fun qdrantGrpcClient(
    host: String,
    port: Int,
    useTransportLayerSecurity: Boolean = false,
    block: QdrantGrpcClient.Builder.() -> Unit = {}
): QdrantGrpcClient =
    QdrantGrpcClient.newBuilder(host, port, useTransportLayerSecurity).apply(block).build()
