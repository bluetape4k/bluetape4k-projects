package io.bluetape4k.hibernate.listeners

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
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
        log.trace { "Insert entity. entity=${event?.entity}" }
    }

    override fun onPostInsertCommitFailed(event: PostInsertEvent?) {
        log.trace { "Fail to insert entity. event=$event" }
    }

    override fun onPostUpdate(event: PostUpdateEvent?) {
        log.trace { "Update entity. entity=${event?.entity}" }
    }

    override fun onPostUpdateCommitFailed(event: PostUpdateEvent?) {
        log.trace { "Fail to update entity. event=$event" }
    }

    override fun onPostDelete(event: PostDeleteEvent?) {
        log.trace { "Delete entity. entity=${event?.entity}" }
    }

    override fun onPostDeleteCommitFailed(event: PostDeleteEvent?) {
        log.trace { "Fail to delete entity. event=$event" }
    }

//    @Suppress("OVERRIDE_DEPRECATION")
//    override fun requiresPostCommitHanding(persister: EntityPersister?): Boolean {
//        return true
//    }

    override fun requiresPostCommitHandling(persister: EntityPersister?): Boolean {
        return true
    }
}
