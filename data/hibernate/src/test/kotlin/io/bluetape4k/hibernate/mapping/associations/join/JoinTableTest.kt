package io.bluetape4k.hibernate.mapping.associations.join

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class JoinTableTest(
    @param:Autowired private val userRepo: JoinUserRepository,
    @param:Autowired private val customerRepo: JoinCustomerRepository,
): AbstractHibernateTest() {

    companion object: KLogging() {
        private fun newUser(): JoinUser {
            return JoinUser(faker.name().name())
                .apply {
                    addresses["Home"] = AddressEntity(
                        faker.address().streetAddress(),
                        faker.address().city(),
                        faker.address().zipCode()
                    )
                    addresses["Office"] = AddressEntity(
                        faker.address().streetAddress(),
                        faker.address().city(),
                        faker.address().zipCode()
                    )

                    nicknames.add(faker.credentials().username())
                    nicknames.add(faker.credentials().username())
                }
        }

        private fun newCustomer(): JoinCustomer {
            return JoinCustomer("debop")
                .apply {
                    addresses["Home"] = AddressEntity(
                        faker.address().streetAddress(),
                        faker.address().city(),
                        faker.address().zipCode()
                    )
                    addresses["Office"] = AddressEntity(
                        faker.address().streetAddress(),
                        faker.address().city(),
                        faker.address().zipCode()
                    )

                    // embeddable
                    address.street = faker.address().streetAddress()
                    address.city = faker.address().city()
                }
        }
    }

    @Test
    fun `context loading`() {
        // Nothing to do 
    }

    @Test
    fun `create user with address by join table`() {
        val user = newUser()
        userRepo.saveAndFlush(user)
        clear()

        val loaded = userRepo.findByIdOrNull(user.id!!)!!
        loaded.addresses.size shouldBeEqualTo 2

        loaded.addresses.remove("Office")
        userRepo.saveAndFlush(loaded)
        clear()

        val loaded2 = userRepo.findByIdOrNull(user.id!!)!!
        loaded2.addresses.size shouldBeEqualTo 1
    }

    @Test
    fun `create customer with embeddable address which saved to secondary table`() {
        val customer = newCustomer()

        customerRepo.saveAndFlush(customer)
        clear()

        val loaded = customerRepo.findByIdOrNull(customer.id!!)!!
        loaded.addresses.size shouldBeEqualTo 2
        loaded.address shouldBeEqualTo customer.address

        loaded.addresses.remove("Office")
        customerRepo.saveAndFlush(loaded)
        clear()

        val loaded2 = customerRepo.findByIdOrNull(customer.id!!)!!
        loaded2.addresses.size shouldBeEqualTo 1

        loaded2.address shouldBeEqualTo customer.address
    }
}
