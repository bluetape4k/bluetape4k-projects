package io.bluetape4k.qdrant.client.grpc

import io.bluetape4k.support.requireNotBlank
import io.qdrant.client.grpc.Common
import io.qdrant.client.grpc.Points
import java.util.*

inline fun deletePoints(
    block: Points.DeletePoints.Builder.() -> Unit
): Points.DeletePoints =
    Points.DeletePoints.newBuilder().apply(block).build()

fun deletePointsOf(
    collectionName: String,
    wait: Boolean = false,
    block: Points.DeletePoints.Builder.() -> Unit = {}
): Points.DeletePoints =
    deletePoints {
        this.collectionName = collectionName.requireNotBlank("collectionName")
        this.wait = wait
        block()
    }

inline fun pointsSelector(
    block: Points.PointsSelector.Builder.() -> Unit
): Points.PointsSelector =
    Points.PointsSelector.newBuilder().apply(block).build()

inline fun pointStruct(
    block: Points.PointStruct.Builder.() -> Unit
): Points.PointStruct =
    Points.PointStruct.newBuilder().apply(block).build()

fun pointStructOf(
    id: Long,
    block: Points.PointStruct.Builder.() -> Unit = {}
): Points.PointStruct =
    pointStruct {
        this.id = pointIdOf(id)
        block()
    }

fun pointStructOf(
    id: UUID,
    block: Points.PointStruct.Builder.() -> Unit = {}
): Points.PointStruct =
    pointStruct {
        this.id = pointIdOf(id)
        block()
    }


inline fun queryPoints(
    block: Points.QueryPoints.Builder.() -> Unit
): Points.QueryPoints =
    Points.QueryPoints.newBuilder().apply(block).build()

inline fun retrievedPoint(
    block: Points.RetrievedPoint.Builder.() -> Unit
): Points.RetrievedPoint =
    Points.RetrievedPoint.newBuilder().apply(block).build()

inline fun scrollPoints(
    block: Points.ScrollPoints.Builder.() -> Unit
): Points.ScrollPoints =
    Points.ScrollPoints.newBuilder().apply(block).build()

inline fun scrollResponse(
    block: Points.ScrollResponse.Builder.() -> Unit
): Points.ScrollResponse =
    Points.ScrollResponse.newBuilder().apply(block).build()

fun scrollResponseOf(
    nextPageOffset: Common.PointId,
    block: Points.ScrollResponse.Builder.() -> Unit = {}
): Points.ScrollResponse =
    scrollResponse {
        this.nextPageOffset = nextPageOffset
        block()
    }

fun scrollResponseOf(
    offset: Long,
    block: Points.ScrollResponse.Builder.() -> Unit = {}
): Points.ScrollResponse =
    scrollResponse {
        nextPageOffset = pointIdOf(offset)
        block()
    }

inline fun upsertPoints(
    block: Points.UpsertPoints.Builder.() -> Unit
): Points.UpsertPoints =
    Points.UpsertPoints.newBuilder().apply(block).build()

fun upsertPointsOf(
    collectionName: String,
    wait: Boolean = false,
    block: Points.UpsertPoints.Builder.() -> Unit = {}
): Points.UpsertPoints =
    upsertPoints {
        this.collectionName = collectionName.requireNotBlank("collections")
        this.wait = wait
        block()
    }
