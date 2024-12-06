package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import io.bluetape4k.logging.info

/**
 * Domain Object의 변화를 [org.slf4j.Logger]로 출력하는 [JaversDispatcher] 입니다.
 *
 * @property logger slf4j logger
 */
class Slf4jDispatcher(private val logger: org.slf4j.Logger): JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        logger.info { "Send saved domain object. $domainObject" }
    }

    override fun sendDeleted(domainObject: Any) {
        logger.info { "Send deleted domain object. $domainObject" }
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        logger.info { "Send deleted domain object by id. id=$domainObjectId, type=$domainType" }
    }
}
