package io.bluetape4k.javatimes.period

/**
 * 검색 방향
 */
enum class SeekDirection(val direction: Int) {

    /** 미래로 (시간 값을 증가 시키는 방향) */
    FORWARD(1),

    /** 과거로 (시간 값을 감소 시키는 방향) */
    BACKWARD(-1);

    val isForward: Boolean get() = this == FORWARD

    companion object {
        @JvmStatic
        fun of(dir: Int): SeekDirection = if (dir > 0) FORWARD else BACKWARD
    }
}
