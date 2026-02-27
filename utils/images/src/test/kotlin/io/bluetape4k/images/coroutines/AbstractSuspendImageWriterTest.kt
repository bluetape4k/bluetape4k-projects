package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.forSuspendWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.images.suspendImmutableImageOf
import io.bluetape4k.io.writeAsync
import io.bluetape4k.io.writeSuspending
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.system.measureTimeMillis

@TempFolderTest
abstract class AbstractSuspendImageWriterTest: AbstractImageTest() {

    companion object: KLoggingChannel()

    abstract val writer: SuspendImageWriter
    abstract val imageFormat: Format

    protected open val useTempFolder = true

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `use async image writer`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        measureTimeMillis {
            val image = suspendImmutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))

            val bytes = image.forSuspendWriter(writer).bytes()
            if (useTempFolder) {
                val dest = tempFolder.createFile("${filename}_compressed.$imageFormat")
                dest.toPath().writeSuspending(bytes)
            } else {
                Path.of("$BASE_PATH/${filename}_compressed.$imageFormat").writeAsync(bytes).await()
            }
        }.apply {
            log.info { "Compressed $filename.$imageFormat in $this ms" }
        }
    }

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `async image writer in coroutines`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        val image = suspendImmutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))

        SuspendedJobTester()
            .workers(4)
            .rounds(8)
            .add {
                val file = tempFolder.createFile().toPath()
                image.forSuspendWriter(writer).write(file)
                log.debug { "Save $filename.$imageFormat to $file" }
            }
            .run()
    }

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `async image writer in multi threading`(filename: String, tempFolder: TempFolder) {
        val image = immutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))

        MultithreadingTester()
            .workers(4)
            .rounds(2)
            .add {
                val file = tempFolder.createFile()
                image.forWriter(writer).write(file)
                log.debug { "Save $filename.$imageFormat to $file" }
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `async image writer in virtual threading`(filename: String, tempFolder: TempFolder) {
        val image = immutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))

        StructuredTaskScopeTester()
            .rounds(8)
            .add {
                val file = tempFolder.createFile()
                image.forWriter(writer).write(file)
                log.debug { "Save $filename.$imageFormat to $file" }
            }
            .run()
    }
}
