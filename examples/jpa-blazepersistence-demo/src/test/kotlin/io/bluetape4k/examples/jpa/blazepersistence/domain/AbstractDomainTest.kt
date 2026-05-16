package io.bluetape4k.examples.jpa.blazepersistence.domain

import io.bluetape4k.examples.jpa.blazepersistence.AbstractBlazePersistenceTest
import io.bluetape4k.examples.jpa.blazepersistence.services.InitMemberService
import io.bluetape4k.support.uninitialized
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired

/**
 * Domain test base that initializes the shared member/team fixture.
 */
abstract class AbstractDomainTest: AbstractBlazePersistenceTest() {

    @Autowired
    private val initMemberService: InitMemberService = uninitialized()

    @BeforeAll
    fun beforeAll() {
        initMemberService.init()
    }
}
