package io.bluetape4k.examples.cassandra.multitenancy.row

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmployeeRepository: CoroutineCrudRepository<Employee, String>, RowAwareEmployeeRepository
