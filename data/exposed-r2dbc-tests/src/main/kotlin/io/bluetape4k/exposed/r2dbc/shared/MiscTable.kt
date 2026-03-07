package io.bluetape4k.exposed.r2dbc.shared

import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import java.math.BigDecimal

/**
 * 다양한 컬럼 타입을 포함하는 범용 테스트 테이블입니다.
 *
 * byte, short, integer, enum, string, decimal, float, double, char 타입의 컬럼을
 * nullable/non-nullable 쌍으로 정의하여 타입별 동작을 검증할 때 사용합니다.
 *
 * Postgres DDL:
 * ```sql
 * CREATE TABLE IF NOT EXISTS misctable (
 *      "by" SMALLINT NOT NULL,
 *      byn SMALLINT NULL,
 *      sm SMALLINT NOT NULL,
 *      smn SMALLINT NULL,
 *      n INT NOT NULL,
 *      nn INT NULL,
 *      e INT NOT NULL,
 *      en INT NULL,
 *      es VARCHAR(5) NOT NULL,
 *      esn VARCHAR(5) NULL,
 *      "c" VARCHAR(4) NOT NULL,
 *      cn VARCHAR(4) NULL,
 *      s VARCHAR(100) NOT NULL,
 *      sn VARCHAR(100) NULL,
 *      dc DECIMAL(12, 2) NOT NULL,
 *      dcn DECIMAL(12, 2) NULL,
 *      fcn REAL NULL,
 *      dblcn DOUBLE PRECISION NULL,
 *      "char" CHAR NULL,
 *
 *      CONSTRAINT chk_Misc_signed_byte_by CHECK ("by" BETWEEN -128 AND 127),
 *      CONSTRAINT chk_Misc_signed_byte_byn CHECK (byn BETWEEN -128 AND 127)
 * );
 * ```
 */
open class MiscTable: Table() {

    val by = byte("by")
    val byn = byte("byn").nullable()

    val sm = short("sm")
    val smn = short("smn").nullable()

    val n = integer("n")
    val nn = integer("nn").nullable()

    val e = enumeration("e", E::class)
    val en = enumeration("en", E::class).nullable()

    val es = enumerationByName("es", 5, E::class)
    val esn = enumerationByName("esn", 5, E::class).nullable()

    val c = varchar("c", 4)
    val cn = varchar("cn", 4).nullable()

    val s = varchar("s", 100)
    val sn = varchar("sn", 100).nullable()

    val dc = decimal("dc", 12, 2)
    val dcn = decimal("dcn", 12, 2).nullable()

    val fcn = float("fcn").nullable()
    val dblcn = double("dblcn").nullable()

    val char = char("char").nullable()

    enum class E {
        ONE,
        TWO,
        THREE
    }
}


/**
 * [MiscTable]에서 조회한 [ResultRow]의 모든 컬럼 값을 기대값과 비교하여 검증합니다.
 *
 * @param row 검증할 [ResultRow]
 * @param by byte 컬럼 기대값
 * @param byn nullable byte 컬럼 기대값
 * @param sm short 컬럼 기대값
 * @param smn nullable short 컬럼 기대값
 * @param n integer 컬럼 기대값
 * @param nn nullable integer 컬럼 기대값
 * @param e enum 컬럼 기대값
 * @param en nullable enum 컬럼 기대값
 * @param es enum-by-name 컬럼 기대값
 * @param esn nullable enum-by-name 컬럼 기대값
 * @param c varchar(4) 컬럼 기대값
 * @param cn nullable varchar(4) 컬럼 기대값
 * @param s varchar(100) 컬럼 기대값
 * @param sn nullable varchar(100) 컬럼 기대값
 * @param dc decimal 컬럼 기대값
 * @param dcn nullable decimal 컬럼 기대값
 * @param fcn nullable float 컬럼 기대값
 * @param dblcn nullable double 컬럼 기대값
 */
