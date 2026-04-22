package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.TodoRecord
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `/jsonplaceholder/todos` PUT/PATCH 및 필터 기능 확장 계약 테스트.
 */
class TodosExtendedContractTest: AbstractJsonplaceholderContractTest() {

    @Test
    @Order(1)
    fun `PUT todos id 전체 교체 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            TodoRecord(userId = 7L, id = 0L, title = "orig-todo", completed = false)
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, TodoRecord::class.java)
        val id = created.id

        val updateBody = jsonMapper.writeValueAsString(
            TodoRecord(userId = 7L, id = id, title = "updated-todo", completed = true)
        )
        mockMvc.perform(
            put("/jsonplaceholder/todos/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("updated-todo"))
            .andExpect(jsonPath("$.completed").value(true))
    }

    @Test
    @Order(2)
    fun `PATCH todos id 부분 수정 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            TodoRecord(userId = 8L, id = 0L, title = "patch-todo", completed = false)
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, TodoRecord::class.java)
        val id = created.id

        val patchBody = jsonMapper.writeValueAsString(
            TodoRecord(userId = 8L, id = id, title = "patch-todo-updated", completed = true)
        )
        mockMvc.perform(
            patch("/jsonplaceholder/todos/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("patch-todo-updated"))
    }

    @Test
    @Order(3)
    fun `GET todos userId 필터가 동작한다`() {
        val createBody = jsonMapper.writeValueAsString(
            TodoRecord(userId = 777L, id = 0L, title = "filtered-todo", completed = false)
        )
        mockMvc.perform(
            post("/jsonplaceholder/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)

        val result = mockMvc.perform(get("/jsonplaceholder/todos?userId=777"))
            .andExpect(status().isOk)
            .andReturn()

        val todos = jsonMapper.readValue(result.response.contentAsString, Array<TodoRecord>::class.java)
        todos.all { it.userId == 777L } shouldBeEqualTo true
    }
}
