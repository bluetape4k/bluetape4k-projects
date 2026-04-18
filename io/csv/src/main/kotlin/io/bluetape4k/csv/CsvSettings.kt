package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 자체 구현 CSV 파서/라이터 설정 데이터 클래스입니다.
 *
 * univocity-parsers에 의존하지 않고, 내부 [CsvLexer] 및 [DelimitedWriter]에 전달되는 불변 설정입니다.
 *
 * ## 빈 값(null) 처리 정책
 * - [emptyValueAsNull]`=true`: 인용 없는 빈 필드(`,,`) → `null` 로 변환합니다.
 * - [emptyQuotedAsNull]`=false`(기본): 인용 빈 필드(`""`) → `""` 빈 문자열로 보존합니다.
 * - [emptyQuotedAsNull]`=true`: 인용 빈 필드(`""`) → `null` 로 추가 변환합니다.
 * - Writer: `null` → 인용 없는 빈 필드 출력, `""` → `""` 인용 출력.
 *
 * ## 기본 사용 예
 * ```kotlin
 * val settings = CsvSettings()
 * // settings.delimiter == ','
 * // settings.lineSeparator == "\r\n"  (RFC 4180 표준)
 *
 * val tabSettings = CsvSettings.DEFAULT_TSV_READER
 * // tabSettings.delimiter == '\t'
 * ```
 *
 * @param delimiter 필드 구분 문자. 기본값은 쉼표(`,`).
 * @param quote 필드를 감싸는 인용 문자. 기본값은 큰따옴표(`"`).
 * @param quoteEscape 인용 문자 이스케이프 방식. V1은 RFC 4180 doubled-quote만 지원하므로 [quote]와 동일해야 합니다.
 * @param lineSeparator 레코드 구분자. RFC 4180 표준에 따라 기본값은 `"\r\n"`.
 * @param trimValues `true`이면 각 필드의 앞뒤 공백을 제거합니다. **reader 전용**; writer에서는 무시됩니다.
 * @param skipEmptyLines `true`이면 빈 줄을 건너뜁니다.
 * @param emptyValueAsNull `true`이면 인용 없는 빈 필드(`,,`)를 `null`로 변환합니다.
 * @param emptyQuotedAsNull `true`이면 인용 빈 필드(`""`)도 `null`로 변환합니다. 기본값은 `false`.
 * @param detectBom `true`이면 입력 스트림 앞의 BOM(Byte Order Mark)을 자동으로 감지·제거합니다.
 * @param maxCharsPerColumn 컬럼 하나에 허용되는 최대 문자 수. 기본값은 [MAX_CHARS_PER_COLUMN].
 * @param maxColumns 레코드당 최대 컬럼 수. 기본값은 512.
 * @param bufferSize 내부 읽기 버퍼 크기(바이트). 기본값은 8192.
 */
data class CsvSettings(
    /** 필드 구분 문자. 기본값: 쉼표(`,`). */
    val delimiter: Char = ',',
    /** 필드를 감싸는 인용 문자. 기본값: 큰따옴표(`"`). */
    val quote: Char = '"',
    /**
     * 인용 문자 이스케이프 방식.
     * V1은 RFC 4180 doubled-quote 이스케이프만 지원하므로 [quote]와 동일해야 합니다.
     * 임의 이스케이프 문자 지원은 V2 이후 예정입니다.
     */
    val quoteEscape: Char = '"',
    /** 레코드 구분자. RFC 4180 표준: `"\r\n"`. */
    val lineSeparator: String = "\r\n",
    /**
     * 각 필드의 앞뒤 공백 제거 여부.
     * **reader 전용** — writer에서는 무시됩니다.
     */
    val trimValues: Boolean = false,
    /** `true`이면 빈 줄을 건너뜁니다. */
    val skipEmptyLines: Boolean = true,
    /**
     * 인용 없는 빈 필드(`,,`)를 `null`로 변환할지 여부.
     * `true`이면 `null`, `false`이면 빈 문자열(`""`)로 처리합니다.
     */
    val emptyValueAsNull: Boolean = true,
    /**
     * 인용 빈 필드(`""`)를 `null`로 변환할지 여부.
     * `true`이면 추가로 `null`로 변환합니다. 기본값은 `false`(빈 문자열 보존).
     */
    val emptyQuotedAsNull: Boolean = false,
    /** `true`이면 입력 스트림 앞의 BOM을 자동으로 감지·제거합니다. */
    val detectBom: Boolean = true,
    /** 컬럼 하나에 허용되는 최대 문자 수. 기본값: [MAX_CHARS_PER_COLUMN]. */
    val maxCharsPerColumn: Int = MAX_CHARS_PER_COLUMN,
    /** 레코드당 최대 컬럼 수. 기본값: 512. */
    val maxColumns: Int = 512,
    /** 내부 읽기 버퍼 크기(바이트). 기본값: 8192. */
    val bufferSize: Int = 8192,
) : Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /** 기본 CSV 설정 (delimiter=`,`, quote=`"`, lineSeparator=`"\r\n"`). */
        @JvmField
        val DEFAULT = CsvSettings()
    }

    init {
        require(delimiter != quote) {
            "delimiter($delimiter)와 quote($quote)는 달라야 합니다"
        }
        require(quoteEscape == quote) {
            "V1은 RFC 4180 doubled-quote 이스케이프만 지원합니다. " +
                "quoteEscape는 quote와 동일해야 합니다. " +
                "임의 이스케이프 문자는 V2 이후 지원 예정입니다"
        }
        require(maxCharsPerColumn > 0) {
            "maxCharsPerColumn($maxCharsPerColumn)은 양수여야 합니다"
        }
        require(maxColumns > 0) {
            "maxColumns($maxColumns)은 양수여야 합니다"
        }
        require(bufferSize > 0) {
            "bufferSize($bufferSize)은 양수여야 합니다"
        }
        require(lineSeparator.isNotEmpty()) {
            "lineSeparator는 비어 있을 수 없습니다"
        }
    }
}
