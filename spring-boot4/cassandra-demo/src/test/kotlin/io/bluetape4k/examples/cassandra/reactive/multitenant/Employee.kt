package io.bluetape4k.examples.cassandra.reactive.multitenant

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table

/**
 * 한 테이블에서 multi-tenant 를 지원하기 위해, tenant-id를 patition key 로 사용하도록 합니다.
 * 이렇게 되면 다양한 회사의 직원을 한 테이블에 저장하지만 각 `tenantId` 별로 구분하여 작업이 가능합니다.
 */
@Table("row_level_multitenancy_employee")
data class Employee(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    val tenantId: String,

    @field:PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED)
    val name: String,
)
