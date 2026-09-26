package io.bluetape4k.examples.cassandra.reactive.multitenant

import com.datastax.oss.driver.api.mapper.annotations.Query
import org.springframework.data.cassandra.repository.AllowFiltering
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface EmployeeRepository: ReactiveCrudRepository<Employee, String> {

    /**
     * 다양한 회사의 직원을 한 테이블에 저장하지만 각 `tenantId` 별로 구분하여 조회합니다
     */
    @AllowFiltering
    @Query("SELECT * FROM row_level_multitenancy_employee WHERE tenantId = :#{getTenantId()} AND name = :name")
    fun findAllByName(name: String): Flux<Employee>
}
