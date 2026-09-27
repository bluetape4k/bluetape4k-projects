package io.bluetape4k.qdrant.client.grpc

import io.qdrant.client.grpc.Common
import io.qdrant.client.grpc.Common.PointId
import java.util.*

inline fun pointId(
    builder: Common.PointId.Builder.() -> Unit
): Common.PointId =
    Common.PointId.newBuilder().apply(builder).build()


fun pointIdOf(num: Long): PointId =
    pointId { this.num = num }

fun pointIdOf(uuid: UUID): PointId =
    pointId { this.uuid = uuid.toString() }


inline fun filter(
    block: Common.Filter.Builder.() -> Unit
): Common.Filter =
    Common.Filter.newBuilder().apply(block).build()
