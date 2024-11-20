package io.bluetape4k.aws.coroutines

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeReservedInstancesRequest
import software.amazon.awssdk.services.ec2.model.DescribeReservedInstancesResponse

class AwsCoroutineSupportTest: AbstractAwsTest() {

    companion object: KLogging()

    private val ec2 = mockk<Ec2Client>(relaxed = true)
    private val result = mockk<DescribeReservedInstancesResponse>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        clearMocks(ec2, result)
    }

    @Test
    fun `run sync method in coroutine`() = runTest {
        coEvery { ec2.describeReservedInstances() } coAnswers { result }

        val actual = suspendCommand {
            log.debug { "Run aws sync method in coroutine" }
            ec2.describeReservedInstances()
        }

        actual shouldBeEqualTo result
        coVerify(exactly = 1) { ec2.describeReservedInstances() }
        confirmVerified(ec2)
    }

    @Test
    fun `run sync method with params in coroutine`() = runTest {
        val request = mockk<DescribeReservedInstancesRequest>(relaxed = true)

        coEvery { ec2.describeReservedInstances(request) } coAnswers { result }

        val actual = suspendCommand {
            log.debug { "Run aws sync method with params in coroutine" }
            ec2.describeReservedInstances(request)
        }

        actual shouldBeEqualTo result
        coVerify(exactly = 1) { ec2.describeReservedInstances(request) }
        confirmVerified(ec2)
    }
}
