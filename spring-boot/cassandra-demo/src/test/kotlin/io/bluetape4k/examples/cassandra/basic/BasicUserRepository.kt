package io.bluetape4k.examples.cassandra.basic

import kotlinx.coroutines.flow.Flow
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface BasicUserRepository: CoroutineCrudRepository<BasicUser, Long> {

    /**
     * {@link Query}가 붙은 sample method입니다. 이 method는 {@link Query} 값의 CQL을 실행합니다.
     *
     * @param id
     * @return
     */
    @Query("SELECT * FROM basic_users WHERE user_id in (?0)")
    suspend fun findUserByIdIn(id: Long): BasicUser?

    /**
     * derived query method입니다. 이 query는 {@code SELECT * FROM users WHERE uname = ?0}에 대응합니다.
     * {@link User#username} is not part of the primary so it requires a secondary index.
     *
     * @param username
     * @return
     */
    suspend fun findByUsername(username: String): BasicUser?

    /**
     * `LIKE` keyword를 통해 SASI(SSTable Attached Secondary Index) 기능을 사용하는 derived query method입니다. 이
     * query corresponds with `SELECT * FROM users WHERE lname LIKE '?0'`}`. `User.lastname` is not part of the
     * primary key so it requires a secondary index.
     *
     * @param lastnamePrefix
     * @return
     */
    fun findAllByLastnameStartsWith(lastnamePrefix: String): Flow<BasicUser>


    fun findAllByAddressCity(city: String): Flow<BasicUser>

}
