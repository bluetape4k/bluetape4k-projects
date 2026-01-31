package io.bluetape4k.examples.mapstruct

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import java.io.Serializable

class FieldMappingExample: AbstractMapstructTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `entity의 컬렉션 속성을 DTO로 변환`() {
        val customer = Customer(
            faker.number().randomNumber(),
            faker.name().fullName(),
            listOf(
                OrderItem(faker.name().fullName(), faker.number().randomNumber()),
                OrderItem(faker.name().fullName(), faker.number().randomNumber())
            )
        )

        val customerDto = CustomerMapper.MAPPER.fromCustomer(customer)

        customerDto.customerId shouldBeEqualTo customer.id
        customerDto.customerName shouldBeEqualTo customer.name
        customerDto.orders.shouldNotBeNull()
        customerDto.orders!!.size shouldBeEqualTo 2
        val orders = customerDto.orders!!.toFastList()

        orders.map { it.name } shouldContainSame customer.orderItems!!.map { it.name }
        orders.map { it.quantity } shouldContainSame customer.orderItems!!.map { it.quantity }

        val actual: Customer = CustomerMapper.MAPPER.toCustomer(customerDto)
        actual shouldBeEqualTo customer
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DTO 의 컬렉션 속성을 Entity로 변환`() {
        val customerDto = CustomerDto(
            faker.number().randomNumber(),
            faker.name().fullName(),
            setOf(
                OrderItemDto(faker.name().fullName(), faker.number().randomNumber()),
                OrderItemDto(faker.name().fullName(), faker.number().randomNumber())
            )
        )

        val customer = CustomerMapper.MAPPER.toCustomer(customerDto)

        customer.id shouldBeEqualTo customerDto.customerId
        customer.name shouldBeEqualTo customerDto.customerName
        customer.orderItems.shouldNotBeNull()
        customer.orderItems!!.size shouldBeEqualTo 2
        val orderItems = customer.orderItems!!.toFastList()

        orderItems.map { it.name } shouldContainSame customerDto.orders!!.map { it.name }
        orderItems.map { it.quantity } shouldContainSame customerDto.orders!!.map { it.quantity }

        val actual: CustomerDto = CustomerMapper.MAPPER.fromCustomer(customer)
        actual shouldBeEqualTo customerDto
    }
}

data class Customer(
    var id: Long?,
    var name: String?,
    var orderItems: List<OrderItem>?,
): Serializable

data class OrderItem(
    var name: String?,
    var quantity: Long?,
): Serializable

data class CustomerDto(
    var customerId: Long?,
    var customerName: String?,
    var orders: Set<OrderItemDto>?,
): Serializable

data class OrderItemDto(
    var name: String?,
    var quantity: Long?,
): Serializable

@Mapper(uses = [OrderItemMapper::class])
interface CustomerMapper {
    companion object {
        val MAPPER: CustomerMapper = mapper<CustomerMapper>()
    }

    @Mappings(
        Mapping(source = "orders", target = "orderItems"),
        Mapping(source = "customerName", target = "name"),
        Mapping(source = "customerId", target = "id")
    )
    fun toCustomer(customerDto: CustomerDto): Customer

    @InheritInverseConfiguration
    fun fromCustomer(customer: Customer): CustomerDto
}

@Mapper
interface OrderItemMapper {
    companion object {
        val MAPPER = mapper<OrderItemMapper>()
    }

    fun toOrderItem(orderItemDto: OrderItemDto): OrderItem

    @InheritInverseConfiguration
    fun fromOrderItem(orderItem: OrderItem): OrderItemDto
}
