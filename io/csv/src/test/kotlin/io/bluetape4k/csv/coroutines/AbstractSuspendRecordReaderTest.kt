package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import io.bluetape4k.csv.model.ProductType
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.flow.buffer
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.text.Charsets.UTF_8

abstract class AbstractSuspendRecordReaderTest {

    companion object: KLoggingChannel()

    protected abstract val reader: SuspendRecordReader

    protected abstract val productTypePath: String
    protected abstract val extraWordsPath: String

    private val transform: (Record) -> ProductType = { record: Record ->
        val tagFamily = record.getValue(0, "").trim()
        val representative = record.getValue(1, "").trim()
        val synonym = record.getValue<String?>(2, null)?.trim()
        val tagType = record.getValue<String?>(3, null)?.trim()
        val priority = record.getValue<Int?>(4, null)
        val parentRepresentative = record.getValue<String?>(5, null)?.trim()
        val level = record.getValue(6, 0)

        ProductType(
            tagFamily,
            representative,
            synonym,
            tagType,
            priority,
            parentRepresentative,
            level
        )
    }

    @Test
    fun `read record from csv file with number types`() = runSuspendIO {
        Resourcex.getInputStream(productTypePath)!!.buffered().use { bis ->
            reader
                .read(bis, UTF_8, true)
                .buffer()
                .collect { record ->
                    log.debug { "product type record=$record" }
                    val row = record.values.toList()
                    row.shouldNotBeEmpty()
                    row.size shouldBeGreaterThan 1
                    row[0]!!.shouldNotBeBlank()
                    row[1]!!.shouldNotBeBlank()
                }
        }
    }

    @Test
    fun `read product type from csv file with mapper`() = runSuspendIO {
        Resourcex.getInputStream(productTypePath)!!.buffered().use { bis ->
            reader
                .read(bis, UTF_8, true, transform)
                .buffer()
                .collect { productType ->
                    log.debug { "ProductType=$productType" }
                    productType.shouldNotBeNull()
                    productType.tagFamily.shouldNotBeBlank()
                    productType.representative.shouldNotBeBlank()
                }
        }
    }

    @Test
    fun `read extra words from csv file `() = runSuspendIO {
        Resourcex.getInputStream(extraWordsPath)!!.buffered().use { bis ->
            reader
                .read(bis, UTF_8, true)
                .buffer()
                .collect { record ->
                    log.debug { "extra words record=$record" }
                    val row = record.values
                    row.shouldNotBeEmpty()
                    row.size shouldBeGreaterThan 1
                    row[0]!!.shouldNotBeBlank()
                    row[4]!!.shouldNotBeBlank()
                }
        }
    }
}
