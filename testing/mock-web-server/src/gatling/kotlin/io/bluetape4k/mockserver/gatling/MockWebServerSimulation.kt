package io.bluetape4k.mockserver.gatling

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration

/**
 * mock-web-server 스트레스 테스트 시뮬레이션.
 *
 * 5가지 시나리오: Health, Httpbin echo, Streaming, Delay, CRUD
 * 실행 전 서버가 포트 80 에서 실행 중이어야 한다.
 */
class MockWebServerSimulation: Simulation() {

    private val baseUrl = System.getProperty("mock.web.server.baseUrl", "http://localhost:80")

    private val httpProtocol = http.baseUrl(baseUrl).acceptHeader("application/json")

    private val healthScenario = scenario("health")
        .exec(http("GET /ping").get("/ping").check(status().`is`(200)))

    private val echoScenario = scenario("httpbin-echo")
        .exec(http("GET /httpbin/get").get("/httpbin/get").check(status().`is`(200)))

    private val streamScenario = scenario("stream")
        .exec(http("GET /httpbin/stream/100").get("/httpbin/stream/100").check(status().`is`(200)))

    private val delayScenario = scenario("delay")
        .exec(http("GET /httpbin/delay/1").get("/httpbin/delay/1").check(status().`is`(200)))

    private val crudScenario = scenario("crud-posts")
        .exec(
            http("POST /jsonplaceholder/posts")
                .post("/jsonplaceholder/posts")
                .body(StringBody("""{"title":"gatling","body":"b","userId":1}"""))
                .header("Content-Type", "application/json")
                .check(status().`is`(201))
        )
        .exec(http("GET /jsonplaceholder/posts/1").get("/jsonplaceholder/posts/1").check(status().`is`(200)))
        .exec(
            http("PATCH /jsonplaceholder/posts/1")
                .patch("/jsonplaceholder/posts/1")
                .body(StringBody("""{"title":"t2"}"""))
                .header("Content-Type", "application/json")
                .check(status().`is`(200))
        )
        .exec(http("DELETE /jsonplaceholder/posts/1").delete("/jsonplaceholder/posts/1").check(status().`is`(200)))

    init {
        setUp(
            healthScenario.injectOpen(constantUsersPerSec(200.0).during(Duration.ofSeconds(30))),
            echoScenario.injectOpen(rampUsers(100).during(Duration.ofSeconds(60))),
            streamScenario.injectOpen(atOnceUsers(20)),
            delayScenario.injectOpen(rampUsers(50).during(Duration.ofSeconds(30))),
            crudScenario.injectOpen(rampUsers(50).during(Duration.ofSeconds(60)))
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(50),
                global().failedRequests().percent().lt(1.0)
            )
    }
}
