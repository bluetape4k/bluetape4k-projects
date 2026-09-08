# measured 및 인프라 변경의 PR 전달

## 대상과 승인 범위

사용자의 다음 단계 진행 지시에 따라 기존 Type A/B 구현을 PR과 CI 단계로 전달한다.

| 저장소 | base | head | 산출물 |
| --- | --- | --- | --- |
| bluetape4k-dependencies | develop | feat/projects-infra-sdks | [PR #248](https://github.com/bluetape4k/bluetape4k-dependencies/pull/248) |
| bluetape4k-projects | develop | feat/measured-infra-1712-1717 | #1712–#1717 구현 PR |

병합은 CI와 최신 head의 준비 상태를 사용자에게 보고한 후 별도 승인을 받는다. tag·Maven 발행은 포함하지 않는다.

## 변경과 근거

- projects를 최신 `origin/develop`에 재배치했다. 기준은 `083417e574`이며 충돌은 없었다. 원격 구현 커밋 `f78ce75cc763747448a75700fab005644415d755`는 중앙 publisher 검증의 고정 입력이다.
- 기본 catalog ref를 중앙 `f16b29a0da64481c19443f76476e8166dbc57618`로 변경했다. 로컬 경로 override 없이 원격 파일을 읽고 SHA-256 sidecar를 검증한다.
- catalog 파일 SHA-256은 `209b15c9d8a8522a53e8f0b7cdda1b94b6df750203a5db40d31b52fcc495be92`다.
- 중앙 CI manifest의 정확한 8개 tree로 187 aliases/8 sub-BOM을 생성했다. 이전 로컬 기록의 188개에는 별도 Exposed checkout의 tenant-jdbc가 포함됐으므로 원격 CI 기준에서는 제외했다. 신규 인프라 3개 alias는 유지된다.
- 중앙 publisher POM 생성기는 공식 `BLUETAPE4K_DEPENDENCIES_CATALOG_PATH` override를 주입하므로, 고정된 구현 커밋의 이전 기본 catalog ref와 무관하게 현재 producer를 검증한다.

## 검증과 리뷰

구현 검증은 [기존 기록](2026-09-08-measured-infra-implementation-review.md)에 있다. 새로운 source 변경은 settings의 catalog SHA 1개이며, 기존 README와 시각 자료의 구현 계약은 유지된다.

- 중앙 관련 테스트 74개, Gradle build 및 BOM POM 생성, 생성 카탈로그 검사가 통과했다.
- 독립 검토에서 publisher SHA의 원격 공개·develop 조상 관계·canonical signing source 일치·POM helper override 호환성을 확인했다. 추가 P0/P1 없음.
- 프로젝트 네 모듈은 재배치 및 원격 기본 catalog 연결 이후 253개 테스트를 다시 통과했다. measured 204, OpenFGA 18, Qdrant 15, Temporal 16이며 실패·오류·제외 0이다. 세 모듈의 POM 생성도 통과했다.
- 한국어 문서 SPW-01–05: 유지보수자 대상의 전달 기록으로 범위를 고정하고, 실제 Git·manifest·카탈로그·테스트 증거와 대조했다. 원격 검사와 병합의 미완료 상태를 구분한다.

## DoD Status

- [x] 기존 작업 보존, 최신 develop 반영, 원격 구현 SHA 확인
- [x] 중앙 CI 검사 SHA·생성 모듈 목록 정합성 및 중앙 로컬 검사
- [x] 중앙 PR 생성과 제목·본문·라벨·milestone·담당자 read-back
- [x] 원격 기본 카탈로그를 사용하는 projects 테스트 재검증
- [ ] 두 PR의 exact-head CI·리뷰·메타데이터 최종 검증
- [ ] 최신 head 병합 승인·병합·동기화

상태: PENDING — 원격 전달 검증 진행 중.
