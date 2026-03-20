package io.bluetape4k.examples.cassandra.basic

import kotlinx.coroutines.flow.Flow
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface BasicUserRepository: CoroutineCrudRepository<BasicUser, Long> {

    /**
     * Sample method annotated with {@link Query}. This method executes the CQL from the {@link Query} value.
     *
     * @param id
     * @return
     */
    @Query("SELECT * FROM basic_users WHERE user_id in (?0)")
    suspend fun findUserByIdIn(id: Long): BasicUser?

    /**
     * Derived query method. This query corresponds with {@code SELECT * FROM users WHERE uname = ?0}.
     * {@link User#username} is not part of the primary so it requires a secondary index.
     *
     * @param username
     * @return
     */
    suspend fun findByUsername(username: String): BasicUser?

    /**
     * Derived query method using SASI (SSTable Attached Secondary Index) features through the `LIKE` keyword. This
     * query corresponds with `SELECT * FROM users WHERE lname LIKE '?0'`}`. `User.lastname` is not part of the
     * primary key so it requires a secondary index.
     *
     * @param lastnamePrefix
     * @return
     */
    fun findAllByLastnameStartsWith(lastnamePrefix: String): Flow<BasicUser>


    fun findAllByAddressCity(city: String): Flow<BasicUser>

}
