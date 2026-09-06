# gitleaks 전체 이력 오탐은 원본 커밋과 경로를 함께 제한한다

## 맥락

정기 `Security` workflow run
[`34059409824`](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/34059409824)는
`develop`의 `f441286e72b0d218012bdd5d9ed0fecf972e7162`에서 `Secret Scan (gitleaks)`만
실패했다. 같은 run의 OWASP Dependency Check는 통과했다.

workflow는 다음 명령으로 현재 파일뿐 아니라 Git 전체 이력을 검사한다.

```bash
gitleaks detect --source . --redact --config .gitleaks.toml
```

`gitleaks v8.30.1`은 커밋 `df56930b8dde59c02d38dc15d75d491c18604d70`의
`LettuceMultiKeyLeasePerformanceTest.kt`에서 성능 메트릭 식별자를
`generic-api-key`로 판정했다. 후속 커밋이 현재 파일의 식별자를 변경해 working tree 검사는
통과했지만, 과거 커밋은 그대로이므로 전체 이력 검사는 계속 실패했다. 실제 credential 값이나
운영 secret 노출은 없었다.

## 결정

기본 `generic-api-key` rule은 유지하고, rule 전용 allowlist에 원본 커밋과 정확한 파일 경로를
`AND` 조건으로 등록한다. 경로 전체를 허용하거나 메트릭 이름을 stopword로 추가하지 않는다.
이 제한은 같은 rule이 다른 커밋이나 파일에서 검출한 값을 계속 차단한다.

## 결과

전체 이력 검사는 알려진 한 건만 제외한다. 현재 source를 고치는 방식과 달리, immutable Git
history를 검사하는 scheduled workflow의 입력을 직접 다룬다. 설정은 공개 이력을 재작성하지
않고도 기존 secret 탐지 범위를 보존한다.

## 검증

- 실패 run과 동일한 `gitleaks v8.30.1` 및 현재 GitHub remote refs를 받은 깨끗한 clone에서
  `gitleaks detect --source . --redact --config .gitleaks.toml`을 실행했다. 4,187개 커밋을
  검사했고 exit 0과 `no leaks found`를 확인했다.
- `df56930b8dde59c02d38dc15d75d491c18604d70`만 검사했을 때도 exit 0이었다.
- 동일한 파일 경로의 별도 합성 커밋에 fake API key를 넣은 negative control은
  `generic-api-key` 한 건과 exit 1을 반환했다. rule 전체나 경로 전체가 허용되지 않았다.
- 커밋 후보 tracked tree를 별도 디렉터리에 풀어 검사해 exit 0과 `no leaks found`를 확인했다.

## 놓친 점

현재 source의 식별자 변경만으로 full-history scanner가 복구된다고 가정했다. working tree와 Git
history는 서로 다른 검사 입력이다. scheduled workflow가 `--no-git` 없이 실행된다면 현재 파일의
GREEN만으로는 회귀 수정을 증명하지 못한다.

## 향후 지침

1. gitleaks 실패를 조사할 때 workflow 명령, scanner 버전, `headSha`, 검출 커밋의 ancestor 여부를
   먼저 고정한다.
2. 전체 이력 오탐은 가능하면 `rule id + commit + path`의 교집합으로 제한한다.
3. allowlist 변경 뒤에는 원래 오탐의 GREEN과 범위 밖 실제 탐지의 RED를 함께 확인한다.
4. current-tree 검사 결과를 full-history 검사의 대체 증거로 사용하지 않는다.
