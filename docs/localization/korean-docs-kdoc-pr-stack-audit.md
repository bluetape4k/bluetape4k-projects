# Korean docs and KDoc PR stack audit

Issue: #1109
Date: 2026-07-28

## 범위

이 문서는 milestone `1.12.0`의 한국어 문서/KDoc localization Epic에 대해 현재 생성된 PR stack과 guardrail 상태를 기록한다.
GitHub issue/PR 제목과 본문은 repository 정책에 따라 영어로 유지했고, README와 LLM-facing operating 문서는 rewrite scope에서 제외했다.
`docs/manual/en` 및 `docs/manual/ko` bilingual manual pair는 rewrite primary scope가 아니라 parity 검증 대상으로만 유지했다.

## PR stack

| Issue | PR | Branch | Base | Status |
|---|---:|---|---|---|
| #1093 | #1110 | `docs/issue-1093-localization-inventory` | `develop` | Open |
| #1094 | #1111 | `docs/issue-1094-localization-guardrails` | `docs/issue-1093-localization-inventory` | Open |
| #1095 | #1112 | `docs/issue-1095-top-level-korean-docs` | `docs/issue-1094-localization-guardrails` | Open |
| #1096 | #1113 | `docs/issue-1096-lessons-batch-1` | `docs/issue-1095-top-level-korean-docs` | Open |
| #1096 | #1115 | `docs/issue-1096-lessons-batch-2` | `docs/issue-1096-lessons-batch-1` | Open |
| #1096 | #1116 | `docs/issue-1096-lessons-batch-3` | `docs/issue-1096-lessons-batch-2` | Open |
| #1100 | #1117 | `docs/issue-1100-manual-parity-audit` | `docs/issue-1096-lessons-batch-3` | Open |
| #1099 | #1118 | `docs/issue-1099-benchmark-notes` | `docs/issue-1100-manual-parity-audit` | Open |
| #1098 | #1119 | `docs/issue-1098-process-security-notes` | `docs/issue-1099-benchmark-notes` | Open |
| #1097 | #1120 | `docs/issue-1097-superpowers-checklists-batch` | `docs/issue-1098-process-security-notes` | Open |
| #1101 | #1121 | `kdoc/issue-1101-core-coroutines-logging-batch` | `docs/issue-1097-superpowers-checklists-batch` | Open |
| #1102 | #1122 | `kdoc/issue-1102-io-http-kdoc-batch` | `kdoc/issue-1101-core-coroutines-logging-batch` | Open |
| #1103 | #1123 | `kdoc/issue-1103-data-kdoc-batch` | `kdoc/issue-1102-io-http-kdoc-batch` | Open |
| #1104 | #1124 | `kdoc/issue-1104-infra-cache-kdoc-batch` | `kdoc/issue-1103-data-kdoc-batch` | Open |
| #1105 | #1125 | `kdoc/issue-1105-frameworks-kdoc-batch` | `kdoc/issue-1104-infra-cache-kdoc-batch` | Open |
| #1106 | #1126 | `kdoc/issue-1106-testing-kdoc-batch` | `kdoc/issue-1105-frameworks-kdoc-batch` | Open |
| #1107 | #1127 | `kdoc/issue-1107-utils-virtualthread-kdoc-batch` | `kdoc/issue-1106-testing-kdoc-batch` | Open |
| #1108 | #1128 | `kdoc/issue-1108-examples-kdoc-batch` | `kdoc/issue-1107-utils-virtualthread-kdoc-batch` | Open |
| #1109 | #1129 | `docs/issue-1109-localization-pr-stack-audit` | `kdoc/issue-1108-examples-kdoc-batch` | Open |

## 세부 실행 이슈 분해

큰 umbrella issue는 다음의 review 가능한 batch issue로 다시 분해했다. 각 issue는 GitHub 정책에 따라 title/body를 영어로 유지한다.

| Parent | Batch issues |
|---|---|
| #1096 | #1130, #1131, #1132, #1133, #1134, #1135, #1136, #1137, #1138 |
| #1097 | #1139, #1140, #1141, #1142, #1143, #1144, #1145, #1146, #1147, #1148, #1149, #1150 |
| #1098 | #1151, #1152, #1153, #1154, #1155, #1156 |
| #1101 | #1157, #1158, #1159 |
| #1102 | #1160, #1161, #1162 |
| #1103 | #1163, #1164, #1165 |
| #1104 | #1166, #1167, #1168 |
| #1105 | #1169, #1170, #1171 |
| #1106 | #1172, #1173, #1174 |
| #1107 | #1175, #1176, #1177 |
| #1108 | #1178, #1179, #1180 |

## Guardrail evidence

`python3 scripts/docs-localization-inventory.py --check`:

```text
Korean localization guardrail
- manual EN missing KO: 0
- manual KO missing EN: 0
- English-KDoc policy drift: 0
```

`python3 scripts/docs-localization-inventory.py` at this stack head:

```text
Git-tracked files scanned: 7426
In-scope single-language docs: 729
Bilingual manual parity-only docs: 508
Excluded docs: 208
Kotlin/KTS files for KDoc follow-up: 4492
KDoc blocks found in Kotlin/KTS files: 34356
Manual EN files missing KO pair: 0
Manual KO files missing EN pair: 0
English-KDoc policy drift findings: 0
```

## 남은 위험

이 stack은 모든 subissue에 대해 PR을 열고 검증 가능한 review lane을 만든 상태다. 다만 #1096, #1097, #1098, #1101-#1108은 scope가 매우 커서 첫 batch 또는 여러 batch로 열려 있으며, 각 issue의 전체 rewrite 완료는 후속 batch와 merge 후 최종 재감사가 필요하다.
