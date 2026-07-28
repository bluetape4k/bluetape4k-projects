# 이슈 829 Ktor OpenAPI metadata source

## 배경

`bluetape4k-ktor-openapi`는 static file, compiler-generated OpenAPI metadata, runtime
`.describe {}` metadata를 application-owned document source로 안내했다.

## 교훈

Ktor의 `openAPI(path, swaggerFile, block)`와 `swaggerUI(path, swaggerFile, block)`
overload는 caller block을 먼저 적용한 뒤 `source = OpenApiDocSource.File(swaggerFile)`을
강제로 설정한다. configuration block을 받는 wrapper helper가 caller-owned
`OpenApiDocSource` 값을 보존해야 한다면 Ktor의 `block`-only overload를 통해 delegate해야
한다.

## 가드

README 또는 KDoc이 Ktor routing-tree OpenAPI metadata를 지원한다고 말한다면, test는
OpenAPI endpoint와 Swagger UI specification endpoint 양쪽에서 `OpenApiDocSource.Routing`을
cover해야 한다. static YAML happy path만으로는 caller-owned `source` contract를 증명하지
못한다.

## 증거

- RED: `KtorOpenApiRoutesTest`가 routing metadata source test를 추가했고, 기존 wrapper는
  Ktor static-file overload가 source를 덮어써 6 tests 중 2 tests가 실패했다.
- GREEN: `./gradlew :bluetape4k-ktor-openapi:compileKotlin :bluetape4k-ktor-openapi:compileTestKotlin :bluetape4k-ktor-openapi:test --no-build-cache`
  가 6개 `KtorOpenApiRoutesTest` case로 통과했다.
- official Ktor docs와 local Ktor 3.5.0 source 모두 runtime metadata를
  `OpenApiDocSource.Routing`으로 보여준다.
