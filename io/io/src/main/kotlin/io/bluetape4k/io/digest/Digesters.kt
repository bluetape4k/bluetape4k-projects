package io.bluetape4k.io.digest

/** 자주 사용하는 digest 구현에 대한 기본 접근점입니다. */
object Digesters {

    /** 표준 SHA-256 digest 구현입니다. */
    @JvmField
    val SHA256: Digester = JdkDigester("SHA-256")
}
