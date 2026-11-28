package io.bluetape4k.io.artifact

import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

/** artifact 경로와 directory ownership을 검증하는 domain-independent helper입니다. */
object TrustedArtifactPaths {

    private val identifierPattern = Regex("[a-z0-9][a-z0-9._-]{0,63}")

    /** 파일명·run id로 사용할 bounded identifier를 검증합니다. */
    fun requireIdentifier(
        value: String,
        name: String,
    ): String {
        require(value.isNotBlank()) { "$name must not be blank" }
        require(identifierPattern.matches(value)) {
            "$name must match ${identifierPattern.pattern}"
        }
        require(value != "." && value != "..") { "$name must not be a dot segment" }
        return value
    }

    /** report에 기록할 normalized relative evidence path를 검증합니다. */
    fun requireRelativeEvidence(value: String): String {
        require(value.isNotBlank()) { "evidenceReference must not be blank" }
        val relative = Path.of(value)
        require(
            !relative.isAbsolute &&
                relative.normalize() == relative &&
                relative.none { it.toString() == "." || it.toString() == ".." },
        ) {
            "evidenceReference must be a normalized relative path"
        }
        return value
    }

    /** 경로의 모든 생성·기존 component를 확인하고 symlink 없는 real directory를 반환합니다. */
    fun ensureTrustedDirectory(path: Path): Path {
        val absolute = path.toAbsolutePath().normalize()
        var trusted = absolute.root
            ?: throw ArtifactPrimitiveException("artifact directory must be absolute")

        absolute.root.relativize(absolute).forEachIndexed { index, name ->
            trusted = trusted.resolve(name)
            if (Files.exists(trusted, NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(trusted) && index == 0) {
                    // macOS의 /var 같은 system root alias만 canonicalize한다.
                    trusted = trusted.toRealPath()
                } else if (Files.isSymbolicLink(trusted) || !Files.isDirectory(trusted, NOFOLLOW_LINKS)) {
                    throw ArtifactPrimitiveException("artifact path contains an unsafe component")
                }
            } else {
                try {
                    Files.createDirectory(trusted)
                } catch (error: FileAlreadyExistsException) {
                    if (Files.isSymbolicLink(trusted) || !Files.isDirectory(trusted, NOFOLLOW_LINKS)) {
                        throw ArtifactPrimitiveException(
                            "artifact directory raced with an unsafe path",
                            error,
                        )
                    }
                } catch (error: Exception) {
                    throw ArtifactPrimitiveException("artifact directory could not be created", error)
                }
            }
        }
        if (!Files.isDirectory(trusted, NOFOLLOW_LINKS) || Files.isSymbolicLink(trusted)) {
            throw ArtifactPrimitiveException("artifact root must be a directory")
        }
        return trusted.toRealPath(NOFOLLOW_LINKS)
    }

    /** trusted root 아래에 새 run directory를 no-replace로 생성합니다. */
    fun createNewTrustedDirectory(
        root: Path,
        name: String,
    ): Path {
        val trustedRoot = ensureTrustedDirectory(root)
        val target = trustedRoot.resolve(requireIdentifier(name, "directory name"))
        try {
            Files.createDirectory(target)
        } catch (error: FileAlreadyExistsException) {
            throw ArtifactPrimitiveException("artifact run directory must be created exactly once", error)
        } catch (error: Exception) {
            throw ArtifactPrimitiveException("artifact run directory could not be created", error)
        }
        if (Files.isSymbolicLink(target)) {
            throw ArtifactPrimitiveException("artifact run directory must not be a symlink")
        }
        val real = target.toRealPath(NOFOLLOW_LINKS)
        if (real.parent != trustedRoot) {
            throw ArtifactPrimitiveException("artifact run directory escaped its trusted root")
        }
        forceDirectory(trustedRoot)
        return real
    }

    /** parent-owned run directory가 이미 존재하고 trusted root에 바로 속하는지 확인합니다. */
    fun openExistingTrustedDirectory(
        root: Path,
        name: String,
    ): Path {
        val trustedRoot = ensureTrustedDirectory(root)
        val target = trustedRoot.resolve(requireIdentifier(name, "directory name"))
        if (!Files.isDirectory(target, NOFOLLOW_LINKS) || Files.isSymbolicLink(target)) {
            throw ArtifactPrimitiveException("parent-owned run directory is not trusted")
        }
        val real = target.toRealPath(NOFOLLOW_LINKS)
        if (real.parent != trustedRoot) {
            throw ArtifactPrimitiveException("parent-owned run directory escaped its trusted root")
        }
        return real
    }

    /** 새 파일을 만들 상대 경로를 검증하고 필요한 descendant directory를 생성합니다. */
    fun resolveCreateTarget(
        artifactRoot: Path,
        relativePath: Path,
    ): Path {
        require(!relativePath.isAbsolute) { "artifact relative path must not be absolute" }
        require(relativePath.nameCount > 0) { "artifact relative path must not be empty" }
        require(relativePath.none { it.toString() == ".." || it.toString() == "." }) {
            "artifact relative path must not contain dot segments"
        }

        val root = ensureTrustedDirectory(artifactRoot)
        val target = root.resolve(relativePath).normalize()
        require(target.startsWith(root)) { "artifact path must remain beneath its root" }
        ensureDescendantDirectory(root, root.relativize(target.parent))
        if (Files.exists(target, NOFOLLOW_LINKS)) {
            throw ArtifactPrimitiveException("artifact target already exists")
        }
        return target
    }

    /** directory fsync를 지원하는 provider에서만 수행합니다. */
    fun forceDirectory(directory: Path) {
        try {
            FileChannel.open(directory, READ).use { it.force(true) }
        } catch (_: UnsupportedOperationException) {
            // Directory fsync is not supported by every file provider.
        }
    }

    private fun ensureDescendantDirectory(
        root: Path,
        relativeDirectory: Path,
    ) {
        var current = root
        relativeDirectory.forEach { component ->
            current = current.resolve(component)
            if (Files.isSymbolicLink(current)) {
                throw ArtifactPrimitiveException("artifact path must not traverse a symlink")
            }
            if (Files.exists(current, NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(current, NOFOLLOW_LINKS)) {
                    throw ArtifactPrimitiveException("artifact path component must be a directory")
                }
            } else {
                try {
                    Files.createDirectory(current)
                } catch (error: FileAlreadyExistsException) {
                    if (Files.isSymbolicLink(current) || !Files.isDirectory(current, NOFOLLOW_LINKS)) {
                        throw ArtifactPrimitiveException(
                            "artifact directory raced with an unsafe path",
                            error,
                        )
                    }
                }
            }
        }
    }
}
