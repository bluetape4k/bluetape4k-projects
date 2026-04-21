package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.mockserver.jsonplaceholder.model.AlbumRecord
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `GET/POST/DELETE /jsonplaceholder/albums` 엔드포인트 계약 테스트 (E31).
 *
 * 앨범 생성(201) → 단건 조회(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class AlbumsContractTest: AbstractJsonplaceholderContractTest() {

    /**
     * E31: POST → GET → DELETE 왕복 흐름으로 albums CRUD 전 과정을 검증한다.
     */
    @Test
    @Order(1)
    fun `E31 albums CRUD roundtrip - POST GET DELETE`() {
        val body = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 1L, id = 0L, title = "crud-album")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/albums")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, AlbumRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/albums/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/albums/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
