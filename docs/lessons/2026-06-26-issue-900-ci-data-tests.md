# 교훈: CI data test (2026-06-26)

관련 이슈: #900
대상 workflow: `.github/workflows/ci.yml`

## L1: path filter는 대응되는 test job과 summary needs까지 연결해야 한다

### 문제

CI workflow는 build job에서 data module을 compile했지만 `data` path-filter output과
`Test / Data` job이 없었다. 따라서 `data/**` 직접 변경은 PR CI에서 data-module
test coverage 없이 merge될 수 있었다.

### 교훈

CI path group을 도입할 때는 전체 chain을 같은 변경에서 연결한다.
`changes.outputs`, paths-filter entry, test job, coverage aggregation `needs`,
최종 CI status `needs`가 모두 포함되어야 한다. Downstream `needs` entry 하나가
빠져도 새 test job은 summary gate에서 보이지 않을 수 있다.

### 향후 방지책

Workflow coverage fix는 syntax와 wiring을 모두 검증한다. `actionlint`를 실행하고,
escaped `${{ }}` quote를 grep하며, 새 job이 coverage와 CI status dependency에
나타나는지 확인한다.
