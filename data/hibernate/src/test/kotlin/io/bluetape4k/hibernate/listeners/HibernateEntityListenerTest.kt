package io.bluetape4k.hibernate.listeners

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class HibernateEntityListenerTest {

    private val listener = HibernateEntityListener()

    @Test
    fun `requiresPostCommitHandling는 true를 반환한다`() {
        listener.requiresPostCommitHandling(null) shouldBeEqualTo true
    }

    @Test
    fun `onPostInsert는 null 이벤트를 처리할 수 있다`() {
        listener.onPostInsert(null)
    }

    @Test
    fun `onPostInsertCommitFailed는 null 이벤트를 처리할 수 있다`() {
        listener.onPostInsertCommitFailed(null)
    }

    @Test
    fun `onPostUpdate는 null 이벤트를 처리할 수 있다`() {
        listener.onPostUpdate(null)
    }

    @Test
    fun `onPostUpdateCommitFailed는 null 이벤트를 처리할 수 있다`() {
        listener.onPostUpdateCommitFailed(null)
    }

    @Test
    fun `onPostDelete는 null 이벤트를 처리할 수 있다`() {
        listener.onPostDelete(null)
    }

    @Test
    fun `onPostDeleteCommitFailed는 null 이벤트를 처리할 수 있다`() {
        listener.onPostDeleteCommitFailed(null)
    }
}
