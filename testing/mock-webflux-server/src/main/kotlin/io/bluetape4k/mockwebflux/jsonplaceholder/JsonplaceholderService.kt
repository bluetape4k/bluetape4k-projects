package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mockwebflux.jsonplaceholder.model.AlbumRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.CommentRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.PhotoRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.PostRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.TodoRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.UserRecord
import org.springframework.stereotype.Service
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * jsonplaceholder 인메모리 데이터 서비스.
 *
 * fixture 데이터를 로드하고 CRUD 저장소를 관리한다.
 * [reloadFromFixtures]는 원자적으로 전체 데이터를 재적재한다.
 *
 * ```kotlin
 * // Spring context 에서 주입된 서비스 사용 예
 * @Autowired lateinit var service: JsonplaceholderService
 *
 * // 전체 posts 조회
 * val posts = service.posts.all()
 * // ID로 단건 조회
 * val post = service.posts.find(1L)
 * // 테스트 후 fixture 재적재로 상태 초기화
 * service.reloadFromFixtures()
 * ```
 *
 * @param fixtureLoader 클래스패스 JSON fixture를 로드하는 컴포넌트
 */
@Service
class JsonplaceholderService(private val fixtureLoader: FixtureLoader) {
    companion object: KLogging()

    // synchronized 대신 ReentrantLock 사용: Virtual Thread에서 monitor lock은 carrier thread를 pin함
    private val reloadLock = ReentrantLock()

    /** 게시글 저장소 */
    val posts = InMemoryRepository<PostRecord>({ it.id }, { item, id -> item.copy(id = id) })

    /** 댓글 저장소 */
    val comments = InMemoryRepository<CommentRecord>({ it.id }, { item, id -> item.copy(id = id) })

    /** 앨범 저장소 */
    val albums = InMemoryRepository<AlbumRecord>({ it.id }, { item, id -> item.copy(id = id) })

    /** 사진 저장소 */
    val photos = InMemoryRepository<PhotoRecord>({ it.id }, { item, id -> item.copy(id = id) })

    /** 할 일 저장소 */
    val todos = InMemoryRepository<TodoRecord>({ it.id }, { item, id -> item.copy(id = id) })

    /** 사용자 저장소 */
    val users = InMemoryRepository<UserRecord>({ it.id }, { item, id -> item.copy(id = id) })

    /**
     * fixture 파일에서 모든 데이터를 원자적으로 재적재한다.
     *
     * 모든 fixture를 먼저 로컬에 로드한 후 [ReentrantLock] 안에서 일괄 교체하여
     * 부분 업데이트 상태를 방지한다. Virtual Thread 환경에서도 안전하게 동작한다.
     * 로드 단계에서 예외 발생 시 기존 데이터는 변경되지 않는다.
     */
    fun reloadFromFixtures() {
        log.info { "Reloading all fixtures..." }

        // 1단계: 모두 로드 (예외 발생 시 교체 미수행)
        val newPosts = fixtureLoader.load("jsonplaceholder/posts.json", PostRecord::class.java)
        val newComments = fixtureLoader.load("jsonplaceholder/comments.json", CommentRecord::class.java)
        val newAlbums = fixtureLoader.load("jsonplaceholder/albums.json", AlbumRecord::class.java)
        val newPhotos = fixtureLoader.load("jsonplaceholder/photos.json", PhotoRecord::class.java)
        val newTodos = fixtureLoader.load("jsonplaceholder/todos.json", TodoRecord::class.java)
        val newUsers = fixtureLoader.load("jsonplaceholder/users.json", UserRecord::class.java)

        // 2단계: 원자적 교체 (ReentrantLock — Virtual Thread 호환)
        reloadLock.withLock {
            posts.loadAll(newPosts)
            comments.loadAll(newComments)
            albums.loadAll(newAlbums)
            photos.loadAll(newPhotos)
            todos.loadAll(newTodos)
            users.loadAll(newUsers)
        }

        log.info {
            "Fixtures reloaded: posts=${newPosts.size}, comments=${newComments.size}, " +
                    "albums=${newAlbums.size}, photos=${newPhotos.size}, " +
                    "todos=${newTodos.size}, users=${newUsers.size}"
        }
    }
}
