package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.UserRecord
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `GET/POST/DELETE /jsonplaceholder/users` 엔드포인트 계약 테스트 (E34).
 *
 * 사용자 생성(201) → 단건 조회(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class UsersContractTest : AbstractJsonplaceholderContractTest() {

    /**
     * E34: POST → GET → DELETE 왕복 흐름으로 users CRUD 전 과정을 검증한다.
     */
    @Test
    @Order(1)
    fun `E34 users CRUD roundtrip - POST GET DELETE`() {
        val body = jsonMapper.writeValueAsString(
            UserRecord(
                id = 0L,
                name = "crud-user",
                username = "crud",
                email = "c@u.io",
                phone = "010",
                website = "crud.dev",
            )
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, UserRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/users/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/users/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
