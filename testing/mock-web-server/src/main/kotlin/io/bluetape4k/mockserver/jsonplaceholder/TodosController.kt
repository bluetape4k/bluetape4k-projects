package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.jsonplaceholder.model.TodoRecord
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
 * jsonplaceholder `/jsonplaceholder/todos` CRUD 엔드포인트.
 *
 * 할 일 목록 조회, 개별 조회, 생성, 수정, 삭제 기능을 제공한다.
 * `userId` 쿼리 파라미터로 특정 사용자의 할 일만 필터링할 수 있다.
 *
 * @param service jsonplaceholder 인메모리 데이터 서비스
 */
@RestController
@RequestMapping("/jsonplaceholder/todos")
class TodosController(private val service: JsonplaceholderService) {
    companion object : KLogging()

    /**
     * 할 일 목록을 반환한다. `userId`가 지정되면 해당 사용자의 할 일만 반환한다.
     *
     * @param userId 필터링할 사용자 ID (선택)
     * @return 할 일 목록
     */
    @GetMapping
    fun list(@RequestParam(required = false) userId: Long?): List<TodoRecord> =
        if (userId != null) service.todos.all().filter { it.userId == userId }
        else service.todos.all()

    /**
     * ID로 단일 할 일을 조회한다.
     *
     * @param id 할 일 ID
     * @return 해당 할 일
     * @throws NoSuchElementException 할 일이 없을 경우
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TodoRecord =
        service.todos.find(id) ?: throw NoSuchElementException("Todo $id not found")

    /**
     * 새 할 일을 생성한다.
     *
     * @param todo 생성할 할 일 데이터
     * @return 생성된 할 일 (ID 할당됨)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody todo: TodoRecord): TodoRecord = service.todos.add(todo)

    /**
     * 할 일 전체를 교체한다.
     *
     * @param id 수정할 할 일 ID
     * @param todo 새 할 일 데이터
     * @return 수정된 할 일
     * @throws NoSuchElementException 할 일이 없을 경우
     */
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody todo: TodoRecord): TodoRecord =
        service.todos.update(id, todo) ?: throw NoSuchElementException("Todo $id not found")

    /**
     * 할 일 일부를 수정한다.
     *
     * @param id 수정할 할 일 ID
     * @param todo 수정할 할 일 데이터
     * @return 수정된 할 일
     * @throws NoSuchElementException 할 일이 없을 경우
     */
    @PatchMapping("/{id}")
    fun patch(@PathVariable id: Long, @RequestBody todo: TodoRecord): TodoRecord =
        service.todos.update(id, todo) ?: throw NoSuchElementException("Todo $id not found")

    /**
     * 할 일을 삭제한다.
     *
     * @param id 삭제할 할 일 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.todos.delete(id)
    }
}
