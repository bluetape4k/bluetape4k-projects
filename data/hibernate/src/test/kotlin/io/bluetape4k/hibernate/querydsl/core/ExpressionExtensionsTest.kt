package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Ops
import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class ExpressionExtensionsTest {

    private val flag = Expressions.booleanPath("flag")
    private val num = Expressions.numberPath(Int::class.javaObjectType, "num")
    private val str = Expressions.stringPath("str")

    @Test
    fun `boolean operators produce composed expression`() {
        (!flag).toString().shouldNotBeEmpty()
        (flag and flag).toString().shouldNotBeEmpty()
        (flag or flag).toString().shouldNotBeEmpty()
        (flag xor flag).toString().shouldNotBeEmpty()
        (flag xnor flag).toString().shouldNotBeEmpty()
    }

    @Test
    fun `numeric operators support expression and constant operands`() {
        (num + num).toString().shouldNotBeEmpty()
        (num - num).toString().shouldNotBeEmpty()
        (num * num).toString().shouldNotBeEmpty()
        (num / num).toString().shouldNotBeEmpty()
        (num % num).toString().shouldNotBeEmpty()

        (num + 1).toString().shouldNotBeEmpty()
        (num - 1).toString().shouldNotBeEmpty()
        (num * 2).toString().shouldNotBeEmpty()
        (num / 2).toString().shouldNotBeEmpty()
        (num % 2).toString().shouldNotBeEmpty()

        (-num).toString().shouldNotBeEmpty()
    }

    @Test
    fun `string operators handle concat and index access`() {
        val concat = str + "suffix"
        concat.toString().shouldNotBeEmpty()

        val concatExpr = str + Expressions.constant("expr")
        concatExpr.toString().shouldNotBeEmpty()

        val ch = str[1]
        ch.shouldNotBeNull()
        ch.type shouldBeEqualTo Character::class.java
    }

    @Test
    fun `Expression Boolean not and or on non-BooleanExpression receiver`() {
        // ConstantImpl<Boolean>은 BooleanExpression 이 아니므로 extension이 호출됨
        val boolConst: Expression<Boolean> = Expressions.constant(true)
        (!boolConst).toString().shouldNotBeEmpty()
        (boolConst and boolConst).toString().shouldNotBeEmpty()
        (boolConst or boolConst).toString().shouldNotBeEmpty()
    }

    @Test
    fun `Expression div with generic Expression receiver uses generic overload`() {
        // Expression<Int> 로 ascription 하면 NumberExpression 오버로드 대신 generic 오버로드 호출
        val numExpr: Expression<Int> = num
        (numExpr / num).toString().shouldNotBeEmpty()
        (numExpr / 2).toString().shouldNotBeEmpty()
    }

    @Test
    fun `Expression String operators on raw Operation receiver`() {
        // OperationImpl 은 StringExpression 이 아니므로 extension 이 호출됨
        val strOp: Expression<String> = ExpressionUtils.operation(String::class.java, Ops.TRIM, str)
        (strOp + str).toString().shouldNotBeEmpty()
        (strOp + "hello").toString().shouldNotBeEmpty()
        strOp[Expressions.constant(0)].shouldNotBeNull()
    }

    @Test
    fun `string trimming and padding helpers are exposed`() {
        str.ltrim().toString().shouldContain("ltrim")
        str.rtrim().toString().shouldContain("rtrim")

        str.lpad(5).toString().shouldContain("lpad")
        str.lpad(5, '0').toString().shouldContain("lpad")
        str.rpad(6).toString().shouldContain("rpad")
        str.rpad(6, ' ').toString().shouldContain("rpad")

        // expression overloads
        val len = Expressions.numberPath(Int::class.javaObjectType, "len")
        str.lpad(len).toString().shouldContain("lpad")
        str.rpad(len).toString().shouldContain("rpad")
    }
}
