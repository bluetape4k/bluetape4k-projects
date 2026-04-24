package io.bluetape4k.csv

import io.bluetape4k.csv.model.ProductType
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.text.Charsets.UTF_8

abstract class AbstractRecordReaderTest {

    companion object : KLogging()

    abstract val reader: RecordReader

    abstract val productTypePath: String
    abstract val extraWordsPath: String

    val mapper = { record: Record ->
        val tagFamily = record.getValue(0, "").trim()
        val representative = record.getValue(1, "").trim()
        val synonym = record.getString(2)?.trim()
        val tagType = record.getString(3)?.trim()
        val priority = record.getIntOrNull(4)
        val parentRepresentative = record.getString(5)?.trim()
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
    fun `read record from csv file with number types`() {
        Resourcex.getInputStream(productTypePath)!!.buffered().use { bis ->

            val records = reader.read(bis, UTF_8, true)

            records.forEach { record ->
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
    fun `read product type from csv file with mapper`() {
        Resourcex.getInputStream(productTypePath)!!.buffered().use { bis ->
            val productTypes = reader.read(bis, UTF_8, true, mapper)

            productTypes.forEach { productType ->
                log.debug { "ProductType=$productType" }
                productType.shouldNotBeNull()
                productType.tagFamily.shouldNotBeBlank()
                productType.representative.shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `read extra words from csv file`() {
        Resourcex.getInputStream(extraWordsPath)!!.buffered().use { bis ->
            val records = reader.read(bis, UTF_8, true)

            records.forEach { record ->
                log.debug { "extra words record=$record" }
                val row = record.values.toList()
                row.shouldNotBeEmpty()
                row.size shouldBeGreaterThan 1
                row[0]!!.shouldNotBeBlank()
                row[4]!!.shouldNotBeBlank()
            }
        }
    }
}
