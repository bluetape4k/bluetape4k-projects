package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.CommentRecord
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `GET/POST/DELETE /jsonplaceholder/comments` 엔드포인트 계약 테스트 (E30).
 *
 * 댓글 생성(201) → 단건 조회(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class CommentsContractTest: AbstractJsonplaceholderContractTest() {

    /**
     * E30: POST → GET → DELETE 왕복 흐름으로 comments CRUD 전 과정을 검증한다.
     */
    @Test
    @Order(1)
    fun `E30 comments CRUD roundtrip - POST GET DELETE`() {
        val body = jsonMapper.writeValueAsString(
            CommentRecord(postId = 1L, id = 0L, name = "crud", email = "a@b.c", body = "hi")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, CommentRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/comments/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/comments/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
