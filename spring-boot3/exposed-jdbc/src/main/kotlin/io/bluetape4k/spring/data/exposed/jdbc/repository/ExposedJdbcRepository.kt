package io.bluetape4k.spring.data.exposed.jdbc.repository

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.ListPagingAndSortingRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.QueryByExampleExecutor

/**
 * Exposed DAO Entity를 위한 Spring Data Repository 인터페이스입니다.
 *
 * Spring Data 3.x의 [ListCrudRepository], [ListPagingAndSortingRepository],
 * [QueryByExampleExecutor]를 모두 지원합니다.
 *
 * Exposed DSL Op 직접 사용을 위한 확장 메서드도 제공합니다.
 *
 * ```kotlin
 * @ExposedEntity
 * class User(id: EntityID<Long>) : LongEntity(id) {
 *     companion object : LongEntityClass<User>(Users)
 *     var name by Users.name
 *     var age  by Users.age
 * }
 *
 * interface UserRepository : ExposedJdbcRepository<User, Long> {
 *     override val table get() = Users
 *     override fun extractId(entity: User): Long? =
 *         if (entity.id._value == null) null else entity.id.value
 *
 *     // PartTree 파생 쿼리
 *     fun findByName(name: String): List<User>
 *
 *     // Exposed DSL 직접 사용
 *     fun findAdults(): List<User> = findAll { Users.age greaterEq 18 }
 * }
 * ```
 */
@NoRepositoryBean
interface ExposedJdbcRepository<E: Entity<ID>, ID: Any>: ListCrudRepository<E, ID>,
                                                         ListPagingAndSortingRepository<E, ID>,
                                                         QueryByExampleExecutor<E> {

    /**
     * 이 Repository가 사용하는 Exposed [IdTable].
     */
    val table: IdTable<ID>

    /**
     * 도메인 객체 [entity]에서 ID를 추출합니다. 신규 엔티티는 null을 반환합니다.
     */
    fun extractId(entity: E): ID?

    /**
     * 주어진 Exposed DSL 조건으로 Entity 목록을 조회합니다.
     *
     * ```kotlin
     * userRepository.findAll { Users.age greaterEq 18 }
     * ```
     */
    fun findAll(op: () -> Op<Boolean>): List<E>

    /**
     * 주어진 Exposed DSL 조건에 맞는 Entity 수를 반환합니다.
     *
     * ```kotlin
     * val adultCount = userRepository.count { Users.age greaterEq 18 }
     * // adultCount → 42
     * ```
     */
    fun count(op: () -> Op<Boolean>): Long

    /**
     * 주어진 Exposed DSL 조건에 맞는 Entity가 존재하는지 확인합니다.
     *
     * ```kotlin
     * val hasAdmin = userRepository.exists { Users.role eq "ADMIN" }
     * // hasAdmin → true
     * ```
     */
    fun exists(op: () -> Op<Boolean>): Boolean
}
