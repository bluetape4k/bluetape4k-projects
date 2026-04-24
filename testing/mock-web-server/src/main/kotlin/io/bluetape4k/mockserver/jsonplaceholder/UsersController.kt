package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.jsonplaceholder.model.UserRecord
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * jsonplaceholder `/jsonplaceholder/users` CRUD 엔드포인트.
 *
 * 사용자 목록 조회, 개별 조회, 생성, 수정, 삭제 기능을 제공한다.
 *
 * @param service jsonplaceholder 인메모리 데이터 서비스
 */
@RestController
@RequestMapping("/jsonplaceholder/users")
class UsersController(private val service: JsonplaceholderService) {
    companion object : KLogging()

    /**
     * 모든 사용자 목록을 반환한다.
     *
     * @return 사용자 목록
     */
    @GetMapping
    fun list(): List<UserRecord> = service.users.all()

    /**
     * ID로 단일 사용자를 조회한다.
     *
     * @param id 사용자 ID
     * @return 해당 사용자
     * @throws NoSuchElementException 사용자가 없을 경우
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): UserRecord =
        service.users.find(id) ?: throw NoSuchElementException("User $id not found")

    /**
     * 새 사용자를 생성한다.
     *
     * @param user 생성할 사용자 데이터
     * @return 생성된 사용자 (ID 할당됨)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody user: UserRecord): UserRecord = service.users.add(user)

    /**
     * 사용자 정보 전체를 교체한다.
     *
     * @param id 수정할 사용자 ID
     * @param user 새 사용자 데이터
     * @return 수정된 사용자
     * @throws NoSuchElementException 사용자가 없을 경우
     */
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody user: UserRecord): UserRecord =
        service.users.update(id, user) ?: throw NoSuchElementException("User $id not found")

    /**
     * 사용자 정보 일부를 수정한다.
     *
     * @param id 수정할 사용자 ID
     * @param user 수정할 사용자 데이터
     * @return 수정된 사용자
     * @throws NoSuchElementException 사용자가 없을 경우
     */
    @PatchMapping("/{id}")
    fun patch(@PathVariable id: Long, @RequestBody user: UserRecord): UserRecord =
        service.users.update(id, user) ?: throw NoSuchElementException("User $id not found")

    /**
     * 사용자를 삭제한다.
     *
     * @param id 삭제할 사용자 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.users.delete(id)
    }
}
