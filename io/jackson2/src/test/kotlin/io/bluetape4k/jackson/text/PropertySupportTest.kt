package io.bluetape4k.jackson.text

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [getNode] 및 [getNodeOrNull] 확장 함수 단위 테스트.
 */
@Suppress("UNCHECKED_CAST")
class PropertySupportTest {
    private val root: Map<Any, Any?> =
        mapOf(
            "a" to
                mapOf(
                    "b" to
                        mapOf(
                            "c" to 42
                        )
                ),
            "x" to "leaf"
        ) as Map<Any, Any?>

    // ── getNode ──────────────────────────────────────────────────────────────

    @Test
    fun `getNode - 단일 키로 중간 노드를 반환한다`() {
        val node = root.getNode("a")
        node.shouldNotBeNull()
        node["b"].shouldNotBeNull()
    }

    @Test
    fun `getNode - 다단계 경로로 하위 노드를 반환한다`() {
        val node = root.getNode("a.b")
        node shouldBeEqualTo mapOf("c" to 42)
    }

    @Test
    fun `getNode - 커스텀 구분자를 사용할 수 있다`() {
        val node = root.getNode("a/b", delimiter = "/")
        node shouldBeEqualTo mapOf("c" to 42)
    }

    @Test
    fun `getNode - 존재하지 않는 경로는 예외를 발생시킨다`() {
        assertThrows<Exception> {
            root.getNode("a.z")
        }
    }

    @Test
    fun `getNode - 리프 노드를 Map으로 캐스팅하면 예외가 발생한다`() {
        assertThrows<Exception> {
            root.getNode("x.sub")
        }
    }

    // ── getNodeOrNull ─────────────────────────────────────────────────────────

    @Test
    fun `getNodeOrNull - 유효한 경로에서 노드를 반환한다`() {
        val node = root.getNodeOrNull("a.b")
        node shouldBeEqualTo mapOf("c" to 42)
    }

    @Test
    fun `getNodeOrNull - 존재하지 않는 경로는 null을 반환한다`() {
        root.getNodeOrNull("a.z").shouldBeNull()
    }

    @Test
    fun `getNodeOrNull - 리프 노드를 Map으로 캐스팅하면 null을 반환한다`() {
        root.getNodeOrNull("x.sub").shouldBeNull()
    }

    @Test
    fun `getNodeOrNull - 단일 키로 중간 노드를 안전하게 반환한다`() {
        val node = root.getNodeOrNull("a")
        node.shouldNotBeNull()
    }

    @Test
    fun `getNodeOrNull - 커스텀 구분자를 사용할 수 있다`() {
        val node = root.getNodeOrNull("a/b", delimiter = "/")
        node shouldBeEqualTo mapOf("c" to 42)
    }

    @Test
    fun `getNodeOrNull - 빈 Map에서 조회하면 null을 반환한다`() {
        val empty = emptyMap<Any, Any?>()
        empty.getNodeOrNull("any.path").shouldBeNull()
    }
}
