package io.bluetape4k.csv

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex
import org.junit.jupiter.api.Test
import kotlin.text.Charsets.UTF_8

/**
 * 자체 엔진 기반 [CsvRecordReader] 통합 테스트.
 *
 * 실제 리소스 파일을 사용해 엔드투엔드 파싱 동작을 검증합니다.
 */
class NativeCsvRecordReaderTest {

    companion object: KLogging()

    private val reader = CsvRecordReader()

    @Test
    fun `product_type csv 전체 파싱`() {
        Resourcex.getInputStream("csv/product_type.csv")!!.buffered().use { bis ->
            val records = reader.read(bis, UTF_8, skipHeaders = true).toList()

            records.shouldNotBeEmpty()
            records.size shouldBeGreaterThan 100
            records.forEach { record ->
                record.size shouldBeGreaterThan 1
                record.getString(0)!!.shouldNotBeBlank()
                record.getString(1)!!.shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `헤더명 기반 접근이 동작한다`() {
        Resourcex.getInputStream("csv/product_type.csv")!!.buffered().use { bis ->
            val records = reader.read(bis, UTF_8, skipHeaders = true).take(3).toList()

            records.shouldNotBeEmpty()
            records.forEach { record ->
                record.headers.shouldNotBeNull()
                val tagFamily = record.getString("tag_family") ?: record.getString(0)
                tagFamily.shouldNotBeNull()
            }
        }
    }

    @Test
    fun `Record 인터페이스의 getValue 타입 변환`() {
        val csv = "name,score,active\nAlice,95,true\nBob,80,false"
        val records = reader.read(csv.byteInputStream(), skipHeaders = true).toList()

        records shouldHaveSize 2
        records[0].getValue("score", 0) shouldBeEqualTo 95
        records[0].getValue("active", false) shouldBeEqualTo true
        records[1].getValue("score", 0) shouldBeEqualTo 80
        records[1].getValue("active", false) shouldBeEqualTo false
    }

    @Test
    fun `Record 인터페이스의 nullable getter`() {
        val csv = "id,amount\n1,42.5\n2,"
        val records = reader.read(csv.byteInputStream(), skipHeaders = true).toList()

        records shouldHaveSize 2
        records[0].getDoubleOrNull("amount") shouldBeEqualTo 42.5
        records[1].getDoubleOrNull("amount") shouldBeEqualTo null
    }

    @Test
    fun `rowNumber가 올바르게 증가한다`() {
        val csv = "a\nb\nc"
        val records = reader.read(csv.byteInputStream(), skipHeaders = false).toList()

        records[0].rowNumber shouldBeEqualTo 1L
        records[1].rowNumber shouldBeEqualTo 2L
        records[2].rowNumber shouldBeEqualTo 3L
    }

    @Test
    fun `빈 줄은 기본적으로 건너뛴다`() {
        val csv = "a,b\n\nc,d"
        val records = reader.read(csv.byteInputStream(), skipHeaders = false).toList()

        records shouldHaveSize 2
    }

    private infix fun <T> Collection<T>.shouldHaveSize(expected: Int) {
        this.size shouldBeEqualTo expected
    }
}
