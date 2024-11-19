package io.nats.examples.service

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.natsOptions
import io.bluetape4k.nats.client.requestAsync
import io.bluetape4k.nats.service.endpointOf
import io.bluetape4k.nats.service.natsService
import io.bluetape4k.nats.service.serviceEndpoint
import io.bluetape4k.support.toUtf8String
import io.nats.client.Connection
import io.nats.client.ErrorListener
import io.nats.client.Nats
import io.nats.client.support.JsonSerializable
import io.nats.client.support.JsonValue
import io.nats.client.support.JsonValueUtils
import io.nats.service.Discovery
import io.nats.service.Endpoint
import io.nats.service.Group
import io.nats.service.ServiceMessage
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class ServiceExample: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `service example`() {
        val options = natsOptions {
            server(nats.url)
            errorListener(object: ErrorListener {})
        }
        Nats.connect(options).use { nc ->
            // endpoints can be created ahead of time
            // or created directly by the ServiceEndpoint builder.
            val epEcho = endpointOf("EchoEndpoint", "echo")

            // Sort is going to be grouped. This will affect the actual subject
            val sortGroup = Group("sort")

            // 4 service endpoints. 3 in service 1, 1 in service 2
            // - We will reuse an endpoint definition, so we make it ahead of time
            // - For echo, we could have reused a handler as well, if we wanted to.
            val seEcho1 = serviceEndpoint {
                endpoint(epEcho)
                handler { msg -> handleEchoMessage(nc, msg, "S1E") }
                statsDataSupplier(ExampleStatsDataSupplier())
            }

            val seEcho2 = serviceEndpoint {
                endpoint(epEcho)
                handler { msg -> handleEchoMessage(nc, msg, "S2E") }
            }

            // you can make the Endpoint directly on the Service Endpoint Builder
            val seSort1A = serviceEndpoint {
                group(sortGroup)
                endpointName("SortEndpointAscending")
                endpointSubject("ascending")
                handler { msg -> handleSortAscending(nc, msg, "S1A") }
            }

            // you can also make an endpoint with a constructor instead of a builder.
            val endSortD = Endpoint("SortEndpointDescending", "descending")
            val seSort1D = serviceEndpoint {
                group(sortGroup)
                endpoint(endSortD)
                handler { msg -> handleSortDescending(nc, msg, "S1D") }
            }

            // Create the service from service endpoints.
            val service1 = natsService {
                connection(nc)
                name("Service1")
                description("Service1 Description")
                version("0.0.1")
                addServiceEndpoint(seEcho1)
                addServiceEndpoint(seSort1A)
                addServiceEndpoint(seSort1D)
            }

            val service2 = natsService {
                connection(nc)
                name("Service2")
                version("0.0.1")
                addServiceEndpoint(seEcho2)
            }

            log.debug { "service1=$service1" }
            log.debug { "service2=$service2" }

            // ----------------------------------------------------------------------------------------------------
            // Start the services
            // ----------------------------------------------------------------------------------------------------
            val serviceStoppedFuture1 = service1.startService()
            val serviceStoppedFuture2 = service2.startService()

            // ----------------------------------------------------------------------------------------------------
            // Call the services
            // ----------------------------------------------------------------------------------------------------
            val subject = "echo"
            var request = randomText()
            for (x in 1..9) {  // run ping a few times to see it hit different services
                request = randomText()
                val reply = nc.requestAsync(subject, request)
                val response = reply.get().data?.toUtf8String()
                log.debug { "$x. Called $subject with [$request] Received: $response" }
            }

            // sort subjects are formed this way because the endpoints have groups
            val subjectAscending = "sort.ascending"
            val reply = nc.requestAsync(subjectAscending, request)
            val response = reply.get().data?.toUtf8String()
            log.debug { "1. Called $subjectAscending with [$request] Received: $response" }

            val subjectDescending = "sort.descending"
            val reply2 = nc.requestAsync(subjectDescending, request)
            val response2 = reply2.get().data?.toUtf8String()
            log.debug { "2. Called $subjectDescending with [$request] Received: $response2" }

            // ----------------------------------------------------------------------------------------------------
            // discovery
            // ----------------------------------------------------------------------------------------------------
            val discovery = Discovery(nc, 1000, 3)

            // ----------------------------------------------------------------------------------------------------
            // ping discover variations
            // ----------------------------------------------------------------------------------------------------
            var pingResponses = discovery.ping()
            printDiscovery("Ping", "[All]", pingResponses)

            pingResponses = discovery.ping("Service1")
            printDiscovery("Ping", "Service1", pingResponses)

            pingResponses = discovery.ping("Service2")
            printDiscovery("Ping", "Service2", pingResponses)

            // ----------------------------------------------------------------------------------------------------
            // info discover variations
            // ----------------------------------------------------------------------------------------------------
            var infoResponses = discovery.info()
            printDiscovery("Info", "[All]", infoResponses)

            infoResponses = discovery.info("Service1")
            printDiscovery("Info", "Service1", infoResponses)

            infoResponses = discovery.info("Service2")
            printDiscovery("Info", "Service2", infoResponses)

            // ----------------------------------------------------------------------------------------------------
            // info discover variations
            // ----------------------------------------------------------------------------------------------------
            var statsResponses = discovery.stats()
            printDiscovery("Stats", "[All]", statsResponses)

            statsResponses = discovery.stats("Service1")
            printDiscovery("Stats", "Service1", statsResponses)

            statsResponses = discovery.stats("Service2")
            printDiscovery("Stats", "Service2", statsResponses)

            // ----------------------------------------------------------------------------------------------------
            // stop the service
            // ----------------------------------------------------------------------------------------------------
            service1.stop()
            service2.stop()

            // stopping the service will complete the futures received when starting the service
            log.debug { "Service 1 stopped ? ${serviceStoppedFuture1[1, TimeUnit.SECONDS]}" }
            log.debug { "Service 2 stopped ? ${serviceStoppedFuture2[2, TimeUnit.SECONDS]}" }
        }
    }

    private fun replyBody(label: String, data: ByteArray, handlerId: String): JsonValue {
        return JsonValueUtils.mapBuilder()
            .put(label, data.toUtf8String())
            .put("hid", handlerId)
            .toJsonValue()
    }


    private fun handleSortDescending(nc: Connection, smsg: ServiceMessage, handlerId: String) {
        val data = smsg.data
        Arrays.sort(data)
        val len = data.size
        val descending = ByteArray(len)
        for (x in 0..<len) {
            descending[x] = data[len - x - 1]
        }
        smsg.respond(nc, replyBody("sort_descending", descending, handlerId))
    }

    private fun handleSortAscending(nc: Connection, smsg: ServiceMessage, handlerId: String) {
        val ascending = smsg.data
        Arrays.sort(ascending)
        smsg.respond(nc, replyBody("sort_ascending", ascending, handlerId))
    }

    private fun handleEchoMessage(nc: Connection, smsg: ServiceMessage, handlerId: String) {
        smsg.respond(nc, replyBody("echo", smsg.data, handlerId))
    }

    private fun printDiscovery(action: String, label: String, objects: List<*>) {
        log.debug { "$action $label" }
        objects.forEach {
            log.debug { "  $it" }
        }
    }

    class ExampleStatsData(
        val sData: String,
        val iData: Int,
    ): JsonSerializable {
        override fun toJson(): String {
            return toJsonValue().toJson()
        }

        override fun toJsonValue(): JsonValue {
            return JsonValueUtils.mapBuilder()
                .put("sdata", sData)
                .put("idata", iData)
                .toJsonValue()
        }

        override fun toString(): String = toJsonValue().toString(javaClass)
    }

    class ExampleStatsDataSupplier: Supplier<JsonValue> {
        private var x = 0
        override fun get(): JsonValue {
            ++x
            return ExampleStatsData("s-" + hashCode(), x).toJsonValue()
        }
    }

    fun randomText(): String =
        System.currentTimeMillis().toHexString() + System.nanoTime().toHexString()
}
