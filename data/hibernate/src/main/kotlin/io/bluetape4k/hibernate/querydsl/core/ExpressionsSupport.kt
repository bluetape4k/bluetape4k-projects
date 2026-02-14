package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.Tuple
import com.querydsl.core.types.CollectionExpression
import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.NullExpression
import com.querydsl.core.types.Operator
import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.Template
import com.querydsl.core.types.dsl.ArrayPath
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.BooleanOperation
import com.querydsl.core.types.dsl.BooleanPath
import com.querydsl.core.types.dsl.BooleanTemplate
import com.querydsl.core.types.dsl.CollectionPath
import com.querydsl.core.types.dsl.ComparableEntityPath
import com.querydsl.core.types.dsl.ComparableExpression
import com.querydsl.core.types.dsl.ComparableOperation
import com.querydsl.core.types.dsl.ComparablePath
import com.querydsl.core.types.dsl.ComparableTemplate
import com.querydsl.core.types.dsl.DateExpression
import com.querydsl.core.types.dsl.DateOperation
import com.querydsl.core.types.dsl.DatePath
import com.querydsl.core.types.dsl.DateTemplate
import com.querydsl.core.types.dsl.DateTimeExpression
import com.querydsl.core.types.dsl.DateTimePath
import com.querydsl.core.types.dsl.DateTimeTemplate
import com.querydsl.core.types.dsl.DslOperation
import com.querydsl.core.types.dsl.DslPath
import com.querydsl.core.types.dsl.DslTemplate
import com.querydsl.core.types.dsl.EnumExpression
import com.querydsl.core.types.dsl.EnumOperation
import com.querydsl.core.types.dsl.EnumPath
import com.querydsl.core.types.dsl.EnumTemplate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.ListPath
import com.querydsl.core.types.dsl.MapPath
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.NumberOperation
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.core.types.dsl.NumberTemplate
import com.querydsl.core.types.dsl.SetPath
import com.querydsl.core.types.dsl.SimpleExpression
import com.querydsl.core.types.dsl.SimpleOperation
import com.querydsl.core.types.dsl.SimplePath
import com.querydsl.core.types.dsl.SimpleTemplate
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.core.types.dsl.StringOperation
import com.querydsl.core.types.dsl.StringPath
import com.querydsl.core.types.dsl.StringTemplate
import com.querydsl.core.types.dsl.TimeExpression
import com.querydsl.core.types.dsl.TimeOperation
import com.querydsl.core.types.dsl.TimePath
import com.querydsl.core.types.dsl.TimeTemplate
import io.bluetape4k.support.requireNotBlank
import java.sql.Time
import java.util.*


/**
 * 현 날짜를 나태내는 [DateExpression]를 반환합니다.
 */
fun currentDateExpr(): DateExpression<Date> = Expressions.currentDate()

/**
 * 현 시각을 나태내는 [TimeExpression]를 반환합니다.
 */
fun currentTimeExpr(): TimeExpression<Time> = Expressions.currentTime()

/**
 * 현 시각을 나타내는 [DateTimeExpression]를 반환합니다.
 */
fun currentTimestampExpr(): DateTimeExpression<Date> = Expressions.currentTimestamp()

/**
 * 현 [Expression]에 대한 alias 를 설정합니다.
 */
fun <D> Expression<D>.alias(alias: Path<D>): SimpleExpression<D> =
    Expressions.`as`(this, alias)

/**
 * [BooleanExpression] 리스트의 모든 항목이 참인지 확인하는 [BooleanExpression]을 반환합니다.
 */
fun Collection<BooleanExpression>.all(): BooleanExpression =
    if (isEmpty()) Expressions.TRUE else Expressions.allOf(*toTypedArray())

/**
 * [BooleanExpression] 리스트의 모든 항목 중 하나라도 참인지 확인하는 [BooleanExpression]을 반환합니다.
 */
fun Collection<BooleanExpression>.any(): BooleanExpression =
    if (isEmpty()) Expressions.FALSE else Expressions.anyOf(*toTypedArray())

