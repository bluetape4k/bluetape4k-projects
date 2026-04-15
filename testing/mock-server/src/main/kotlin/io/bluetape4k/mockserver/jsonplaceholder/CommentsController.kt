package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.jsonplaceholder.model.CommentRecord
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
 * jsonplaceholder `/jsonplaceholder/comments` CRUD 엔드포인트.
 *
 * 댓글 목록 조회, 개별 조회, 생성, 수정, 삭제 기능을 제공한다.
 * `postId` 쿼리 파라미터로 특정 게시글의 댓글만 필터링할 수 있다.
 *
 * @param service jsonplaceholder 인메모리 데이터 서비스
 */
@RestController
@RequestMapping("/jsonplaceholder/comments")
class CommentsController(private val service: JsonplaceholderService) {
    companion object : KLogging()

    /**
     * 댓글 목록을 반환한다. `postId`가 지정되면 해당 게시글의 댓글만 반환한다.
     *
     * @param postId 필터링할 게시글 ID (선택)
     * @return 댓글 목록
     */
    @GetMapping
    fun list(@RequestParam(required = false) postId: Long?): List<CommentRecord> =
        if (postId != null) service.comments.all().filter { it.postId == postId }
        else service.comments.all()

    /**
     * ID로 단일 댓글을 조회한다.
     *
     * @param id 댓글 ID
     * @return 해당 댓글
     * @throws NoSuchElementException 댓글이 없을 경우
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): CommentRecord =
        service.comments.find(id) ?: throw NoSuchElementException("Comment $id not found")

    /**
     * 새 댓글을 생성한다.
     *
     * @param comment 생성할 댓글 데이터
     * @return 생성된 댓글 (ID 할당됨)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody comment: CommentRecord): CommentRecord = service.comments.add(comment)

    /**
     * 댓글 전체를 교체한다.
     *
     * @param id 수정할 댓글 ID
     * @param comment 새 댓글 데이터
     * @return 수정된 댓글
     * @throws NoSuchElementException 댓글이 없을 경우
     */
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody comment: CommentRecord): CommentRecord =
        service.comments.update(id, comment) ?: throw NoSuchElementException("Comment $id not found")

    /**
     * 댓글 일부를 수정한다.
     *
     * @param id 수정할 댓글 ID
     * @param comment 수정할 댓글 데이터
     * @return 수정된 댓글
     * @throws NoSuchElementException 댓글이 없을 경우
     */
    @PatchMapping("/{id}")
    fun patch(@PathVariable id: Long, @RequestBody comment: CommentRecord): CommentRecord =
        service.comments.update(id, comment) ?: throw NoSuchElementException("Comment $id not found")

    /**
     * 댓글을 삭제한다.
     *
     * @param id 삭제할 댓글 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.comments.delete(id)
    }
}
