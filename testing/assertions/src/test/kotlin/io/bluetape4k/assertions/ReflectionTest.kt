package io.bluetape4k.assertions

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class ReflectionTest {

    // ── shouldBeInstanceOf (reified) ──────────────────────────────────────

    @Test
    fun `shouldBeInstanceOf reified with smart cast`() {
        val obj: Any = "hello"
        val str = obj.shouldBeInstanceOf<String>()
        str.length // smart-cast: String (컴파일 검증)
        str.length shouldBeEqualTo 5
    }

    @Test
    fun `shouldBeInstanceOf reified passes for exact type`() {
        val obj: Any = 42
        val num = obj.shouldBeInstanceOf<Int>()
        num shouldBeEqualTo 42
    }

    @Test
    fun `shouldBeInstanceOf reified passes for subtype`() {
        val obj: Any = ArrayList<String>()
        obj.shouldBeInstanceOf<List<*>>()
    }

    @Test
    fun `shouldBeInstanceOf reified fails when type does not match`() {
        assertFailsWith<AssertionFailedError> {
            val obj: Any = 42
            obj.shouldBeInstanceOf<String>()
        }
    }

    @Test
    fun `shouldBeInstanceOf null receiver fails`() {
        val obj: Any? = null
        assertFailsWith<AssertionFailedError> {
            obj.shouldBeInstanceOf<String>()
        }
    }

    // ── shouldNotBeInstanceOf (reified) ──────────────────────────────────

    @Test
    fun `shouldNotBeInstanceOf reified passes when type does not match`() {
        val obj: Any = 42
        obj.shouldNotBeInstanceOf<String>()
    }

    @Test
    fun `shouldNotBeInstanceOf reified fails when type matches`() {
        assertFailsWith<AssertionFailedError> {
            val obj: Any = "hello"
            obj.shouldNotBeInstanceOf<String>()
        }
    }

    @Test
    fun `shouldNotBeInstanceOf null receiver passes`() {
        val obj: Any? = null
        obj.shouldNotBeInstanceOf<String>() // 통과해야 함
    }

    // ── shouldBeInstanceOf (KClass) ───────────────────────────────────────

    @Test
    fun `shouldBeInstanceOf KClass passes for matching type`() {
        val obj: Any = "hello"
        obj shouldBeInstanceOf String::class
    }

    @Test
    fun `shouldBeInstanceOf KClass passes for subtype`() {
        val obj: Any = ArrayList<String>()
        obj shouldBeInstanceOf List::class
    }

    @Test
    fun `shouldBeInstanceOf KClass fails when type does not match`() {
        assertFailsWith<AssertionFailedError> {
            val obj: Any = 42
            obj shouldBeInstanceOf String::class
        }
    }

    @Test
    fun `shouldBeInstanceOf KClass fails for null receiver`() {
        val obj: Any? = null
        assertFailsWith<AssertionFailedError> {
            obj shouldBeInstanceOf String::class
        }
    }

    // ── shouldNotBeInstanceOf (KClass) ────────────────────────────────────

    @Test
    fun `shouldNotBeInstanceOf KClass passes when type does not match`() {
        val obj: Any = 42
        obj shouldNotBeInstanceOf String::class
    }

    @Test
    fun `shouldNotBeInstanceOf KClass fails when type matches`() {
        assertFailsWith<AssertionFailedError> {
            val obj: Any = "hello"
            obj shouldNotBeInstanceOf String::class
        }
    }

    @Test
    fun `shouldNotBeInstanceOf KClass passes for null receiver`() {
        val obj: Any? = null
        obj shouldNotBeInstanceOf String::class // 통과해야 함
    }

    // ── sealed class hierarchy ────────────────────────────────────────────

    sealed class Shape
    data class Circle(val radius: Double) : Shape()
    data class Rectangle(val width: Double, val height: Double) : Shape()

    @Test
    fun `shouldBeInstanceOf works with sealed class subtype`() {
        val shape: Shape = Circle(5.0)
        val circle = shape.shouldBeInstanceOf<Circle>()
        circle.radius shouldBeEqualTo 5.0
    }

    @Test
    fun `shouldBeInstanceOf fails for wrong sealed class subtype`() {
        assertFailsWith<AssertionFailedError> {
            val shape: Shape = Rectangle(3.0, 4.0)
            shape.shouldBeInstanceOf<Circle>()
        }
    }

    @Test
    fun `shouldNotBeInstanceOf works with sealed class hierarchy`() {
        val shape: Shape = Circle(5.0)
        shape.shouldNotBeInstanceOf<Rectangle>()
    }

    @Test
    fun `shouldBeInstanceOf KClass works with sealed class subtype`() {
        val shape: Shape = Rectangle(3.0, 4.0)
        shape shouldBeInstanceOf Rectangle::class
    }

    @Test
    fun `shouldNotBeInstanceOf KClass works with sealed class hierarchy`() {
        val shape: Shape = Circle(5.0)
        shape shouldNotBeInstanceOf Rectangle::class
    }

    // ── chaining ──────────────────────────────────────────────────────────

    @Test
    fun `shouldBeInstanceOf reified supports chaining`() {
        val obj: Any = "hello"
        obj.shouldBeInstanceOf<String>()
            .shouldNotBeNull()
            .shouldBeEqualTo("hello")
    }

    @Test
    fun `shouldNotBeInstanceOf reified supports chaining`() {
        val obj: Any = 42
        obj.shouldNotBeInstanceOf<String>()
            .shouldNotBeNull()
    }

    @Test
    fun `shouldBeInstanceOf KClass supports chaining`() {
        val obj: Any = "hello"
        (obj shouldBeInstanceOf String::class)
            .shouldNotBeNull()
    }

    @Test
    fun `shouldNotBeInstanceOf KClass supports chaining`() {
        val obj: Any = 42
        (obj shouldNotBeInstanceOf String::class)
            .shouldNotBeNull()
    }
}
