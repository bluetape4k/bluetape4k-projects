package io.bluetape4k.exposed.core.phone

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * [PhoneNumber]을 문자열 컬럼과 상호 변환하는 [ColumnTransformer] 구현체.
 *
 * DB에는 E.164 형식 문자열로 저장하고, 읽을 때는 [PhoneNumber] 객체로 변환한다.
 *
 * ```kotlin
 * val transformer = PhoneNumberTransformer("KR")
 * val phone = transformer.wrap("+821012345678")
 * // phone.countryCode == 82
 * ```
 *
 * @property defaultRegion 기본 국가 코드 (예: "KR", "US")
 */
class PhoneNumberTransformer(
    val defaultRegion: String = "KR",
): ColumnTransformer<String, PhoneNumber> {

    init {
        defaultRegion.requireNotBlank("defaultRegion")
    }

    /**
     * DB에서 읽은 문자열 값을 [PhoneNumber] 객체로 변환한다.
     *
     * @param value DB에서 읽은 전화번호 문자열
     * @return 파싱된 [PhoneNumber] 객체
     * @throws IllegalArgumentException 전화번호 파싱 실패 시
     */
    override fun wrap(value: String): PhoneNumber {
        return try {
            PhoneNumberUtil.getInstance().parse(value, defaultRegion)
        } catch (e: NumberParseException) {
            throw IllegalArgumentException("전화번호 파싱 실패: '$value' (region: $defaultRegion)", e)
        }
    }

    /**
     * [PhoneNumber] 객체를 E.164 형식 문자열로 변환하여 DB에 저장한다.
     *
     * @param value 저장할 [PhoneNumber] 객체
     * @return E.164 형식 전화번호 문자열 (예: "+821012345678")
     */
    override fun unwrap(value: PhoneNumber): String {
        return PhoneNumberUtil.getInstance().format(value, PhoneNumberUtil.PhoneNumberFormat.E164)
    }
}

/**
 * 전화번호를 E.164 형식으로 저장하는 컬럼 타입.
 *
 * [PhoneNumber] 객체를 E.164 문자열로 변환하여 VARCHAR(20) 컬럼에 저장하고,
 * 읽을 때는 [PhoneNumber] 객체로 복원한다.
 *
 * **의존성**: `com.googlecode.libphonenumber:libphonenumber`가 런타임에 필요하다.
 *
 * ```kotlin
 * object Contacts : Table("contacts") {
 *     val phone = registerColumn<PhoneNumber>("phone", PhoneNumberColumnType("KR"))
 * }
 * // Contacts.phone.columnType is PhoneNumberColumnType
 * ```
 *
 * @property defaultRegion 전화번호 파싱 시 사용할 기본 국가 코드 (기본값: "KR")
 */
class PhoneNumberColumnType(
    defaultRegion: String = "KR",
): ColumnWithTransform<String, PhoneNumber>(VarCharColumnType(20), PhoneNumberTransformer(defaultRegion))

/**
 * 전화번호를 E.164 형식 문자열로 정규화하여 저장하는 컬럼 타입.
 *
 * 입력된 전화번호 문자열을 E.164 형식으로 변환한 뒤 VARCHAR(20) 컬럼에 저장한다.
 * 읽을 때도 E.164 형식 문자열 그대로 반환한다.
 *
 * **의존성**: `com.googlecode.libphonenumber:libphonenumber`가 런타임에 필요하다.
 *
 * ```kotlin
 * object Contacts : Table("contacts") {
 *     val phone = registerColumn<String>("phone", PhoneNumberStringColumnType("KR"))
 * }
 * // Contacts.phone.columnType is PhoneNumberStringColumnType
 * ```
 *
 * @property defaultRegion 전화번호 파싱 시 사용할 기본 국가 코드 (기본값: "KR")
 */
class PhoneNumberStringColumnType(
    val defaultRegion: String = "KR",
): ColumnType<String>() {

    private val transformer = PhoneNumberTransformer(defaultRegion)

    override fun sqlType(): String = "VARCHAR(20)"

    override fun valueFromDB(value: Any): String = value.toString()

    override fun notNullValueToDB(value: String): Any {
        val phoneNumber = transformer.wrap(value)
        return transformer.unwrap(phoneNumber)
    }
}
