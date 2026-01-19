package io.nats.examples

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.objectMeta
import io.bluetape4k.nats.client.api.objectStoreConfiguration
import io.bluetape4k.nats.client.tryDelete
import io.bluetape4k.support.toUtf8Bytes
import io.nats.client.Connection
import io.nats.client.api.ObjectInfo
import io.nats.client.api.StorageType
import io.nats.client.support.Digester
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.random.Random

/**
 * Nats 의 Object Store 를 사용하면 대용량 파일을 S3 + Kafka 를 사용하지 않고도 가능하네요.
 */
@TempFolderTest
class ObjectStoreExample: AbstractNatsTest() {

    companion object: KLogging() {
        private val STORAGE_TYPE = StorageType.Memory
        private const val STORE_BUCKET_NAME = "transfer"
        private const val CHUNK_SIZE = 8 * 1024
    }

    fun createDataFiles(tempFolder: TempFolder): Array<DataFile> {
        return arrayOf(
            DataFile(1024, true, tempFolder),
            DataFile(10 * 1024, true, tempFolder),
            DataFile(1024 * 1024, true, tempFolder),
            DataFile(10 * 1024 * 1024, true, tempFolder),
            DataFile(100 * 1024 * 1024, true, tempFolder),

            DataFile(1024, false, tempFolder),
            DataFile(10 * 1024, false, tempFolder),
            DataFile(1024 * 1024, false, tempFolder),
            DataFile(10 * 1024 * 1024, false, tempFolder),
            DataFile(100 * 1024 * 1024, false, tempFolder),
        )
    }

    @Test
    fun `file transfer by object store`(tempFolder: TempFolder) {
        val dataFiles = createDataFiles(tempFolder)
        getConnection().use { nc ->
            // Setup deletes an existing store, makes a new one.
            // Comment out if the store is set up, mave to see what upload over an object does
            setupStore(nc)

            // upload objects
            upload(nc, dataFiles)

            // print objects in the store
            printObjectStoreInfo(nc)

            // download objects
            download(nc, dataFiles)
        }
    }

    private fun setupStore(nc: Connection) {
        val osm = nc.objectStoreManagement()
        osm.tryDelete(STORE_BUCKET_NAME)

        val osc = objectStoreConfiguration(STORE_BUCKET_NAME) {
            storageType(STORAGE_TYPE)
        }
        osm.create(osc)
    }

    private fun upload(nc: Connection, dataFiles: Array<DataFile>) {
        val store = nc.objectStore(STORE_BUCKET_NAME)
        dataFiles.forEach { df ->
            val input = Files.newInputStream(df.inputFile.toPath())
            val meta = objectMeta(df.name) {
                description(df.description)
                chunkSize(CHUNK_SIZE)
            }
            store.put(meta, input)
            val oi = store.getInfo(df.name)
            printObjectInfo("Upload: ", oi)
            val digest = getDigestEntry(df.inputFile)
            checkDigests("  | Input vs ObjectInfo", digest, oi.digest!!)
        }
    }

    private fun printObjectStoreInfo(nc: Connection) {
        val os = nc.objectStore(STORE_BUCKET_NAME)
        os.list.forEach { oi ->
            printObjectInfo("Info: ", oi)
        }
    }

    private fun printObjectInfo(label: String, oi: ObjectInfo) {
        val meta = oi.objectMeta
        val str = buildString {
            append(label).append(meta.objectName)
            appendLine(" [${meta.description}]")
            append("  | nuid=`${oi.nuid}`")
            append(" size=${oi.size}")
            append(" chunks=${oi.chunks}")
            append(" digest=`${oi.digest}`")
            append(" modified=${oi.modified}")
        }
        println(str)
    }

    private fun download(nc: Connection, dataFiles: Array<DataFile>) {
        val store = nc.objectStore(STORE_BUCKET_NAME)
        dataFiles.forEach { df ->
            println("Downloading ${df.name}")
            if (df.outputFile.exists()) {
                df.outputFile.delete()
            }
            Files.newOutputStream(df.outputFile.toPath()).use { out ->
                store.get(df.name, out)
                out.flush()
            }

            val digest = getDigestEntry(df.inputFile)
            val oi = store.getInfo(df.name)
            checkDigests("  | Download vs ObjectInfo", digest, oi.digest!!)
        }
    }

    private fun checkDigests(label: String, d1: String, d2: String) {
        if (d1 == d2) {
            println("$label digests match.")
        } else {
            println("$label digests do not match: $d1 vs $d2")
        }
    }

    fun getDigestEntry(file: File): String {
        val buff = ByteArray(1024)
        val digest = Digester()
        FileInputStream(file).use { fi ->
            var read = fi.read(buff, 0, 1024)
            while (read != -1) {
                digest.update(buff, 0, read)
                read = fi.read(buff, 0, 1024)
            }
        }
        return digest.digestEntry
    }

    class DataFile(val size: Long, val isTextIsNotBinary: Boolean, tempFolder: TempFolder) {

        val name: String = if (isTextIsNotBinary) "text-$size.txt" else "binary-$size.dat"
        val description: String = "$size bytes " + if (isTextIsNotBinary) "text file" else "binary file"

        val inputFile: File = tempFolder.createFile("input-$name")
        val outputFile: File = tempFolder.createFile("output-$name")

        companion object: KLogging() {
            @JvmStatic
            fun generate(dataFiles: Array<DataFile>) {
                val r = Random
                val textBytes = Fakers.randomString(1024).toUtf8Bytes()

                dataFiles.forEach { df ->
                    if (df.isTextIsNotBinary) {
                        generateTextFile(df, textBytes)
                    } else {
                        generateBinaryFile(df, r)
                    }
                }
            }

            @JvmStatic
            fun generateBinaryFile(df: DataFile, r: Random) {
                FileOutputStream(df.inputFile).use { out ->
                    var left = df.size
                    val buf = ByteArray(1024)
                    while (left >= 1024) {
                        r.nextBytes(buf)
                        out.write(buf)
                        left -= 1024
                    }
                    if (left > 0) {
                        r.nextBytes(buf)
                        out.write(buf, 0, left.toInt())
                    }
                }
            }

            @JvmStatic
            fun generateTextFile(df: DataFile, textBytes: ByteArray) {
                FileOutputStream(df.inputFile).use { out ->
                    var tbPtr = -1024
                    var left = df.size
                    while (left >= 1024) {
                        tbPtr += 1024
                        if (tbPtr + 1023 > textBytes.size) {
                            tbPtr = 0
                        }
                        out.write(textBytes, tbPtr, 1024)
                        left -= 1024
                    }
                    if (left > 0) {
                        out.write(textBytes, 0, left.toInt())
                    }
                }
            }
        }

    }
}
