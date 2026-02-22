package io.bluetape4k.exposed.shared

import io.bluetape4k.exposed.shared.MiscTable.E
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import java.math.BigDecimal

/**
 * Postgres:
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


@Suppress("LongParameterList")
fun MiscTable.checkRow(
    row: ResultRow,
    by: Byte,
    byn: Byte?,
    sm: Short,
    smn: Short?,
    n: Int,
    nn: Int?,
    e: E,
    en: E?,
    es: E,
    esn: E?,
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

@Suppress("LongParameterList")
fun MiscTable.checkInsert(
    row: InsertStatement<Number>,
    by: Byte,
    byn: Byte?,
    sm: Short,
    smn: Short?,
    n: Int,
    nn: Int?,
    e: E,
    en: E?,
    es: E,
    esn: E?,
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
