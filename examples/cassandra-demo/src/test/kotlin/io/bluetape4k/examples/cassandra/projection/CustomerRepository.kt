package io.bluetape4k.examples.cassandra.projection

import org.springframework.data.repository.CrudRepository

interface CustomerRepository: CrudRepository<Customer, String> {

    fun findAllProjectedBy(): Collection<CustomerProjection>

    fun findAllSummarizedBy(): Collection<CustomerSummary>

    fun <T: Any> findById(id: String, projection: Class<T>): Collection<T>

    fun findProjectedById(id: String): CustomerProjection?

    fun <T: Any> findProjectedById(id: String, projection: Class<T>): T?

}
