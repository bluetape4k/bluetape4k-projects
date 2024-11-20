package io.bluetape4k.avro

import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.avro.message.examples.EmployeeList
import io.bluetape4k.avro.message.examples.EventType
import io.bluetape4k.avro.message.examples.ProductProperty
import io.bluetape4k.avro.message.examples.ProductRoot
import io.bluetape4k.avro.message.examples.Suit
import io.bluetape4k.junit5.faker.Fakers
import kotlin.random.Random

object TestMessageProvider {

    private const val COUNT = 1000

    private val faker = Fakers.faker

    fun createEmployee(): Employee {
        return Employee.newBuilder()
            .setId(faker.random().nextInt())
            .setName(faker.name().fullName())
            .setAddress(faker.address().fullAddress())
            .setAge(faker.random().nextInt(100))
            .setSalary(faker.random().nextLong())
            .setEventType(EventType.CREATED)
            .setHireAt(faker.date().birthday().time)
            .setLastUpdatedAt(faker.date().birthday().time)
            .build()
    }

    fun createEmployeeList(count: Int = COUNT): EmployeeList =
        EmployeeList.newBuilder()
            .setEmps(List(count) { createEmployee() })
            .build()


    private fun getValues() = mapOf(
        "name" to faker.name().fullName(),
        "nick" to faker.funnyName().name(),
    )

    fun createProductProperty(id: Long = 1L, valid: Boolean = true): ProductProperty =
        ProductProperty.newBuilder()
            .setId(id)
            .setKey(id.toString())
            .setCreatedAt(Random.nextLong(System.currentTimeMillis()))
            .setUpdatedAt(Random.nextLong(System.currentTimeMillis()))
            .setValid(valid)
            .setValues(getValues())
            .build()

    fun createProductRoot(): ProductRoot =
        ProductRoot.newBuilder()
            .setId(faker.random().nextLong())
            .setCategoryId(faker.random().nextLong())
            .setProductProperties(listOf(createProductProperty(1L), createProductProperty(2L)))
            .setSuit(Suit.HEARTS)
            .build()
}
