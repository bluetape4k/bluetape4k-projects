package io.bluetape4k.science.shapefile

import io.bluetape4k.science.coords.BoundingBox
import org.locationtech.jts.geom.Geometry

/**
 * Shapefile 헤더 정보를 담는 데이터 클래스입니다.
 *
 * @param fileCode   파일 코드 (보통 9994)
 * @param fileLength 파일 길이 (16비트 워드 단위)
 * @param version    버전 (보통 1000)
 * @param shapeType  도형 유형 코드
 * @param bbox       전체 데이터의 경계 사각형
 */
data class ShapeHeader(
    val fileCode: Int,
    val fileLength: Int,
    val version: Int,
    val shapeType: Int,
    val bbox: BoundingBox,
)

/**
 * Shapefile의 DBF 속성 필드 정의를 담는 데이터 클래스입니다.
 *
 * @param name    필드 이름
 * @param type    필드 유형 문자 (C=문자, N=숫자, D=날짜, L=논리 등)
 * @param length  필드 길이
 * @param decimal 소수점 자리 수
 */
data class ShapeAttribute(
    val name: String,
    val type: Char,
    val length: Int,
    val decimal: Int,
)

/**
 * Shapefile의 개별 레코드를 담는 데이터 클래스입니다.
 *
 * @param recordNumber 레코드 번호 (0 기반)
 * @param shapeType    도형 유형 코드
 * @param bbox         이 레코드의 경계 사각형 (NULL 도형인 경우 null)
 * @param geometry     JTS [Geometry] 객체
 * @param attributes   DBF 속성 값 맵 (필드명 → 값)
 */
data class ShapeRecord(
    val recordNumber: Int,
    val shapeType: Int,
    val bbox: BoundingBox?,
    val geometry: Geometry,
    val attributes: Map<String, Any?> = emptyMap(),
)

/**
 * Shapefile 전체 데이터(헤더, 레코드 목록, 속성 정의)를 담는 클래스입니다.
 *
 * @param header     Shapefile 헤더
 * @param records    레코드 목록
 * @param attributes DBF 속성 필드 정의 목록
 */
data class Shape(
    val header: ShapeHeader,
    val records: List<ShapeRecord>,
    val attributes: List<ShapeAttribute>,
) {
    /** 레코드 수 */
    val size: Int get() = records.size

    /** 레코드가 비어있는지 여부 */
    val isEmpty: Boolean get() = records.isEmpty()
}
