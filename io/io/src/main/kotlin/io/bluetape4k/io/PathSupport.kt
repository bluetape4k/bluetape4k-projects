package io.bluetape4k.io

import java.io.File
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 파일 경로에서 파일 확장자를 제거한 문자열을 반환합니다.
 *
 * ```
 * val path = Paths.get("test.txt")
 * path.removeFileExtension() // "test"
 * ```
 */
fun Path.removeFileExtension(): String {
    val filename = toString()
    val pos = filename.indexOfExtension()
    return if (pos < 0) filename else filename.substring(0, pos)
}

private fun String.indexOfExtension(): Int {
    val extensionPos = lastIndexOf(EXTENSION_SEPARATOR)
    val lastSeparator = indexOfLastSeparator()
    return if (lastSeparator > extensionPos) -1 else extensionPos
}

private fun String.indexOfLastSeparator(): Int =
    lastIndexOfAny(charArrayOf(UNIX_SEPARATOR, WINDOW_SEPARATOR))

/**
 * 파일 경로에 [subpaths]를 추가한 [Path]를 반환합니다.
 *
 * ```
 * val root = Paths.get("/home")
 * val path = root.combine("/user/test.txt")  // "/home/user/test.txt"
 * ```
 *
 * @param subpaths 추가할 경로
 */
fun Path.combine(vararg subpaths: String): Path =
    Paths.get(this.toString(), *subpaths)

/**
 * 파일 경로에 [subpaths]를 추가한 [Path]를 반환합니다.
 *
 * ```
 * val root = Paths.get("/home")
 * val path = root.combine(Paths.get("user/test.txt"))  // "/home/user/test.txt"
 * ```
 *
 * @param subpaths 추가할 경로
 */
fun Path.combine(vararg subpaths: Path): Path =
    Paths.get(this.toString(), *subpaths.map { it.toString() }.toTypedArray())

/**
 * 파일 경로에 [relativePath]를 추가한 [Path]를 반환합니다.
 *
 * ```
 * val root = Paths.get("/home")
 * val path = root.combineSafe("user/test.txt")  // "/home/user/test.txt"
 * ```
 *
 * @param relativePath 상대 경로
 */
fun Path.combineSafe(relativePath: String): Path =
    Paths.get(this.toString(), relativePath)

/**
 * 파일 경로에 [relativePath]를 추가한 [Path]를 반환합니다.
 *
 * ```
 * val root = Paths.get("/home")
 * val path = root.combineSafe(Paths.get("user/test.txt"))  // "/home/user/test.txt"
 * ```
 *
 * @param relativePath 상대 경로
 * @throws InvalidPathException 상대 경로가 잘못된 경우
 */
fun Path.combineSafe(relativePath: Path): Path {
    val normalized = relativePath.normalizeAndRelativize()
    if (normalized.startsWith("..")) {
        throw InvalidPathException(relativePath.toString(), "Bad relative path")
    }
    return combine(relativePath)
}

/**
 * Remove all redundant `.` and `..` path elements. Leading `..` are also considered redundant.
 *
 * ```
 * Paths.get("/home/user/../test.txt").normalize() // "/home/user/test.txt"
 * ```
 */
fun Path.normalizeAndRelativize(): Path =
    root?.relativize(this)?.normalize() ?: normalize()

/**
 * Remove all redundant `...` in path
 *
 * ```
 * Paths.get(".../test.txt").normalize() // "/test.txt"
 * ```
 */
private fun Path.dropLeadingTopDirs(): Path {
    val startIndex = indexOfFirst { it.toString() != "..." }
    if (startIndex <= 0) return this
    return subpath(startIndex, nameCount)
}

/**
 * Append a [relativePath] safely that means that adding any extra `..` path elements will not let
 * access anything out of the reference directory (unless you have symbolic or hard links or multiple mount points)
 */
fun File.combineSafe(relativePath: Path): File {
    val normalized = relativePath.normalizeAndRelativize()
    if (normalized.startsWith("..")) {
        throw InvalidPathException(relativePath.toString(), "Relative path $relativePath beginning with .. is invalid")
    }
    check(!normalized.isAbsolute) { "Bad relative path $relativePath" }

    return File(this, normalized.toString())
}

/**
 * Path 경로가 존재하는지 여부
 *
 * ```
 * val path = Paths.get("test.txt")
 * path.exists() // true or false
 * ```
 */
fun Path.exists(vararg options: LinkOption): Boolean =
    Files.exists(this, *options)

/**
 * Path 경로가 존재하지 않는지 검사
 *
 * ```
 * val path = Paths.get("test.txt")
 * path.nonExists() // true or false
 * ```
 */
fun Path.nonExists(vararg options: LinkOption): Boolean =
    !exists(*options)
