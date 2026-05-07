package io.bluetape4k.junit5.system

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class SystemPropertyExtensionOverloadTest {

    @Test
    @SystemProperty("overloadA", "valueA")
    fun `중복 메소드명`() {
        System.getProperty("overloadA") shouldBeEqualTo "valueA"
    }

    @Test
    @SystemProperty("overloadB", "valueB")
    fun `중복 메소드명`(testInfo: TestInfo) {
        testInfo.displayName shouldContain "중복 메소드명"
        System.getProperty("overloadB") shouldBeEqualTo "valueB"
    }
}
