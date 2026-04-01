package io.bluetape4k.science.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.database.PostgisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

/**
 * [NetCdfFileTable], [NetCdfGridValueTable] 및 [NetCdfFileRepository], [NetCdfCatalogService] 통합 테스트.
 *
 * PostGIS 컨테이너(`postgis/postgis:16-3.4`)를 Testcontainers로 구동하여
 * DDL 생성, 더미 데이터 insert/findById, CatalogService의 미구현 메서드 예외를 검증합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetCdfTableTest {

    companion object: KLogging() {
        @JvmStatic
        val postgisContainer = PostgisServer.Launcher.postgis

        @JvmStatic
        val db: Database by lazy {
            Database.connect(
                url = postgisContainer.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgisContainer.username!!,
                password = postgisContainer.password!!,
            )
        }
    }

    private val fileRepo = NetCdfFileRepository()
    private val catalogService = NetCdfCatalogService(fileRepo)

    @BeforeAll
    fun setUp() {
        transaction(db) {
            SchemaUtils.create(NetCdfFileTable, NetCdfGridValueTable)
        }
    }

    @AfterAll
    fun tearDown() {
        transaction(db) {
            runCatching { SchemaUtils.drop(NetCdfGridValueTable, NetCdfFileTable) }
        }
    }

    @Test
    fun `DDL - NetCdfFileTable과 NetCdfGridValueTable 생성 확인`() {
        transaction(db) {
            val count = NetCdfFileTable.selectAll().count()
            log.debug { "NetCdfFileTable 레코드 수: $count" }
            count shouldBeEqualTo 0L
        }
    }

    @Test
    fun `NetCdfFileRecord 더미 데이터 insert 및 findById`() {
        transaction(db) {
            val record = NetCdfFileRecord(
                filename = "ERA5_2024_01.nc",
                filePath = "/data/netcdf/ERA5_2024_01.nc",
                fileSize = 1_024_000L,
                variables = listOf(
                    NetCdfVariableInfo(
                        name = "temperature",
                        dataType = "float",
                        shape = listOf(24, 181, 360),
                        attributes = mapOf("units" to "K", "long_name" to "Air Temperature"),
                    ),
                    NetCdfVariableInfo(
                        name = "precipitation",
                        dataType = "float",
                        shape = listOf(24, 181, 360),
                        attributes = mapOf("units" to "mm", "long_name" to "Total Precipitation"),
                    ),
                ),
                dimensions = mapOf("time" to 24, "lat" to 181, "lon" to 360),
                globalAttrs = mapOf(
                    "Conventions" to "CF-1.8",
                    "institution" to "ECMWF",
                    "source" to "ERA5 reanalysis",
                ),
            )

            val saved = fileRepo.save(record)

            saved.id shouldBeGreaterThan 0L
            saved.filename shouldBeEqualTo record.filename
            saved.filePath shouldBeEqualTo record.filePath
            saved.fileSize shouldBeEqualTo record.fileSize

            log.debug { "저장된 NetCdfFileRecord: $saved" }

            val found = fileRepo.findByIdOrNull(saved.id)
            found.shouldNotBeNull()
            found.filename shouldBeEqualTo "ERA5_2024_01.nc"
            found.variables.size shouldBeEqualTo 2
            found.dimensions["time"] shouldBeEqualTo 24
            found.globalAttrs["Conventions"] shouldBeEqualTo "CF-1.8"

            log.debug { "조회된 NetCdfFileRecord: $found" }

            // cleanup
            fileRepo.deleteById(saved.id)
        }
    }

    @Test
    fun `NetCdfFileRecord 빈 메타데이터로 저장`() {
        transaction(db) {
            val minimal = NetCdfFileRecord(
                filename = "minimal.nc",
                filePath = "/tmp/minimal.nc",
            )
            val saved = fileRepo.save(minimal)

            saved.id shouldBeGreaterThan 0L
            saved.filename shouldBeEqualTo "minimal.nc"
            saved.fileSize shouldBeEqualTo 0L
            saved.variables.isEmpty() shouldBeEqualTo true
            saved.dimensions.isEmpty() shouldBeEqualTo true
            saved.globalAttrs.isEmpty() shouldBeEqualTo true

            log.debug { "최소 NetCdfFileRecord 저장 성공: id=${saved.id}" }

            // cleanup
            fileRepo.deleteById(saved.id)
        }
    }

    @Test
    fun `여러 NetCdfFileRecord 저장 및 전체 조회`() {
        transaction(db) {
            val records = listOf(
                NetCdfFileRecord(filename = "ERA5_2024_01.nc", filePath = "/data/ERA5_2024_01.nc"),
                NetCdfFileRecord(filename = "ERA5_2024_02.nc", filePath = "/data/ERA5_2024_02.nc"),
                NetCdfFileRecord(filename = "ERA5_2024_03.nc", filePath = "/data/ERA5_2024_03.nc"),
            )
            val savedIds = records.map { fileRepo.save(it).id }

            val all = fileRepo.findAll()
            all.size shouldBeEqualTo records.size

            log.debug { "전체 NetCdfFileRecord 수: ${all.size}" }

            // cleanup
            savedIds.forEach { fileRepo.deleteById(it) }
        }
    }

    @Test
    fun `NetCdfCatalogService - registerFile 호출 시 NotImplementedError 발생`() {
        assertThrows<NotImplementedError> {
            kotlinx.coroutines.runBlocking {
                catalogService.registerFile("/data/test.nc")
            }
        }
    }

    @Test
    fun `NetCdfCatalogService - importGridValues 호출 시 NotImplementedError 발생`() {
        assertThrows<NotImplementedError> {
            kotlinx.coroutines.runBlocking {
                catalogService.importGridValues(1L, "temperature")
            }
        }
    }
}