/**
 * 해당 값을 `constant`로 표현하는 [Expression]을 반환합니다.
 */
fun <T> T.toExpression(): Expression<T> = Expressions.constant(this)

/**
 * 해당 값을 `constant`로 표현하는 `alias`를 [SimpleExpression]을 반환합니다.
 */
fun <T> T.toExpression(alias: Path<T>): SimpleExpression<T> = Expressions.constantAs(this, alias)

// SimpleTemplate

/**
 * [SimpleTemplate]을 생성합니다.
 *
 * @param template 템플릿 문자열
 * @param args 템플릿 인자
 * @return [SimpleTemplate] 인스턴스
 */
inline fun <reified T: Any> simpleTemplateOf(template: String, vararg args: Any?): SimpleTemplate<T> =
    Expressions.template(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [SimpleTemplate]을 생성합니다.
 * @param template 템플릿 문자열
 * @param args 템플릿 인자
 */
inline fun <reified T: Any> simpleTemplateOf(template: String, args: List<*>): SimpleTemplate<T> =
    Expressions.template(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]을 [SimpleTemplate]로 변환합니다.
 *
 * @receiver [Template] 인스턴스
 * @param args 템플릿 인자
 * @return [SimpleTemplate] 인스턴스
 */
inline fun <reified T: Any> Template.simpleTemplate(vararg args: Any?): SimpleTemplate<T> =
    Expressions.template(T::class.java, this, *args)

/**
 * [Template]을 [SimpleTemplate]로 변환합니다.
 *
 * @receiver [Template] 인스턴스
 * @param args 템플릿 인자
 * @return [SimpleTemplate] 인스턴스
 */
inline fun <reified T: Any> Template.simpleTemplate(args: List<*>): SimpleTemplate<T> =
    Expressions.template(T::class.java, this, args)

// DslTemplate

/**
 * [DslTemplate]을 생성합니다.
 *
 * @param template 템플릿 문자열
 * @param args 템플릿 인자
 * @return [DslTemplate] 인스턴스
 */
inline fun <reified T: Any> dslTemplateOf(template: String, vararg args: Any?): DslTemplate<T> =
    Expressions.dslTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [DslTemplate]을 생성합니다.
 *
 * @param template 템플릿 문자열
 * @param args 템플릿 인자
 * @return [DslTemplate] 인스턴스
 */
inline fun <reified T: Any> dslTemplateOf(template: String, args: List<*>): DslTemplate<T> =
    Expressions.dslTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]을 [DslTemplate]로 변환합니다.
 *
 * @receiver [Template] 인스턴스
 * @param args 템플릿 인자
 * @return [DslTemplate] 인스턴스
 */
inline fun <reified T: Any> Template.dslTemplate(vararg args: Any?): DslTemplate<T> =
    Expressions.dslTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [DslTemplate]를 생성합니다.
 *
 * @receiver [Template] 인스턴스
 * @param args 템플릿 인자
 * @return [DslTemplate] 인스턴스
 */
inline fun <reified T: Any> Template.dslTemplate(args: List<*>): DslTemplate<T> =
    Expressions.dslTemplate(T::class.java, this, args)

// ComparableTemplate

/**
 * [ComparableTemplate]을 생성합니다.
 *
 * @param template 템플릿 문자열
 * @param args 템플릿 인자
 * @return [ComparableTemplate] 인스턴스
 */
inline fun <reified T: Comparable<*>> comparableTemplateOf(template: String, vararg args: Any?): ComparableTemplate<T> =
    Expressions.comparableTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [ComparableTemplate]을 생성합니다.
 *
 * @param template 템플릿 문자열
 * @param args 템플릿 인자
 * @return [ComparableTemplate] 인스턴스
 */
inline fun <reified T: Comparable<*>> comparableTemplateOf(template: String, args: List<*>): ComparableTemplate<T> =
    Expressions.comparableTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]으로 [ComparableTemplate]를 생성합니다.
 *
 * @receiver [Template] 인스턴스
 * @param args 템플릿 인자
 * @return [ComparableTemplate] 인스턴스
 */
