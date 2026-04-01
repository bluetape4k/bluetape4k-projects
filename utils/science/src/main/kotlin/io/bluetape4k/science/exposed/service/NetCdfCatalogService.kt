package io.bluetape4k.science.exposed.service

import io.bluetape4k.science.exposed.repository.NetCdfFileRepository
import io.bluetape4k.science.exposed.schema.NetCdfFileTable
import io.bluetape4k.science.exposed.schema.NetCdfGridValueTable

/**
 * NetCDF 파일 등록 및 격자 값 임포트를 담당하는 서비스입니다.
 *
 * **⚠️ 미구현**: UCAR netcdfAll Maven 아티팩트 의존성 해결 후 구현 예정 (Phase 4).
 * 현재 모든 메서드가 [NotImplementedError]를 발생시킵니다.
 * 테이블 DDL([NetCdfFileTable], [NetCdfGridValueTable])과 [NetCdfFileRepository]는 사용 가능합니다.
 * 구현 완료 시 모든 DB 작업은 Virtual Thread에서 실행됩니다.
 *
 * @param fileRepo NetCDF 파일 Repository
 */
internal class NetCdfCatalogService(
    private val fileRepo: NetCdfFileRepository,
) {
    /**
     * NetCDF 파일을 읽어 메타데이터를 DB에 등록합니다.
     *
     * Virtual Thread에서 JDBC 트랜잭션을 실행합니다.
     *
     * @param filePath 파일 경로
     * @return 등록된 파일 레코드 ID
     */
    fun registerFile(filePath: String): Long {
        TODO("Phase 4 (UCAR netcdfAll) 완료 후 구현 예정")
    }

    /**
     * NetCDF 파일의 격자 값을 DB에 임포트합니다.
     *
     * Virtual Thread에서 배치 JDBC 트랜잭션을 실행합니다.
     *
     * @param fileId       파일 레코드 ID
     * @param variableName 변수 이름
     */
    fun importGridValues(fileId: Long, variableName: String) {
        TODO("Phase 4 (UCAR netcdfAll) 완료 후 구현 예정")
    }
}
