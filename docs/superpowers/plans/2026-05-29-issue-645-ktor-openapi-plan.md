# Issue 645 Ktor OpenAPI 계획

## 단계

1. 관리되는 Ktor 버전을 사용해 Ktor OpenAPI, routing OpenAPI, Swagger UI alias를
   추가한다.
2. Ktor 공식 route helper를 명시적으로 감싸는 `ktor/openapi`를 추가한다.
3. health/readiness와 도메인 route 하나를 포함하는 정적 OpenAPI 문서를 테스트한다.
4. 모듈 README, 루트 README locale 세트, CI, Nightly, lesson을 갱신한다.
5. 모듈 컴파일, 모듈 테스트, Kover XML, actionlint, diff 위생 검사로 검증한다.

## 수용 기준 매핑

- 의존성 선택: 설계 문서에 기록한다.
- 생성/제공 결과: 정적 spec을 사용하는 `/openapi` 및 `/swagger` 테스트로 검증한다.
- 명시적 메타데이터: Ktor runtime `.describe {}` 지침으로 문서화한다.
- README locale 세트: `README.md`와 `README.ko.md`를 갱신한다.
