package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.PhotoRecord
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `GET/POST/DELETE /jsonplaceholder/photos` 엔드포인트 계약 테스트 (E32).
 *
 * 사진 생성(201) → 단건 조회(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class PhotosContractTest: AbstractJsonplaceholderContractTest() {

    /**
     * E32: POST → GET → DELETE 왕복 흐름으로 photos CRUD 전 과정을 검증한다.
     */
    @Test
    @Order(1)
    fun `E32 photos CRUD roundtrip - POST GET DELETE`() {
        val body = jsonMapper.writeValueAsString(
            PhotoRecord(
                albumId = 1L,
                id = 0L,
                title = "crud-photo",
                url = "http://example.com/u.png",
                thumbnailUrl = "http://example.com/t.png",
            )
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/photos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, PhotoRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/photos/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/photos/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
