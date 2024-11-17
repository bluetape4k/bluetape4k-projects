package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.FlowNoElementException
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class FlowEventTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `toString of FlowEvent Value`() {
        FlowEvent.Value(1).toString() shouldBeEqualTo "FlowEvent.Value(1)"
        FlowEvent.Value("Hello, World!").toString() shouldBeEqualTo "FlowEvent.Value(Hello, World!)"
    }

    @Test
    fun `equals and hashCode of FlowEvent Value`() {
        val list1 = listOf(1, 2, 3)
        val list2 = listOf(1, 2, 3)
        list1 shouldBeEqualTo list2

        val ev1 = FlowEvent.Value(list1)
        val ev2 = FlowEvent.Value(list2)
        ev1 shouldBeEqualTo ev2
        ev1.hashCode() shouldBeEqualTo ev2.hashCode()
    }

    @Test
    fun `toString ofFlowEvent Error`() {
        val error = RuntimeException("Boom!")
        FlowEvent.Error(error).toString() shouldBeEqualTo "FlowEvent.Error($error)"
    }

    @Test
    fun `equals and hashCode of FlowEvent Error`() {
        val e = RuntimeException("Boom!")

        FlowEvent.Error(e) shouldBeEqualTo FlowEvent.Error(e)
        FlowEvent.Error(e).hashCode() shouldBeEqualTo FlowEvent.Error(e).hashCode()

        e.hashCode() shouldBeEqualTo FlowEvent.Error(e).hashCode()
    }

    @Test
    fun `toString ofFlowEvent Complete`() {
        FlowEvent.Complete.toString() shouldBeEqualTo "FlowEvent.Complete"
    }

    @Test
    fun `map FlowEvent`() {

        FlowEvent.Value(1).map { it + 1 } shouldBeEqualTo FlowEvent.Value(2)

        assertFailsWith<RuntimeException> {
            FlowEvent.Value(1).map { throw RuntimeException("Boom!") }
        }.message shouldBeEqualTo "Boom!"

        val e2: FlowEvent<Int> = FlowEvent.Error(RuntimeException("1"))
        e2.map { it + 1 } shouldBeEqualTo e2

        val completeEvent: FlowEvent<Int> = FlowEvent.Complete
        completeEvent.map { it + 1 } shouldBeEqualTo completeEvent
    }

    @Test
    fun `flatMap FlowEvent`() {
        FlowEvent.Value(1).flatMap { FlowEvent.Value(it + 1) } shouldBeEqualTo FlowEvent.Value(2)

        FlowEvent.Value(1).flatMap { FlowEvent.Complete } shouldBeEqualTo FlowEvent.Complete

        val ex = RuntimeException("Boom!")
        FlowEvent.Value(1).flatMap { FlowEvent.Error(ex) } shouldBeEqualTo FlowEvent.Error(ex)

        assertFailsWith<RuntimeException> {
            FlowEvent.Value(1).flatMap<Int, String> { throw RuntimeException("error") }
        }.message shouldBeEqualTo "error"

        val errorEvent: FlowEvent<Int> = FlowEvent.Error(RuntimeException("1"))
        errorEvent.flatMap { FlowEvent.Value(it + 1) } shouldBeEqualTo errorEvent

        val complete: FlowEvent<Int> = FlowEvent.Complete
        complete.flatMap { FlowEvent.Value(it + 1) } shouldBeEqualTo complete
    }

    @Test
    fun `valueOrNull for FlowEvent`() {
        FlowEvent.Value(1).valueOrNull() shouldBeEqualTo 1
        FlowEvent.Error(RuntimeException("Boom!")).valueOrNull<Int>().shouldBeNull()
        FlowEvent.Complete.valueOrNull<Int>().shouldBeNull()
    }

    @Test
    fun `valueOrDefault for FlowEvent`() {
        val defaultValue = 2

        FlowEvent.Value(1).valueOrDefault(defaultValue) shouldBeEqualTo 1
        FlowEvent.Error(RuntimeException("Boom!")).valueOrDefault(defaultValue) shouldBeEqualTo defaultValue
        FlowEvent.Complete.valueOrDefault(defaultValue) shouldBeEqualTo defaultValue
    }

    @Test
    fun `valueOrThrow for FlowEvent`() {
        FlowEvent.Value(1).valueOrThrow() shouldBeEqualTo 1
        assertFailsWith<RuntimeException> {
            FlowEvent.Error(RuntimeException("1")).valueOrThrow()
        }.message shouldBeEqualTo "1"

        assertFailsWith<FlowNoElementException> {
            FlowEvent.Complete.valueOrThrow()
        }.message shouldBeEqualTo "FlowEvent.Complete has no value!"
    }

    @Test
    fun `valueOrElse for FlowEvent`() {
        val defaultValue = 2

        FlowEvent.Value(1).valueOrElse { defaultValue } shouldBeEqualTo 1
        FlowEvent.Error(RuntimeException("Boom!")).valueOrElse { defaultValue } shouldBeEqualTo defaultValue
        FlowEvent.Complete.valueOrElse { defaultValue } shouldBeEqualTo defaultValue
    }

    @Test
    fun `errorOrNull for FlowEvent`() {
        val ex = RuntimeException("Boom!")
        FlowEvent.Value(1).errorOrNull().shouldBeNull()
        FlowEvent.Error(ex).errorOrNull() shouldBeEqualTo ex
        FlowEvent.Complete.errorOrNull().shouldBeNull()
    }

    @Test
    fun `errorOrThrow for FlowEvent`() {
        val ex = RuntimeException("Boom!")

        assertFailsWith<FlowNoElementException> {
            FlowEvent.Value(1).errorOrThrow()
        }.message shouldBeEqualTo "FlowEvent.Value(1) has no error!"

        FlowEvent.Error(ex).errorOrThrow() shouldBeEqualTo ex

        assertFailsWith<FlowNoElementException> {
            FlowEvent.Complete.errorOrThrow()
        }.message shouldBeEqualTo "FlowEvent.Complete has no error!"
    }
}
