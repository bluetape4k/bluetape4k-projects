package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.science.exposed.NetCdfException
import java.net.URI
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/** 신뢰된 로컬 NetCDF 파일의 path/identity 경계를 한 곳에서 검증합니다. */
internal object NetCdfFileGuard {

    /** 등록 레코드의 globalAttrs에 저장하는 identity fingerprint key입니다. */
    const val FINGERPRINT_ATTRIBUTE: String = "__bluetape4k_source_fingerprint"

    /** 등록·resume 시점의 immutable 파일 identity입니다. */
    data class Identity(
        val path: Path,
        val fingerprint: String,
        val fileSize: Long,
    )

    /** 등록 전에 local regular file과 크기를 확인하고 fingerprint를 반환합니다. */
    fun validateForRegister(filePath: String): Identity {
        val path = parseLocalPath(filePath)
        val identity = capture(path, fileId = 0L, expectedFingerprint = null)
        return identity
    }

    /** 기존 등록 fingerprint와 현재 파일 identity가 같은지 확인합니다. */
    fun verifyForResume(fileId: Long, filePath: String, expectedFingerprint: String): Identity {
        val path = parseLocalPath(filePath)
        return capture(path, fileId, expectedFingerprint)
    }

    /** open 전후 stat을 비교하는 generic helper입니다. */
    fun <T : AutoCloseable> openVerified(
        fileId: Long,
        filePath: String,
        expectedFingerprint: String?,
        opener: () -> T,
    ): T {
        val before = if (expectedFingerprint == null) {
            validateForRegister(filePath)
        } else {
            verifyForResume(fileId, filePath, expectedFingerprint)
        }
        val opened = try {
            opener()
        } catch (e: NetCdfException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: Exception) {
            throw NetCdfException.FileOpen(filePath, e)
        }
        val after = try {
            capture(before.path, fileId, before.fingerprint)
        } catch (e: Exception) {
            try {
                opened.close()
            } catch (_: Exception) {
                // 원래 fingerprint 오류를 보존합니다.
            }
            throw e
        }
        if (before.fingerprint != after.fingerprint) {
            try {
                opened.close()
            } catch (_: Exception) {
                // 원래 FileChanged를 보존합니다.
            }
            throw NetCdfException.FileChanged(fileId, before.fingerprint, after.fingerprint)
        }
        return opened
    }

    private fun parseLocalPath(filePath: String): Path {
        if (filePath.isBlank()) {
            throw IllegalArgumentException("filePath must not be blank")
        }
        if (filePath.any { it.code < 0x20 || it == '\u007f' || it == '\u0000' }) {
            throw NetCdfException.FileOpen(filePath, IllegalArgumentException("path contains control characters"))
        }
        try {
            val uri = URI(filePath)
            if (uri.scheme != null) {
                throw NetCdfException.FileOpen(filePath, IllegalArgumentException("remote or URI paths are not allowed"))
            }
        } catch (e: NetCdfException) {
            throw e
        } catch (e: Exception) {
            throw NetCdfException.FileOpen(filePath, e)
        }
        val path = try {
            Paths.get(filePath).toAbsolutePath().normalize()
        } catch (e: InvalidPathException) {
            throw NetCdfException.FileOpen(filePath, e)
        }
        return path
    }

    private fun capture(path: Path, fileId: Long, expectedFingerprint: String?): Identity {
        val absolute = try {
            path.toAbsolutePath().normalize()
        } catch (e: Exception) {
            throw NetCdfException.FileOpen(path.toString(), e)
        }
        val attributes = try {
            Files.readAttributes(
                absolute,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: Exception) {
            throw NetCdfException.FileOpen(absolute.toString(), e)
        }
        if (Files.isSymbolicLink(absolute)) {
            if (fileId == 0L) {
                throw NetCdfException.FileOpen(
                    absolute.toString(),
                    IllegalArgumentException("NetCDF path must not be a symbolic link"),
                )
            }
            throw NetCdfException.FileChanged(fileId, "regular-file", "symlink:$absolute")
        }
        rejectSymlinkComponents(absolute, fileId)
        // macOS exposes /var and /tmp as system symlinks. They are the only allowed
        // aliases; caller-provided parent/final symlinks are rejected above/below.
        val realPath = try {
            absolute.toRealPath()
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: Exception) {
            throw NetCdfException.FileOpen(absolute.toString(), e)
        }
        if (!attributes.isRegularFile) {
            throw NetCdfException.FileOpen(
                absolute.toString(),
                IllegalArgumentException("NetCDF path must be a regular file"),
            )
        }
        val size = attributes.size()
        if (size > MAX_FILE_BYTES) {
            throw NetCdfException.ResourceLimitExceeded("file-bytes", MAX_FILE_BYTES, size)
        }
        val fileKey = attributes.fileKey()?.toString()
            ?: throw NetCdfException.FileOpen(
                absolute.toString(),
                IllegalStateException("file identity key is unavailable"),
            )
        val modifiedNanos = attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS)
        val fingerprint = "$fileKey|$size|$modifiedNanos"
        if (expectedFingerprint != null && expectedFingerprint != fingerprint) {
            throw NetCdfException.FileChanged(fileId, expectedFingerprint, fingerprint)
        }
        return Identity(realPath, fingerprint, size)
    }

    private fun rejectSymlinkComponents(path: Path, fileId: Long) {
        var current = path.root
        for (component in path) {
            current = if (current == null) component else current.resolve(component)
            if (Files.isSymbolicLink(current) && !isAllowedSystemAlias(current)) {
                if (fileId == 0L) {
                    throw NetCdfException.FileOpen(
                        path.toString(),
                        IllegalArgumentException("NetCDF parent path must not be a symbolic link: $current"),
                    )
                } else {
                    throw NetCdfException.FileChanged(
                        fileId,
                        expectedFingerprint = "regular-file",
                        actualFingerprint = "symlink:$current",
                    )
                }
            }
        }
    }

    private fun isAllowedSystemAlias(path: Path): Boolean =
        path == Paths.get("/tmp") || path == Paths.get("/var")
}
