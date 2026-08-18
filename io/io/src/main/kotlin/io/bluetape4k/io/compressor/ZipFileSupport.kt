@file:JvmName("ZipFileSupport")

package io.bluetape4k.io.compressor

import io.bluetape4k.io.DEFAULT_BUFFER_SIZE
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.SecureDirectoryStream
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private object ZipFileSupportLogger: KLogging()

private val log = ZipFileSupportLogger.log

/** ZIP 압축 해제 시 허용하는 최대 엔트리 수 (zip bomb 방지) */
const val ZIP_MAX_ENTRIES = 10_000

/** ZIP 압축 해제 시 허용하는 최대 비압축 크기 (1 GB, zip bomb 방지) */
const val ZIP_MAX_UNCOMPRESSED_SIZE = 1L * 1024 * 1024 * 1024

/** ZIP 파일 확장자 */
const val ZIP_EXT = ".zip"

/** GZIP 파일 확장자 */
const val GZIP_EXT = ".gz"

/** ZLIB 파일 확장자 */
const val ZLIB_EXT = ".zlib"

/**
 * 파일을 ZLIB(Deflate) 압축하여 `.zlib` 파일로 생성합니다.
 *
 * 예시:
 * ```kotlin
 * val source = File("/tmp/data.txt")
 * source.writeText("압축할 내용")
 *
 * val zlibFile = zlib("/tmp/data.txt")
 * // zlibFile.name == "data.txt.zlib"
 * zlibFile.exists() // true
 * ```
 *
 * @param filename 압축할 파일 경로
 * @return 생성된 zlib 파일
 */
fun zlib(filename: String): File = zlib(File(filename))

/**
 * 파일을 ZLIB(Deflate) 압축하여 `.zlib` 파일로 생성합니다.
 *
 * 예시:
 * ```kotlin
 * val source = File(tempDir, "data.txt")
 * source.writeText("zlib 압축 테스트 데이터")
 *
 * val zlibFile = zlib(source)
 * // zlibFile.name == "data.txt.zlib"
 * zlibFile.exists() // true
 * zlibFile.length() > 0 // true
 * ```
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
    try {
        FileInputStream(file).use { fis ->
            DeflaterOutputStream(FileOutputStream(zlibName), deflater).use { dos ->
                fis.copyTo(dos)
            }
        }
    } finally {
        deflater.end()
    }
    return File(zlibName)
}

/**
 * 파일을 GZIP 압축하여 `.gz` 파일로 생성합니다.
 *
 * 예시:
 * ```kotlin
 * val source = File("/tmp/report.txt")
 * source.writeText("gzip 압축 대상 텍스트")
 *
 * val gzipFile = gzip("/tmp/report.txt")
 * // gzipFile.name == "report.txt.gz"
 * gzipFile.exists() // true
 * ```
 *
 * @param filename 압축할 파일 경로
 * @return 생성된 gzip 파일
 */
fun gzip(filename: String): File = gzip(File(filename))

/**
 * 파일을 GZIP 압축하여 `.gz` 파일로 생성합니다.
 *
 * 예시:
 * ```kotlin
 * val source = File(tempDir, "test.txt")
 * source.writeText("gzip 라운드트립 테스트 데이터")
 *
 * val gzipFile = gzip(source)
 * // gzipFile.name == "test.txt.gz"
 * gzipFile.exists() // true
 * ```
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
 * 예시:
 * ```kotlin
 * val gzipFile = File("/tmp/test.txt.gz")
 *
 * val restored = ungzip("/tmp/test.txt.gz")
 * // restored.name == "test.txt"
 * restored.exists() // true
 * ```
 *
 * @param filename 압축 해제할 파일 경로
 * @return 압축 해제된 파일
 */
fun ungzip(filename: String): File = ungzip(File(filename))

