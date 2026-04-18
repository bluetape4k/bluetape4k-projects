package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 자체 구현 TSV 파서/라이터 설정 데이터 클래스입니다.
 *
 * univocity-parsers에 의존하지 않고, 내부 [TsvLexer] 및 [DelimitedWriter]에 전달되는 불변 설정입니다.
 *
 * TSV 형식은 탭(`\t`) 문자를 필드 구분자로 사용하며, 인용(quote) 문자 없이
 * 백슬래시 이스케이프 방식을 사용합니다. delimiter는 `\t`로 고정됩니다.
 *
 * ## 빈 값(null) 처리 정책
 * - [emptyValueAsNull]`=true`: 빈 필드(`\t\t`) → `null` 로 변환합니다.
 * - [emptyValueAsNull]`=false`: 빈 필드 → `""` 빈 문자열로 보존합니다.
 *
 * ## 기본 사용 예
 * ```kotlin
 * val settings = TsvSettings()
 * // settings.lineSeparator == "\n"  (Unix 관례)
 * // settings.maxCharsPerColumn == MAX_CHARS_PER_COLUMN
 *
 * val custom = TsvSettings(trimValues = true, skipEmptyLines = false)
 * ```
 *
 * @param lineSeparator 레코드 구분자. Unix 관례에 따라 기본값은 `"\n"`.
 *                      기존 univocity TsvWriter 기본값과 일치합니다.
 * @param trimValues `true`이면 각 필드의 앞뒤 공백을 제거합니다. **reader 전용**; writer에서는 무시됩니다.
 * @param skipEmptyLines `true`이면 빈 줄을 건너뜁니다.
 * @param emptyValueAsNull `true`이면 빈 필드를 `null`로 변환합니다.
 * @param maxCharsPerColumn 컬럼 하나에 허용되는 최대 문자 수. 기본값은 [MAX_CHARS_PER_COLUMN].
 * @param maxColumns 레코드당 최대 컬럼 수. 기본값은 512.
 * @param bufferSize 내부 읽기 버퍼 크기(바이트). 기본값은 8192.
 */
data class TsvSettings(
    /** 레코드 구분자. Unix 관례: `"\n"`. 기존 univocity TsvWriter 기본값과 일치합니다. */
    val lineSeparator: String = "\n",
    /**
     * 각 필드의 앞뒤 공백 제거 여부.
     * **reader 전용** — writer에서는 무시됩니다.
     */
    val trimValues: Boolean = false,
    /** `true`이면 빈 줄을 건너뜁니다. */
    val skipEmptyLines: Boolean = true,
    /**
     * 빈 필드(`\t\t`)를 `null`로 변환할지 여부.
     * `true`이면 `null`, `false`이면 빈 문자열(`""`)로 처리합니다.
     */
    val emptyValueAsNull: Boolean = true,
    /** 컬럼 하나에 허용되는 최대 문자 수. 기본값: [MAX_CHARS_PER_COLUMN]. */
    val maxCharsPerColumn: Int = MAX_CHARS_PER_COLUMN,
    /** 레코드당 최대 컬럼 수. 기본값: 512. */
    val maxColumns: Int = 512,
    /** 내부 읽기 버퍼 크기(바이트). 기본값: 8192. */
    val bufferSize: Int = 8192,
) : Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L

        /** 기본 TSV 설정 (lineSeparator=`"\n"`, emptyValueAsNull=`true`). */
        @JvmField
        val DEFAULT = TsvSettings()
    }

    init {
        require(maxCharsPerColumn > 0) {
            "maxCharsPerColumn은 양수여야 합니다"
        }
        require(maxColumns > 0) {
            "maxColumns은 양수여야 합니다"
        }
        require(bufferSize > 0) {
            "bufferSize은 양수여야 합니다"
        }
        require(lineSeparator.isNotEmpty()) {
            "lineSeparator는 비어 있을 수 없습니다"
        }
    }
}
