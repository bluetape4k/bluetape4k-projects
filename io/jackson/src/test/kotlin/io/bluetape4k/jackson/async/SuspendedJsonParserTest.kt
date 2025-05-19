package io.bluetape4k.jackson.async

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.treeToValueOrNull
import io.bluetape4k.jackson.writeAsBytes
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class SuspendedJsonParserTest {

    companion object: KLoggingChannel()

    data class Model(
        val stringValue: String? = null,
        val intValue: Int? = null,

        val inner: Model? = null,
        val nullable: Double? = null,
        val booleanValue: Boolean = true,
    ) {
        var innerArray: Array<Model>? = null
        var intArray: IntArray? = null
    }

    private val mapper = Jackson.defaultJsonMapper

    private val model = Model(
        stringValue = "안녕하세요",
        intValue = 2,
        inner = Model(
            stringValue = "inner",
        ).apply {
            intArray = intArrayOf(5, 6, 7)
        },
        nullable = null,
        booleanValue = true
    ).apply {
        innerArray = arrayOf(
            Model(stringValue = "innerArray1"),
            Model(stringValue = "innerArray2"),
        )
        intArray = intArrayOf(2, 3, 4)
    }

    @Test
    fun `parse one byte`() = runTest {
        val parsed = atomic(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model)!!
        // 1 byte 씩 consume 한다
        val flow = bytes.map { byteArrayOf(it) }.asFlow()
        parser.consume(flow)

        parsed.value shouldBeEqualTo 1
    }

    @Test
    fun `parse chunks`() = runTest {
        val parsed = atomic(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model)!!
        val chunkSize = 20

        val flow: Flow<ByteArray> = bytes.toList().chunked(chunkSize).map { it.toByteArray() }.asFlow()
        parser.consume(flow)

        parsed.value shouldBeEqualTo 1
    }

    @Test
    fun `parse object sequence`() = runTest {
        val parsed = atomic(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model)!!
        val repeatSize = 3
        repeat(repeatSize) {
            val flow = bytes.map { byteArrayOf(it) }.asFlow()
            parser.consume(flow)
        }
        parsed.value shouldBeEqualTo repeatSize
    }

    @Test
    fun `parse chunk sequence`() = runTest {
        val parsed = atomic(0)
        val parser = getSingleModelParser(parsed)

        val bytes: ByteArray = mapper.writeAsBytes(model)!!
        val repeatSize = 3
        val chunkSize = 20
        repeat(repeatSize) {
            val flow = bytes.toList().chunked(chunkSize).map { it.toByteArray() }.asFlow()
                .onEach { log.debug { it.toUtf8String() } }
            parser.consume(flow)
        }

        parsed.value shouldBeEqualTo repeatSize
    }

    private fun getSingleModelParser(parsed: AtomicInt): SuspendedJsonParser {
        return SuspendedJsonParser { root ->
            try {
                parsed.incrementAndGet()
                mapper.treeToValueOrNull<Model>(root) shouldBeEqualTo model
            } catch (e: JsonProcessingException) {
                fail(e)
            }
        }
    }


    @Test
    fun `parse array object`() = runTest {
        val parsed = atomic(0)
        val modelSize = 5

        val parser = SuspendedJsonParser { root ->
            parsed.incrementAndGet()

            val deserialized: Array<Model> = mapper.treeToValue<Array<Model>>(root)
            log.debug { deserialized.contentToString() }
            deserialized shouldHaveSize modelSize
            deserialized shouldBeEqualTo Array(modelSize) { model }
        }

        val bytes = mapper.writeAsBytes(model)!!
        parser.consume(flowOf("[".toByteArray()))

        repeat(modelSize) {
            val flow = bytes.map { b -> byteArrayOf(b) }.asFlow()
            parser.consume(flow)

            if (it != modelSize - 1) {
                parser.consume(flowOf(",".toByteArray()))
            }
        }
        parser.consume(flowOf("]".toByteArray()))

        parsed.value shouldBeEqualTo 1
    }
}
