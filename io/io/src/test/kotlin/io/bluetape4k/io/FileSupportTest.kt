package io.bluetape4k.io

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest

@RandomizedTest
@TempFolderTest
class FileSupportTest: AbstractIOTest() {

    companion object: KLogging()

    private lateinit var tempFolder: TempFolder

    @BeforeAll
    fun setup(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `비동기 방식으로 ByteArray를 파일에 읽고 쓰기`(@RandomValue bytes: ByteArray) {
        val filename = Fakers.randomUuid().encodeBase62() + ".dat"
        val path = tempFolder.createFile(filename).toPath()
        log.debug { "Write and Read ByteArray asynchronously. path=$path" }

        path.writeAsync(bytes)
            .thenApply { written ->
                written shouldBeEqualTo bytes.size.toLong()
            }
            .thenCompose {
                path.readAllBytesAsync()
            }
            .thenAccept { loaded ->
                loaded shouldBeEqualTo bytes
            }
            .join()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Coroutine 환경에서 비동기 방식으로 ByteArray를 파일에 읽고 쓰기`(@RandomValue bytes: ByteArray) = runSuspendIO {
        val filename = Fakers.randomUuid().encodeBase62() + ".dat"
        val path = tempFolder.createFile(filename).toPath()
        log.debug { "Write and Read ByteArray in coroutines. path=$path" }

        val written = path.writeAsync(bytes).await()
        written shouldBeEqualTo bytes.size.toLong()

        val loaded = path.readAllBytesAsync().await()
        loaded shouldBeEqualTo bytes
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `비동기 방식으로 문자열 컬렉션을 파일에 읽고 쓰기`(tempDir: TempFolder) {
        val contents = randomStrings()
        val filename = Fakers.randomUuid().encodeBase62() + ".txt"
        val path = tempDir.createFile(filename).toPath()
        log.debug { "Write and Read contents asynchronously. path=$path" }

        path.writeLinesAsync(contents)
            .thenCompose { written ->
                written shouldBeGreaterThan 0L
                path.readAllLinesAsync()
            }.thenAccept { loaded ->
                loaded shouldBeEqualTo contents
            }
            .join()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Coroutine 환경에서 비동기 방식으로 문자열 컬렉션을 파일에 읽고 쓰기`() = runSuspendIO {
        val contents = randomStrings()
        val filename = Fakers.randomUuid().encodeBase62() + ".txt"
        val path = tempFolder.createFile(filename).toPath()
        log.debug { "Write and Read contents in coroutines. path=$path" }

        val writtenSize = path.writeLinesAsync(contents).await()
        writtenSize shouldBeGreaterThan 0L

        val loaded = path.readAllLinesAsync().await()
        loaded shouldBeEqualTo contents
    }
}
