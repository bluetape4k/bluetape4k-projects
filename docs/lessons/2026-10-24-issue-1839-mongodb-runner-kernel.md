# Issue #1839: MongoDB 통합 테스트의 runner kernel 호환성

## 원인과 완료 조건

[run `39408775102`](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/39408775102)의 `Test / Spring Boot (batch-nosql)`은 `mongo:8.0.28` 시작 중 Linux kernel `6.19+` 비호환 fatal 로그를 남겼다. run log는 Ubuntu 24.04 runner image `20261018.339.1`을 기록하지만 `uname -r` 출력은 없고, 보관된 테스트 artifact도 실행 kernel을 기록하지 않는다. 따라서 이 실행에서 사용된 정확한 kernel은 확인되지 않았다. [MongoDB 8.0 릴리스 노트](https://www.mongodb.com/docs/manual/release-notes/8.0/)는 Linux kernel `6.19`부터 `7.0.13`까지의 비호환 범위를 설명한다. 장애는 제품 assertion이나 dependency catalog가 아니라 runner kernel 호환성과 연결되며, 변경 후에는 exact-head CI에서 실제 kernel을 기록해야 한다.

임시 [PR #1838](https://github.com/bluetape4k/bluetape4k-projects/pull/1838)은 두 테스트 job을 `ubuntu-22.04`로 고정했다. 이 runner는 지원 종료가 예정되어 있어 Issue #1839는 지원되는 runner/kernel 조합으로 옮기고, PR CI와 Nightly에서 실제 이미지·kernel 출력 및 MongoDB 테스트 성공을 확인해야 한다.

## Workflow 결정

PR CI의 Test/Data는 MongoDB와 다른 데이터 통합 테스트가 한 job에 묶여 있어 `ubuntu-24.04`를 사용한다. Spring Boot CI는 `batch-nosql` matrix에서만 그 runner를 고른다. Nightly에서는 MongoDB가 포함된 Test/Data `nosql` group과 Spring Boot `batch-nosql` group만 `ubuntu-24.04`를 사용하며, 다른 Data group은 `ubuntu-latest`를 유지한다.

이번 변경은 PR CI의 Test/Data와 Spring Boot `batch-nosql`, Nightly의 Data `nosql`, Spring Boot `batch-nosql`, Testcontainers `storage-cache`, image-gate shard까지 여섯 실행 경로를 다룬다. 각 MongoDB 실행 경로는 테스트 전에 `ImageOS`, `ImageVersion`, `/etc/os-release`, `uname -r`를 기록한다. Ubuntu 24.04 runner image 사전 릴리스 [`20261023.334`](https://github.com/actions/runner-images/releases/tag/ubuntu24/20261023.334)는 `20261023.334.3` image version과 `6.17.0-1022-azure` kernel을 보고했지만, 후보 이미지 metadata는 실제 hosted job 출력의 대체 증거가 아니다.

Nightly의 Testcontainers `storage-cache` group은 `MongoDBServerTest`를 실행하므로 해당 matrix 항목만 `ubuntu-24.04`를 사용한다. 이 group은 `full` 및 `testcontainers` scope에서 선택되며 test step 전에 runner/image/kernel 진단을 수행한다. image-gate의 `mongo-d-b` 항목도 같은 test를 실행하고 job은 이미 `ubuntu-24.04`를 사용하므로, 여기에 진단을 추가해 실제 host kernel까지 출력한다. workflow verifier는 group-to-test-pattern, runner, 진단 순서와 실패 전파, manifest의 MongoDB family를 함께 고정한다.

## Verifier 회귀와 재발 방지

초기 독립 리뷰는 folded YAML의 빈 줄 처리, 필수 테스트 step의 실행/실패 가드, escaped YAML mapping key, `nick-fields/retry@v4`의 nested action inputs를 통해 MongoDB 테스트 실패를 숨기거나 테스트를 건너뛸 수 있음을 찾았다. 네 CI/Nightly test 경로의 task, matrix, runner, 실행 조건, 실패 전달 및 retry 입력을 고정하는 회귀 검사를 추가했다. 후속 리뷰는 보호된 workflow/job/step `env`에 `BASH_ENV`, `NODE_OPTIONS`, `PATH` 등을 주입해 테스트 실패를 성공으로 바꿀 수 있음을 확인했다. 보호 실행 범위의 위험 환경 변수를 거부하고, MongoDB test step의 `GRADLE_OPTS`와 `TEST_TASKS` 값 및 허용 키를 고정한다. Nightly workflow/job의 custom `defaults.run.shell`이 필수 runner 진단을 빈 성공 실행으로 대체할 수도 있어 해당 기본값을 거부하고 진단 step에 `shell: bash`를 명시한다. 검증기에는 실제 runner/kernel 성공을 증명하는 능력이 없으므로 static contract 성공과 hosted 실행 증거를 분리한다.

후속 리뷰는 runner diagnostics 자체도 우회할 수 있음을 추가로 찾았다. `shell` 재정의가 생성된 script를 건너뛰거나 `continue-on-error: true`가 진단 실패를 숨기고, 선행 `exit 0`이 필수 출력을 실행하지 않도록 할 수 있었다. 진단 script의 네 개 명령과 순서를 고정하고, custom shell 및 `continue-on-error`를 금지한다.

마지막 code review는 matrix `test_tasks`에 `-x`, `--exclude-task`, `--dry-run` 같은 Gradle 옵션을 추가해도 검증기가 허용하는 P1 우회를 찾았다. 각 대상 matrix의 값이 Gradle task path만 포함하는지 검사하고, 세 job/group 조합에 우회 옵션 아홉 가지를 주입하는 회귀 테스트를 추가했다.

최종 architecture review는 Nightly Testcontainers job 전체에 `if: false`를 추가하거나 image-gate matrix에서 shard 3을 제거해도 verifier가 통과하는 P1 우회를 찾았다. verifier는 이제 `test-testcontainers` job의 조건부 생략을 거부하고 image-gate matrix가 정확히 `[0, 1, 2, 3]`인지 고정한다. `test_nightly_storage_cache_job_cannot_be_conditionally_skipped`와 `test_nightly_image_gate_requires_all_four_shards`는 두 변형이 수정 전 허용되고 수정 후 거부되는지 검증한다.

후속 architecture review는 plan Python heredoc 앞의 shell이 `python3()` 함수를 정의해도 AST 검증이 그대로 통과하고, status step의 사용자 지정 shell이 실패를 삼킬 수 있음을 찾았다. 검증기는 이제 matrix 명령 앞의 shell prelude 전체를 고정하고, `Check all jobs`는 직접 `bash`로 실행하도록 workflow와 verifier에서 요구한다. 회귀 검사는 `python3()` shadow와 `bash {0}; exit 0` wrapper가 각각 허용되지 않는지 확인한다.

후속 code review는 plan job이 shell에서 만든 `run_smoke`, `run_standard`, `run_testcontainers_coverage` 값을 다른 output에 연결해도 검증기가 통과하고, Nightly status가 필수 job의 `skipped`를 실패로 다루지 않으며, image-gate aggregate가 custom shell로 shard 실패를 감출 수 있음을 찾았다. 검증기는 plan의 다섯 output 연결과 status의 전체 16개 dependency 목록, smoke·standard·Testcontainers bridge의 실행 조건을 고정한다. Full scope는 전체 직접 dependency가 성공해야 하고, smoke는 Detekt와 Core, `full`·`testcontainers`는 Spring bridge 성공도 요구한다. Aggregate step은 `shell: bash`를 명시하고 verifier도 이를 고정한다. 회귀 테스트는 다섯 output 오배선, scope별 job skip과 결과, full-scope `skipped`, bridge skip, aggregate shell wrapper를 검사한다.

후속 code review는 image-gate aggregate가 shard 실패 결과나 aggregation 종료 코드를 무시해도 verifier가 이를 막지 못하고, plan Python 주석에 MongoDB pattern이나 scope 표현을 남겨 실제 matrix에서 MongoDB 실행을 뺄 수 있음을 찾았다. verifier는 plan의 Python heredoc을 AST로 읽어 `storage-cache`의 실제 test pattern, `full`·`testcontainers` scope 선택, matrix 출력 연결을 확인한다. Aggregate job의 scope guard, shard 의존성, `always()` 실행, shard 결과 및 aggregation 종료 코드 검사를 고정하고 실패 마스킹 환경 변수도 거부한다. `test_rejects_comments_that_decoy_storage_cache_plan_contract`는 pattern과 scope 주석 decoy를, `test_image_gate_aggregate_cannot_mask_missing_or_failed_shards`는 종료 코드·조건부 생략·실패 마스킹 변형을 검사한다.

최종 code review는 AST 검사에서 확인된 `selected` 할당 뒤 `discard`, `clear`, MongoDB group 제거 호출을 추가하면 실제 출력 matrix에서 `storage-cache`가 빠질 수 있음을 찾았다. verifier는 Python plan의 최상위 문장 종류·순서와 `matrix_json` 할당부터 `GITHUB_OUTPUT` 기록까지의 shell 연결을 고정하고, plan job output과 Testcontainers job matrix가 그 값을 그대로 쓰는지도 확인한다. `test_rejects_mutations_after_validated_testcontainers_plan_assignments`는 세 AST 변형을, `test_testcontainers_plan_matrix_output_is_connected_to_the_job`는 hardcoded/overwritten matrix, output 연결, job·step 생략 또는 오류 허용, shell 오류 무시를 포함한 10개 변형을 검사한다. Architecture review는 `nightly-status`가 plan/Testcontainers job을 건너뛰거나 결과를 잘못 연결해도 넘어갈 수 있음을 추가로 지적했다. 이제 모든 지원 scope에서 plan 성공과 요청 scope 일치, Testcontainers 성공을 요구하며, full scope에서는 Data·Spring Docker·image-gate 성공도 요구한다. `test_nightly_status_requires_testcontainers_success_for_its_scope`는 needs·env·job/step 실행·결과 조건을 약화하는 22개 변형을 거부한다.

후속 code review는 Nightly `plan`과 `nightly-status` job의 위험 환경 변수가 보호 목록에서 빠졌고 workflow `env`의 YAML alias/merge가 원시 key 검사를 우회할 수 있음을 P1로 지적했다. 검증기는 두 job을 위험 환경 검사에 포함하고, plan job·step에는 환경 재정의를 금지한다. `nightly-status` job에는 job-level 환경을 허용하지 않으며 `Check all jobs` step은 결과 검사에 필요한 정확한 env key/value만 받는다. Nightly 전체의 환경 mapping alias와 merge도 거부한다. 회귀 검사는 `BASH_ENV`, `PATH`, `PYTHONPATH` 여섯 job 변형과 plan alias, status merge를 확인한다.

후속 architecture review는 Testcontainers Spring bridge job이 성공해도 실제 테스트 step이 생략되거나 실패를 감추면 status가 이를 성공으로 볼 수 있음을 P1로 지적했다. verifier는 `Test Testcontainers Spring bridge` step의 직접 `bash` 실행, exact Gradle command, `GRADLE_OPTS`만 허용하는 env mapping, `if`·`continue-on-error` 부재와 job-level 오류 전달을 고정한다. 같은 architecture review는 비-full scope에서 `coverage-report`가 `skipped`여도 Nightly가 통과할 수 있음을 P2로 지적했다. 이 job은 모든 scope에서 `always()`로 실행하고 status가 성공을 요구한다. `test-io-http`는 `run_standard=false`인 비-full scope에서 의도적으로 생략되며, full scope는 모든 직접 dependency의 성공을 요구한다. 회귀 테스트는 skipped coverage report를 실패로 판정하고 full-scope skipped 결과를 거부한다.

후속 code review는 image-gate shard의 실제 실행 step이 기본 shell에 의존하고 임의 env를 받을 수 있음을 P2로 지적했다. workflow와 verifier는 직접 `bash` 실행, 정확한 `DOCKER_AUTH_CONFIG` 값, 전체 command를 고정한다. custom shell과 추가 env mutation은 더는 허용되지 않는다. 별도 P3 지적에 따라 image-gate 실행 오류 회귀 테스트도 진단 문자열의 부분 포함을 확인하도록 고쳤다. Aggregate 테스트의 다섯 step mutation 외에 job-level 생략 mutation 하나를 더해 총 여섯 변형을 검사한다.

Nightly Data workflow의 `rdb` group까지 runner를 바꾸지 않도록 지원 runner pin과 진단을 MongoDB가 속한 `nosql` group에 한정한다. 이 범위 축소는 CI Test/Data에는 그대로 적용할 수 없다. 해당 CI job은 matrix 분리 없이 MongoDB와 다른 데이터 suite를 함께 실행한다.

이 lesson의 canonical GNO 색인은 merge 뒤 canonical integration checkout을 동기화한 다음 CG-18에서 수행한다. feature worktree의 색인을 canonical 증거로 사용하지 않는다.

## 검증 상태

이전 후보의 workflow contract suite는 101개가 통과했다. 후속 독립 리뷰에서 Nightly Testcontainers `storage-cache`와 image-gate 경로가 빠진 P1을 찾았다. 이 두 경로의 runner·진단·실패 전파, `MongoDBServerTest` pattern과 manifest 연결을 고정하는 회귀 테스트는 보완 전 기존 verifier가 우회 변형을 허용하는 RED를 확인했고, 수정 뒤 통과했다. 문서의 정확한 kernel 표현과 2026-10-20 당시 PR head의 날짜 누락도 writer review에서 찾아 정정했다.

이전 후보의 `test_catalog_workflows.py` suite 124개 통과 기록은 후속 변경 전 결과다. 최신 source patch는 base `b0b1720220e7f2e3432683e75082538b2602f4fa` 기준으로 `.github/workflows/ci.yml`, `.github/workflows/nightly-tests.yml`, `scripts/verify-catalog-workflows.py`, `tests/test_catalog_workflows.py`를 SHA-256 `81540fcf51fde60c8be1e567ce8a44e3ec66ae5ca0273ca66a698d720b9fb4bc`로 고정한다. 이 후보의 최종 suite와 exact-hash 리뷰 결과는 test log의 뒤따르는 행에 기록한다. Hosted exact-head CI, 실제 runner kernel, MongoDB/Testcontainers 성공 증거는 아직 수집 전이다.

변경된 exact PR head의 `Test / Data`와 `Test / Spring Boot (batch-nosql)` 결과에서 MongoDB 테스트, image/kernel 출력, runner 실제 상태를 확인해야 한다. Nightly 실행은 별도 후속 실행이며, 실행하지 않았다면 완료 증거로 표시하지 않는다.

2026-10-24 exact source patch의 native code, architecture, writer review lane은 보고서를 내기 전에 각각 tool-level `usage limit` 오류로 종료됐다. 이 오류는 사용자 계정 전체의 사용량 상태를 증명하지 않는다. main session은 독립 attestation으로 표시하지 않는 inline fallback review를 남겼다. 코드 검토는 P0=0/P1=0으로 통과했고, architecture 검토는 범위와 runner 배치를 확인했으나 실제 hosted kernel과 MongoDB 테스트 결과가 올 때까지 WATCH다. writer 검토는 lesson과 testlog의 사실·용어·검증 경계를 read-back했다. 자세한 범위와 판단은 [inline fallback review](../reviews/2026-10-24-issue-1839-inline-fallback-review.md)에 기록한다.

## 유지보수 경계

MongoDB/Testcontainers 계약은 현재 catalog-governance 단계에서 호출하는 Projects workflow verifier 안에서 검증한다. verifier는 관련 CI와 Nightly 경로 여섯 개 및 image-gate manifest를 명시적으로 검사한다. 별도 reviewer는 이 MongoDB 정책을 전용 verifier로 분리하자는 유지보수 제안을 남겼다. 이 제안은 현재 수용 조건을 만족하는 데 필요하진 않지만, 정책 검증을 다른 workflow에서도 재사용하게 되면 분리를 검토한다. 텍스트 기반 검사는 hosted runner 상태를 증명하지 않으므로 실제 kernel 출력과 테스트 결과는 실행 artifact로 확인한다.
