package io.bluetape4k.hibernate.mapping.customid

import org.springframework.data.jpa.repository.JpaRepository

/**
 * [CustomIdEntity]의 Identifier인 [Email]이 string value class 이므로 ID는 String 수형으로 지정해야 합니다.
 */
interface CustomIdRepository: JpaRepository<CustomIdEntity, String> {

    /**
     * [CustomIdEntity]의 Id 는 value class인 [Email] 이고, value 수형이 string 이므로, string 으로 바로 전환이 된다.
     */
    fun findAllByIdIn(customIds: Collection<String>): List<CustomIdEntity>

    fun findAllByIdInOrderByName(customIds: Collection<String>): List<CustomIdEntity>

    /**
     * Ssn 속성은 [Ssn] value class 인데, 이를 적용할 수 없고, value의 수형으로 사용해야 한다.
     */
    fun findBySsn(ssn: String): CustomIdEntity?

}
