package io.bluetape4k.grpc.examples.routeguide

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.grpc.AbstractGrpcClient
import io.bluetape4k.grpc.managedChannel
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ForkJoinPool
import kotlin.random.Random

class RouteGuideClient private constructor(channel: ManagedChannel): AbstractGrpcClient(channel) {

    companion object: KLogging() {
        private val random = Random(System.currentTimeMillis())

        operator fun invoke(host: String, port: Int): RouteGuideClient {
            return invoke(managedChannel(host, port) {
                usePlaintext()
                executor(ForkJoinPool.commonPool())
            })
        }

        operator fun invoke(channel: ManagedChannel): RouteGuideClient {
            check(!channel.isShutdown) { "Channel must not be shutdown." }
            return RouteGuideClient(channel)
        }

        operator fun invoke(
            channelBuilder: ManagedChannelBuilder<*>,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): RouteGuideClient {
            return invoke(channelBuilder.executor(dispatcher.asExecutor()).build())
        }
    }

    private val stub = RouteGuideGrpcKt.RouteGuideCoroutineStub(channel)

    fun getFeature(latitude: Int, longitude: Int) = runBlocking {
        log.info { "GetFeature: lat=$latitude, lon=$longitude" }

        val point = pointOf(latitude, longitude)
        val feature = stub.getFeature(point)

        if (feature.exists()) {
            log.debug { "Found feature called '${feature.name}' at ${feature.location.toStr()}" }
        } else {
            log.debug { "Found no feature at ${point.toStr()}" }
        }
    }

    fun listFeatures(lowLat: Int, lowLon: Int, hiLat: Int, hiLon: Int) = runBlocking {
        log.info { "ListFeatures: lowLat=$lowLat, lowLon=$lowLon, hiLat=$hiLat, hiLon=$hiLon" }

        val rectangle = Rectangle.newBuilder()
            .apply {
                lo = pointOf(lowLat, lowLon)
                hi = pointOf(hiLat, hiLon)
            }
            .build()

        var i = 1
        stub.listFeatures(rectangle)
            .buffer()
            .log("feature")
            .collect { feature ->
                log.debug { "Result #${i++}: $feature" }
            }
    }

    fun recordRoute(points: Flow<Point>) = runBlocking(Dispatchers.IO) {
        log.info { "Record route..." }

        val summary = stub.recordRoute(points)

        log.debug {
            """
            |
            |Finished trip with ${summary.pointCount} points.
            |Passed ${summary.featureCount} features.
            |Travelled ${summary.distance} meters.
            """.trimMargin()
        }

        val durationSeconds = summary.elapsedTime.seconds
        log.debug { "It took $durationSeconds seconds." }
    }

    fun generateRoutePoints(features: List<Feature>, numPoints: Int): Flow<Point> = flow {
        repeat(numPoints) {
            val feature = features.random(random)
            log.debug { "Visiting point ${feature.location.toStr()}" }

            emit(feature.location)
            delay(random.nextLong(100L, 300L))
        }
    }

    fun routeChat() {
        runBlocking {
            log.info { "RouteChat ..." }
            val routeNotes = generateOutgoingNotes()

            stub.routeChat(routeNotes)
                .log("note")
                .collect { note ->
                    log.debug { "Got message '${note.message}' at ${note.location.toStr()}" }
                }

            log.info { "Finish RouteChat." }
        }
    }

    private fun generateOutgoingNotes(): Flow<RouteNote> = flow {
        val notes = listOf(
            routeNoteOf("First message", pointOf(0, 0)),
            routeNoteOf("Second message", pointOf(0, 0)),
            routeNoteOf("Third message", pointOf(10_000_000, 0)),
            routeNoteOf("Fourth message", pointOf(10_000_000, 10_000_000)),
            routeNoteOf("Last message", pointOf(0, 0))
        )

        notes.forEach { note ->
            log.debug { "Sending message '${note.message}' at ${note.location.toStr()}" }
            emit(note)
            delay(100)
        }
    }
}