/**
 * GZIP 압축 파일을 풉니다. 확장자(`.gz`)를 제거한 이름으로 출력 파일을 생성합니다.
 *
 * 예시:
 * ```kotlin
 * val source = File(tempDir, "test.txt")
 * source.writeText("gzip 라운드트립 테스트 데이터")
 * val gzipFile = gzip(source)
 *
 * val restored = ungzip(gzipFile)
 * // restored.name == "test.txt"
 * restored.readText() // "gzip 라운드트립 테스트 데이터"
 * ```
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
 * 예시:
 * ```kotlin
 * val source = File("/tmp/hello.txt")
 * source.writeText("zip 압축 대상 텍스트")
 *
 * val zipFile = zip("/tmp/hello.txt")
 * // zipFile?.name == "hello.txt.zip"
 * zipFile?.exists() // true
 * ```
 *
 * @param filename 압축할 파일 경로
 * @return 생성된 ZIP 파일. 실패 시 null
 */
fun zip(filename: String): File? = zip(File(filename))

/**
 * 파일을 ZIP 압축합니다.
 *
 * 예시:
 * ```kotlin
 * val source = File(tempDir, "hello.txt")
 * source.writeText("zip 라운드트립 테스트")
 *
 * val zipFile = zip(source)
 * // zipFile?.name == "hello.txt.zip"
 * zipFile?.exists() // true
 * ```
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
 * 예시:
 * ```kotlin
 * unzip("/tmp/archive.zip", "/tmp/output")
 * // /tmp/output/ 하위에 ZIP 내 모든 파일이 추출됨
 *
 * // 특정 확장자만 추출하려면 glob 패턴 사용
 * unzip("/tmp/archive.zip", "/tmp/output", "*.txt")
 * ```
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
 * Zip Slip 공격을 방어하여, 정규화한 엔트리 경로가 대상 디렉토리 외부를 가리키면
 * [IllegalArgumentException]을 발생시킵니다. 출력 파일은 [SecureDirectoryStream]의
 * 디렉토리 상대 handle과 [LinkOption.NOFOLLOW_LINKS]로 열고, 추출 중 디렉토리 식별자가
 * 바뀌면 [IOException]으로 중단합니다. secure directory handle을 지원하지 않는 파일
 * 시스템에서도 외부 경로에 쓰지 않고 [IOException]으로 실패합니다.
 *
 * 예시:
 * ```kotlin
 * val zipFile = File(tempDir, "archive.zip")
 * val destDir = File(tempDir, "extracted")
 * destDir.mkdirs()
 *
 * // 모든 파일 추출
 * unzip(zipFile, destDir)
 *
 * // .txt 파일만 추출
 * unzip(zipFile, destDir, "*.txt")
 * ```
 *
 * @param zipFile ZIP 파일
 * @param destDir 대상 디렉토리
 * @param patterns glob 패턴 (비어 있으면 모든 엔트리 추출)
 * @throws IllegalArgumentException Zip Slip 공격이 감지되면 발생
 * @throws IOException 출력 경로의 심볼릭 링크·디렉토리 경합이 감지되거나 파일 시스템이
 * secure directory handle을 지원하지 않으면 발생
 */
fun unzip(zipFile: File, destDir: File, vararg patterns: String) {
    val zip = ZipFile(zipFile)
    try {
        val entries = zip.entries()
        val matchers = if (patterns.isNotEmpty()) {
            val fs = FileSystems.getDefault()
            patterns.map { fs.getPathMatcher("glob:$it") }
        } else {
            emptyList()
        }

        var entryCount = 0
        var declaredUncompressedSize = 0L
        var extractedUncompressedSize = 0L
        val preparedDestination = prepareZipDestination(destDir)
        val canonicalDestDir = preparedDestination.path
        openSecureDirectory(canonicalDestDir).use { destinationDirectory ->
            verifyDirectoryIdentity(preparedDestination, destinationDirectory)

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                // zip bomb 방어: 엔트리 수 제한
                entryCount++
                require(entryCount <= ZIP_MAX_ENTRIES) {
                    "ZIP 엔트리 수가 허용 한도를 초과했습니다: $entryCount > $ZIP_MAX_ENTRIES"
                }

                // zip bomb 방어: 비압축 크기 제한 (엔트리 헤더의 크기 정보 기준)
                if (entry.size > 0) {
                    declaredUncompressedSize += entry.size
                    require(declaredUncompressedSize <= ZIP_MAX_UNCOMPRESSED_SIZE) {
                        "ZIP 비압축 총 크기가 허용 한도를 초과했습니다: " +
                            "$declaredUncompressedSize > $ZIP_MAX_UNCOMPRESSED_SIZE bytes"
                    }
                }

                extractedUncompressedSize += extractZipEntry(
                    zip = zip,
                    entry = entry,
                    matchers = matchers,
                    canonicalDestDir = canonicalDestDir,
                    destinationDirectory = destinationDirectory,
                    remainingLimit = ZIP_MAX_UNCOMPRESSED_SIZE - extractedUncompressedSize,
                )
            }
        }
    } finally {
        runCatching { zip.close() }
            .onFailure { log.warn(it) { "ZipFile 닫기 실패" } }
    }
}

