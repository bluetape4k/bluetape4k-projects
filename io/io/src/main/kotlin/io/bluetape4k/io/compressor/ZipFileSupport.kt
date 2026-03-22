@file:JvmName("ZipFileSupport")

package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.FileSystems
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private object ZipFileSupportLogger: KLogging()

private val log = ZipFileSupportLogger.log

/** ZIP 파일 확장자 */
const val ZIP_EXT = ".zip"

/** GZIP 파일 확장자 */
const val GZIP_EXT = ".gz"

/** ZLIB 파일 확장자 */
const val ZLIB_EXT = ".zlib"

/**
 * 파일을 ZLIB(Deflate) 압축하여 `.zlib` 파일로 생성합니다.
 *
 * @param filename 압축할 파일 경로
 * @return 생성된 zlib 파일
 */
fun zlib(filename: String): File = zlib(File(filename))

/**
 * 파일을 ZLIB(Deflate) 압축하여 `.zlib` 파일로 생성합니다.
 *
 * @param file 압축할 파일
 * @return 생성된 zlib 파일
 * @throws IOException 디렉토리를 압축하려고 하면 발생
 */
fun zlib(file: File): File {
    if (file.isDirectory) {
        throw IOException("Can't zlib folder. file=$file")
    }

    val zlibName = file.absolutePath + ZLIB_EXT
    log.debug { "파일을 zlib 압축합니다. 원본=${file.absolutePath}, 압축=$zlibName" }

    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    FileInputStream(file).use { fis ->
        DeflaterOutputStream(FileOutputStream(zlibName), deflater).use { dos ->
            fis.copyTo(dos)
        }
    }
    return File(zlibName)
}

/**
 * 파일을 GZIP 압축하여 `.gz` 파일로 생성합니다.
 *
 * @param filename 압축할 파일 경로
 * @return 생성된 gzip 파일
 */
fun gzip(filename: String): File = gzip(File(filename))

/**
 * 파일을 GZIP 압축하여 `.gz` 파일로 생성합니다.
 *
 * @param file 압축할 파일
 * @return 생성된 gzip 파일
 * @throws IOException 디렉토리를 압축하려고 하면 발생
 */
fun gzip(file: File): File {
    if (file.isDirectory) {
        throw IOException("Can't gzip folder. file=$file")
    }

    val gzipName = file.absolutePath + GZIP_EXT
    log.debug { "파일을 gzip 압축합니다. 원본=${file.absolutePath}, 압축=$gzipName" }

    FileInputStream(file).use { fis ->
        GZIPOutputStream(FileOutputStream(gzipName)).use { gzos ->
            fis.copyTo(gzos)
        }
    }
    return File(gzipName)
}

/**
 * GZIP 압축 파일을 풉니다. 확장자(`.gz`)를 제거한 이름으로 출력 파일을 생성합니다.
 *
 * @param filename 압축 해제할 파일 경로
 * @return 압축 해제된 파일
 */
fun ungzip(filename: String): File = ungzip(File(filename))

/**
 * GZIP 압축 파일을 풉니다. 확장자(`.gz`)를 제거한 이름으로 출력 파일을 생성합니다.
 *
 * @param file 압축 해제할 GZIP 파일
 * @return 압축 해제된 파일
 */
fun ungzip(file: File): File {
    log.debug { "gzip 파일의 압축을 풉니다. file=${file.absolutePath}" }
    val outFilename = file.absolutePath.removeSuffix(GZIP_EXT)
    val out = File(outFilename)
    out.createNewFile()

    FileOutputStream(out).use { fos ->
        GZIPInputStream(FileInputStream(file)).use { gzip ->
            gzip.copyTo(fos)
        }
    }
    return out
}

/**
 * 파일을 ZIP 압축합니다.
 *
 * @param filename 압축할 파일 경로
 * @return 생성된 ZIP 파일. 실패 시 null
 */
fun zip(filename: String): File? = zip(File(filename))

/**
 * 파일을 ZIP 압축합니다.
 *
 * @param file 압축할 파일
 * @return 생성된 ZIP 파일. 실패 시 null
 */
fun zip(file: File): File? {
    val zipFilename = file.absolutePath + ZIP_EXT

    return ZipBuilder.of(zipFilename)
        .add(file)
        .apply { recursive = true }
        .save()
        .toZipFile()
}

/**
 * ZIP 파일을 대상 디렉토리에 압축 해제합니다.
 *
 * Zip Slip 공격을 방어합니다.
 *
 * @param zipFilename ZIP 파일 경로
 * @param destDirName 대상 디렉토리 경로
 * @param patterns 와일드카드 패턴 (비어 있으면 모든 엔트리 추출)
 */
fun unzip(zipFilename: String, destDirName: String, vararg patterns: String) {
    unzip(File(zipFilename), File(destDirName), *patterns)
}

