package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class EmitHandlerTest {
    companion object: KLogging()

    @Test
    fun `DefaultEmitHandler 기본 동작`() {
        val handler = DefaultEmitHandler()
        val emit = Emit(0, 4, "hello")

        val result = handler.emit(emit)

        result.shouldBeTrue()
        handler.emits.size shouldBeEqualTo 1
        handler.emits[0] shouldBeEqualTo emit
    }

    @Test
    fun `DefaultEmitHandler 여러 Emit 추가`() {
        val handler = DefaultEmitHandler()

        handler.emit(Emit(0, 2, "abc"))
        handler.emit(Emit(4, 6, "def"))
        handler.emit(Emit(8, 10, "ghi"))

        handler.emits.size shouldBeEqualTo 3
    }

    @Test
    fun `커스텀 EmitHandler 구현`() {
        val collectedEmits = mutableListOf<Emit>()

        // 함수형 인터페이스로 EmitHandler 구현
        val handler =
            EmitHandler { emit ->
                collectedEmits.add(emit)
                true
            }

        handler.emit(Emit(0, 2, "test"))

        collectedEmits.size shouldBeEqualTo 1
    }

    @Test
    fun `EmitHandler에서 false 반환 시 중단`() {
        var count = 0

        val handler =
            EmitHandler { emit ->
                count++
                false // false를 반환하면 중단
            }

        // 첫 번째 emit만 처리하고 중단
        handler.emit(Emit(0, 2, "abc"))
        count shouldBeEqualTo 1
    }

    @Test
    fun `AbstractStatefulEmitHandler 확장`() {
        val handler =
            object: AbstractStatefulEmitHandler() {
                override fun emit(emit: Emit): Boolean {
                    // 조건에 따라 emit 처리
                    if ((emit.keyword?.length ?: 0) > 2) {
                        return addEmit(emit)
                    }
                    return false
                }
            }

        handler.emit(Emit(0, 1, "ab")) // 길이 2, 처리되지 않음
        handler.emit(Emit(0, 4, "hello")) // 길이 5, 처리됨

        handler.emits.size shouldBeEqualTo 1
        handler.emits[0].keyword shouldBeEqualTo "hello"
    }

    @Test
    fun `StatefulEmitHandler를 사용한 Trie 파싱`() {
        val PRONOUNS = listOf("hers", "his", "she", "he")
        val trie =
            Trie
                .builder()
                .addKeywords(PRONOUNS)
                .build()

        val handler = DefaultEmitHandler()
        trie.runParseText("ushers", handler)

        handler.emits.size shouldBeEqualTo 3
    }

    @Test
    fun `stopOnHit과 EmitHandler 함께 사용`() {
        val trie =
            Trie
                .builder()
                .addKeywords("he", "she", "hers")
                .stopOnHit()
                .build()

        val handler = DefaultEmitHandler()
        trie.runParseText("ushers", handler)

        // stopOnHit이 설정되어 있으므로 첫 번째 매치 후 중단
        handler.emits.size shouldBeEqualTo 1
    }

    @Test
    fun `EmitHandler emit 순서 검증`() {
        val handler = DefaultEmitHandler()

        val emit1 = Emit(0, 2, "abc")
        val emit2 = Emit(3, 5, "def")
        val emit3 = Emit(6, 8, "ghi")

        handler.emit(emit1)
        handler.emit(emit2)
        handler.emit(emit3)

        handler.emits[0] shouldBeEqualTo emit1
        handler.emits[1] shouldBeEqualTo emit2
        handler.emits[2] shouldBeEqualTo emit3
    }
}
