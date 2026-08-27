package io.bluetape4k.science.exposed

/**
 * NetCDF 파일 처리 중 발생하는 예외의 기본 타입입니다.
 *
 * sealed class 로 처리 정책을 명시적으로 분기할 수 있습니다.
 *
 * 분류:
 * - [FileOpen] : 파일 시스템·HDF5·NetCDF 포맷 오류로 파일을 열 수 없을 때
 * - [FileRecordNotFound] : DB 에 등록된 파일 레코드가 없을 때
 * - [VariableNotFound] : 지정된 변수가 파일에 없을 때
 * - [UnsupportedVariable] : 변수 rank 가 지원 범위(1~4) 외일 때
 * - [MissingCoordinate] : lat/lon/level 좌표축이 누락되었을 때
 * - [UnsupportedCoordinateAxis] : 축이 CF 역할·차원·자료형 계약을 만족하지 않을 때
 * - [UnsupportedProjection] : 좌표 참조계가 화이트리스트(EPSG:4326/4269/3857/3031/3413/UTM 32601~32660·32701~32760) 외이거나 proj4j 변환 실패 시
 * - [DuplicateCoordinate] : 한 slice 안에서 정규화된 좌표 키가 중복될 때
 * - [ResourceLimitExceeded] : bounded import resource budget 을 초과할 때
 * - [FileChanged] : 등록·재개 중 파일 identity fingerprint 가 바뀔 때
 * - [CorruptProgress] : progress checkpoint/status invariant 가 손상되었을 때
 * - [ImportAlreadyRunning] : 동일 (fileId, variableName) import 가 이미 lease 를 소유 중일 때
 * - [ImportLeaseLost] : lease 만료 후 다른 importer 가 같은 progress row 를 재획득했을 때
 *
 * @see io.bluetape4k.science.exposed.service.NetCdfCatalogService
 */
sealed class NetCdfException(message: String, cause: Throwable? = null): RuntimeException(message, cause) {

    /** NetCDF 파일을 열 수 없을 때 발생합니다. */
    class FileOpen(path: String, cause: Throwable):
        NetCdfException("Failed to open NetCDF file: $path", cause)

    /** DB 에 등록된 파일 레코드를 찾을 수 없을 때 발생합니다. */
    class FileRecordNotFound(fileId: Long):
        NetCdfException("NetCDF file record not found (fileId=$fileId)")

    /** 지정된 변수를 파일에서 찾을 수 없을 때 발생합니다. */
    class VariableNotFound(fileId: Long, variableName: String):
        NetCdfException("Variable '$variableName' not found in file (id=$fileId)")

    /** 변수의 rank(차원 수)가 지원 범위(1·2·3·4) 외일 때 발생합니다. */
    class UnsupportedVariable(variableName: String, rank: Int):
        NetCdfException("Variable '$variableName' has unsupported rank=$rank (must be 1, 2, 3, or 4)")

    /** 필수 좌표축이 누락되었을 때 발생합니다. */
    class MissingCoordinate(axisName: String):
        NetCdfException("Required coordinate axis '$axisName' is missing")

    /**
     * 좌표축이 CF 역할·차원·자료형 계약을 만족하지 않을 때 발생합니다.
     *
     * 이 subtype 추가는 2.0.0 major API migration 대상입니다. 기존 sealed
     * `when` 소비자는 명시적인 branch 또는 `else`를 추가해야 합니다.
     */
    class UnsupportedCoordinateAxis(
        val variableName: String,
        val coordinateName: String?,
        val reason: String,
    ): NetCdfException(
        "Unsupported coordinate axis: variable=$variableName coordinate=$coordinateName reason=$reason",
    )

    /**
     * proj4j 로 EPSG:4326 으로 재투영할 수 없는 CRS 일 때 발생합니다.
     *
     * 화이트리스트 외 CRS / 좌표축 해석 실패 / proj4j 변환 예외 세 가지가 모두 이 예외로 통합됩니다.
     */
    class UnsupportedProjection(srcCrs: String, cause: Throwable? = null):
        NetCdfException("Unsupported projection: '$srcCrs' (cannot reproject to EPSG:4326)", cause)

    /** 동일 slice 안에서 canonical 좌표 키가 중복될 때 발생합니다. */
    class DuplicateCoordinate(
        val fileId: Long,
        val variableName: String,
        val timeIdx: Int,
        val levelIdx: Int,
        val longitude: Double,
        val latitude: Double,
    ): NetCdfException(
        "Duplicate coordinate: fileId=$fileId variable=$variableName time=$timeIdx level=$levelIdx " +
            "lon=$longitude lat=$latitude",
    )

    /** import 가 bounded resource budget 을 초과할 때 발생합니다. */
    class ResourceLimitExceeded(
        val resource: String,
        val limit: Long,
        val actual: Long,
    ): NetCdfException("Resource limit exceeded: $resource limit=$limit actual=$actual")

    /** 파일 fingerprint 가 등록 시점 또는 resume 시점과 달라졌을 때 발생합니다. */
    class FileChanged(
        val fileId: Long,
        val expectedFingerprint: String,
        val actualFingerprint: String,
    ): NetCdfException("NetCDF file changed while import was resumable: fileId=$fileId")

    /** progress checkpoint/status invariant 가 손상되었을 때 발생합니다. */
    class CorruptProgress(
        val progressId: Long,
        val detail: String,
    ): NetCdfException("Corrupt NetCDF import progress: progressId=$progressId detail=$detail")

    /**
     * 동일 (fileId, variableName) import 가 활성 lease 를 보유 중일 때 발생합니다.
     *
     * lease TTL 만료 후 호출 시 정상 재개되며, 이 예외는 발생하지 않습니다.
     */
    class ImportAlreadyRunning(fileId: Long, variableName: String):
        NetCdfException("Import already running: fileId=$fileId var=$variableName")

    /**
     * 같은 progress row 를 다른 importer 가 재획득하여 현재 importer 가 더 이상 lease owner 가 아닐 때 발생합니다.
     */
    class ImportLeaseLost(progressId: Long):
        NetCdfException("Import lease lost: progressId=$progressId")
}
