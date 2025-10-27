package io.bluetape4k.junit5.faker

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator
import io.bluetape4k.junit5.faker.Fakers.letterString
import io.bluetape4k.junit5.faker.Fakers.numberString
import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import net.datafaker.service.RandomService
import java.util.*


/**
 * [Data FakeValue](https://github.com/datafaker-net/datafaker) 를 이용하여 테스트용 램덤 데이터를 생성합니다.
 *
 * ```
 * val randomString = Fakers.randomString(10, 20)
 * val numberString = Fakers.numberString("010-####-####")       // "010-1234-5678"
 * val letterString = Fakers.letterString("?-103")               // "A-103", "c-701"
 * val alphaNumericString = Fakers.alphaNumericString("?-#00#")  // a-4007
 * val randomUuid = Fakers.randomUuid()
 * ```
 */
object Fakers: KLogging() {

    val faker: Faker = Faker()

    val random: RandomService by unsafeLazy { faker.random() }

    private val timeBasedUuidGenerator: NoArgGenerator by unsafeLazy {
        Generators.timeBasedReorderedGenerator()
    }

    private inline fun <reified T> unsafeLazy(noinline initializer: () -> T): Lazy<T> =
        lazy(LazyThreadSafetyMode.NONE, initializer)

    /**
     * 임의의 길이의 fake 문자열을 생성합니다.
     *
     * ```
     * val randomString = Fakers.randomString(10, 20)
     * ```
     *
     * @param minLength    최소크기
     * @param maxLength    최대크기
     * @param includeUppercase 대문자 포함 여부
     * @return 임의의 길이의 fake 문자열
     */
    fun randomString(
        minLength: Int = 2,
        maxLength: Int = 255,
        includeUppercase: Boolean = true,
        includeSpecial: Boolean = true,
        includeDigit: Boolean = true,
    ): String =
        faker.text().text(
            minLength,
            maxOf(minLength, maxLength),
            includeUppercase,
            includeSpecial,
            includeDigit
        )

    /**
     * [length] 크기의 fake 문자열을 반환합니다.
     *
     * ```
     * val fixedString = Fakers.fixedString(10)
     * ```
     *
     * @param length 문자열 길이
     * @return [length] 길이의 fake 문자열
     */
    fun fixedString(
        length: Int,
        includeUppercase: Boolean = true,
        includeSpecial: Boolean = true,
        includeDigit: Boolean = true,
    ): String =
        randomString(length, length, includeUppercase, includeSpecial, includeDigit)


    /**
     * [format]에 `#`을 임의의 숫자(0~9)로 치환하는 문자열을 빌드합니다.
     *
     * ```
     * val phone = numberString("010-####-####")   // "010-1234-5678"
     * ```
     *
     * @param format 원하는 문자열 포맷
     * @return 랜덤 수로 치환된 문자열
     */
    fun numberString(format: String = "#,##0"): String = faker.numerify(format)

    /**
     * [format]에 `?`를 임의의 character(`a`~`z`)로 치환한 문자열을 빌드합니다.
     *
     * ```
     * val text = letterString("?-103")  // "A-103", "c-701"
     * ```
     *
     * @param format  원하는 문자열 포맷
     * @param isUpper 대문자로 할 것인가?
     * @return 랜덤 character로 치환된 문자열
     */
    fun letterString(format: String, isUpper: Boolean = false): String =
        faker.letterify(format, isUpper)

    /**
     * [numberString], [letterString] 을 조합하여, `#` 는 숫자로, `?` 는 문자로 치환한 문자열을 빌드합니다.
     *
     * ```
     * val text = alphaNumbericString("?-#00#")   // a-4007
     * ```
     *
     * @param format  원하는 문자열 포맷
     * @param isUpper 대문자로 할 것인가?
     * @return
     */
    fun alphaNumericString(format: String, isUpper: Boolean = false): String =
        faker.bothify(format, isUpper)


    /**
     * Time-based UUID를 생성합니다.
     */
    fun randomUuid(): UUID = timeBasedUuidGenerator.generate()
}
