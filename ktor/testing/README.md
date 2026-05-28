# bluetape4k-ktor-testing

[English](./README.md) | [한국어](./README.ko.md)

Ktor testing helpers for the bluetape4k ecosystem.

## Features

- `testApplication` setup helper that installs `bluetape4k-ktor-core` without hiding the Ktor test lifecycle.
- JSON test client factory using `Bluetape4kKtorJson.defaultJson()`.
- Response decode helpers for kotlinx serialization.
- Status, JSON body, and standard `ApiErrorResponse` assertions.
- Small JSON `MockEngine` helper for one-response client tests.

## Dependency

```kotlin
testImplementation("io.github.bluetape4k:bluetape4k-ktor-testing")
```

## Usage

```kotlin
@Test
fun `endpoint returns json`() = testApplication {
    installBluetape4kKtorCoreForTest {
        get("/echo") {
            call.respond(EchoResponse("blue"))
        }
    }

    val response = client.get("/echo")

    response shouldHaveStatus HttpStatusCode.OK
    response.shouldHaveJsonBody(EchoResponse("blue"))
}
```

Create a JSON-aware Ktor test client when the test uses typed request or
response bodies:

```kotlin
val jsonClient = bluetape4kJsonClient()
val body = jsonClient.get("/echo").body<EchoResponse>()
```

Verify the standard bluetape4k error payload:

```kotlin
client.get("/bad").shouldHaveApiError(
    ExpectedApiError(
        status = HttpStatusCode.BadRequest,
        error = "bad_request",
        message = "Invalid input",
        path = "/bad"
    )
)
```

Use `bluetape4kJsonMockEngine` for simple client-side tests:

```kotlin
val client = HttpClient(
    bluetape4kJsonMockEngine(EchoResponse("mock"))
)
```
