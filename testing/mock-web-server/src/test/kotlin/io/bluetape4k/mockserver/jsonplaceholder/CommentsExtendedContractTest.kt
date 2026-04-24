package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.CommentRecord
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
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
 * `/jsonplaceholder/comments` PUT/PATCH 및 필터 기능 확장 계약 테스트.
 */
class CommentsExtendedContractTest: AbstractJsonplaceholderContractTest() {

    @Test
    @Order(1)
    fun `PUT comments id 전체 교체 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            CommentRecord(postId = 10L, id = 0L, name = "original", email = "a@b.c", body = "original body")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, CommentRecord::class.java)
        val id = created.id

        val updateBody = jsonMapper.writeValueAsString(
            CommentRecord(postId = 10L, id = id, name = "updated", email = "b@c.d", body = "updated body")
        )
        mockMvc.perform(
            put("/jsonplaceholder/comments/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("updated"))
    }

    @Test
    @Order(2)
    fun `PATCH comments id 부분 수정 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            CommentRecord(postId = 20L, id = 0L, name = "patch-orig", email = "x@y.z", body = "patch orig")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, CommentRecord::class.java)
        val id = created.id

        val patchBody = jsonMapper.writeValueAsString(
            CommentRecord(postId = 20L, id = id, name = "patch-updated", email = "x@y.z", body = "patch updated")
        )
        mockMvc.perform(
            patch("/jsonplaceholder/comments/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("patch-updated"))
    }

    @Test
    @Order(3)
    fun `GET comments postId 필터가 동작한다`() {
        val createBody = jsonMapper.writeValueAsString(
            CommentRecord(postId = 999L, id = 0L, name = "filtered", email = "f@g.h", body = "filtered body")
        )
        mockMvc.perform(
            post("/jsonplaceholder/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)

        val result = mockMvc.perform(get("/jsonplaceholder/comments?postId=999"))
            .andExpect(status().isOk)
            .andReturn()

        val comments = jsonMapper.readValue(result.response.contentAsString, Array<CommentRecord>::class.java)
        comments.shouldNotBeEmpty()
        comments.all { it.postId == 999L } shouldBeEqualTo true
    }
}
