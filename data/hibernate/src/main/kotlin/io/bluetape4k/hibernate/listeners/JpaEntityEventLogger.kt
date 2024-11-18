package io.bluetape4k.hibernate.listeners

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate

/**
 * JPA Entity 변화에 대한 Listener 입니다.
 * [PostLoad], [PrePersist], [PostPersist], [PreUpdate], [PostUpdate], [PreRemove], [PostRemove] 를 구현합니다.
 *
 * ```
 * @Entity
 * @EntityListeners(JpaEntityEventLogger::class)
 * class Entity {
 *    @Id
 *    var id: Long? = null
 *    var name: String? = null
 *    var age: Int? = null
 *    var createdAt: LocalDateTime? = null
 *    var updatedAt: LocalDateTime? = null
 *    var deletedAt: LocalDateTime? = null
 *    var deleted: Boolean = false
 * }
 */
open class JpaEntityEventLogger {

    companion object: KLogging()

    @PostLoad
    open fun onPostLoad(entity: Any) {
        log.trace { "Post load entity. entity=$entity" }
    }

    @PrePersist
    open fun onPrePersist(entity: Any) {
        log.trace { "Pre persist entity. entity=$entity" }
    }

    @PostPersist
    open fun onPostPersist(entity: Any) {
        log.trace { "Post persist entity. entity=$entity" }
    }

    @PreUpdate
    open fun onPreUpdate(entity: Any) {
        log.trace { "Pre update entity. entity=$entity" }
    }

    @PostUpdate
    open fun onPostUpdate(entity: Any) {
        log.trace { "Post update entity. entity=$entity" }
    }

    @PreRemove
    open fun onPreRemove(entity: Any) {
        log.trace { "Pre remove entity. entity=$entity" }
    }

    @PostRemove
    open fun onPostRemove(entity: Any) {
        log.trace { "Post remove entity. entity=$entity" }
    }
}
