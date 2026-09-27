package io.bluetape4k.rule.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.logging.KLogging
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
        bean1 shouldBeLessThan bean2
        bean2 shouldBeGreaterThan bean1
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
        bean1.compareTo(bean2) shouldNotBeEqualTo 0
    }

    @Test
    fun `equals same method and order`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 1)
        bean1 shouldBeEqualTo bean2
    }

    @Test
    fun `equals different order`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 2)
        bean1 shouldNotBeEqualTo bean2
    }

    @Test
    fun `hashCode consistent`() {
        val method = getMethod("action1")
        val bean1 = ActionMethodOrderBean(method, order = 1)
        val bean2 = ActionMethodOrderBean(method, order = 1)
        bean1.hashCode() shouldBeEqualTo bean2.hashCode()
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
