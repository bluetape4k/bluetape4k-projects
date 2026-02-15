package io.bluetape4k.avro

import io.bluetape4k.avro.TestMessageProvider.COUNT
import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.avro.message.examples.EmployeeList
import io.bluetape4k.avro.message.examples.EventType
import io.bluetape4k.avro.message.examples.ProductProperty
import io.bluetape4k.avro.message.examples.ProductRoot
import io.bluetape4k.avro.message.examples.Suit
import io.bluetape4k.junit5.faker.Fakers
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Avro 테스트에서 사용할 테스트 데이터를 생성하는 팩토리 객체입니다.
 *
 * [Employee], [EmployeeList], [ProductRoot], [ProductProperty] 등
 * Avro 스키마 기반 테스트 메시지를 랜덤 데이터로 생성합니다.
 *
 * ```
 * val employee = TestMessageProvider.createEmployee()
 * val productRoot = TestMessageProvider.createProductRoot()
 * val employeeList = TestMessageProvider.createEmployeeList(100)
 * ```
 */
object TestMessageProvider {

    private const val COUNT = 1000

    private val faker = Fakers.faker

    /**
     * 랜덤 데이터를 가진 [Employee] 인스턴스를 생성합니다.
     *
     * @return 랜덤 데이터가 채워진 [Employee] 인스턴스
     */
    fun createEmployee(): Employee {
        return Employee.newBuilder()
            .setId(faker.random().nextInt())
            .setName(faker.name().fullName())
            .setAddress(faker.address().fullAddress())
            .setAge(faker.random().nextInt(100))
            .setSalary(faker.random().nextLong())
            .setEventType(EventType.CREATED)
            .setHireAt(faker.time().past(1000, ChronoUnit.HOURS))
            .setLastUpdatedAt(faker.time().past(10, ChronoUnit.HOURS))
            .build()
    }

    /**
     * [Employee] 리스트를 포함하는 [EmployeeList] 인스턴스를 생성합니다.
     *
     * @param count 생성할 Employee 수 (기본값: [COUNT])
     * @return [Employee] 리스트가 포함된 [EmployeeList] 인스턴스
     */
    fun createEmployeeList(count: Int = COUNT): EmployeeList =
        EmployeeList.newBuilder()
            .setEmps(List(count) { createEmployee() })
            .build()

    private fun getValues() = mapOf(
        "name" to faker.name().fullName(),
        "nick" to faker.funnyName().name(),
    )

    /**
     * 랜덤 데이터를 가진 [ProductProperty] 인스턴스를 생성합니다.
     *
     * @param id 속성 ID (기본값: 1L)
     * @param valid 유효 여부 (기본값: true)
     * @return 랜덤 데이터가 채워진 [ProductProperty] 인스턴스
     */
    fun createProductProperty(id: Long = 1L, valid: Boolean = true): ProductProperty =
        ProductProperty.newBuilder()
            .setId(id)
            .setKey(id.toString())
            .setCreatedAt(Random.nextLong(System.currentTimeMillis()))
            .setUpdatedAt(Random.nextLong(System.currentTimeMillis()))
            .setValid(valid)
            .setValues(getValues())
            .build()

    /**
     * 중첩 구조를 가진 [ProductRoot] 인스턴스를 생성합니다.
     *
     * 내부에 [ProductProperty] 리스트를 포함합니다.
     *
     * @return 랜덤 데이터가 채워진 [ProductRoot] 인스턴스
     */
    fun createProductRoot(): ProductRoot =
        ProductRoot.newBuilder()
            .setId(faker.random().nextLong())
            .setCategoryId(faker.random().nextLong())
            .setProductProperties(listOf(createProductProperty(1L), createProductProperty(2L)))
            .setSuit(Suit.HEARTS)
            .build()
}
