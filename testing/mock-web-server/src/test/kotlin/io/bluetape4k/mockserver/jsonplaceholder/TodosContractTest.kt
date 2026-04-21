package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.TodoRecord
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `GET/POST/DELETE /jsonplaceholder/todos` 엔드포인트 계약 테스트 (E33).
 *
 * 할 일 생성(201) → 단건 조회(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class TodosContractTest : AbstractJsonplaceholderContractTest() {

    /**
     * E33: POST → GET → DELETE 왕복 흐름으로 todos CRUD 전 과정을 검증한다.
     */
    @Test
    @Order(1)
    fun `E33 todos CRUD roundtrip - POST GET DELETE`() {
        val body = jsonMapper.writeValueAsString(
            TodoRecord(userId = 1L, id = 0L, title = "crud-todo", completed = false)
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, TodoRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/todos/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/todos/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