inline fun <reified T: Comparable<*>> Template.comparableTemplate(vararg args: Any?): ComparableTemplate<T> =
    Expressions.comparableTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [ComparableTemplate]를 생성합니다.
 *
 * @receiver [Template] 인스턴스
 * @param args 템플릿 인자
 * @return [ComparableTemplate] 인스턴스
 */
inline fun <reified T: Comparable<*>> Template.comparableTemplate(args: List<*>): ComparableTemplate<T> =
    Expressions.comparableTemplate(T::class.java, this, args)

// DateTemplate

/**
 * [DateTemplate]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> dateTemplateOf(template: String, vararg args: Any?): DateTemplate<T> =
    Expressions.dateTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [DateTemplate]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> dateTemplateOf(template: String, args: List<*>): DateTemplate<T> =
    Expressions.dateTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]으로 [DateTemplate]를 생성합니다.
 */
inline fun <reified T: Comparable<*>> Template.dateTemplate(vararg args: Any?): DateTemplate<T> =
    Expressions.dateTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [DateTemplate]를 생성합니다.
 */
inline fun <reified T: Comparable<*>> Template.dateTemplate(args: List<*>): DateTemplate<T> =
    Expressions.dateTemplate(T::class.java, this, args)

// DateTimeTemplate

/**
 * [DateTimeTemplate]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> dateTimeTemplateOf(template: String, vararg args: Any?): DateTimeTemplate<T> =
    Expressions.dateTimeTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [DateTimeTemplate]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> dateTimeTemplateOf(template: String, args: List<*>): DateTimeTemplate<T> =
    Expressions.dateTimeTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]으로 [DateTimeTemplate]를 생성합니다.
 */
inline fun <reified T: Comparable<*>> Template.dateTimeTemplate(vararg args: Any?): DateTimeTemplate<T> =
    Expressions.dateTimeTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [DateTimeTemplate]를 생성합니다.
 */
inline fun <reified T: Comparable<*>> Template.dateTimeTemplate(args: List<*>): DateTimeTemplate<T> =
    Expressions.dateTimeTemplate(T::class.java, this, args)

// TimeTemplate

/**
 * [TimeTemplate]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> timeTemplateOf(template: String, vararg args: Any?): TimeTemplate<T> =
    Expressions.timeTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [TimeTemplate]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> timeTemplateOf(template: String, args: List<*>): TimeTemplate<T> =
    Expressions.timeTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]으로 [TimeTemplate]를 생성합니다.
 */
inline fun <reified T: Comparable<*>> Template.timeTemplate(vararg args: Any?): TimeTemplate<T> =
    Expressions.timeTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [TimeTemplate]를 생성합니다.
 */
inline fun <reified T: Comparable<*>> Template.timeTemplate(args: List<*>): TimeTemplate<T> =
    Expressions.timeTemplate(T::class.java, this, args)

// EnumTemplate

/**
 * [EnumTemplate]을 생성합니다.
 */
inline fun <reified T: Enum<T>> enumTemplateOf(template: String, vararg args: Any?): EnumTemplate<T> =
    Expressions.enumTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [EnumTemplate]을 생성합니다.
 */
inline fun <reified T: Enum<T>> enumTemplateOf(template: String, args: List<*>): EnumTemplate<T> =
    Expressions.enumTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]으로 [EnumTemplate]를 생성합니다.
 */
inline fun <reified T: Enum<T>> Template.enumTemplate(vararg args: Any?): EnumTemplate<T> =
    Expressions.enumTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [EnumTemplate]를 생성합니다.
 */
inline fun <reified T: Enum<T>> Template.enumTemplate(args: List<*>): EnumTemplate<T> =
    Expressions.enumTemplate(T::class.java, this, args)

// NumberTemplate

/**
 * [NumberTemplate]을 생성합니다.
 */
inline fun <reified T> numberTemplateOf(
    template: String,
    vararg args: Any?,
): NumberTemplate<T> where T: Number, T: Comparable<*> =
    Expressions.numberTemplate(T::class.java, template.requireNotBlank("template"), *args)

