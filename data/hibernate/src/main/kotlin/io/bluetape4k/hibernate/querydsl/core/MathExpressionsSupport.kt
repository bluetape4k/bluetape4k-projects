package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.MathExpressions
import com.querydsl.core.types.dsl.NumberExpression


fun <T> Expression<T>.acos(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.acos(this)

fun <T> Expression<T>.asin(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.asin(this)

fun <T> Expression<T>.atan(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.atan(this)

fun <T> Expression<T>.cos(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.cos(this)

fun <T> Expression<T>.cosh(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.cosh(this)

fun <T> Expression<T>.cot(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.cot(this)

fun <T> Expression<T>.coth(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.coth(this)

fun <T> Expression<T>.degrees(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.degrees(this)


fun <T> Expression<T>.exp(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.exp(this)

fun <T> Expression<T>.ln(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.ln(this)

fun <T> Expression<T>.log(base: Int): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.log(this, base)

infix fun <T> Expression<T>.max(right: Expression<T>): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.max(this, right)

infix fun <T> Expression<T>.min(right: Expression<T>): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.min(this, right)

fun <T> Expression<T>.power(expoent: Int): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.power(this, expoent)

fun <T> Expression<T>.radians(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.radians(this)


fun randomExprOf(): NumberExpression<Double> = MathExpressions.random()
fun randomExprOf(seed: Int): NumberExpression<Double> = MathExpressions.random(seed)


fun <T> Expression<T>.round(): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.round(this)

fun <T> Expression<T>.round(decimal: Int): NumberExpression<T> where T: Number, T: Comparable<*> =
    MathExpressions.round(this, decimal)

fun <T> Expression<T>.sign(): NumberExpression<Int> where T: Number, T: Comparable<*> =
    MathExpressions.sign(this)

fun <T> Expression<T>.sin(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.sin(this)

fun <T> Expression<T>.sinh(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.sinh(this)

fun <T> Expression<T>.tan(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.tan(this)

fun <T> Expression<T>.tanh(): NumberExpression<Double> where T: Number, T: Comparable<*> =
    MathExpressions.tanh(this)
