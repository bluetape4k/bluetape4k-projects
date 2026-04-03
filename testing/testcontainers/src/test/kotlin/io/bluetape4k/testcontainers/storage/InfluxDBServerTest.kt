package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest

@TestInstance(Lifecycle.PER_CLASS)
class InfluxDBServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `influxdb server 실행`() {
        val influxdb = InfluxDBServer.Launcher.influxDB

        // 서버 상태 확인
        influxdb.isRunning.shouldBeTrue()
        influxdb.port.shouldBeGreaterThan(0)

        // 시스템 프로퍼티 확인
        System.getProperty("testcontainers.influxdb.host").shouldNotBeNullOrBlank()
        System.getProperty("testcontainers.influxdb.port").shouldNotBeNullOrBlank()
        System.getProperty("testcontainers.influxdb.url").shouldNotBeNullOrBlank()
        System.getProperty("testcontainers.influxdb.organization").shouldNotBeNullOrBlank()
        System.getProperty("testcontainers.influxdb.bucket").shouldNotBeNullOrBlank()
        System.getProperty("testcontainers.influxdb.admin-token").shouldNotBeNullOrBlank()

        // InfluxDB ping 확인 (health check)
        val client = HttpClient.newHttpClient()
        val pingRequest = HttpRequest.newBuilder()
            .uri(URI.create("${influxdb.url}/ping"))
            .GET()
            .build()

        val response = client.send(pingRequest, java.net.http.HttpResponse.BodyHandlers.discarding())
        response.statusCode() shouldBeEqualTo 204

        // 관리자 토큰 확인
        influxdb.adminToken.shouldNotBeNullOrBlank()

        // API 호출로 버킷 목록 조회 (Authorization 헤더 포함)
        val bucketsRequest = HttpRequest.newBuilder()
            .uri(URI.create("${influxdb.url}/api/v2/buckets"))
            .header("Authorization", "Token ${influxdb.adminToken}")
            .GET()
            .build()

        val bucketsResponse = client.send(bucketsRequest, java.net.http.HttpResponse.BodyHandlers.discarding())
        bucketsResponse.statusCode() shouldBeEqualTo 200
    }

    @Test
    fun `default 프로퍼티 확인`() {
        InfluxDBServer.Launcher.influxDB.apply {
            organization shouldBeEqualTo InfluxDBServer.DEFAULT_ORG
            bucket shouldBeEqualTo InfluxDBServer.DEFAULT_BUCKET
            adminToken.shouldNotBeNullOrBlank()
        }
    }
}