private data class PreparedZipDestination(val path: Path, val fileKey: Any)

private fun prepareZipDestination(destDir: File): PreparedZipDestination {
    val lexicalDestination = destDir.toPath().toAbsolutePath().normalize()
    verifyNoSymlinkAncestors(lexicalDestination)
    Files.createDirectories(lexicalDestination)
    verifyNoSymlinkAncestors(lexicalDestination)
    if (!Files.isDirectory(lexicalDestination, LinkOption.NOFOLLOW_LINKS)) {
        throw IOException("ZIP 대상 경로는 디렉토리여야 합니다: $destDir")
    }
    val destination = lexicalDestination.toRealPath()
    val fileKey = readFileKey(destination)
    return PreparedZipDestination(destination, fileKey)
}

private fun resolveZipTarget(canonicalDestDir: Path, entryName: String): Path {
    val entryPath = Path.of(entryName)
    require(!entryPath.isAbsolute) {
        "Zip entry is outside of the target dir: $entryName"
    }

    val normalizedEntry = entryPath.normalize()
    require(normalizedEntry.nameCount > 0 && normalizedEntry.toString() != ".") {
        "Zip entry must not be empty: $entryName"
    }

    val targetPath = canonicalDestDir.resolve(normalizedEntry).normalize()
    if (!targetPath.startsWith(canonicalDestDir)) {
        throw IllegalArgumentException("Zip entry is outside of the target dir: $entryName")
    }
    return targetPath
}

private fun extractZipEntry(
    zip: ZipFile,
    entry: ZipEntry,
    matchers: List<PathMatcher>,
    canonicalDestDir: Path,
    destinationDirectory: SecureDirectoryStream<Path>,
    remainingLimit: Long,
): Long {
    val entryName = entry.name
    val matched = matchers.isEmpty() || matchers.any { it.matches(Path.of(entryName)) }
    return when {
        !matched -> 0L
        entry.isDirectory -> {
            createZipDirectories(canonicalDestDir, resolveZipTarget(canonicalDestDir, entryName))
            0L
        }

        else -> {
            val target = resolveZipTarget(canonicalDestDir, entryName)
            val parent = target.parent ?: canonicalDestDir
            createZipDirectories(canonicalDestDir, parent)
            verifyNoSymlinkPath(canonicalDestDir, target)
            val relativeTarget = canonicalDestDir.relativize(target)
            val fileName = relativeTarget.fileName
                ?: throw IOException("ZIP 대상 파일 이름이 비어 있습니다: $entryName")
            val relativeParent = relativeTarget.parent
            val expectedParentKeys = captureDirectoryKeys(canonicalDestDir, relativeParent)

            withSecureParentDirectory(
                destinationDirectory,
                relativeParent,
                expectedParentKeys,
            ) { parentDirectory ->
                zip.getInputStream(entry).use { input ->
                    openZipOutput(parentDirectory, fileName).buffered().use { output ->
                        input.copyToLimited(output = output, remainingLimit = remainingLimit)
                    }
                }
            }
        }
    }
}

