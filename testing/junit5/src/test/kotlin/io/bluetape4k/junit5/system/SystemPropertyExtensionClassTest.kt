package io.bluetape4k.junit5.system

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@SystemProperties(
    [
        SystemProperty("classPropertyA", "classPropertyValueA"),
        SystemProperty("classPropertyB", "classPropertyValueB"),
        SystemProperty("classPropertyC", "classPropertyValueC")
    ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class SystemPropertyExtensionClassTest {

    @Test
    fun `클래스 단위로 테스트를 위한 시스템 속성을 설정`() {
        System.getProperty("classPropertyA") shouldBeEqualTo "classPropertyValueA"
        System.getProperty("classPropertyB") shouldBeEqualTo "classPropertyValueB"
        System.getProperty("classPropertyC") shouldBeEqualTo "classPropertyValueC"
    }

    @Test
    @io.bluetape4k.junit5.system.SystemProperties(
        [
            SystemProperty("keyA", "valueA"),
            SystemProperty("keyB", "valueB")
        ]
    )
    fun `메소드 단위로 테스트를 위한 시스템 속성을 설정`() {
        System.getProperty("classPropertyA") shouldBeEqualTo "classPropertyValueA"
        System.getProperty("classPropertyB") shouldBeEqualTo "classPropertyValueB"
        System.getProperty("classPropertyC") shouldBeEqualTo "classPropertyValueC"

        System.getProperty("keyA") shouldBeEqualTo "valueA"
        System.getProperty("keyB") shouldBeEqualTo "valueB"
    }

    @Test
    @SystemProperties(
        [
            SystemProperty("keyA", "valueA-1"),
            SystemProperty("keyB", "valueB-1"),
            SystemProperty("keyC", "valueC-1")
        ]
    )
    @SystemProperty("keyD", "valueD-1")
    fun `다른 메소드의 시스템 속성을 재정의`() {
        System.getProperty("keyA") shouldBeEqualTo "valueA-1"
        System.getProperty("keyB") shouldBeEqualTo "valueB-1"
        System.getProperty("keyC") shouldBeEqualTo "valueC-1"
        System.getProperty("keyD") shouldBeEqualTo "valueD-1"
    }
}
