package io.bluetape4k.spring.data.exposed.jdbc.annotation

/**
 * Exposed DAO Entity 클래스에 부착하여 Spring Data가 관리 대상 엔티티로 인식하게 합니다.
 * Exposed의 `Entity<ID>`를 상속하면서 이 어노테이션도 달아야 Spring Data 스캐닝에 포함됩니다.
 *
 * ```kotlin
 * @ExposedEntity
 * class User(id: EntityID<Long>) : LongEntity(id) {
 *     companion object : LongEntityClass<User>(Users)
 *     var name by Users.name
 *     var email by Users.email
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ExposedEntity
