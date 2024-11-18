package io.bluetape4k.hibernate.model

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.*

/**
 * [UUID] 수형의 Identifier를 가지는 JPA Entity의 추상 클래스입니다.
 */
@MappedSuperclass
abstract class UuidJpaEntity: AbstractJpaEntity<UUID>() {

    /**
     * Timebased UUID를 사용하여 Identifier를 생성합니다.
     */
    @field:Id
    @field:Column(columnDefinition = "BINARY(16)")
    override var id: UUID? = TimebasedUuid.Reordered.nextId()

}
