package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.PhotoRecord
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
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
 * `/jsonplaceholder/photos` PUT/PATCH 및 필터 기능 확장 계약 테스트.
 */
class PhotosExtendedContractTest: AbstractJsonplaceholderContractTest() {

    @Test
    @Order(1)
    fun `PUT photos id 전체 교체 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            PhotoRecord(albumId = 5L, id = 0L, title = "orig-photo", url = "http://a.com/o.png", thumbnailUrl = "http://a.com/t.png")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/photos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, PhotoRecord::class.java)
        val id = created.id

        val updateBody = jsonMapper.writeValueAsString(
            PhotoRecord(albumId = 5L, id = id, title = "updated-photo", url = "http://a.com/u.png", thumbnailUrl = "http://a.com/tu.png")
        )
        mockMvc.perform(
            put("/jsonplaceholder/photos/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("updated-photo"))
    }

    @Test
    @Order(2)
    fun `PATCH photos id 부분 수정 후 변경된 값이 조회된다`() {
        val createBody = jsonMapper.writeValueAsString(
            PhotoRecord(albumId = 6L, id = 0L, title = "patch-photo", url = "http://b.com/p.png", thumbnailUrl = "http://b.com/pt.png")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/photos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, PhotoRecord::class.java)
        val id = created.id

        val patchBody = jsonMapper.writeValueAsString(
            PhotoRecord(albumId = 6L, id = id, title = "patch-photo-updated", url = "http://b.com/pu.png", thumbnailUrl = "http://b.com/ptu.png")
        )
        mockMvc.perform(
            patch("/jsonplaceholder/photos/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("patch-photo-updated"))
    }

    @Test
    @Order(3)
    fun `GET photos albumId 필터가 동작한다`() {
        val createBody = jsonMapper.writeValueAsString(
            PhotoRecord(albumId = 888L, id = 0L, title = "filtered-photo", url = "http://f.com/fp.png", thumbnailUrl = "http://f.com/fpt.png")
        )
        mockMvc.perform(
            post("/jsonplaceholder/photos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)

        val result = mockMvc.perform(get("/jsonplaceholder/photos?albumId=888"))
            .andExpect(status().isOk)
            .andReturn()

        val photos = jsonMapper.readValue(result.response.contentAsString, Array<PhotoRecord>::class.java)
        photos.shouldNotBeEmpty()
        photos.all { it.albumId == 888L } shouldBeEqualTo true
    }
}
