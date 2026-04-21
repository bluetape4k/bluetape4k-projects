package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.MockServerApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper

/**
 * jsonplaceholder 컨트롤러 계약 테스트를 위한 공통 기반 클래스.
 *
 * [MockServerApplication]을 MockMvc로 구동하여 jsonplaceholder 엔드포인트를 테스트한다.
 * 각 컨트롤러별 하위 클래스에서 구체적인 테스트 메서드를 구현한다.
 */
@SpringBootTest(classes = [MockServerApplication::class])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AbstractJsonplaceholderContractTest {

    companion object : KLogging()

    @Autowired
    protected lateinit var ctx: WebApplicationContext

    @Autowired
    protected lateinit var jsonMapper: JsonMapper

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build()
    }
}
