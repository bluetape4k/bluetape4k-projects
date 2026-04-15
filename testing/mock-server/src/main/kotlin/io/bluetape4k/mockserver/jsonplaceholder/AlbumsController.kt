package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.jsonplaceholder.model.AlbumRecord
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
 * jsonplaceholder `/jsonplaceholder/albums` CRUD 엔드포인트.
 *
 * 앨범 목록 조회, 개별 조회, 생성, 수정, 삭제 기능을 제공한다.
 * `userId` 쿼리 파라미터로 특정 사용자의 앨범만 필터링할 수 있다.
 *
 * @param service jsonplaceholder 인메모리 데이터 서비스
 */
@RestController
@RequestMapping("/jsonplaceholder/albums")
class AlbumsController(private val service: JsonplaceholderService) {
    companion object : KLogging()

    /**
     * 앨범 목록을 반환한다. `userId`가 지정되면 해당 사용자의 앨범만 반환한다.
     *
     * @param userId 필터링할 사용자 ID (선택)
     * @return 앨범 목록
     */
    @GetMapping
    fun list(@RequestParam(required = false) userId: Long?): List<AlbumRecord> =
        if (userId != null) service.albums.all().filter { it.userId == userId }
        else service.albums.all()

    /**
     * ID로 단일 앨범을 조회한다.
     *
     * @param id 앨범 ID
     * @return 해당 앨범
     * @throws NoSuchElementException 앨범이 없을 경우
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): AlbumRecord =
        service.albums.find(id) ?: throw NoSuchElementException("Album $id not found")

    /**
     * 새 앨범을 생성한다.
     *
     * @param album 생성할 앨범 데이터
     * @return 생성된 앨범 (ID 할당됨)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody album: AlbumRecord): AlbumRecord = service.albums.add(album)

    /**
     * 앨범 전체를 교체한다.
     *
     * @param id 수정할 앨범 ID
     * @param album 새 앨범 데이터
     * @return 수정된 앨범
     * @throws NoSuchElementException 앨범이 없을 경우
     */
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody album: AlbumRecord): AlbumRecord =
        service.albums.update(id, album) ?: throw NoSuchElementException("Album $id not found")

    /**
     * 앨범 일부를 수정한다.
     *
     * @param id 수정할 앨범 ID
     * @param album 수정할 앨범 데이터
     * @return 수정된 앨범
     * @throws NoSuchElementException 앨범이 없을 경우
     */
    @PatchMapping("/{id}")
    fun patch(@PathVariable id: Long, @RequestBody album: AlbumRecord): AlbumRecord =
        service.albums.update(id, album) ?: throw NoSuchElementException("Album $id not found")

    /**
     * 앨범을 삭제한다.
     *
     * @param id 삭제할 앨범 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.albums.delete(id)
    }
}
