package io.bluetape4k.javatimes.period

/**
 * 검색 방향
 *
 * ```kotlin
 * val dir = SeekDirection.FORWARD
 * dir.isForward  // true
 * dir.direction  // 1
 *
 * val backward = SeekDirection.of(-1) // BACKWARD
 * ```
 */
enum class SeekDirection(val direction: Int) {

    /** 미래로 (시간 값을 증가 시키는 방향) */
    FORWARD(1),

    /** 과거로 (시간 값을 감소 시키는 방향) */
    BACKWARD(-1);

    val isForward: Boolean get() = this == FORWARD

    companion object {
        /**
         * 방향 정수값으로부터 [SeekDirection]을 반환합니다.
         *
         * ```kotlin
         * SeekDirection.of(1)  // FORWARD
         * SeekDirection.of(-1) // BACKWARD
         * ```
         */
        @JvmStatic
        fun of(dir: Int): SeekDirection = if (dir > 0) FORWARD else BACKWARD
    }
}
