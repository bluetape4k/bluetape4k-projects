# 이슈 645 Ktor OpenAPI 지원

## 배경

Ktor OpenAPI support는 core, testing, observability, client, resilience slice 이후의
backlog follow-up이었다.

## 결정

작은 `bluetape4k-ktor-openapi` module을 통해 Ktor official OpenAPI와 Swagger UI
plugin을 사용한다. 이 module은 custom renderer generation을 소유하지 않으므로 Ktor
version-governed alias만 추가하고 Swagger Codegen은 피한다.

## 결과

application은 spec과 route metadata를 application-owned로 유지하면서 명시적인
`bluetape4kOpenApi()`와 `bluetape4kSwaggerUi()` route helper를 얻는다.

## 검증

merge 전에 module compile/test/Kover, workflow lint, diff hygiene를 실행한다.

## 향후 지침

여기에 generated route behavior를 추가하지 않는다. Ktor runtime annotation API가 바뀌면
official API가 안정적으로 남아 있는 곳에서만 documentation과 wrapper를 조정한다.
