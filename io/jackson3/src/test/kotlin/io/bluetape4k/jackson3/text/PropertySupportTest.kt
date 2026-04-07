package io.bluetape4k.jackson3.text

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [getNode] 및 [getNodeOrNull] 확장 함수 테스트
 */
class PropertySupportTest {
    private val root: Map<Any, Any?> =
        mapOf(
            "a" to
                    mapOf(
                        "b" to
                                mapOf(
                                    "c" to 1
                                )
                    ),
            "x" to "leaf"
        )

    // ─── getNode ─────────────────────────────────────────────────────────────

    @Test
    fun `getNode - 단일 세그먼트 경로 조회`() {
        val node = root.getNode("a")
        node.shouldNotBeNull()
        node shouldBeEqualTo mapOf("b" to mapOf("c" to 1))
    }

    @Test
    fun `getNode - 중첩 경로 조회`() {
        val node = root.getNode("a.b")
        node.shouldNotBeNull()
        node shouldBeEqualTo mapOf("c" to 1)
    }

    @Test
    fun `getNode - 존재하지 않는 경로는 예외 발생`() {
        assertThrows<Exception> {
            root.getNode("a.z")
        }
    }

    @Test
    fun `getNode - 리프 노드를 맵으로 조회 시 예외 발생`() {
        assertThrows<Exception> {
            root.getNode("x")
        }
    }

    @Test
    fun `getNode - 커스텀 delimiter 사용`() {
        val node = root.getNode("a/b", delimiter = "/")
        node.shouldNotBeNull()
        node shouldBeEqualTo mapOf("c" to 1)
    }

    // ─── getNodeOrNull ────────────────────────────────────────────────────────

    @Test
    fun `getNodeOrNull - 단일 세그먼트 경로 조회`() {
        val node = root.getNodeOrNull("a")
        node.shouldNotBeNull()
        node shouldBeEqualTo mapOf("b" to mapOf("c" to 1))
    }

    @Test
    fun `getNodeOrNull - 중첩 경로 조회`() {
        val node = root.getNodeOrNull("a.b")
        node.shouldNotBeNull()
        node shouldBeEqualTo mapOf("c" to 1)
    }

    @Test
    fun `getNodeOrNull - 존재하지 않는 중간 경로는 null 반환`() {
        val node = root.getNodeOrNull("a.z")
        node.shouldBeNull()
    }

    @Test
    fun `getNodeOrNull - 존재하지 않는 루트 키는 null 반환`() {
        val node = root.getNodeOrNull("nonexistent")
        node.shouldBeNull()
    }

    @Test
    fun `getNodeOrNull - 리프 노드를 맵으로 조회 시 null 반환`() {
        // "x"는 String 이므로 Map으로 캐스팅 불가 → null 반환
        val node = root.getNodeOrNull("x")
        node.shouldBeNull()
    }

    @Test
    fun `getNodeOrNull - 커스텀 delimiter 사용`() {
        val node = root.getNodeOrNull("a/b", delimiter = "/")
        node.shouldNotBeNull()
        node shouldBeEqualTo mapOf("c" to 1)
    }

    @Test
    fun `getNodeOrNull - 빈 맵에서 조회 시 null 반환`() {
        val empty: Map<Any, Any?> = emptyMap()
        empty.getNodeOrNull("a.b").shouldBeNull()
    }
}