private fun createZipDirectories(canonicalDestDir: Path, target: Path) {
    val relative = canonicalDestDir.relativize(target)
    var current = canonicalDestDir
    for (part in relative) {
        val child = current.resolve(part)
        verifyNoSymlinkPath(canonicalDestDir, child)
        if (!Files.exists(child, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectory(child)
            } catch (_: java.nio.file.FileAlreadyExistsException) {
                // 동시 생성자가 먼저 만든 경우이며, 아래 no-follow 검사가
                // 안전성을 판단합니다.
            }
        }
        verifyNoSymlinkPath(canonicalDestDir, child)
        if (Files.isSymbolicLink(child) || !Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
            throw IOException("ZIP 대상 경로에 심볼릭 링크 또는 파일이 있습니다: $child")
        }
        current = child
    }
    verifyNoSymlinkPath(canonicalDestDir, target)
}

private fun verifyNoSymlinkPath(canonicalDestDir: Path, target: Path) {
    require(target.startsWith(canonicalDestDir)) {
        "ZIP 대상 경로가 대상 디렉토리 밖에 있습니다: $target"
    }

    verifyNoSymlinkAncestors(target)
}

private fun verifyNoSymlinkAncestors(path: Path) {
    val root = path.root ?: return
    var current = path
    while (current != root) {
        if (Files.isSymbolicLink(current)) {
            if (current == path || current.parent != root) {
                throw IOException("ZIP 대상 경로에 심볼릭 링크가 있습니다: $current")
            }
            // macOS의 /var, /tmp 같은 루트 직계 alias는 real path로 고정한 뒤 허용합니다.
            return
        }
        current = current.parent ?: break
    }
}

private fun readFileKey(path: Path): Any {
    val attributes = Files.readAttributes(
        path,
        BasicFileAttributes::class.java,
        LinkOption.NOFOLLOW_LINKS,
    )
    return attributes.fileKey()
        ?: throw IOException("ZIP 대상 경로의 파일 식별자를 확인할 수 없습니다: $path")
}

private fun readDirectoryFileKey(directory: SecureDirectoryStream<Path>): Any {
    val view = directory.getFileAttributeView(BasicFileAttributeView::class.java)
        ?: throw IOException("ZIP 대상 디렉토리의 파일 속성 보기를 열 수 없습니다")
    return view.readAttributes().fileKey()
        ?: throw IOException("ZIP 대상 디렉토리의 파일 식별자를 확인할 수 없습니다")
}

private fun verifyDirectoryIdentity(
    preparedDestination: PreparedZipDestination,
    destinationDirectory: SecureDirectoryStream<Path>,
) {
    if (preparedDestination.fileKey != readDirectoryFileKey(destinationDirectory)) {
        throw IOException("ZIP 대상 디렉토리가 열기 전에 교체되었습니다: ${preparedDestination.path}")
    }
}

private fun openSecureDirectory(path: Path): SecureDirectoryStream<Path> {
    val root = path.root ?: throw IOException("ZIP 대상 경로에 루트가 없습니다: $path")
    val rootStream = Files.newDirectoryStream(root)
    val rootDirectory = rootStream as? SecureDirectoryStream<Path>
        ?: run {
            rootStream.close()
            throw IOException(
                "ZIP 추출에 secure directory handle을 지원하는 파일 시스템이 필요합니다",
            )
        }

    var current = rootDirectory
    try {
        for (part in root.relativize(path)) {
            val next = current.newDirectoryStream(part, LinkOption.NOFOLLOW_LINKS)
            current.close()
            current = next
        }
        return current
    } catch (error: IOException) {
        current.close()
        throw error
    }
}

private fun captureDirectoryKeys(canonicalDestDir: Path, relativeParent: Path?): List<Any> {
    val keys = mutableListOf(readFileKey(canonicalDestDir))
    if (relativeParent == null) return keys

    var current = canonicalDestDir
    for (part in relativeParent) {
        current = current.resolve(part)
        keys += readFileKey(current)
    }
    return keys
}

