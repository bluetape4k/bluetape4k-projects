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
 * - [MissingCoordinate] : lat/lon/level 좌표축이 누락되었거나 [ucar.nc2.dataset.CoordinateAxis1D] 가 아닐 때
 * - [UnsupportedProjection] : 좌표 참조계가 화이트리스트(EPSG:4326/3857/3031/3413/UTM 32601~32660·32701~32760) 외이거나 proj4j 변환 실패 시
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

    /**
     * lat/lon/level 좌표축이 누락되었거나 [ucar.nc2.dataset.CoordinateAxis1D] 가 아닐 때 발생합니다.
     *
     * `CoordinateAxis2D` (curvilinear / rotated pole / tripolar grid) 는 본 구현 스코프 외입니다.
     */
    class MissingCoordinate(axisName: String):
        NetCdfException("Required coordinate axis '$axisName' is missing or not a 1D numeric axis")

    /**
     * proj4j 로 EPSG:4326 으로 재투영할 수 없는 CRS 일 때 발생합니다.
     *
     * 화이트리스트 외 CRS / 좌표축 해석 실패 / proj4j 변환 예외 세 가지가 모두 이 예외로 통합됩니다.
     */
    class UnsupportedProjection(srcCrs: String, cause: Throwable? = null):
        NetCdfException("Unsupported projection: '$srcCrs' (cannot reproject to EPSG:4326)", cause)

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
