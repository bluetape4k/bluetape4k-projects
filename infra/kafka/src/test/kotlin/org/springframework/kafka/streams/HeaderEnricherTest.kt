package org.springframework.kafka.streams

import io.bluetape4k.logging.KLogging

class HeaderEnricherTest {

    companion object: KLogging() {
        private const val INPUT = "input"
        private const val OUTPUT = "output"
    }

    //    @Test
    //    fun `enrich header with driver`() {
    //        val builder = StreamsBuilder()
    //        val headers = mutableMapOf<String, Expression>()
    //        headers.put("foo", LiteralExpression("bar"))
    //        val parser = SpelExpressionParser()
    //        headers.put("spel", parser.parseExpression("""context.timestamp() + '_' + key + '_' + value"""))
    //        val enricher = HeaderEnricher<String, String>(headers)
    //
    //        val stream = builder.stream<String, String>(INPUT)
    //
    //        stream
    //            .transform({ enricher })
    //            .to(OUTPUT)
    //
    //        val config = Properties()
    //        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test")
    //        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9999")
    //        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde::class.java)
    //        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde::class.java)
    //
    //        val driver = TopologyTestDriver(builder.build(), config)
    //
    //        val inputTopic = driver.createInputTopic(
    //            INPUT,
    //            StringSerializer(),
    //            StringSerializer()
    //        )
    //
    //        inputTopic.pipeInput("key1", "value1")
    //
    //        val outputTopic = driver.createOutputTopic(
    //            OUTPUT,
    //            StringDeserializer(),
    //            StringDeserializer()
    //        )
    //
    //        val result = outputTopic.readRecord()
    //        result.headers.lastHeader("foo").shouldNotBeNull()
    //        result.headers.lastHeader("foo").value() shouldBeEqualTo "bar".toUtf8Bytes()
    //        result.headers.lastHeader("spel").shouldNotBeNull()
    //
    //        log.debug { "spel=${result.headers.lastHeader("spel").value().toUtf8String()}" }
    //        result.headers.lastHeader("spel").value().toUtf8String().endsWith("key1_value1").shouldBeTrue()
    //
    //        driver.close()
    //    }
}