private inline fun <T> withSecureParentDirectory(
    destinationDirectory: SecureDirectoryStream<Path>,
    relativeParent: Path?,
    expectedKeys: List<Any>,
    block: (SecureDirectoryStream<Path>) -> T,
): T {
    if (relativeParent == null) {
        return block(destinationDirectory)
    }

    val parentDirectory = openSecureRelativeDirectory(destinationDirectory, relativeParent, expectedKeys)
    return try {
        block(parentDirectory)
    } finally {
        parentDirectory.close()
    }
}

private fun openSecureRelativeDirectory(
    destinationDirectory: SecureDirectoryStream<Path>,
    relativeParent: Path,
    expectedKeys: List<Any>,
): SecureDirectoryStream<Path> {
    var current = destinationDirectory
    var owned = false
    var expectedIndex = 1
    try {
        for (part in relativeParent) {
            val next = current.newDirectoryStream(part, LinkOption.NOFOLLOW_LINKS)
            try {
                val actualKey = readDirectoryFileKey(next)
                val expectedKey = expectedKeys.getOrNull(expectedIndex)
                    ?: throw IOException("ZIP 대상 디렉토리 식별자 수가 일치하지 않습니다")
                if (actualKey != expectedKey) {
                    throw IOException("ZIP 대상 중간 디렉토리가 열기 전에 교체되었습니다: $part")
                }
            } catch (error: IOException) {
                next.close()
                throw error
            }
            if (owned) current.close()
            current = next
            owned = true
            expectedIndex++
        }
        if (expectedIndex != expectedKeys.size) {
            throw IOException("ZIP 대상 디렉토리 식별자 검증이 완료되지 않았습니다")
        }
        return current
    } catch (error: IOException) {
        if (owned) current.close()
        throw error
    }
}

private fun openZipOutput(parentDirectory: SecureDirectoryStream<Path>, fileName: Path): OutputStream {
    val channel = parentDirectory.newByteChannel(
        fileName,
        setOf(
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS,
        ),
    )
    return Channels.newOutputStream(channel)
}

private fun InputStream.copyToLimited(output: OutputStream, remainingLimit: Long): Long {
    require(remainingLimit >= 0) {
        "ZIP 비압축 총 크기가 허용 한도를 초과했습니다: $ZIP_MAX_UNCOMPRESSED_SIZE bytes"
    }

    var copied = 0L
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read < 0) {
            return copied
        }
        if (read == 0) {
            continue
        }

        copied += read
        require(copied <= remainingLimit) {
            "ZIP 실제 비압축 총 크기가 허용 한도를 초과했습니다: " +
                "${ZIP_MAX_UNCOMPRESSED_SIZE - remainingLimit + copied} > $ZIP_MAX_UNCOMPRESSED_SIZE bytes"
        }
        output.write(buffer, 0, read)
    }
}

/**
 * 파일을 [ZipOutputStream]에 단일 엔트리로 추가합니다.
 *
 * 예시:
 * ```kotlin
 * val zipFile = File(tempDir, "output.zip")
 * val sourceFile = File(tempDir, "readme.txt")
 * sourceFile.writeText("파일 내용")
 *
 * ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
 *     addToZip(zos, sourceFile)
 *     // ZIP 내 엔트리명: "readme.txt"
 *
 *     addToZip(zos, sourceFile, path = "docs/readme.txt", comment = "문서")
 *     // ZIP 내 엔트리명: "docs/readme.txt"
 * }
 * ```
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
 * 예시:
 * ```kotlin
 * val zipFile = File(tempDir, "output.zip")
 *
 * ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
 *     val content = "바이트 배열 내용".toByteArray()
 *     addToZip(zos, content, path = "data/payload.bin", comment = "페이로드")
 *     // ZIP 내 엔트리명: "data/payload.bin"
 * }
 * ```
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
 * 예시:
 * ```kotlin
 * val zipFile = File(tempDir, "output.zip")
 *
 * ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
 *     addFolderToZip(zos, path = "logs/archive", comment = "아카이브 폴더")
 *     // ZIP 내 엔트리명: "logs/archive/" (빈 폴더)
 * }
 * ```
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
