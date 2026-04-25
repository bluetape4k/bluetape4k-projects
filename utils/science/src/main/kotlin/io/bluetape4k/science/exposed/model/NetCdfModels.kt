package io.bluetape4k.science.exposed.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * NetCDF 변수 정보를 담는 데이터 클래스입니다.
 *
 * ```kotlin
 * val varInfo = NetCdfVariableInfo(
 *     name = "temperature",
 *     dataType = "float",
 *     shape = listOf(24, 90, 180),
 *     attributes = mapOf("units" to "K", "long_name" to "Air Temperature")
 * )
 * println(varInfo.name)            // "temperature"
 * println(varInfo.shape)           // [24, 90, 180]
 * println(varInfo.attributes["units"]) // "K"
 * ```
 *
 * @param name       변수 이름
 * @param dataType   데이터 타입 (float, double 등)
 * @param shape      차원별 크기 목록
 * @param attributes 변수 메타데이터 속성
 */
data class NetCdfVariableInfo(
    val name: String,
    val dataType: String,
    val shape: List<Int>,
    val attributes: Map<String, String>,
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * NetCDF 차원 정보를 담는 데이터 클래스입니다.
 *
 * ```kotlin
 * val timeDim = NetCdfDimensionInfo(name = "time", length = 24, isUnlimited = true)
 * println(timeDim.name)        // "time"
 * println(timeDim.length)      // 24
 * println(timeDim.isUnlimited) // true
 *
 * val latDim = NetCdfDimensionInfo(name = "lat", length = 90, isUnlimited = false)
 * println(latDim.isUnlimited)  // false
 * ```
 *
 * @param name        차원 이름 (time, lat, lon 등)
 * @param length      차원 길이
 * @param isUnlimited 무제한 차원 여부
 */
data class NetCdfDimensionInfo(
    val name: String,
    val length: Int,
    val isUnlimited: Boolean,
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * NetCDF 파일 메타데이터 레코드를 담는 데이터 클래스입니다.
 *
 * ```kotlin
 * val record = NetCdfFileRecord(
 *     filename = "era5_2023.nc",
 *     filePath = "/data/era5_2023.nc",
 *     fileSize = 104_857_600L,
 *     variables = listOf(NetCdfVariableInfo("temperature", "float", listOf(24, 90, 180), emptyMap())),
 *     dimensions = mapOf("time" to 24, "lat" to 90, "lon" to 180),
 *     globalAttrs = mapOf("source" to "ERA5", "institution" to "ECMWF")
 * )
 * println(record.filename)              // "era5_2023.nc"
 * println(record.dimensions["time"])    // 24
 * println(record.globalAttrs["source"]) // "ERA5"
 * ```
 *
 * @param id          기본키 (자동 생성)
 * @param filename    파일 이름
 * @param filePath    파일 전체 경로
 * @param fileSize    파일 크기 (바이트)
 * @param variables   변수 목록
 * @param dimensions  차원 이름-크기 매핑
 * @param globalAttrs 전역 속성
 */
data class NetCdfFileRecord(
    val id: Long = 0L,
    val filename: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val variables: List<NetCdfVariableInfo> = emptyList(),
    val dimensions: Map<String, Int> = emptyMap(),
    val globalAttrs: Map<String, String> = emptyMap(),
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * NetCDF 변수 단위 import 진행 상태입니다.
 *
 * - [PENDING] : row 가 막 생성된 초기 상태 (`acquireLease` 직전)
 * - [IN_PROGRESS] : lease 보유 중 — 활성 import 또는 stale (lease 만료 후 다른 호출자가 재획득 가능)
 * - [COMPLETED] : 정상 완료 — 재호출 시 즉시 no-op
 * - [FAILED] : 예외 발생으로 중단 — 재호출 시 lastSliceIdx + 1 부터 재시작
 */
enum class NetCdfImportStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }

/**
 * NetCDF 변수 단위 import 진행 상태 레코드입니다.
 *
 * `(fileId, variableName)` unique 로 동일 변수 한 row 를 보장.
 * heartbeat lease 기반 동시성 제어 — `leaseExpiresAt` 이 미래이고 `status=IN_PROGRESS` 면 활성 import.
 *
 * ```kotlin
 * val progress = NetCdfImportProgress(
 *     fileId = 1L,
 *     variableName = "temperature",
 *     status = NetCdfImportStatus.IN_PROGRESS,
 *     lastSliceIdx = 12L,
 *     leaseExpiresAt = Instant.now().plus(Duration.ofMinutes(5)),
 *     startedAt = Instant.now(),
 *     updatedAt = Instant.now(),
 * )
 * ```
 *
 * @param id              기본키 (자동 생성)
 * @param fileId          소속 NetCDF 파일 ID
 * @param variableName    임포트 대상 변수 이름
 * @param status          진행 상태
 * @param lastSliceIdx    마지막 commit 된 슬라이스의 선형 인덱스 (sliceIdx = timeIdx × levelN + levelIdx)
 * @param leaseExpiresAt  heartbeat lease 만료 시각 (COMPLETED/FAILED 면 null)
 * @param errorMessage    실패 메시지 (FAILED 일 때만)
 * @param startedAt       임포트 시작 시각
 * @param completedAt     완료 시각 (COMPLETED 일 때만)
 * @param updatedAt       마지막 갱신 시각 (heartbeat / 상태 전환 시)
 */
data class NetCdfImportProgress(
    val id: Long = 0L,
    val fileId: Long,
    val variableName: String,
    val status: NetCdfImportStatus = NetCdfImportStatus.PENDING,
    val lastSliceIdx: Long? = null,
    val leaseExpiresAt: java.time.Instant? = null,
    val errorMessage: String? = null,
    val startedAt: java.time.Instant = java.time.Instant.now(),
    val completedAt: java.time.Instant? = null,
    val updatedAt: java.time.Instant = java.time.Instant.now(),
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}
