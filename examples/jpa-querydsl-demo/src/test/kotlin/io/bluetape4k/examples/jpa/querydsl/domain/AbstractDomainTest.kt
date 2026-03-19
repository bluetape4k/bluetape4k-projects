package io.bluetape4k.examples.jpa.querydsl.domain

import io.bluetape4k.examples.jpa.querydsl.AbstractQuerydslTest
import io.bluetape4k.examples.jpa.querydsl.services.InitMemberService
import io.bluetape4k.support.uninitialized
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractDomainTest: AbstractQuerydslTest() {

    @Autowired
    private val initMemberService: InitMemberService = uninitialized()

    @BeforeAll
    fun beforeAll() {
        initMemberService.init()
    }
}
