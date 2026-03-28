package io.bluetape4k.spring.data.exposed.jdbc.repository

import io.bluetape4k.spring.data.exposed.jdbc.annotation.Query
import io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity

interface UserJdbcRepository: ExposedJdbcRepository<UserEntity, Long> {

    fun findByName(name: String): List<UserEntity>

    fun findByAgeGreaterThan(age: Int): List<UserEntity>

    fun findByEmailContaining(keyword: String): List<UserEntity>

    fun findByNameAndAge(name: String, age: Int): UserEntity?

    fun countByAge(age: Int): Long

    fun existsByEmail(email: String): Boolean

    fun deleteByName(name: String): Long

    fun findByAgeBetween(min: Int, max: Int): List<UserEntity>

    fun findByNameOrderByAgeDesc(name: String): List<UserEntity>

    fun findFirstByNameOrderByAgeDesc(name: String): UserEntity?

    fun findTop3ByOrderByAgeDesc(): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = ?1")
    fun findByEmailNative(email: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE age = ?2 AND email = ?1")
    fun findByEmailAndAgeNative(email: String, age: Int): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = ?1 OR email = ?1")
    fun findByEmailNativeDuplicatedPlaceholder(email: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE age = ?1")
    fun findByAgeNativeLong(age: Long): List<UserEntity>

    @Query("SELECT * FROM users WHERE age BETWEEN ?1 AND ?2")
    fun findByAgeRangeNative(minAge: Int, maxAge: Int): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = ?10")
    fun findByEmailNativeTenthPlaceholder(
        p1: String,
        p2: String,
        p3: String,
        p4: String,
        p5: String,
        p6: String,
        p7: String,
        p8: String,
        p9: String,
        p10: String,
    ): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = ?2")
    fun findByEmailNativeBrokenPlaceholder(email: String): List<UserEntity>
}
