## Implementation Plan

1. `infra/lettuce`에 hash key TTL을 적용할 수 있는 보조 API를 추가한다.
2. `infra/cache-lettuce`의 sync/suspend JCache 구현에서 TTL 누락 경로를 수정한다.
3. `LettuceCacheTest`, `LettuceSuspendCacheTest`, `LettuceCachingProviderTest`에 회귀 테스트를 추가/강화한다.
4. JCache 관련 KDoc과 `infra/lettuce/README.md`, `infra/cache-lettuce/README.md`를 현재 계약에 맞게 갱신한다.
5. 대상 모듈 테스트를 실행하고 실패가 있으면 수정 후 재검증한다.
