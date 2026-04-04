package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.MathExpressions
import com.querydsl.core.types.dsl.NumberExpression

/**
 * 수치 [Expression]의 역코사인(acos) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = angleExpr.acos()
 * // SQL: ACOS(angle)
 * ```
 */
fun <T> Expression<T>.acos(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.acos(this)

/**
 * 수치 [Expression]의 역사인(asin) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = angleExpr.asin()
 * // SQL: ASIN(angle)
 * ```
 */
fun <T> Expression<T>.asin(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.asin(this)

/**
 * 수치 [Expression]의 역탄젠트(atan) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = angleExpr.atan()
 * // SQL: ATAN(angle)
 * ```
 */
fun <T> Expression<T>.atan(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.atan(this)

/**
 * 수치 [Expression]의 코사인(cos) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = angleExpr.cos()
 * // SQL: COS(angle)
 * ```
 */
fun <T> Expression<T>.cos(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.cos(this)

/**
 * 수치 [Expression]의 쌍곡 코사인(cosh) 표현식을 반환합니다.
 */
fun <T> Expression<T>.cosh(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.cosh(this)

/**
 * 수치 [Expression]의 코탄젠트(cot) 표현식을 반환합니다.
 */
fun <T> Expression<T>.cot(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.cot(this)

/**
 * 수치 [Expression]의 쌍곡 코탄젠트(coth) 표현식을 반환합니다.
 */
fun <T> Expression<T>.coth(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.coth(this)

/**
 * 라디안 [Expression]을 도(degrees)로 변환하는 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = radianExpr.degrees()
 * // SQL: DEGREES(radian)
 * ```
 */
fun <T> Expression<T>.degrees(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.degrees(this)

/**
 * 수치 [Expression]의 지수(exp, e^x) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = xExpr.exp()
 * // SQL: EXP(x)
 * ```
 */
fun <T> Expression<T>.exp(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.exp(this)

/**
 * 수치 [Expression]의 자연 로그(ln) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = xExpr.ln()
 * // SQL: LN(x)
 * ```
 */
fun <T> Expression<T>.ln(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.ln(this)

/**
 * 수치 [Expression]의 밑수 [base]인 로그(log) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = xExpr.log(10)
 * // SQL: LOG(10, x)
 * ```
 */
fun <T> Expression<T>.log(base: Int): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.log(this, base)

/**
 * 두 수치 [Expression] 중 큰 값을 반환하는 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = aExpr max bExpr
 * // SQL: GREATEST(a, b)
 * ```
 */
infix fun <T> Expression<T>.max(right: Expression<T>): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.max(this, right)

/**
 * 두 수치 [Expression] 중 작은 값을 반환하는 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = aExpr min bExpr
 * // SQL: LEAST(a, b)
 * ```
 */
infix fun <T> Expression<T>.min(right: Expression<T>): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.min(this, right)

/**
 * 수치 [Expression]의 거듭제곱(power) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = xExpr.power(2)
 * // SQL: POWER(x, 2)
 * ```
 */
fun <T> Expression<T>.power(expoent: Int): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.power(this, expoent)

/**
 * 도(degrees) [Expression]을 라디안으로 변환하는 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = degreeExpr.radians()
 * // SQL: RADIANS(degree)
 * ```
 */
fun <T> Expression<T>.radians(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.radians(this)

/**
 * 랜덤 수치 표현식을 반환합니다.
 *
 * ```kotlin
 * val rand = randomExprOf()
 * // SQL: RAND()
 * ```
 */
fun randomExprOf(): NumberExpression<Double> = MathExpressions.random()

/**
 * 시드(seed)가 고정된 랜덤 수치 표현식을 반환합니다.
 *
 * ```kotlin
 * val rand = randomExprOf(42)
 * // SQL: RAND(42)
 * ```
 */
fun randomExprOf(seed: Int): NumberExpression<Double> = MathExpressions.random(seed)

/**
 * 수치 [Expression]을 반올림하는 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = xExpr.round()
 * // SQL: ROUND(x)
 * ```
 */
fun <T> Expression<T>.round(): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.round(this)

/**
 * 수치 [Expression]을 소수점 [decimal] 자리까지 반올림하는 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = xExpr.round(2)
 * // SQL: ROUND(x, 2)
 * ```
 */
fun <T> Expression<T>.round(decimal: Int): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.round(this, decimal)

/**
 * 수치 [Expression]의 부호(sign)를 반환하는 표현식입니다. 양수=1, 0=0, 음수=-1.
 *
 * ```kotlin
 * val result = xExpr.sign()
 * // SQL: SIGN(x)
 * ```
 */
fun <T> Expression<T>.sign(): NumberExpression<Int> where T: Number, T: Comparable<*> =
    MathExpressions.sign(this)

/**
 * 수치 [Expression]의 사인(sin) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = angleExpr.sin()
 * // SQL: SIN(angle)
 * ```
 */
fun <T> Expression<T>.sin(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.sin(this)

/**
 * 수치 [Expression]의 쌍곡 사인(sinh) 표현식을 반환합니다.
 */
fun <T> Expression<T>.sinh(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.sinh(this)

/**
 * 수치 [Expression]의 탄젠트(tan) 표현식을 반환합니다.
 *
 * ```kotlin
 * val result = angleExpr.tan()
 * // SQL: TAN(angle)
 * ```
 */
fun <T> Expression<T>.tan(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.tan(this)

/**
 * 수치 [Expression]의 쌍곡 탄젠트(tanh) 표현식을 반환합니다.
 */
fun <T> Expression<T>.tanh(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.tanh(this)
