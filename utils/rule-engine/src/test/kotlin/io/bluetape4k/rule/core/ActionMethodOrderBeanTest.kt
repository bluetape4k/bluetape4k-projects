package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class ActionMethodOrderBeanTest {

    companion object: KLogging()

    private fun getMethod(name: String): Method {
        return SampleActions::class.java.getMethod(name)
    }

    class SampleActions {
        fun action1() {}
        fun action2() {}
    }

    @Test
    fun `compareTo lower order is first`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 2)
        (bean1.compareTo(bean2) < 0).shouldBeTrue()
        (bean2.compareTo(bean1) > 0).shouldBeTrue()
    }

    @Test
    fun `compareTo same order same method returns 0`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 1)
        bean1.compareTo(bean2) shouldBeEqualTo 0
    }

    @Test
    fun `compareTo same order different methods returns nonzero`() {
        val method1 = getMethod("action1")
        val method2 = getMethod("action2")
        val bean1 = ActionMethodOrderBean(method1, order = 1)
        val bean2 = ActionMethodOrderBean(method2, order = 1)
        (bean1.compareTo(bean2) != 0).shouldBeTrue()
    }

    @Test
    fun `equals same method and order`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 1)
        (bean1 == bean2).shouldBeTrue()
    }

    @Test
    fun `equals different order`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 2)
        (bean1 == bean2).shouldBeFalse()
    }

    @Test
    fun `hashCode consistent`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 1)
        (bean1.hashCode() == bean2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `sorted set orders correctly`() {
        val method1 = getMethod("action1")
        val method2 = getMethod("action2")
        val bean3 = ActionMethodOrderBean(method1, order = 3)
        val bean1 = ActionMethodOrderBean(method1, order = 1)
        val bean2 = ActionMethodOrderBean(method2, order = 2)
        val sorted = sortedSetOf(bean3, bean1, bean2)
        sorted.first().order shouldBeEqualTo 1
    }
}