/**
 * [NumberTemplate]을 생성합니다.
 */
inline fun <reified T> numberTemplateOf(
    template: String,
    args: List<*>,
): NumberTemplate<T> where T: Number, T: Comparable<*> =
    Expressions.numberTemplate(T::class.java, template.requireNotBlank("template"), args)

/**
 * [Template]으로 [NumberTemplate]를 생성합니다.
 */
inline fun <reified T> Template.numberTemplate(vararg args: Any?): NumberTemplate<T> where T: Number, T: Comparable<*> =
    Expressions.numberTemplate(T::class.java, this, *args)

/**
 * [Template]으로 [NumberTemplate]를 생성합니다.
 */
inline fun <reified T> Template.numberTemplate(args: List<*>): NumberTemplate<T> where T: Number, T: Comparable<*> =
    Expressions.numberTemplate(T::class.java, this, args)

// StringTemplate

/**
 * [StringTemplate]을 생성합니다.
 */
fun stringTemplateOf(template: String, vararg args: Any?): StringTemplate =
    Expressions.stringTemplate(template.requireNotBlank("template"), *args)

/**
 * [StringTemplate]을 생성합니다.
 */
fun simpleTemplateOf(template: String, args: List<*>): StringExpression =
    Expressions.stringTemplate(template.requireNotBlank("template"), args)

/**
 * [Template]으로 [StringTemplate]를 생성합니다.
 */
fun Template.stringTemplate(vararg args: Any?): StringExpression =
    Expressions.stringTemplate(this, *args)

/**
 * [Template]으로 [StringTemplate]를 생성합니다.
 */
fun Template.stringTemplate(args: List<*>): StringExpression =
    Expressions.stringTemplate(this, args)

// BooleanTemplate

/**
 * [BooleanTemplate]을 생성합니다.
 */
fun booleanTemplateOf(template: String, vararg args: Any?): BooleanTemplate =
    Expressions.booleanTemplate(template.requireNotBlank("template"), *args)

/**
 * [BooleanTemplate]을 생성합니다.
 */
fun booleanTemplateOf(template: String, args: List<*>): BooleanTemplate =
    Expressions.booleanTemplate(template.requireNotBlank("template"), args)

/**
 * [Template]으로 [BooleanTemplate]를 생성합니다.
 */
fun Template.booleanTemplate(vararg args: Any?): BooleanTemplate =
    Expressions.booleanTemplate(this, *args)

/**
 * [Template]으로 [BooleanTemplate]를 생성합니다.
 */
fun Template.booleanTemplate(args: List<*>): BooleanTemplate =
    Expressions.booleanTemplate(this, args)

// Operation

/**
 * [Operator]를 이용하여 [SimpleOperation]을 생성합니다.
 *
 * @param args 연산의 인자
 * @return [SimpleOperation] 인스턴스
 */
inline fun <reified T: Any> Operator.simpleOperation(vararg args: Expression<*>): SimpleOperation<T> =
    Expressions.simpleOperation(T::class.java, this, *args)

/**
 * [Operator]를 이용하여 [DslOperation]을 생성합니다.
 *
 * @param args 연산의 인자
 * @return [DslOperation] 인스턴스
 */
inline fun <reified T: Any> Operator.dslOperation(vararg args: Expression<*>): DslOperation<T> =
    Expressions.dslOperation(T::class.java, this, *args)

/**
 * [Operator]를 이용하여 [BooleanOperation]을 생성합니다.
 *
 * @param args 연산의 인자
 * @return [BooleanOperation] 인스턴스
 */
fun Operator.booleanOperation(vararg args: Expression<*>): BooleanOperation =
    Expressions.booleanOperation(this, *args)

