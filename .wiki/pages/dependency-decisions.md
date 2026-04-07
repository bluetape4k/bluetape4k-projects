# 의존성 버전 결정 이력

> 마지막 업데이트: 2026-04-07 | 소스: git log

## 개요

버전 다운그레이드, 충돌 해결, 의존성 제거 등 의존성 관련 주요 결정 이력.
새로운 버전 문제 발생 시 여기서 선례를 먼저 확인한다.

## 핵심 설계 결정 (ADR)

| 라이브러리 | 결정 | 버전 변경 | 이유 | 날짜 | 커밋 |
|-----------|------|---------|------|------|------|
| lettuce | 다운그레이드 | 7.5.0 → 6.8.2 | NearCache RESP3 CLIENT TRACKING 호환성 문제 | 2026-03-28 | 8ad00ab94 |
| httpclient5 (Apache) | 다운그레이드 | — | Apache HttpComponents 의존성 충돌 | 2026-04-06 | 7e264f368 |
| hibernate-lettuce | 충돌 수정 | — | Spring Boot 3/4 classpath 충돌 | 2026-03-29 | acec909ec |
| lettuce HyperLogLog / BloomFilter / CuckooFilter | Revert | — | 기능 구현 롤백 | 2026-03-27 | 84a5bc1e4 |
| aws-kotlin / aws-smithy-kotlin | 다운그레이드 | — | 호환성 문제 | 2026-03-20 | 1775790b0 |
| aws-kotlin HTTP 모듈 | 변경 | okhttp3 → crt | okhttp3 버전 충돌 | 2024-12-05 | ed7583b9b |
| rest-assured | 다운그레이드 | — | Jackson 버전 충돌 | 2026-03-04 | 9edeb194f |
| Jackson | 다운그레이드 | — | 버전 충돌 | 2026-01-27 | 5f25ae9e5 |
| jakarta_persistence_api | 다운그레이드 | 3.2.0 → 3.1.0 | 호환성 문제 | 2025-09-24 | e8381950a |
| Gradle | 다운그레이드 | 9.1.0 → 8.14.3 | 안정성/호환성 문제 | 2025-09-21 | 9009b2de5 |
| Exposed | 업그레이드 | → 0.61.0 | 버전 픽스 | 2025-04-17 | b247e9457 |
| hibernate (H2) | 충돌 수정 | — | 테스트 H2 설정 + 엔티티 충돌 해결 | 2026-03-17 | 2586a3e2b |
| 의존성 다수 | 다운그레이드 | — | 전반적 호환성 조정 | 2025-10~11 | 3e3e294b7 b25cb5e4b e17a60f92 |

## 패턴 & 교훈

- **lettuce 7.x 사용 금지**: NearCache (RESP3 CLIENT TRACKING) 비호환 — lettuce 6.8.x 고정
- **rest-assured**: Jackson 버전을 프로젝트 BOM과 동일하게 맞춰야 함
- **Spring Boot 3/4 동시 지원**: classpath 격리 필수, `compileOnly` 범위 사용
- **aws-kotlin**: 버전 업그레이드 시 aws-smithy-kotlin 버전도 함께 확인; HTTP 엔진은 CRT 권장 (okhttp3 충돌 이력)
- **Gradle 최신 버전**: 안정화 전까지 최신 버전 미사용, 검증된 버전 고정
- **jakarta.persistence**: 3.2.x는 호환성 문제 있음 — 3.1.x 사용

## 선택하지 않은 방식 / 트레이드오프

- **lettuce 최신 버전 유지**: NearCache 기능 포기 시 가능하나 cache-lettuce 모듈 핵심 기능 손실
- **Jackson BOM 관리**: Spring Boot BOM이 Jackson 버전을 관리하므로 명시적 재정의 최소화
- **okhttp3 유지**: aws-kotlin HTTP 엔진으로 CRT로 전환하여 okhttp3 의존성 제거

## 관련 페이지

- [[cache-architecture]] — lettuce 다운그레이드 배경 상세
- [[spring-boot-integration]] — SB3/SB4 classpath 충돌 맥락
