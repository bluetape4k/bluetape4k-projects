package io.bluetape4k.io.artifact

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.WRITE

/** file/directory force와 hard-link no-replace를 포함하는 공용 artifact writer입니다. */
object AtomicArtifactWriter {

    fun writeNoReplace(
        artifactRoot: Path,
        relativePath: Path,
        bytes: ByteArray,
        temporaryPrefix: String = ".artifact-",
        temporarySuffix: String = ".tmp",
    ): Path {
        require(temporaryPrefix.length >= 3) { "temporaryPrefix must contain at least three characters" }
        val trustedRoot = TrustedArtifactPaths.ensureTrustedDirectory(artifactRoot)
        val target = TrustedArtifactPaths.resolveCreateTarget(trustedRoot, relativePath)
        val temporary = try {
            Files.createTempFile(target.parent, temporaryPrefix, temporarySuffix)
        } catch (error: Exception) {
            throw ArtifactPrimitiveException("artifact temporary file could not be created", error)
        }

        var published = false
        try {
            FileChannel.open(temporary, WRITE).use { channel ->
                writeFully(channel, bytes)
                channel.force(true)
            }
            try {
                Files.createLink(target, temporary)
                published = true
            } catch (error: FileAlreadyExistsException) {
                throw ArtifactPrimitiveException("artifact must not replace an existing file", error)
            }

            val finalized = target.toRealPath(NOFOLLOW_LINKS)
            if (
                !Files.isRegularFile(target, NOFOLLOW_LINKS) ||
                Files.isSymbolicLink(target) ||
                !finalized.startsWith(trustedRoot)
            ) {
                Files.deleteIfExists(target)
                throw ArtifactPrimitiveException("finalized artifact escaped its trusted run root")
            }
            TrustedArtifactPaths.forceDirectory(target.parent)
            return target
        } catch (error: ArtifactPrimitiveException) {
            if (published) {
                Files.deleteIfExists(target)
            }
            throw error
        } catch (error: Exception) {
            if (published) {
                Files.deleteIfExists(target)
            }
            throw ArtifactPrimitiveException("artifact could not be finalized atomically", error)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun writeFully(
        channel: FileChannel,
        bytes: ByteArray,
    ) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
    }
}
