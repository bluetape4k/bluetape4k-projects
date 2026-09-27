package io.bluetape4k.examples.cassandra.multitenancy.keyspace

import com.datastax.oss.driver.api.mapper.annotations.Query
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmployeeRepository: CoroutineCrudRepository<Employee, String> {

    @Query("SELECT * FROM #{getTenantId()}.ks_mt_emp WHERE name = :name")
    fun findAllByName(name: String): Flow<Employee>

}
