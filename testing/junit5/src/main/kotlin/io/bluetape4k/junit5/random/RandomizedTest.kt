package io.bluetape4k.junit5.random

import org.junit.jupiter.api.extension.ExtendWith

/**
 * Property based testing 을 위해, 테스트 메소드에 랜덤 값을 주입해주는 annotation 입니다.
 *
 *
 * ```
 * @RandomizedTest
 * class TestClass {
 *   @Test
 *   fun `test with random value`(@RandomValue text:String) {
 *       // text is random string
 *   }
 *
 *   data class TestData(
 *      val name:String,
 *      val description: String,
 *      val amount: Double
 *   ): Serializable
 *
 *   @Test
 *   fun `test with random list`(@RandomValue(type=TestData::class, size=10) testDatas: TestData) {
 *       // testDatas has random value TestData
 *   }
 * }
 * ```
 *
 * @see [RandomValue]
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION
)
@MustBeDocumented
@Repeatable
@ExtendWith(RandomExtension::class)
annotation class RandomizedTest
