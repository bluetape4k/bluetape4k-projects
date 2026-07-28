# 한국어 문서화 Guardrail

Issue: #1094

## 목적

이 문서는 `#1093`에서 만든 inventory 기준을 검증 가능한 guardrail로 고정한다.
후속 번역 PR은 이 기준을 사용해 README, 운영 지침, GitHub 공개 메타데이터,
`docs/manual/en`/`docs/manual/ko` bilingual pair를 잘못 번역 범위에 넣지 않아야 한다.

## 검사 명령

```bash
python3 scripts/docs-localization-inventory.py --check
```

현재 검사 결과:

```text
Korean localization guardrail
- manual EN missing KO: 0
- manual KO missing EN: 0
- English-KDoc policy drift: 0
```

## 실패 조건

- `docs/manual/en`에 대응되는 `docs/manual/ko` 파일이 없으면 실패한다.
- `docs/manual/ko`에 대응되는 `docs/manual/en` 파일이 없으면 실패한다.
- tracked documentation scope 안에 `KDoc in English`, `English KDoc`,
  `Write KDoc in English` 같은 영어 KDoc 정책 문구가 다시 들어오면 실패한다.

## 제외 기준

- `README*` 파일은 이번 Epic의 primary rewrite 대상이 아니다.
- `AGENTS.md`, `CLAUDE.md`, `SKILL.md`, prompts, hooks, `.omx`, `.omc`,
  `.codex` 같은 LLM-facing 또는 generated operating surface는 영어 유지 대상이다.
- `CHANGELOG.md`, `SECURITY.md`, GitHub issue/PR 본문, release notes,
  pushed commit message는 공개 contributor metadata이므로 영어 유지 대상이다.
- `docs/manual/en`과 `docs/manual/ko`는 이미 bilingual pair로 관리되므로
  rewrite 대상이 아니라 parity 검증 대상이다.

## 후속 작업 규칙

- 후속 이슈는 `docs/localization/korean-docs-kdoc-inventory.md`의 bucket을 기준으로
  작고 reviewable한 PR로 나눈다.
- 번역 중 code identifier, API name, command, URL, exact error text, issue/PR number,
  benchmark number는 원문 그대로 보존한다.
- public/internal KDoc과 의미 있는 internal/data-class constructor property 설명은
  한국어로 작성한다.
