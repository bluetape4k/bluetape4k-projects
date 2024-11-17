package io.bluetape4k.junit5.system

import org.junit.Assume

@Deprecated("Use assumeNotWindows instead", ReplaceWith("assumeNotWindows()"))
fun assumeNoWindows() {
    Assume.assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
}

fun assumeNotWindows() {
    Assume.assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
}
