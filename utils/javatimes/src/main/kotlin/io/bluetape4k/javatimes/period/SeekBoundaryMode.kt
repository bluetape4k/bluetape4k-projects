package io.bluetape4k.javatimes.period

/**
 * Seek 검색 시 반환 값의 종류
 */
enum class SeekBoundaryMode {

    /** Temporal 검색 시 검색한 값을 반환하도록 한다  */
    FILL,

    /** Temporal 검색 시 검색한 다음 값을 반환하도록 한다. */
    NEXT;

    val isFill: Boolean get() = this == FILL

    val isNext: Boolean get() = this == NEXT
}
