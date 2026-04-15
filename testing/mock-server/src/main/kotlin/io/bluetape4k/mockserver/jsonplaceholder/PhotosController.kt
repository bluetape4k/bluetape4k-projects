package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.jsonplaceholder.model.PhotoRecord
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * jsonplaceholder `/jsonplaceholder/photos` CRUD 엔드포인트.
 *
 * 사진 목록 조회, 개별 조회, 생성, 수정, 삭제 기능을 제공한다.
 * `albumId` 쿼리 파라미터로 특정 앨범의 사진만 필터링할 수 있다.
 *
 * @param service jsonplaceholder 인메모리 데이터 서비스
 */
@RestController
@RequestMapping("/jsonplaceholder/photos")
class PhotosController(private val service: JsonplaceholderService) {
    companion object : KLogging()

    /**
     * 사진 목록을 반환한다. `albumId`가 지정되면 해당 앨범의 사진만 반환한다.
     *
     * @param albumId 필터링할 앨범 ID (선택)
     * @return 사진 목록
     */
    @GetMapping
    fun list(@RequestParam(required = false) albumId: Long?): List<PhotoRecord> =
        if (albumId != null) service.photos.all().filter { it.albumId == albumId }
        else service.photos.all()

    /**
     * ID로 단일 사진을 조회한다.
     *
     * @param id 사진 ID
     * @return 해당 사진
     * @throws NoSuchElementException 사진이 없을 경우
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): PhotoRecord =
        service.photos.find(id) ?: throw NoSuchElementException("Photo $id not found")

    /**
     * 새 사진을 생성한다.
     *
     * @param photo 생성할 사진 데이터
     * @return 생성된 사진 (ID 할당됨)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody photo: PhotoRecord): PhotoRecord = service.photos.add(photo)

    /**
     * 사진 전체를 교체한다.
     *
     * @param id 수정할 사진 ID
     * @param photo 새 사진 데이터
     * @return 수정된 사진
     * @throws NoSuchElementException 사진이 없을 경우
     */
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody photo: PhotoRecord): PhotoRecord =
        service.photos.update(id, photo) ?: throw NoSuchElementException("Photo $id not found")

    /**
     * 사진 일부를 수정한다.
     *
     * @param id 수정할 사진 ID
     * @param photo 수정할 사진 데이터
     * @return 수정된 사진
     * @throws NoSuchElementException 사진이 없을 경우
     */
    @PatchMapping("/{id}")
    fun patch(@PathVariable id: Long, @RequestBody photo: PhotoRecord): PhotoRecord =
        service.photos.update(id, photo) ?: throw NoSuchElementException("Photo $id not found")

    /**
     * 사진을 삭제한다.
     *
     * @param id 삭제할 사진 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.photos.delete(id)
    }
}
