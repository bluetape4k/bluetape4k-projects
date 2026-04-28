package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.info
import io.bluetape4k.mockserver.jsonplaceholder.model.PostRecord
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `GET/POST/PUT/DELETE /jsonplaceholder/posts` 엔드포인트 계약 테스트 (E29).
 *
 * 게시글 목록 조회, 단건 조회, 존재하지 않는 ID 조회(404), 생성(201), 수정(200), 삭제(204),
 * admin 리셋 후 복원 확인, CRUD 왕복 흐름을 순서대로 검증한다.
 */
class PostsContractTest: AbstractJsonplaceholderContractTest() {

    /**
     * GET /jsonplaceholder/posts → 200, 비어있지 않은 게시글 목록 반환
     */
    @Test
    @Order(1)
    fun `GET posts returns non-empty list with 200`() {
        mockMvc.perform(get("/jsonplaceholder/posts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(greaterThan(0)))
            .andDo { log.info { "GET /jsonplaceholder/posts → 200" } }
    }

    /**
     * GET /jsonplaceholder/posts/1 → 200, id=1인 PostRecord 반환
     */
    @Test
    @Order(2)
    fun `GET posts by id returns single post with 200`() {
        mockMvc.perform(get("/jsonplaceholder/posts/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andDo { log.info { "GET /jsonplaceholder/posts/1 → 200" } }
    }

    /**
     * GET /jsonplaceholder/posts/9999 → 404 (존재하지 않는 ID)
     */
    @Test
    @Order(3)
    fun `GET posts with non-existent id returns 404`() {
        mockMvc.perform(get("/jsonplaceholder/posts/9999"))
            .andExpect(status().isNotFound)
            .andDo { log.info { "GET /jsonplaceholder/posts/9999 → 404" } }
    }

    /**
     * POST /jsonplaceholder/posts → 201, 새 id가 부여된 PostRecord 반환
     */
    @Test
    @Order(4)
    fun `POST posts creates new post with 201 and new id`() {
        val newPost = PostRecord(id = 0L, userId = 1L, title = "test title", body = "test body")
        val body = jsonMapper.writeValueAsString(newPost)

        mockMvc.perform(
            post("/jsonplaceholder/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(greaterThan(0)))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.title").value("test title"))
            .andExpect(jsonPath("$.body").value("test body"))
            .andDo { log.info { "POST /jsonplaceholder/posts → 201" } }
    }

    /**
     * PUT /jsonplaceholder/posts/1 → 200, 수정된 PostRecord 반환
     */
    @Test
    @Order(5)
    fun `PUT posts updates existing post with 200`() {
        val updatedPost = PostRecord(id = 1L, userId = 1L, title = "updated title", body = "updated body")
        val body = jsonMapper.writeValueAsString(updatedPost)

        mockMvc.perform(
            put("/jsonplaceholder/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("updated title"))
            .andExpect(jsonPath("$.body").value("updated body"))
            .andDo { log.info { "PUT /jsonplaceholder/posts/1 → 200" } }
    }

    /**
     * DELETE /jsonplaceholder/posts/1 → 204 (삭제 성공)
     */
    @Test
    @Order(6)
    fun `DELETE posts removes post with 204`() {
        mockMvc.perform(delete("/jsonplaceholder/posts/1"))
            .andExpect(status().is2xxSuccessful)
            .andDo { log.info { "DELETE /jsonplaceholder/posts/1 → 204" } }
    }

    /**
     * POST /admin/reset → 200, 이후 GET /jsonplaceholder/posts가 픽스처로 복원됨을 확인
     */
    @Test
    @Order(7)
    fun `POST admin reset restores fixtures and posts are non-empty`() {
        mockMvc.perform(post("/admin/reset").header("X-Admin-Token", "dev-only-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
            .andDo { log.info { "POST /admin/reset → 200" } }

        mockMvc.perform(get("/jsonplaceholder/posts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(greaterThan(0)))
            .andDo { log.info { "GET /jsonplaceholder/posts after reset → 200" } }
    }

    /**
     * E29: POST → GET → DELETE 왕복 흐름으로 posts CRUD 전 과정을 검증한다.
     */
    @Test
    @Order(8)
    fun `E29 posts CRUD roundtrip - POST GET DELETE`() {
        val body = jsonMapper.writeValueAsString(
            PostRecord(id = 0L, userId = 1L, title = "crud-title", body = "crud-body")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, PostRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/posts/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/posts/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
