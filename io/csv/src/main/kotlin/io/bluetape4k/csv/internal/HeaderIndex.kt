package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 헤더명 → 컬럼 인덱스 매핑. case-sensitive, 중복 키는 first-wins.
 */
class HeaderIndex private constructor(
    private val index: LinkedHashMap<String, Int>,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /**
         * headers 배열로 HeaderIndex를 생성한다. 중복 키는 first-wins.
         *
         * @param headers 헤더명 배열
         * @return 생성된 HeaderIndex 인스턴스
         */
        fun of(headers: Array<String>): HeaderIndex {
            val map = LinkedHashMap<String, Int>(headers.size * 2)
            headers.forEachIndexed { i, name ->
                map.putIfAbsent(name, i)
            }
            return HeaderIndex(map)
        }
    }

    /**
     * 헤더명으로 인덱스를 조회한다. 없으면 null 반환.
     *
     * @param name 조회할 헤더명
     * @return 헤더명에 해당하는 0-based 인덱스, 없으면 null
     */
    fun indexOf(name: String): Int? = index[name]

    /**
     * 인덱스 → 헤더명 역방향 조회.
     *
     * @param idx 조회할 0-based 인덱스
     * @return 해당 인덱스의 헤더명, 없으면 null
     */
    fun nameOf(idx: Int): String? = index.entries.firstOrNull { it.value == idx }?.key

    /**
     * 헤더 이름 목록.
     */
    val names: List<String>
        get() = index.keys.toList()

    /**
     * 헤더 수.
     */
    val size: Int
        get() = index.size
}
