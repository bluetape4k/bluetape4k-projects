package io.bluetape4k.grpc.examples.routeguide

import io.bluetape4k.junit5.output.OutputCapture
import io.bluetape4k.junit5.output.OutputCapturer
import io.bluetape4k.logging.KLogging
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.asCoroutineDispatcher
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

@OutputCapture
class RouteGuideServiceTest {

    companion object: KLogging() {
        private const val ROUTE_GUIDE_PORT = 8990
    }

    private lateinit var server: RouteGuideServer
    private lateinit var client: RouteGuideClient

    @BeforeAll
    fun setup() {
        server = RouteGuideServer(ROUTE_GUIDE_PORT).apply { start() }
        client = RouteGuideClient("localhost", ROUTE_GUIDE_PORT)
    }

    @AfterAll
    fun cleanup() {
        client.close()
        server.close()
    }

    @Test
    fun `instancing server and client`() {
        server.shouldNotBeNull()
        server.isRunning.shouldBeTrue()
        client.shouldNotBeNull()
    }

    @Test
    fun `get feature`(output: OutputCapturer) {
        client.getFeature(0, 0)
        output.capture() shouldContain "Found no feature at 0.0, 0.0"

        client.getFeature(406109563, -742186778)
        output.capture() shouldContain "Found feature called '4001 Tremley Point Road, Linden, NJ 07036, USA' at 40.6109563, -74.2186778"
    }

    @Test
    fun `list features`(output: OutputCapturer) {
        client.listFeatures(400000000, -750000000, 420000000, -730000000)

        with(output.capture()) {
            this shouldContain "Result #1:"
            this shouldContain "Result #64:"
        }
    }

    @Test
    fun `record routes`(output: OutputCapturer) {
        val features = defaultFeatureString().parseJsonFeatures()
        client.recordRoute(client.generateRoutePoints(features, 10))

        with(output.capture()) {
            this shouldContain "Visiting point"
            this shouldContain "Finished trip with 10 points."
        }
    }

    @Test
    fun `route chat`(output: OutputCapturer) {
        client.routeChat()

        with(output.capture()) {
            this shouldContain "'First message'"
            this shouldContain "'Second message'"
        }
    }


    @Test
    fun `route guide with coroutines`() {
        val features = defaultFeatureString().parseJsonFeatures()

        Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
            val channelBuilder = ManagedChannelBuilder
                .forAddress("localhost", ROUTE_GUIDE_PORT)
                .usePlaintext()

            RouteGuideClient(channelBuilder, dispatcher).use { client ->
                client.getFeature(409146138, -746188906)
                client.getFeature(0, 0)
                client.listFeatures(400000000, -750000000, 420000000, -730000000)
                client.recordRoute(client.generateRoutePoints(features, 10))
                client.routeChat()
            }
        }
    }
}
