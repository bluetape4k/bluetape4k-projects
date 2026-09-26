package io.bluetape4k.examples.cassandra.multitenancy.row

import kotlinx.coroutines.flow.Flow

interface RowAwareEmployeeRepository {

    fun findAllByName(name: String): Flow<Employee>
}
