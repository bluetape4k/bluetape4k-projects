package io.bluetape4k.examples.cassandra.multitenancy.row

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import io.bluetape4k.cassandra.querybuilder.eq
import io.bluetape4k.cassandra.querybuilder.literal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.select
import org.springframework.stereotype.Repository

@Repository
class EmployeeRepositoryImpl: RowAwareEmployeeRepository {

    @Autowired
    private lateinit var session: CqlSession

    @Autowired
    private lateinit var operations: ReactiveCassandraOperations

    override fun findAllByName(name: String): Flow<Employee> {
        //        val selectStmt = selectFrom("tbl_mt_emp").all()
        //            .whereColumn("tenantId").eq("tenantId".bindMarker())
        //            .whereColumn("name").eq("name".bindMarker())
        //            .build()
        //
        //        val ps = session.prepare(selectStmt)
        //        val bs = ps.bind()
        //            .setString("tenantId", TenantIdProvider.tenantId.get())
        //            .setString("name", name)
        //
        //        return operations.select<Employee>(bs).asFlow()

        // 예제를 정확한 수행을 확인하기 위해 CQL에서 명시적인 tenantId 값을 볼 수 있는 SimpleStatement를 사용합니다.
        // production에서는 prepared statement 를 사용하세요
        val selectStmt = QueryBuilder
            .selectFrom("row_mt_emp").all()
            .whereColumn("tenantId").eq(TenantIdProvider.tenantId.get().literal())
            .whereColumn("name").eq(name.literal())
            .build()

        return operations.select<Employee>(selectStmt).asFlow()
    }
}