/**
 * [Operator]를 이용하여 [ComparableOperation]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> Operator.comparableOperation(vararg args: Expression<*>): ComparableOperation<T> =
    Expressions.comparableOperation(T::class.java, this, *args)

/**
 * [Operator]를 이용하여 [DateOperation]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> Operator.dateOperation(vararg args: Expression<*>): DateOperation<T> =
    Expressions.dateOperation(T::class.java, this, *args)

/**
 * [Operator]를 이용하여 [TimeOperation]을 생성합니다.
 */
inline fun <reified T: Comparable<*>> Operator.timeOperation(vararg args: Expression<*>): TimeOperation<T> =
    Expressions.timeOperation(T::class.java, this, *args)

/**
 * [Operator]를 이용하여 [NumberOperation]을 생성합니다.
 */
inline fun <reified T> Operator.numberOperation(vararg args: Expression<*>): NumberOperation<T> where T: Number, T: Comparable<*> =
    Expressions.numberOperation(T::class.java, this, *args)

fun Operator.stringOperation(vararg args: Expression<*>): StringOperation =
    Expressions.stringOperation(this, *args)

// SimplePath

/**
 * [SimplePath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Any> simplePathOf(variable: String): SimplePath<T> =
    Expressions.simplePath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [SimplePath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param variable 변수명
 */
inline fun <reified T: Any> simplePathOf(parent: Path<*>, variable: String): SimplePath<T> =
    Expressions.simplePath(T::class.java, parent, variable.requireNotBlank("variable"))

/**
 * [SimplePath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Any> simplePathOf(metadata: PathMetadata): SimplePath<T> =
    Expressions.simplePath(T::class.java, metadata)

// DslPath

/**
 * [DslPath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Any> dslPathOf(variable: String): DslPath<T> =
    Expressions.dslPath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [DslPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Any> dslPathOf(parent: Path<*>, property: String): DslPath<T> =
    Expressions.dslPath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [DslPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Any> dslPathOf(metadata: PathMetadata): DslPath<T> =
    Expressions.dslPath(T::class.java, metadata)

// ComparablePath

/**
 * [ComparablePath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Comparable<*>> comparablePathOf(variable: String): ComparablePath<T> =
    Expressions.comparablePath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [ComparablePath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Comparable<*>> comparablePathOf(parent: Path<*>, property: String): ComparablePath<T> =
    Expressions.comparablePath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [ComparablePath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Comparable<*>> comparablePathOf(metadata: PathMetadata): ComparablePath<T> =
    Expressions.comparablePath(T::class.java, metadata)

// ComparableEntityPath

/**
 * [ComparableEntityPath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Comparable<*>> comparableEntityPathOf(variable: String): ComparableEntityPath<T> =
    Expressions.comparableEntityPath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [ComparableEntityPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Comparable<*>> comparableEntityPathOf(
    parent: Path<*>,
    property: String,
): ComparableEntityPath<T> =
    Expressions.comparableEntityPath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [ComparableEntityPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Comparable<*>> comparableEntityPathOf(metadata: PathMetadata): ComparableEntityPath<T> =
    Expressions.comparableEntityPath(T::class.java, metadata)

// DatePath

/**
 * [DatePath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Comparable<*>> datePathOf(variable: String): DatePath<T> =
    Expressions.datePath(T::class.java, variable.requireNotBlank("property"))

/**
 * [DatePath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Comparable<*>> datePathOf(parent: Path<*>, property: String): DatePath<T> =
    Expressions.datePath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [DatePath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Comparable<*>> datePathOf(metadata: PathMetadata): DatePath<T> =
    Expressions.datePath(T::class.java, metadata)

// DateTimePath

/**
 * [DateTimePath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Comparable<*>> dateTimePathOf(variable: String): DateTimePath<T> =
    Expressions.dateTimePath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [DateTimePath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Comparable<*>> dateTimePathOf(parent: Path<*>, property: String): DateTimePath<T> =
    Expressions.dateTimePath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [DateTimePath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Comparable<*>> dateTimePathOf(metadata: PathMetadata): DateTimePath<T> =
    Expressions.dateTimePath(T::class.java, metadata)

// TimePath

/**
 * [TimePath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Comparable<*>> timePathOf(variable: String): TimePath<T> =
    Expressions.timePath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [TimePath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Comparable<*>> timePathOf(parent: Path<*>, property: String): TimePath<T> =
    Expressions.timePath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [TimePath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Comparable<*>> timePathOf(metadata: PathMetadata): TimePath<T> =
    Expressions.timePath(T::class.java, metadata)

// NumberPath

/**
 * [NumberPath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T> numberPathOf(variable: String): NumberPath<T> where T: Number, T: Comparable<*> =
    Expressions.numberPath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [NumberPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T> numberPathOf(
    parent: Path<*>,
    property: String,
): NumberPath<T> where T: Number, T: Comparable<*> =
    Expressions.numberPath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [NumberPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T> numberPathOf(metadata: PathMetadata): NumberPath<T> where T: Number, T: Comparable<*> =
    Expressions.numberPath(T::class.java, metadata)

// StringPath

/**
 * [StringPath]를 생성합니다.
 *
 * @param variable 변수명
 */
