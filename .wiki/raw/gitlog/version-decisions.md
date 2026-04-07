# Git Log: 버전 결정 이력

git log에서 추출한 버전 다운그레이드/충돌/revert 관련 커밋 목록.
/wiki-update 스킬이 dependency-decisions.md 갱신 시 이 파일을 참조한다.

추출 날짜: 2026-04-07
명령: git log --format="%h %as %s" | grep -iE 'downgrad|revert|fix.*version|버전.*다운|다운그레이드|충돌|classpath'

---

7e264f368 2026-04-06 chore: Apache HttpComponents 의존성 추가 및 httpclient5 버전 다운그레이드
acec909ec 2026-03-29 fix(spring-boot4): hibernate-lettuce 통합 테스트 SB3/SB4 classpath 충돌 수정
8ad00ab94 2026-03-28 ### feat(cache-lettuce): lettuce downgrade (7.5.0 -> 6.8.2)
84a5bc1e4 2026-03-27 Revert "feat(lettuce): HyperLogLog / BloomFilter / CuckooFilter 구현"
9ebdbdbaf 2026-03-20 chore: deprecated kotlin.incremental.useClasspathSnapshot 속성 제거
1775790b0 2026-03-20 build(deps): aws-kotlin 및 aws-smithy-kotlin 버전 다운그레이드
2586a3e2b 2026-03-17 fix: bluetape4k-hibernate 테스트 H2 설정 수정 및 엔티티 충돌 해결
9edeb194f 2026-03-04 fix: rest-assured 라이브러리 버전 다운그래이드 (Jackson 버전 때문)
5f25ae9e5 2026-01-27 chore: Jackson 라이브러리 버전 다운그레이드
3e3e294b7 2025-11-12 feat: 의존성 버전 다운그레이드
b25cb5e4b 2025-10-29 feat: 의존성 버전 다운그레이드
e17a60f92 2025-10-28 feat: 의존성 버전 다운그레이드
e8381950a 2025-09-24 chore: jakarta_persistence_api 제거 및 버전 다운그레이드 (3.2.0 → 3.1.0)
9009b2de5 2025-09-21 build(deps): Gradle 버전 9.1.0 -> 8.14.3로 다운그레이드
b247e9457 2025-04-17 fix: update Exposed library version to 0.61.0
5eeaff96e 2025-02-22 bug: OllamaServer 의 getPort 충돌 해결, PulsarServer, ZipkinServer 는 실행 안됨.
ed7583b9b 2024-12-05 fix: aws-kotlin 의 기본 http 통신 모듈을 okhttp3 에서 crt 로 변경 (okhttp3 버전 충돌 때문)
