## Scope

- 대상: `infra/lettuce`, `infra/cache-lettuce`
- 목표: 코드리뷰 결과를 반영해 안정성 결함을 수정하고, 회귀 테스트/KDoc/README를 현재 계약에 맞게 갱신한다.

## Confirmed Findings

- `LettuceCache`의 `putIfAbsent`는 TTL 설정이 있는 캐시에서도 만료를 적용하지 않는다.
- `LettuceSuspendCache`는 `putAll`, `putIfAbsent`, `replace` 계열 일부에서 TTL을 적용하지 않아 동일 캐시의 쓰기 경로별 계약이 불일치한다.
- `LettuceCachingProviderTest`의 동일 인스턴스 검증 assertion이 약하다.
- JCache 관련 README/KDoc은 TTL 동작과 최신 Maven coordinates를 충분히 설명하지 않는다.

## Required Outcomes

- sync/suspend JCache write 경로에서 TTL 계약이 일관되게 적용된다.
- 위 계약을 검증하는 회귀 테스트가 추가된다.
- JCache 관련 공개 API 문서와 README가 TTL/의존성/사용 예시 기준으로 최신화된다.
