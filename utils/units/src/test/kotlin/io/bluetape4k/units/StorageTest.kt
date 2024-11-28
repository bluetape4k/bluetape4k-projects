package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.units.Storage.Companion.KBYTES
import io.bluetape4k.units.Storage.Companion.MBYTES
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@RandomizedTest
class StorageTest {

    companion object: KLogging()

    @Test
    fun `convert storage unit`() {
        100.bytes().inBytes() shouldBeEqualTo 100.0
        100.kbytes().inBytes() shouldBeEqualTo 100.0 * KBYTES
        100.mbytes().inBytes() shouldBeEqualTo 100.0 * MBYTES

        100.gbytes().inKBytes() shouldBeEqualTo 100.0 * MBYTES
        100.tbytes().inMBytes() shouldBeEqualTo 100.0 * MBYTES
        100.pbytes().inGBytes() shouldBeEqualTo 100.0 * MBYTES
        100.zbytes().inPBytes() shouldBeEqualTo 100.0 * MBYTES
        100.ybytes().inXBytes() shouldBeEqualTo 100.0 * MBYTES
    }

    @Test
    fun `convert storage unit by random`(@RandomValue(type = Double::class) storages: List<Double>) {
        storages.forEach { storage ->
            storage.bytes().inBytes() shouldBeEqualTo storage
            storage.bytes().inMBytes() shouldBeEqualTo storage / MBYTES
        }
    }

    @Test
    fun `convert human expression`() {
        100.bytes().toHuman() shouldBeEqualTo "100 B"
        123.kbytes().toHuman() shouldBeEqualTo "123.0 KB"
        123.43.mbytes().toHuman() shouldBeEqualTo "123.4 MB"
        12.59.gbytes().toHuman() shouldBeEqualTo "12.6 GB"
    }

    @Test
    fun `parse with null or blank string to NaN`() {
        Storage.parse(null) shouldBeEqualTo Storage.NaN
        Storage.parse("") shouldBeEqualTo Storage.NaN
        Storage.parse(" \t ") shouldBeEqualTo Storage.NaN
    }

    @Test
    fun `parse Storage expression`() {
        Storage.parse("100 B") shouldBeEqualTo 100.bytes()
        Storage.parse("17.5 KB") shouldBeEqualTo 17.5.kbytes()
        Storage.parse("8.1 MB") shouldBeEqualTo 8.1.mbytes()
        Storage.parse("8.1 mbs") shouldBeEqualTo 8.1.mbytes()
    }

    @Test
    fun `parse invalid expression`() {
        assertFailsWith<IllegalArgumentException> {
            Storage.parse("9.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Storage.parse("9.1 bytes")
        }
        assertFailsWith<IllegalArgumentException> {
            Storage.parse("9.1 Bytes")
        }
        assertFailsWith<IllegalArgumentException> {
            Storage.parse("9.1.0.1 B")
        }
    }

    @Test
    fun `storage neative`() {
        (-100).bytes() shouldBeEqualTo storageOf(-100.0)
        -(100.bytes()) shouldBeEqualTo storageOf(-100.0)
    }

    @Test
    fun `storage oprators`() {
        val a = 100.0.bytes()
        val b = 200.0.bytes()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `compare storage`() {
        1.78.kbytes() shouldBeGreaterThan 1.7.kbytes()
        1.78.mbytes() shouldBeGreaterThan 1.2.mbytes()
        123.mbytes() shouldBeLessThan 0.9.gbytes()
    }
}
