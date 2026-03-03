package io.bluetape4k.junit5.faker

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator
import io.bluetape4k.junit5.faker.Fakers.faker
import io.bluetape4k.logging.KLogging
import net.datafaker.Faker
import net.datafaker.service.RandomService
import java.util.*


/**
 * DataFaker 기반 테스트 데이터 생성 유틸리티를 제공합니다.
 *
 * ## 동작/계약
 * - 내부 [faker] 인스턴스를 재사용해 문자열/UUID 생성 함수를 제공합니다.
 * - 함수 대부분은 입력 포맷을 그대로 DataFaker에 위임하며, 포맷 오류 예외는 그대로 전파될 수 있습니다.
 * - 모든 생성 함수는 기존 객체를 변경하지 않고 새 값을 반환합니다.
 *
 * ```kotlin
 * val id = Fakers.randomUuid()
 * val phone = Fakers.numberString("010-####-####")
 * // phone.length == 13
 * ```
 */
object Fakers: KLogging() {

    /** 공유 DataFaker 인스턴스입니다. */
    val faker: Faker = Faker()

    /** 공유 난수 서비스입니다. */
    val random: RandomService by lazy { faker.random() }

    private val timeBasedUuidGenerator: NoArgGenerator by lazy {
        Generators.timeBasedReorderedGenerator()
    }

    private inline fun <reified T> unsafeLazy(
        @BuilderInference noinline initializer: () -> T,
    ): Lazy<T> =
        lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

    /**
     * 길이 범위를 지정해 임의 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - 실제 생성은 `faker.text().text(...)`에 위임합니다.
     * - 최대 길이는 `maxOf(minLength, maxLength)`로 보정됩니다.
     * - 포함 옵션에 따라 대문자/특수문자/숫자 포함 여부가 달라집니다.
     *
     * ```kotlin
     * val value = Fakers.randomString(minLength = 5, maxLength = 5)
     * // value.length == 5
     * ```
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
     * 고정 길이 임의 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `randomString(length, length, ...)`를 호출합니다.
     * - `length`가 음수면 DataFaker 내부 검증에서 예외가 발생할 수 있습니다.
     *
     * ```kotlin
     * val token = Fakers.fixedString(8)
     * // token.length == 8
     * ```
     */
    fun fixedString(
        length: Int,
        includeUppercase: Boolean = true,
        includeSpecial: Boolean = true,
        includeDigit: Boolean = true,
    ): String =
        randomString(length, length, includeUppercase, includeSpecial, includeDigit)


    /**
     * 포맷의 `#` 문자를 숫자로 치환한 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - `#`만 숫자로 대체되고 나머지 문자는 유지됩니다.
     * - 포맷 해석은 DataFaker 규칙을 따릅니다.
     *
     * ```kotlin
     * val phone = Fakers.numberString("010-####-####")
     * // phone.startsWith("010-") == true
     * ```
     */
    fun numberString(format: String = "#,##0"): String = faker.numerify(format)

    /**
     * 포맷의 `?` 문자를 알파벳 문자로 치환한 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - `isUpper=true`면 대문자 알파벳으로 치환합니다.
     * - 포맷의 다른 문자는 그대로 유지됩니다.
     *
     * ```kotlin
     * val code = Fakers.letterString("??-2026", isUpper = true)
     * // code.length == 7
     * ```
     */
    fun letterString(format: String, isUpper: Boolean = false): String =
        faker.letterify(format, isUpper)

    /**
     * 포맷의 `#`/`?`를 숫자/문자로 함께 치환한 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - DataFaker `bothify` 규칙을 따르며 문자/숫자를 한 번에 치환합니다.
     * - 입력 포맷 문자열은 변경하지 않고 결과 문자열만 반환합니다.
     *
     * ```kotlin
     * val value = Fakers.alphaNumericString("?-#00#")
     * // value.length == 6
     * ```
     */
    fun alphaNumericString(format: String, isUpper: Boolean = false): String =
        faker.bothify(format, isUpper)


    /**
     * 시간 기반 UUID를 생성합니다.
     *
     * ## 동작/계약
     * - 내부 `timeBasedUuidGenerator`를 재사용해 새 [UUID]를 반환합니다.
     * - 호출마다 새로운 UUID가 생성됩니다.
     *
     * ```kotlin
     * val id1 = Fakers.randomUuid()
     * val id2 = Fakers.randomUuid()
     * // id1 != id2
     * ```
     */
    fun randomUuid(): UUID = timeBasedUuidGenerator.generate()
}