fun stringPathOf(variable: String): StringPath =
    Expressions.stringPath(variable.requireNotBlank("variable"))

/**
 * [StringPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param variable 변수명
 */
fun simplePathOf(parent: Path<*>, variable: String): StringPath =
    Expressions.stringPath(parent, variable.requireNotBlank("variable"))

/**
 * [StringPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
fun simplePathOf(metadata: PathMetadata): StringPath =
    Expressions.stringPath(metadata)

/**
 * [StringPath] 가 [name]과 같거나 null 인지를 표현하는 [BooleanExpression]을 반환합니다.
 */
fun StringPath.eqOrNull(name: String?): BooleanExpression? = name?.let { this.eq(it) }

// BooleanPath

/**
 * [BooleanPath]를 생성합니다.
 *
 * @param variable 변수명
 */
fun booleanPathOf(variable: String): BooleanPath =
    Expressions.booleanPath(variable.requireNotBlank("variable"))

/**
 * [BooleanPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param variable 변수명
 */
fun booleanPathOf(parent: Path<*>, variable: String): BooleanPath =
    Expressions.booleanPath(parent, variable.requireNotBlank("variable"))

/**
 * [BooleanPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
fun booleanPathOf(metadata: PathMetadata): BooleanPath =
    Expressions.booleanPath(metadata)

// Expression List

/**
 * [SimpleExpression]`<Tuple>`을 생성합니다.
 *
 * @param exprs [SimpleExpression] 리스트
 */
@JvmName("simpleExpressionListOfTuple")
fun simpleExpressionListOf(vararg exprs: SimpleExpression<*>): SimpleExpression<Tuple> =
    Expressions.list(*exprs)

/**
 * [SimpleExpression]`<T>`를 생성합니다.
 *
 * @param exprs [SimpleExpression] 리스트
 */
inline fun <reified T: Any> simpleExpressionListOf(vararg exprs: SimpleExpression<*>): SimpleExpression<T> =
    Expressions.list(T::class.java, *exprs)

/**
 * [Expression]`<Tuple>` 를 생성합니다.
 *
 * @param exprs [SimpleExpression] 리스트
 */
@JvmName("expressionListOfTuple")
fun expressionListOf(vararg exprs: Expression<*>): Expression<Tuple> =
    Expressions.list(*exprs)

/**
 * [Expression]`<T>`를 생성합니다.
 *
 * @param exprs [SimpleExpression] 리스트
 */
inline fun <reified T: Any> expressionListOf(vararg exprs: Expression<*>): Expression<T> =
    Expressions.list(T::class.java, *exprs)

/**
 * [Expression]`<T>`를 생성합니다.
 *
 * @param exprs [SimpleExpression] 리스트
 */
inline fun <reified T: Any> expressionListOf(exprs: List<Expression<*>>): Expression<T> =
    ExpressionUtils.list(T::class.java, exprs)

// Expression Set

/**
 * [SimpleExpression]`<T>`을 생성합니다.
 *
 * @param exprs [SimpleExpression] 들
 */
