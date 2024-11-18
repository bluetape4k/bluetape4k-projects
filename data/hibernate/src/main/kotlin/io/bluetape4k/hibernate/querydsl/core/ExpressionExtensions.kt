@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.Ops
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.SimpleExpression
import com.querydsl.core.types.dsl.StringExpression


/**
 * 현 [Expression]의 부정의 [BooleanExpression]을 반환합니다.
 *
 * @return [BooleanExpression] 인스턴스
 */
operator fun Expression<Boolean>.not(): BooleanExpression =
    Expressions.booleanOperation(Ops.NOT, this)

/**
 * 현 Expression과 주어진 [predicate]의 `AND` 조건의 [BooleanExpression]을 반환합니다.
 *
 * @receiver [Expression<Boolean>] 인스턴스. AND의 왼쪽 항
 * @param predicate AND의 오른쪽 항
 * @return 두 Expression의 AND 조건을 나타내는 [BooleanExpression] 인스턴스
 */
infix fun Expression<Boolean>.and(predicate: Expression<Boolean>): BooleanExpression =
    Expressions.booleanOperation(Ops.AND, this, predicate)

/**
 * 현 Expression과 주어진 [predicate]의 `OR` 조건의 [BooleanExpression]을 반환합니다.
 *
 * @receiver [Expression<Boolean>] 인스턴스. OR의 왼쪽 항
 * @param predicate OR의 오른쪽 항
 * @return 두 Expression의 OR 조건을 나타내는 [BooleanExpression] 인스턴스
 */
infix fun Expression<Boolean>.or(predicate: Expression<Boolean>): BooleanExpression =
    Expressions.booleanOperation(Ops.OR, this, predicate)

/**
 * 현 Expression과 주어진 [predicate]의 `XOR` 조건의 [BooleanExpression]을 반환합니다.
 *
 * @receiver [Expression<Boolean>] 인스턴스. XOR의 왼쪽 항
 * @param predicate XOR의 오른쪽 항
 * @return 두 Expression의 XOR 조건을 나타내는 [BooleanExpression] 인스턴스
 */
infix fun Expression<Boolean>.xor(predicate: Expression<Boolean>): BooleanExpression =
    Expressions.booleanOperation(Ops.XOR, this, predicate)

/**
 * 현 Expression과 주어진 [predicate]의 `XNOR` 조건의 [BooleanExpression]을 반환합니다.
 *
 * @receiver [Expression<Boolean>] 인스턴스. XNOR의 왼쪽 항
 * @param predicate XNOR의 오른쪽 항
 * @return 두 Expression의 XNOR 조건을 나타내는 [BooleanExpression] 인스턴스
 */
infix fun Expression<Boolean>.xnor(predicate: Expression<Boolean>): BooleanExpression =
    Expressions.booleanOperation(Ops.XNOR, this, predicate)

/**
 * Get the negation of this expression
 *
 * @return this * -1
 */
operator fun <T> Expression<T>.unaryMinus(): NumberExpression<T> where T: Comparable<*>, T: Number {
    return Expressions.numberOperation(type, Ops.NEGATE, this)
}

/**
 * 두 [Expression]의 덧셈(`PLUS`) 연산 결과를 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 덧셈의 왼쪽 항
 * @param other 덧셈의 오른쪽 항
 * @return 두 Expression의 덧셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.plus(
    other: Expression<V>,
): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.ADD, this, other)

/**
 * 두 [Expression]의 뺄셈(`MINUS`) 연산 결과를 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 뺄셈의 왼쪽 항
 * @param other 뺄셈의 오른쪽 항
 * @return 두 Expression의 뺄셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.minus(
    other: Expression<V>,
): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.SUB, this, other)

/**
 * 두 [Expression]의 곱셈(`TIMES`) 연산 결과를 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 곱셈의 왼쪽 항
 * @param other 곱셈의 오른쪽 항
 * @return 두 Expression의 곱셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.times(
    other: Expression<V>,
): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.MULT, this, other)

/**
 * 두 [Expression]의 나눗셈(`DIV`) 연산 결과를 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 나눗셈의 왼쪽 항
 * @param other 나눗셈의 오른쪽 항
 * @return 두 Expression의 나눗셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.div(
    other: Expression<V>,
): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.DIV, this, other)

/**
 * 두 [Expression]의 나눗셈(`DIV`) 연산 결과를 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 나눗셈의 왼쪽 항
 * @param other 나눗셈의 오른쪽 항
 * @return 두 Expression의 나눗셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> NumberExpression<T>.div(
    other: Expression<V>,
): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number, V: Comparable<*> =
    this.divide(other)

/**
 * 두 [Expression]의 나머지(`REM`, `MOD`) 연산 결과를 반환합니다.
 */
