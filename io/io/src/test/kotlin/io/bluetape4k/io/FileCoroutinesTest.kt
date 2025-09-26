package io.bluetape4k.io

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.RepeatedTest

@RandomizedTest
@TempFolderTest
class FileCoroutinesTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
        private val faker = Fakers.faker
        private fun randomString(length: Int = 256): String = Fakers.fixedString(length)
        private fun randomStrings(size: Int = 20): List<String> = List(size) { randomString() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Coroutine 하에서 ByteArray를 파일에 쓰고 읽기`(
        tempDir: TempFolder,
        @RandomValue bytes: ByteArray,
    ) = runSuspendIO {
        val filename = Fakers.randomUuid().encodeBase62() + ".dat"
        val path = tempDir.createFile(filename).toPath()
        log.debug { "Write and Read contents. path=$path" }

        val written = path.suspendWrite(bytes)
        written shouldBeEqualTo bytes.size.toLong()
        yield()

        val loaded = path.suspendReadAllBytes()
        loaded shouldBeEqualTo bytes
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Coroutine 하에서 문자열 컬렉션을 파일에 쓰고 읽기`(tempDir: TempFolder) = runSuspendIO {
        val contents = randomStrings()
        val filename = Fakers.randomUuid().encodeBase62() + ".txt"
        val path = tempDir.createFile(filename).toPath()
        log.debug { "Write and Read contents. path=$path" }

        // 비동기로 쓰기
        val written = path.suspendWriteLines(contents)
        written shouldBeGreaterThan 0
        yield()

        // 비동기로 파일 읽기
        val loaded = path.suspendReadAllLines().toList()
        loaded.size shouldBeEqualTo contents.size
        loaded shouldBeEqualTo contents
    }
}
