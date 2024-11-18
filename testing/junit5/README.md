# Module bluetape4k-junit5

## 개요

JUnit 5 를 위한 다양한 Extensions를 제공합니다.

## Extensions

### StopWatchExtension

`StopWatchExtension` 은 테스트 메소드의 수행 시각을 측정할 수 있도록 합니다.

```kotlin
@StopWatchTest
class FooTest {

    @Test
    fun `some test case`() {
        Thread.sleep(10)
    }
}
```

### TempFolderExtension

`TempFolderExtension` 은 테스트 시에만 사용하는 임시 폴더를 제공합니다.

```kotlin
@TempFolderTest
@TestInstance(Lifecycle.PER_CLASS)
class TempFolderExtensionBeforeAllTest {

    lateinit var tempFolder: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @AfterAll
    fun afterAll() {
        val createdFiles = Files.list(tempFolder.root.toPath()).map { it.toFile().name }.toList()
        createdFiles.size shouldBeEqualTo 2
        createdFiles shouldContainAll listOf("foo.txt", "bar")
    }

    @Test
    fun `임시 파일 생성`() {
        val file = tempFolder.createFile("foo.txt")
        file.exists().shouldBeTrue()
    }

    @Test
    fun `임시 디렉토리 생성`() {
        val dir = tempFolder.createDirectory("bar")
        dir.exists().shouldBeTrue()
    }
}
```

### CaptureSystemOutputExtension

테스트 시, 검증할 값을 받아 올 수 없고, console에 출력만 된다면, 이렇게 출력된 값을 capture하여 검증할 수 있도록 합니다.

```kotlin
@CaptureSystemOutput
@TestMethodOrder(OrderAnnotation::class)
class CaptureSystemOutputExtensionTest {

    companion object: KLogging()

    @BeforeEach
    fun beforeEach(output: OutputCapture) {
        verifyOutput(output, "@BeforeEach")
    }

    @AfterEach
    fun afterEach(output: OutputCapture) {
        verifyOutput(output, "@AfterEach")
    }

    @Test
    @Order(1)
    fun `capture system output`(output: OutputCapture) {
        verifyOutput(output, "SYS OUT #1")
    }

    @Test
    @Order(2)
    fun `capture system error`(output: OutputCapture) {
        verifyError(output, "SYS ERR #2")
    }

    @Test
    @Order(3)
    fun `capture system out and err`(output: OutputCapture) {
        verifyOutput(output, "SYS OUT #2")
        verifyError(output, "SYS ERR #4")
    }

    private fun verifyOutput(output: OutputCapture, expected: String) {
        output.toString() shouldNotContain expected
        println(expected)
        output.expect { it shouldContain expected }
        output.expect { it shouldNotContain expected.toLowerCase() }
    }

    private fun verifyError(output: OutputCapture, expected: String) {
        output.toString() shouldNotContain expected
        println(expected)
        output.expect { it shouldContain expected }
        output.expect { it shouldNotContain expected.toLowerCase() }
    }
}
```

### InMemoryAppender

log 로 출력된 값을 가지고, 테스트를 검증하기 위해 일시적으로 메모리에 log를 쌓는 appender를 제공합니다.

```kotlin
class InMemoryAppenderTest {

    companion object: KLogging()

    private lateinit var appender: InMemoryAppender

    @BeforeEach
    fun beforeEach() {
        appender = InMemoryAppender(InMemoryAppenderTest::class)
    }

    @AfterEach
    fun afterEach() {
        appender.stop()
    }

    @RepeatedTest(5)
    fun `capture logback log message`() {
        log.debug { "First message" }
        appender.lastMessage shouldBeEqualTo "First message"
        appender.size shouldBeEqualTo 1

        log.debug { "Second message" }
        appender.lastMessage shouldBeEqualTo "Second message"
        appender.size shouldBeEqualTo 2

        appender.clear()
        appender.size shouldBeEqualTo 0
        appender.lastMessage.shouldBeNull()
        appender.messages.shouldBeEmpty()
    }
}
```

### RandomExtension

Property based testing 을 위해, 테스트용 객체에 random 값을 주입시켜 임의의 값에 대해서 테스트를 수행할 수 있도록 합니다.

```kotlin
@RandomizedTest
class RandomExtensionTest {
    @Test
    // random string을 가진 리스트를 주입합니다.
    fun `can inject a random list of default size`(@RandomValue(type = String::class) anyList: List<String>) {
        anyList.shouldNotBeNull()
        anyList.shouldNotBeEmpty()
        anyList.size shouldBeEqualTo getDefaultSizeOfRandom()
    }

    @Test
    // `DomainObject` 속성 중 `wotsits`, `id`, `nestedDomainObject` 속성을 제외한 나머지 속성에 random 값을 주입한 2개의 `DomainObject` 를 인자로 제공합니다
    fun `can inject random partially populated domain objects`(
        @RandomValue(size = 2, type = DomainObject::class, excludes = ["wotsits", "id", "nestedDomainObject.address"])
        anyPartiallyPopulatedDomainObjects: List<DomainObject>
    ) {

        anyPartiallyPopulatedDomainObjects.shouldNotBeNull()
        anyPartiallyPopulatedDomainObjects.shouldNotBeEmpty()
        anyPartiallyPopulatedDomainObjects.size shouldBeEqualTo 2
        anyPartiallyPopulatedDomainObjects.forEach {
            it.shouldPartiallyPopulated()
        }
    }
}
```

### SystemPropertyExtension

테스트 시에만 적용되는 System property 을 설정할 수 있도록 합니다. 테스트 후에는 원래 값으로 복원시킵니다.

```kotlin
// 클래스 레벨로 System Property 값을 지정합니다.
@SystemProperties(
    [
        SystemProperty("classPropertyA", "classPropertyValueA"),
        SystemProperty("classPropertyB", "classPropertyValueB"),
        SystemProperty("classPropertyC", "classPropertyValueC")
    ]
)
class SystemPropertyExtensionClassTest {

    @Test
    fun `클래스 단위로 테스트를 위한 시스템 속성을 설정`() {
        System.getProperty("classPropertyA") shouldBeEqualTo "classPropertyValueA"
        System.getProperty("classPropertyB") shouldBeEqualTo "classPropertyValueB"
        System.getProperty("classPropertyC") shouldBeEqualTo "classPropertyValueC"
    }

    @Test
    // 메소드 레벨로 System Property 값을 지정합니다.
    @SystemProperties(
        [
            SystemProperty("keyA", "valueA"),
            SystemProperty("keyB", "valueB")
        ]
    )
    fun `메소드 단위로 테스트를 위한 시스템 속성을 설정`() {
        System.getProperty("classPropertyA") shouldBeEqualTo "classPropertyValueA"
        System.getProperty("classPropertyB") shouldBeEqualTo "classPropertyValueB"
        System.getProperty("classPropertyC") shouldBeEqualTo "classPropertyValueC"

        System.getProperty("keyA") shouldBeEqualTo "valueA"
        System.getProperty("keyB") shouldBeEqualTo "valueB"
    }
}
```
