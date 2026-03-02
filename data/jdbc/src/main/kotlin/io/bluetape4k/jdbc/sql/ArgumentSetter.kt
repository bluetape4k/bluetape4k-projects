package io.bluetape4k.jdbc.sql

import java.io.InputStream
import java.io.Reader
import java.sql.Blob
import java.sql.Clob
import java.sql.NClob

/**
 * 단일 값을 인덱스 기반으로 바인딩하는 setter 계약입니다.
 *
 * ## 동작/계약
 * - [setter]는 `(columnIndex, value)`를 받아 실제 JDBC API 호출을 수행합니다.
 * - `set` 연산자는 [setter]를 그대로 위임 호출하며 추가 상태를 저장하지 않습니다.
 * - 인덱스/타입 오류 예외는 [setter] 구현에서 발생한 그대로 전파됩니다.
 *
 * ```kotlin
 * val intSetter = DefaultArgumentSetter<Int> { idx, value -> ps.setInt(idx, value) }
 * intSetter[1] = 10
 * // ps.getInt(1) == 10
 * ```
 */
interface ArgumentSetter<in T> {

    val setter: (Int, T) -> Unit

    operator fun set(columnIndex: Int, value: T) {
        setter(columnIndex, value)
    }
}

/**
 * 보조 인자 1개를 추가로 받는 setter 계약입니다.
 *
 * ## 동작/계약
 * - [setter2]는 `(columnIndex, value, arg)` 순서로 호출됩니다.
 * - `set(columnIndex, arg, value)`는 내부에서 value/arg 순서를 맞춰 위임합니다.
 * - 예외 처리 규칙은 [setter2] 구현(JDBC 드라이버)에 따릅니다.
 *
 * ```kotlin
 * val dateSetter = object: ArgumentSetter2<java.sql.Date, java.util.Calendar> {
 *   override val setter2 = { i: Int, d: java.sql.Date, c: java.util.Calendar -> ps.setDate(i, d, c) }
 * }
 * dateSetter.set(1, java.util.Calendar.getInstance(), java.sql.Date(0))
 * // ps 파라미터 1번에 Date가 바인딩됨
 * ```
 */
interface ArgumentSetter2<in T, in A> {

    val setter2: (Int, T, A) -> Unit

    operator fun set(columnIndex: Int, arg: A, value: T) {
        setter2(columnIndex, value, arg)
    }
}

/**
 * 단일 인자 setter 구현을 래핑하는 기본 구현체입니다.
 *
 * ## 동작/계약
 * - 전달된 [setter]를 저장해 `ArgumentSetter` 계약을 구현합니다.
 * - 상태를 추가로 갖지 않는 thin wrapper 입니다.
 *
 * ```kotlin
 * val stringSetter = DefaultArgumentSetter<String> { i, v -> ps.setString(i, v) }
 * stringSetter[2] = "ok"
 * // 2번 파라미터가 "ok"로 설정됨
 * ```
 */
open class DefaultArgumentSetter<in T>(override val setter: (Int, T) -> Unit): ArgumentSetter<T>

/**
 * 길이 인자를 함께 받는 setter 계약을 제공합니다.
 *
 * ## 동작/계약
 * - `Int` 길이는 `ArgumentSetter2` 경로를, `Long` 길이는 [setterWithLong] 경로를 사용합니다.
 * - 내부 상태를 변경하지 않고 전달된 람다로 즉시 위임합니다.
 * - 길이 음수 등 유효성 검증은 JDBC 드라이버 호출에서 처리됩니다.
 *
 * ```kotlin
 * val binary = ArgumentWithLengthSetter<InputStream>(
 *   { i, s -> ps.setBinaryStream(i, s) },
 *   { i, s, len -> ps.setBinaryStream(i, s, len) },
 *   { i, s, len -> ps.setBinaryStream(i, s, len) }
 * )
 * // binary.set(1, 128L, stream) 로 길이 지정 가능
 * ```
 */
open class ArgumentWithLengthSetter<in T>(
    override val setter: (Int, T) -> Unit,
    override val setter2: (Int, T, Int) -> Unit,
    val setterWithLong: (Int, T, Long) -> Unit,
): ArgumentSetter<T>, ArgumentSetter2<T, Int> {

    operator fun set(columnIndex: Int, length: Long, value: T) {
        setterWithLong(columnIndex, value, length)
    }
}

/**
 * Blob 계열 스트림 setter 공통 계약을 추상화합니다.
 *
 * ## 동작/계약
 * - 단일 값 바인딩과 길이 지정 바인딩(`Long`) 두 경로를 제공합니다.
 * - 실제 JDBC 호출은 하위 클래스가 전달한 람다로 수행됩니다.
 *
 * ```kotlin
 * // BlobArgumentSetter/ClobArgumentSetter/NClobArgumentSetter의 공통 베이스로 사용됨
 * // setter(index, stream) / setter2(index, stream, length)
 * ```
 */
abstract class AbstractBlobArgumentSetter<in R>(
    override val setter: (Int, R) -> Unit,
    override val setter2: (Int, R, Long) -> Unit,
): ArgumentSetter<R>, ArgumentSetter2<R, Long>

