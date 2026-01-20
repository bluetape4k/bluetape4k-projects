package io.bluetape4k.grpc.examples.routeguide

import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import com.google.protobuf.util.Durations
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class RouteGuideService @JvmOverloads constructor(
    val features: Collection<Feature>,
    val ticker: Ticker = Ticker.systemTicker(),
): RouteGuideGrpcKt.RouteGuideCoroutineImplBase() {

    companion object: KLogging()

    private val routeNotes = mutableMapOf<Point, MutableList<RouteNote>>()

    override suspend fun getFeature(request: Point): Feature {
        return features.find { it.location == request }
            ?: Feature.newBuilder().setLocation(request).build()
    }

    override fun listFeatures(request: Rectangle): Flow<Feature> {
        return features.asFlow().filter { it.exists() && it.location in request }
    }

    override suspend fun recordRoute(requests: Flow<Point>): RouteSummary {
        var pointCount = 0
        var featureCount = 0
        var distance = 0
        var previous: Point? = null
        val stopwatch = Stopwatch.createStarted(ticker)

        requests.collect { request ->
            pointCount++
            if (getFeature(request).exists()) {
                featureCount++
            }
            val prev = previous
            if (prev != null) {
                distance += prev distanceTo request
            }
            previous = request
        }

        return RouteSummary.newBuilder()
            .apply {
                this.pointCount = pointCount
                this.featureCount = featureCount
                this.distance = distance
                this.elapsedTime = Durations.fromMicros(stopwatch.elapsed(TimeUnit.MICROSECONDS))
            }
            .build()
    }

    override fun routeChat(requests: Flow<RouteNote>): Flow<RouteNote> = channelFlow {
        requests
            .collect { note ->
                val notes = routeNotes.computeIfAbsent(note.location) {
                    CopyOnWriteArrayList()
                }

                // thread-safe snapshot
                notes.forEach { prevNote ->
                    send(prevNote)
                }

                notes.add(note)
            }
    }
}
