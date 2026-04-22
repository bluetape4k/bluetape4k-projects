package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.AlbumRecord
import org.amshove.kluent.shouldBeEqualTo
<<<<<<< feat/coverage-improvement
import org.amshove.kluent.shouldNotBeEmpty
=======
>>>>>>> develop
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
 * `/jsonplaceholder/albums` PUT/PATCH 및 필터 기능 확장 계약 테스트.
 *
 * PUT(전체 교체), PATCH(부분 수정), userId 필터 조회 흐름을 검증한다.
 */
class AlbumsExtendedContractTest: AbstractJsonplaceholderContractTest() {

    @Test
    @Order(1)
    fun `PUT albums id 전체 교체 후 변경된 값이 조회된다`() {
        // 먼저 앨범 생성
        val createBody = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 2L, id = 0L, title = "original-album")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/albums")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, AlbumRecord::class.java)
        val id = created.id

        // PUT으로 전체 교체
        val updateBody = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 2L, id = id, title = "updated-album")
        )
        mockMvc.perform(
            put("/jsonplaceholder/albums/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("updated-album"))
    }

    @Test
    @Order(2)
    fun `PATCH albums id 부분 수정 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 3L, id = 0L, title = "patch-original")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/albums")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, AlbumRecord::class.java)
        val id = created.id

        val patchBody = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 3L, id = id, title = "patch-updated")
        )
        mockMvc.perform(
            patch("/jsonplaceholder/albums/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("patch-updated"))
    }

    @Test
    @Order(3)
    fun `GET albums userId 필터가 동작한다`() {
        // userId=99로 앨범 생성
        val createBody = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 99L, id = 0L, title = "filtered-album")
        )
        mockMvc.perform(
            post("/jsonplaceholder/albums")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)

        // userId=99 필터 조회
        val result = mockMvc.perform(get("/jsonplaceholder/albums?userId=99"))
            .andExpect(status().isOk)
            .andReturn()

        val albums = jsonMapper.readValue(result.response.contentAsString, Array<AlbumRecord>::class.java)
<<<<<<< feat/coverage-improvement
        albums.shouldNotBeEmpty()
=======
>>>>>>> develop
        albums.all { it.userId == 99L } shouldBeEqualTo true
    }
}
