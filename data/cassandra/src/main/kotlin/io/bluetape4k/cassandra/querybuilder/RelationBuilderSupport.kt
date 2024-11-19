package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.BindMarker
import com.datastax.oss.driver.api.querybuilder.relation.ArithmeticRelationBuilder
import com.datastax.oss.driver.api.querybuilder.relation.InRelationBuilder
import com.datastax.oss.driver.api.querybuilder.term.Term

infix fun <T> ArithmeticRelationBuilder<T>.eq(rightOperand: Term): T = isEqualTo(rightOperand)
infix fun <T> ArithmeticRelationBuilder<T>.ne(rightOperand: Term): T = isNotEqualTo(rightOperand)

infix fun <T> ArithmeticRelationBuilder<T>.lt(rightOperand: Term): T = isLessThan(rightOperand)
infix fun <T> ArithmeticRelationBuilder<T>.lte(rightOperand: Term): T = isLessThanOrEqualTo(rightOperand)
infix fun <T> ArithmeticRelationBuilder<T>.gt(rightOperand: Term): T = isGreaterThan(rightOperand)
infix fun <T> ArithmeticRelationBuilder<T>.gte(rightOperand: Term): T = isGreaterThanOrEqualTo(rightOperand)


fun <T> InRelationBuilder<T>.inValues(bindMarker: BindMarker): T = `in`(bindMarker)
fun <T> InRelationBuilder<T>.inValues(alternatives: Iterable<Term>): T = `in`(alternatives)
fun <T> InRelationBuilder<T>.inValues(vararg alternatives: Term): T = `in`(*alternatives)