/**
 * Blob 또는 InputStream 기반 Blob 바인딩을 지원합니다.
 *
 * ## 동작/계약
 * - `blob[index] = blobValue`는 [blobSetter]를 호출합니다.
 * - `blob[index] = stream` 및 길이 지정 호출은 상위 스트림 setter를 사용합니다.
 * - JDBC 드라이버가 지원하지 않는 타입/길이 조합은 예외가 전파됩니다.
 *
 * ```kotlin
 * val blob = BlobArgumentSetter(
 *   { i, b -> ps.setBlob(i, b) },
 *   { i, s -> ps.setBlob(i, s) },
 *   { i, s, l -> ps.setBlob(i, s, l) }
 * )
 * // blob[1] = inputStream
 * ```
 */
class BlobArgumentSetter(
    val blobSetter: (Int, Blob) -> Unit,
    setter: (Int, InputStream) -> Unit,
    setter2: (Int, InputStream, Long) -> Unit,
): AbstractBlobArgumentSetter<InputStream>(setter, setter2) {

    operator fun set(columnIndex: Int, blob: Blob) {
        blobSetter(columnIndex, blob)
    }
}

/**
 * Clob 또는 Reader 기반 Clob 바인딩을 지원합니다.
 *
 * ## 동작/계약
 * - `clob[index] = clobValue`는 [clobSetter]를 호출합니다.
 * - Reader 기반 입력은 상위 스트림 setter 경로를 사용합니다.
 * - Reader 길이/드라이버 제약은 JDBC 예외로 전파됩니다.
 *
 * ```kotlin
 * val clob = ClobArgumentSetter(
 *   { i, c -> ps.setClob(i, c) },
 *   { i, r -> ps.setClob(i, r) },
 *   { i, r, l -> ps.setClob(i, r, l) }
 * )
 * // clob[1] = reader
 * ```
 */
class ClobArgumentSetter(
    val clobSetter: (Int, Clob) -> Unit,
    setter: (Int, Reader) -> Unit,
    setter2: (Int, Reader, Long) -> Unit,
): AbstractBlobArgumentSetter<Reader>(setter, setter2) {

    operator fun set(columnIndex: Int, clob: Clob) {
        clobSetter(columnIndex, clob)
    }
}

/**
 * NClob 또는 Reader 기반 NClob 바인딩을 지원합니다.
 *
 * ## 동작/계약
 * - `nclob[index] = nclobValue`는 [nClobSetter]를 호출합니다.
 * - Reader 입력과 길이 지정은 상위 공통 setter 경로를 사용합니다.
 * - 드라이버 미지원 기능은 JDBC 예외로 전파됩니다.
 *
 * ```kotlin
 * val nclob = NClobArgumentSetter(
 *   { i, c -> ps.setNClob(i, c) },
 *   { i, r -> ps.setNClob(i, r) },
 *   { i, r, l -> ps.setNClob(i, r, l) }
 * )
 * // nclob[1] = reader
 * ```
 */
class NClobArgumentSetter(
    val nClobSetter: (Int, NClob) -> Unit,
    setter: (Int, Reader) -> Unit,
    setter2: (Int, Reader, Long) -> Unit,
): AbstractBlobArgumentSetter<Reader>(setter, setter2) {

    operator fun set(columnIndex: Int, nclob: NClob) {
        nClobSetter(columnIndex, nclob)
    }
}

/**
 * 단일 값/보조 인자 1개를 모두 지원하는 결합 setter 입니다.
 *
 * ## 동작/계약
 * - [ArgumentSetter]와 [ArgumentSetter2]를 동시에 구현합니다.
 * - 추가 검증 없이 전달된 람다를 그대로 위임 호출합니다.
 *
 * ```kotlin
 * val nullableSetter = CombinedArgumentSetter<Int, String>(
 *   { i, sqlType -> ps.setNull(i, sqlType) },
 *   { i, sqlType, typeName -> ps.setNull(i, sqlType, typeName) }
 * )
 * // nullableSetter.set(1, "VARCHAR", java.sql.Types.VARCHAR)
 * ```
 */
open class CombinedArgumentSetter<in T, in A>(
    override val setter: (Int, T) -> Unit,
    override val setter2: (Int, T, A) -> Unit,
): ArgumentSetter<T>, ArgumentSetter2<T, A>

/**
 * `setObject` 계열 오버로드(2/3/4인자)를 묶어 제공합니다.
 *
 * ## 동작/계약
 * - 기본 `set(index, value)`와 `set(index, targetSqlType, value)`는 상위 구현을 사용합니다.
 * - `set(index, targetSqlType, scaleOrLength, value)`는 [setter3]으로 위임합니다.
 * - SQL 타입/스케일 제약은 JDBC 드라이버 규칙에 따릅니다.
 *
 * ```kotlin
 * val obj = ObjectArgumentSetter(
 *   { i, v -> ps.setObject(i, v) },
 *   { i, v, t -> ps.setObject(i, v, t) },
 *   { i, v, t, s -> ps.setObject(i, v, t, s) }
 * )
 * // obj.set(1, java.sql.Types.DECIMAL, 2, 12.34)
 * ```
 */
class ObjectArgumentSetter(
    setter: (Int, Any) -> Unit,
    setter2: (Int, Any, Int) -> Unit,
    val setter3: (Int, Any, Int, Int) -> Unit,
): CombinedArgumentSetter<Any, Int>(setter, setter2) {

    operator fun set(columnIndex: Int, targetSqlType: Int, scaleOrLength: Int, value: Any) {
        setter3(columnIndex, value, targetSqlType, scaleOrLength)
    }
}