inline fun <reified T: Any> simpleExpressionSetOf(vararg exprs: SimpleExpression<*>): SimpleExpression<T> =
    Expressions.set(T::class.java, *exprs)

/**
 * [Expression]`<Tuple>`을 생성합니다.
 *
 * @param exprs [Expression] 들
 */
@JvmName("expressionSetOfTuple")
fun expressionSetOf(vararg exprs: Expression<*>): Expression<Tuple> =
    Expressions.set(*exprs)

/**
 * [Expression]`<T>`을 생성합니다.
 *
 * @param exprs [Expression] 들
 */
inline fun <reified T: Any> expressionSetOf(vararg exprs: Expression<*>): Expression<T> =
    Expressions.set(T::class.java, *exprs)

/**
 * [NullExpression]`<T>`를 생성합니다.
 */
inline fun <reified T: Any> nullExpressionOf(): NullExpression<T> =
    Expressions.nullExpression(T::class.java)

/**
 * [NullExpression]`<T>`를 생성합니다.
 */
fun <T: Any> Path<T>.nullExpression(): NullExpression<T> =
    Expressions.nullExpression(this)

// Enum

/**
 * [EnumOperation]을 생성합니다.
 *
 * @param args 연산의 인자
 */
inline fun <reified T: Enum<T>> Operator.enumOperation(vararg args: Expression<*>): EnumOperation<T> =
    Expressions.enumOperation(T::class.java, this, *args)

// EnumPath

/**
 * [EnumPath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified T: Enum<T>> enumPathOf(variable: String): EnumPath<T> =
    Expressions.enumPath(T::class.java, variable.requireNotBlank("variable"))

/**
 * [EnumPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified T: Enum<T>> enumPathOf(parent: Path<*>, property: String): EnumPath<T> =
    Expressions.enumPath(T::class.java, parent, property.requireNotBlank("property"))

/**
 * [EnumPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified T: Enum<T>> enumPathOf(metadata: PathMetadata): EnumPath<T> =
    Expressions.enumPath(T::class.java, metadata)

// CollectionExpression

/**
 * [CollectionExpression]을 생성합니다.
 *
 * @param args 연산의 인자
 */
inline fun <reified T: Any> Operator.collectionOperation(vararg args: Expression<*>): CollectionExpression<MutableCollection<T>, T> =
    Expressions.collectionOperation(T::class.java, this, *args)

/**
 * [CollectionPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified E: Any, reified Q: SimpleExpression<in E>> collectionPathOf(metadata: PathMetadata): CollectionPath<E, Q> =
    Expressions.collectionPath(E::class.java, Q::class.java, metadata)

/**
 * [ListPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified E: Any, reified Q: SimpleExpression<in E>> listPathOf(metadata: PathMetadata): ListPath<E, Q> =
    Expressions.listPath(E::class.java, Q::class.java, metadata)

/**
 * [SetPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified E: Any, reified Q: SimpleExpression<in E>> setPathOf(metadata: PathMetadata): SetPath<E, Q> =
    Expressions.setPath(E::class.java, Q::class.java, metadata)

/**
 * [MapPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified K: Any, reified V: Any, reified E: SimpleExpression<in V>> mapPathOf(metadata: PathMetadata): MapPath<K, V, E> =
    Expressions.mapPath(K::class.java, V::class.java, E::class.java, metadata)

// ArrayPath

/**
 * [ArrayPath]를 생성합니다.
 *
 * @param variable 변수명
 */
inline fun <reified A, E> arrayPathOf(variable: String): ArrayPath<A, E> =
    Expressions.arrayPath(A::class.java, variable.requireNotBlank("variable"))

/**
 * [ArrayPath]를 생성합니다.
 *
 * @param parent 상위 경로
 * @param property 변수명
 */
inline fun <reified A, E> arrayPathOf(parent: Path<*>, property: String): ArrayPath<A, E> =
    Expressions.arrayPath(A::class.java, parent, property.requireNotBlank("property"))

/**
 * [ArrayPath]를 생성합니다.
 *
 * @param metadata 경로 메타데이터
 */
