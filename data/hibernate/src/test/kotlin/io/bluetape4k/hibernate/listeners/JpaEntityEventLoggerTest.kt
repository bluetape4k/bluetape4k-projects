package io.bluetape4k.hibernate.listeners

import org.junit.jupiter.api.Test

class JpaEntityEventLoggerTest {

    private val logger = JpaEntityEventLogger()
    private val entity = object {}

    @Test
    fun `onPostLoad는 엔티티를 로그에 기록한다`() {
        logger.onPostLoad(entity)
    }

    @Test
    fun `onPrePersist는 엔티티를 로그에 기록한다`() {
        logger.onPrePersist(entity)
    }

    @Test
    fun `onPostPersist는 엔티티를 로그에 기록한다`() {
        logger.onPostPersist(entity)
    }

    @Test
    fun `onPreUpdate는 엔티티를 로그에 기록한다`() {
        logger.onPreUpdate(entity)
    }

    @Test
    fun `onPostUpdate는 엔티티를 로그에 기록한다`() {
        logger.onPostUpdate(entity)
    }

    @Test
    fun `onPreRemove는 엔티티를 로그에 기록한다`() {
        logger.onPreRemove(entity)
    }

    @Test
    fun `onPostRemove는 엔티티를 로그에 기록한다`() {
        logger.onPostRemove(entity)
    }
}
