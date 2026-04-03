package io.bluetape4k.exposed.core.phone

import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

object ContactTable : LongIdTable("phone_contacts") {
    val phone = phoneNumber("phone")
    val phoneStr = phoneNumberString("phone_str")
}

/**
 * PhoneNumberColumnType 통합 테스트.
 *
 * H2 및 PostgreSQL 다이얼렉트에서 E.164 정규화 저장/조회를 검증한다.
 */
class PhoneNumberColumnTypeTest : AbstractExposedTest() {

    companion object : KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `한국 번호 E164 정규화 저장 및 조회`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            ContactTable.insert {
                it[phone] = PhoneNumberUtil.getInstance().parse("010-1234-5678", "KR")
                it[phoneStr] = "010-1234-5678"
            }

            val row = ContactTable.selectAll().single()
            val phoneNumber = row[ContactTable.phone]
            phoneNumber.shouldNotBeNull()

            val e164 = PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            e164 shouldBeEqualTo "+821012345678"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `미국 번호 파싱 및 저장`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            ContactTable.insert {
                it[phone] = PhoneNumberUtil.getInstance().parse("+1-650-253-0000", "US")
                it[phoneStr] = "+1-650-253-0000"
            }

            val row = ContactTable.selectAll().single()
            val phoneNumber = row[ContactTable.phone]
            phoneNumber.shouldNotBeNull()

            val e164 = PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            e164 shouldBeEqualTo "+16502530000"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `phoneStr 컬럼에 잘못된 형식 입력 시 예외 발생`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            assertFailsWith<IllegalArgumentException> {
                ContactTable.insert {
                    it[phone] = PhoneNumberUtil.getInstance().parse("+821012345678", "KR")
                    it[phoneStr] = "invalid-number"
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `phoneNumberString 컬럼 E164 정규화`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            ContactTable.insert {
                it[phone] = PhoneNumberUtil.getInstance().parse("010-9999-8888", "KR")
                it[phoneStr] = "010-9999-8888"
            }

            val row = ContactTable.selectAll().single()
            val phoneStr = row[ContactTable.phoneStr]
            phoneStr shouldBeEqualTo "+821099998888"
        }
    }

    @Test
    fun `PhoneNumberTransformer 는 잘못된 region 을 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            PhoneNumberTransformer("")
        }
    }

    @Test
    fun `PhoneNumberTransformer 는 DB 문자열을 PhoneNumber 로 복원한다`() {
        val transformer = PhoneNumberTransformer("US")

        val phoneNumber = transformer.wrap("+1-650-253-0000")
        val e164 = transformer.unwrap(phoneNumber)

        e164 shouldBeEqualTo "+16502530000"
    }

    @Test
    fun `PhoneNumberStringColumnType 은 잘못된 번호를 거부한다`() {
        val columnType = PhoneNumberStringColumnType("KR")

        assertFailsWith<IllegalArgumentException> {
            columnType.notNullValueToDB("not-a-phone-number")
        }
    }
}