/**
 * ZIP 파일을 대상 디렉토리에 압축 해제합니다.
 *
 * Zip Slip 공격을 방어하여, 엔트리 경로가 대상 디렉토리 외부를 가리키면 [IllegalArgumentException]을 발생시킵니다.
 *
 * @param zipFile ZIP 파일
 * @param destDir 대상 디렉토리
 * @param patterns glob 패턴 (비어 있으면 모든 엔트리 추출)
 * @throws IllegalArgumentException Zip Slip 공격이 감지되면 발생
 */
fun unzip(zipFile: File, destDir: File, vararg patterns: String) {
    val zip = ZipFile(zipFile)
    val destCanonical = destDir.canonicalPath

    try {
        val entries = zip.entries()
        val matchers = if (patterns.isNotEmpty()) {
            val fs = FileSystems.getDefault()
            patterns.map { fs.getPathMatcher("glob:$it") }
        } else {
            emptyList()
        }

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name

            // 패턴 필터링
            if (matchers.isNotEmpty()) {
                val entryPath = java.nio.file.Path.of(entryName)
                val matched = matchers.any { it.matches(entryPath) }
                if (!matched) continue
            }

            val file = File(destDir, entryName)

            // Zip Slip 방어
            require(file.canonicalPath.startsWith(destCanonical)) {
                "Zip entry is outside of the target dir: $entryName"
            }

            if (entry.isDirectory) {
                if (!file.mkdirs() && !file.isDirectory) {
                    throw IOException("Fail to create directory: $file")
                }
            } else {
                val parent = file.parentFile
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs() && !parent.isDirectory) {
                        throw IOException("Failed to create directory: $parent")
                    }
                }
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(file).buffered().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    } finally {
        runCatching { zip.close() }
            .onFailure { log.warn(it) { "ZipFile 닫기 실패" } }
    }
}

/**
 * 파일을 [ZipOutputStream]에 단일 엔트리로 추가합니다.
 *
 * @param zos ZIP 출력 스트림
 * @param file 추가할 파일
 * @param path ZIP 내 경로
 * @param comment 엔트리 코멘트
 * @param recursive 디렉토리인 경우 재귀 추가 여부
 */
@JvmOverloads
fun addToZip(
    zos: ZipOutputStream,
    file: File,
    path: String? = null,
    comment: String? = null,
    recursive: Boolean = true,
) {
    if (!file.exists()) {
        throw FileNotFoundException(file.toString())
    }

    var entryPath = (if (path.isNullOrEmpty()) file.name else path).trimStart('/')
    val isDir = file.isDirectory

    if (isDir && !entryPath.endsWith("/")) {
        entryPath += '/'
    }

    val entry = ZipEntry(entryPath).apply {
        time = file.lastModified()
        if (!comment.isNullOrEmpty()) {
            this.comment = comment
        }
        if (isDir) {
            size = 0
            crc = 0
        }
    }

    zos.putNextEntry(entry)

    if (!isDir) {
        FileInputStream(file).buffered().use { input ->
            input.copyTo(zos)
        }
    }

    zos.closeEntry()

    if (recursive && isDir) {
        val children = file.listFiles() ?: return
        for (child in children) {
            val childPath = (if (entryPath.isEmpty()) "" else entryPath) + child.name
            addToZip(zos, child, childPath, comment, recursive)
        }
    }
}

/**
 * 바이트 배열 콘텐트를 [ZipOutputStream]에 엔트리로 추가합니다.
 *
 * @param zos ZIP 출력 스트림
 * @param content 추가할 콘텐트
 * @param path ZIP 내 경로
 * @param comment 엔트리 코멘트
 */
@JvmOverloads
fun addToZip(
    zos: ZipOutputStream,
    content: ByteArray,
    path: String? = null,
    comment: String? = null,
) {
    val entryPath = (path ?: "").trimStart('/')

    val zipEntry = ZipEntry(entryPath).apply {
        time = System.currentTimeMillis()
        if (!comment.isNullOrEmpty()) {
            this.comment = comment
        }
    }
    zos.putNextEntry(zipEntry)

    ByteArrayInputStream(content).buffered().use { input ->
        input.copyTo(zos)
    }

    zos.closeEntry()
}

/**
 * 빈 폴더 엔트리를 [ZipOutputStream]에 추가합니다.
 *
 * @param zos ZIP 출력 스트림
 * @param path 폴더 경로
 * @param comment 엔트리 코멘트
 */
@JvmOverloads
fun addFolderToZip(
    zos: ZipOutputStream,
    path: String? = null,
    comment: String? = null,
) {
    var entryPath = (path ?: "").trimStart('/')

    if (!entryPath.endsWith('/')) {
        entryPath += '/'
    }

    val entry = ZipEntry(entryPath).apply {
        time = System.currentTimeMillis()
        if (!comment.isNullOrEmpty()) {
            this.comment = comment
        }
        size = 0
        crc = 0
    }

    zos.putNextEntry(entry)
    zos.closeEntry()
}