inline fun <reified A, E> arrayPathOf(metadata: PathMetadata): ArrayPath<A, E> =
    Expressions.arrayPath(A::class.java, metadata)

/**
 * [Expression]`<Boolean>`을 [BooleanExpression]으로 변환합니다.
 */
fun Expression<Boolean>.asBoolean(): BooleanExpression =
    Expressions.asBoolean(this)

/**
 * [value]를 가진 [BooleanExpression]을 생성합니다.
 */
fun booleanExpressionOf(value: Boolean): BooleanExpression =
    Expressions.asBoolean(value)

/**
 * [Expression]`<Comparable>`을 [ComparableExpression]으로 변환합니다.
 */
fun <T: Comparable<T>> Expression<T>.asComparable(): ComparableExpression<T> =
    Expressions.asComparable(this)

/**
 * [value]를 가진 [ComparableExpression]을 생성합니다.
 */
fun <T: Comparable<T>> comparableExpressionOf(value: T): ComparableExpression<T> =
    Expressions.asComparable(value)

/**
 * [Expression]`<Comparable>`을 [DateExpression]으로 변환합니다.
 */
fun <T: Comparable<T>> Expression<T>.asDate(): DateExpression<T> =
    Expressions.asDate(this)

/**
 * [value]를 가진 [DateExpression]을 생성합니다.
 */
fun <T: Comparable<T>> dateExpressionOf(value: T): DateExpression<T> =
    Expressions.asDate(value)

/**
 * [Expression]`<Comparable>`을 [DateTimeExpression]으로 변환합니다.
 */
fun <T: Comparable<T>> Expression<T>.asDateTime(): DateTimeExpression<T> =
    Expressions.asDateTime(this)

/**
 * [value]를 가진 [DateTimeExpression]을 생성합니다.
 */
fun <T: Comparable<T>> dateTimeExpressionOf(value: T): DateTimeExpression<T> =
    Expressions.asDateTime(value)

/**
 * [Expression]`<Comparable>`을 [TimeExpression]으로 변환합니다.
 */
fun <T: Comparable<T>> Expression<T>.asTime(): TimeExpression<T> =
    Expressions.asTime(this)

/**
 * [value]를 가진 [TimeExpression]을 생성합니다.
 */
fun <T: Comparable<T>> timeExpressionOf(value: T): TimeExpression<T> =
    Expressions.asTime(value)

/**
 * [Expression]`<Enum>`을 [EnumExpression]으로 변환합니다.
 */
fun <T: Enum<T>> Expression<T>.asEnum(): EnumExpression<T> =
    Expressions.asEnum(this)

/**
 * [value]를 가진 [EnumExpression]을 생성합니다.
 */
fun <T: Enum<T>> enumExpressionOf(value: T): EnumExpression<T> =
    Expressions.asEnum(value)

/**
 * [Expression]`<Number>`을 [NumberExpression]으로 변환합니다.
 */
fun <T> Expression<T>.asNumber(): NumberExpression<T> where T: Number, T: Comparable<T> =
    Expressions.asNumber(this)

/**
 * [value]를 가진 [NumberExpression]을 생성합니다.
 */
fun <T> numberExpressionOf(value: T): NumberExpression<T> where T: Number, T: Comparable<T> =
    Expressions.asNumber(value)

/**
 * [Expression]`<String>`을 [StringExpression]으로 변환합니다.
 */
fun Expression<String>.asString(): StringExpression = Expressions.asString(this)

/**
 * [value]를 가진 [StringExpression]을 생성합니다.
 */
fun stringExpressionOf(value: String): StringExpression = Expressions.asString(value)

/**
 * receiver의 값을 가지는 [SimpleExpression]을 생성합니다.
 */
fun <T> T.asSimple(): SimpleExpression<T> = Expressions.asSimple(this)

/**
 * [Expression]을 [SimpleExpression]으로 변환합니다.
 */
fun <T> Expression<T>.asSimple(): SimpleExpression<T> = Expressions.asSimple(this)
