package io.bluetape4k.hibernate.listeners

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.hibernate.event.spi.PostCommitDeleteEventListener
import org.hibernate.event.spi.PostCommitInsertEventListener
import org.hibernate.event.spi.PostCommitUpdateEventListener
import org.hibernate.event.spi.PostDeleteEvent
import org.hibernate.event.spi.PostInsertEvent
import org.hibernate.event.spi.PostUpdateEvent
import org.hibernate.persister.entity.EntityPersister

/**
 * Hibernate 환경하에서 [PostCommitInsertEventListener], [PostCommitUpdateEventListener],
 * [PostCommitDeleteEventListener] 를 구현
 *
 * 이벤트가 발생한 후에 로깅을 수행합니다.
 */
class HibernateEntityListener: PostCommitDeleteEventListener,
                               PostCommitInsertEventListener,
                               PostCommitUpdateEventListener {

    companion object: KLogging()

    override fun onPostInsert(event: PostInsertEvent?) {
        log.debug { "Insert entity. entity=${event?.entity}" }
    }

    override fun onPostInsertCommitFailed(event: PostInsertEvent?) {
        log.debug { "Fail to insert entity. event=$event" }
    }

    override fun onPostUpdate(event: PostUpdateEvent?) {
        log.debug { "Update entity. entity=${event?.entity}" }
    }

    override fun onPostUpdateCommitFailed(event: PostUpdateEvent?) {
        log.debug { "Fail to update entity. event=$event" }
    }

    override fun onPostDelete(event: PostDeleteEvent?) {
        log.debug { "Delete entity. entity=${event?.entity}" }
    }

    override fun onPostDeleteCommitFailed(event: PostDeleteEvent?) {
        log.debug { "Fail to delete entity. event=$event" }
    }

    override fun requiresPostCommitHandling(persister: EntityPersister?): Boolean {
        return true
    }
}
