# bluetape4k-ktor-openapi

[English](README.md) | [한국어](README.ko.md)

Route 동작을 바꾸지 않고 명시적인 문서 route만 추가하려는 애플리케이션을 위한
선택적 Ktor OpenAPI helper입니다.

## Route 다이어그램

![Ktor OpenAPI Route Helpers](../../docs/images/readme-diagrams/ktor-openapi-routes-01.png)

## 기능

- `bluetape4kOpenApi()`는 Ktor 공식 `openAPI()` route를 감쌉니다.
- `bluetape4kSwaggerUi()`는 Ktor 공식 `swaggerUI()` route를 감쌉니다.
- 기본 endpoint와 specification 경로는 bluetape4k Ktor 예제와 맞춥니다:
  `openapi`, `swagger`, `openapi/documentation.yaml`.
- OpenAPI 문서는 애플리케이션이 소유합니다. 정적 YAML, Ktor compiler가 생성한
  metadata, runtime `.describe {}` metadata, 또는 이들의 조합을 사용할 수 있습니다.

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-ktor-openapi:$bluetape4kVersion")
}
```

## 정적 Specification

`src/main/resources/openapi/documentation.yaml` 같은 문서를 둔 뒤 문서 route를
명시적으로 연결합니다.

```yaml
openapi: 3.1.0
info:
  title: Example API
  version: 1.0.0
components:
  schemas: {}
paths:
  /healthz:
    get:
      responses:
        "200":
          description: The application is accepting traffic.
```

```kotlin
fun Application.module() {
    installBluetape4kKtorCore()

    routing {
        bluetape4kHealthRoutes()
        bluetape4kOpenApi()
        bluetape4kSwaggerUi()
    }
}
```

Ktor OpenAPI HTML renderer가 내부적으로 Swagger Codegen에 위임하고 schema map을
기대하므로 `components.schemas`는 명시해 둡니다.

## Runtime Metadata

Ktor 3.5는 Ktor compiler OpenAPI extension, route comment, runtime `.describe {}`
metadata를 통해 routing tree에서 OpenAPI metadata를 조립할 수 있습니다. Route
metadata는 해당 동작을 소유하는 route 근처에 둡니다.

```kotlin
@OptIn(ExperimentalKtorApi::class)
fun Route.widgetRoutes() {
    get("/widgets/{id}") {
        val id = call.requiredPathParameter("id")
        call.respond(mapOf("id" to id))
    }.describe {
        summary = "Read a widget"
        responses {
            HttpStatusCode.OK {
                description = "Widget payload"
            }
        }
    }
}
```

## 제외 범위

- 전역 Ktor plugin 설치 없음.
- 숨겨진 route 생성 없음.
- Swagger Codegen 의존성 없음.
- Ktor 공식 OpenAPI/Swagger UI plugin 대체 없음.