operator fun <T, V> Expression<T>.rem(
    other: Expression<V>,
): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.MOD, this, other)


/**
 * [Expression]에 주어진 [other] 값의 덧셈 [NumberExpression]을 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 덧셈의 왼쪽 항
 * @param other 덧셈의 오른쪽 항
 * @return [Expression]과 [other]의 덧셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.plus(other: V): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.ADD, this, Expressions.constant(other))

/**
 * [Expression]에 주어진 [other] 값의 뺄셈 [NumberExpression]을 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 뺄셈의 왼쪽 항
 * @param other 뺄셈의 오른쪽 항
 * @return [Expression]과 [other]의 뺄셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.minus(other: V): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.SUB, this, Expressions.constant(other))

/**
 * [Expression]에 주어진 [other] 값의 곱셈 [NumberExpression]을 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 곱셈의 왼쪽 항
 * @param other 곱셈의 오른쪽 항
 * @return [Expression]과 [other]의 곱셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.times(other: V): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.MULT, this, Expressions.constant(other))

/**
 * [Expression]에 주어진 [other] 값의 나눗셈 [NumberExpression]을 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 나눗셈의 왼쪽 항
 * @param other 나눗셈의 오른쪽 항
 * @return [Expression]과 [other]의 나눗셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.div(other: V): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.DIV, this, Expressions.constant(other))

/**
 * [Expression]에 주어진 [other] 값의 나눗셈 [NumberExpression]을 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 나눗셈의 왼쪽 항
 * @param other 나눗셈의 오른쪽 항
 * @return [Expression]과 [other]의 나눗셈 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> NumberExpression<T>.div(other: V): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number, V: Comparable<*> =
    this.divide(other)

/**
 * [Expression]에 주어진 [other] 값의 나머지(`REM`, `MOD`) [NumberExpression]을 반환합니다.
 *
 * @receiver [Expression<T>] 인스턴스. 나머지의 왼쪽 항
 * @param other 나머지의 오른쪽 항
 * @return [Expression]과 [other]의 나머지 결과를 나타내는 [NumberExpression] 인스턴스
 */
operator fun <T, V> Expression<T>.rem(other: V): NumberExpression<T> where T: Comparable<*>, T: Number, V: Number =
    Expressions.numberOperation(type, Ops.MOD, this, Expressions.constant(other))

/**
 * 두 문자열 [Expression]을 `concat` 하는 [StringExpression]을 반환합니다.
 *
 * @receiver [Expression<String>] 인스턴스. 문자열의 왼쪽 항
 * @param x 문자열의 오른쪽 항
 * @return 두 문자열 [Expression]의 `concat` 결과를 나타내는 [StringExpression] 인스턴스
 */
operator fun Expression<String>.plus(x: Expression<String>): StringExpression =
    Expressions.stringOperation(Ops.CONCAT, this, x)

/**
 * 문자열 [Expression<String>]과 [x] 문자열을 `concat` 하는 [StringExpression]을 반환합니다.
 *
 * @receiver [Expression<String>] 인스턴스. 문자열의 왼쪽 항
 * @param x 문자열의 오른쪽 항
 * @return 문자열 [Expression]과 [x] 문자열의 `concat` 결과를 나타내는 [StringExpression] 인스턴스
 */
operator fun Expression<String>.plus(x: String): StringExpression =
    Expressions.stringOperation(Ops.CONCAT, this, Expressions.constant(x))

/**
 * 문자열 [Expression<String>]에서 [x] 위치의 문자를 나타내는 [SimpleExpression]을 반환합니다.
 *
 * @receiver [Expression<String>] 인스턴스. 문자열
 * @param x 문자의 위치
 * @return 문자열 [Expression]에서 [x] 위치의 문자를 나타내는 [SimpleExpression] 인스턴스
 */
operator fun Expression<String>.get(x: Expression<Int>): SimpleExpression<Character> =
    Expressions.simpleOperation(Character::class.java, Ops.CHAR_AT, this, x)

/**
 * 문자열 [Expression<String>]에서 [x] 위치의 문자를 나타내는 [SimpleExpression]을 반환합니다.
 *
 * @receiver [Expression<String>] 인스턴스. 문자열
 * @param x 문자의 위치
 * @return 문자열 [Expression]에서 [x] 위치의 문자를 나타내는 [SimpleExpression] 인스턴스
 */
operator fun Expression<String>.get(x: Int): SimpleExpression<Character> =
    Expressions.simpleOperation(Character::class.java, Ops.CHAR_AT, this, Expressions.constant(x))