@Suppress("LongParameterList")
fun MiscTable.checkRow(
    row: ResultRow,
    by: Byte,
    byn: Byte?,
    sm: Short,
    smn: Short?,
    n: Int,
    nn: Int?,
    e: MiscTable.E,
    en: MiscTable.E?,
    es: MiscTable.E,
    esn: MiscTable.E?,
    c: String,
    cn: String?,
    s: String,
    sn: String?,
    dc: BigDecimal,
    dcn: BigDecimal?,
    fcn: Float?,
    dblcn: Double?,
) {
    row[this.by] shouldBeEqualTo by
    row[this.byn] shouldBeEqualTo byn
    row[this.sm] shouldBeEqualTo sm
    row[this.smn] shouldBeEqualTo smn
    row[this.n] shouldBeEqualTo n
    row[this.nn] shouldBeEqualTo nn
    row[this.e] shouldBeEqualTo e
    row[this.en] shouldBeEqualTo en
    row[this.es] shouldBeEqualTo es
    row[this.esn] shouldBeEqualTo esn
    row[this.c] shouldBeEqualTo c
    row[this.cn] shouldBeEqualTo cn
    row[this.s] shouldBeEqualTo s
    row[this.sn] shouldBeEqualTo sn
    row[this.dc] shouldBeEqualTo dc
    row[this.dcn] shouldBeEqualTo dcn
    row[this.fcn] shouldBeEqualTo fcn
    row[this.dblcn] shouldBeEqualTo dblcn
}

/**
 * [MiscTable]의 [InsertStatement] 결과의 모든 컬럼 값을 기대값과 비교하여 검증합니다.
 *
 * [checkRow]와 달리 varchar(4) 컬럼(`c`, `cn`)은 포함하지 않으며,
 * Insert 직후 반환된 [InsertStatement] 결과 검증에 사용합니다.
 *
 * @param row 검증할 [InsertStatement] 결과
 * @param by byte 컬럼 기대값
 * @param byn nullable byte 컬럼 기대값
 * @param sm short 컬럼 기대값
 * @param smn nullable short 컬럼 기대값
 * @param n integer 컬럼 기대값
 * @param nn nullable integer 컬럼 기대값
 * @param e enum 컬럼 기대값
 * @param en nullable enum 컬럼 기대값
 * @param es enum-by-name 컬럼 기대값
 * @param esn nullable enum-by-name 컬럼 기대값
 * @param s varchar(100) 컬럼 기대값
 * @param sn nullable varchar(100) 컬럼 기대값
 * @param dc decimal 컬럼 기대값
 * @param dcn nullable decimal 컬럼 기대값
 * @param fcn nullable float 컬럼 기대값
 * @param dblcn nullable double 컬럼 기대값
 */
@Suppress("LongParameterList")
fun MiscTable.checkInsert(
    row: InsertStatement<Number>,
    by: Byte,
    byn: Byte?,
    sm: Short,
    smn: Short?,
    n: Int,
    nn: Int?,
    e: MiscTable.E,
    en: MiscTable.E?,
    es: MiscTable.E,
    esn: MiscTable.E?,
    s: String,
    sn: String?,
    dc: BigDecimal,
    dcn: BigDecimal?,
    fcn: Float?,
    dblcn: Double?,
) {
    row[this.by] shouldBeEqualTo by
    row[this.byn] shouldBeEqualTo byn
    row[this.sm] shouldBeEqualTo sm
    row[this.smn] shouldBeEqualTo smn
    row[this.n] shouldBeEqualTo n
    row[this.nn] shouldBeEqualTo nn
    row[this.e] shouldBeEqualTo e
    row[this.en] shouldBeEqualTo en
    row[this.es] shouldBeEqualTo es
    row[this.esn] shouldBeEqualTo esn
    row[this.s] shouldBeEqualTo s
    row[this.sn] shouldBeEqualTo sn
    row[this.dc] shouldBeEqualTo dc
    row[this.dcn] shouldBeEqualTo dcn
    row[this.fcn] shouldBeEqualTo fcn
    row[this.dblcn] shouldBeEqualTo dblcn
}
