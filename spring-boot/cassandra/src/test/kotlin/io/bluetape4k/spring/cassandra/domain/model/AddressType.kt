package io.bluetape4k.spring.cassandra.domain.model

import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.io.Serializable

/**
 * [UserDefinedType] 예제 (JPA Embeddable 에 해당)
 *
 * @see [Person] 에서 사용
 */
@UserDefinedType("address")
data class AddressType(
    val city: String? = "",
    val country: String = "",
): Serializable
