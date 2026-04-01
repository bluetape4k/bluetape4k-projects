package io.bluetape4k.science.exposed.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * NetCDF 변수 정보를 담는 데이터 클래스입니다.
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
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * NetCDF 차원 정보를 담는 데이터 클래스입니다.
 *
 * @param name        차원 이름 (time, lat, lon 등)
 * @param length      차원 길이
 * @param isUnlimited 무제한 차원 여부
 */
data class NetCdfDimensionInfo(
    val name: String,
    val length: Int,
    val isUnlimited: Boolean,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * NetCDF 파일 메타데이터 레코드를 담는 데이터 클래스입니다.
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
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
