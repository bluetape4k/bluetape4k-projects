# Module Documentation Drift Checklist

Module이 추가, rename, move, remove, split되거나 다른 repository로 승격될 때 이 checklist를
사용한다. 목표는 Gradle registration, README catalog, CI coverage, example, release
metadata를 같은 변경에서 동기화하는 것이다.

## 사용 시점

- 새 module 또는 demo directory.
- Module rename 또는 path move.
- Module removal 또는 external repository split.
- Backend-specific variant 추가 같은 module family change.
- Module registration으로 인한 public README, BOM, CI behavior change.

## 필수 갱신

### Gradle Registration

- Automatic registration이 module을 cover하지 않으면 `settings.gradle.kts` 또는 관련 include
  helper를 갱신한다.
- Gradle path와 published artifact name이 directory와 일치하는지 확인한다.
- Module이 publish 대상이면 BOM/catalog aggregation을 확인한다.

### README Locale Set

- Module `README.md`를 추가하거나 갱신한다.
- Module에 Korean README pair가 있으면 matching `README.ko.md`를 추가하거나 갱신하고, 새
  bluetape4k module이면 두 파일을 모두 만든다.
- 두 파일 모두 title 바로 아래에 language switch를 유지한다.
- 두 locale이 있으면 architecture, feature, usage, configuration, dependency, benchmark
  section을 동기화한다.
- Module이 public catalog에 영향을 주면 root `README.md`와 `README.ko.md`의 module catalog와
  diagram을 갱신한다.

### Repo And Agent Reference

- Module group list 또는 workflow rule이 바뀌면 repo-local `AGENTS.md`를 갱신한다.
- Module group map이 바뀌면 `.codex/references/module-groups.md`를 갱신한다.
- Context, decision, outcome, verification, future guidance를 담아
  `docs/lessons/YYYY-MM-DD-{slug}.md`를 추가하거나 갱신한다.

### CI, Nightly, Example

- Module이 PR CI에서 실행되어야 하면 `.github/workflows/ci.yml` path filter,
  changed-module mapping, test job을 갱신한다.
- Testcontainers, external service, long-running example이 관련되면 Nightly 또는 examples
  workflow를 갱신한다.
- 새 job이 추가되면 summary job과 `needs` list를 갱신한다.
- 필요할 때 container-backed verification은 하나의 sequential lane으로 유지한다.

### Release And Dependency Metadata

- Publishable module은 `bluetape4k-bom` 또는 publication aggregation을 확인한다.
- Module이 centrally governed dependency를 도입하거나 이동하면 dependency catalog alias를 확인한다.
- Module change가 user-visible일 때만 release note, changelog, migration note를 갱신한다.

### Manual Publication Contract

- Authoring source가 implementation과 같은 변경에서 review 가능하도록 `docs/manual` repository
  link는 relative로 유지한다.
- Release contract validator를 실행해 모든 relative source와 test target이 문서화된 release
  commit에 존재함을 증명한다.
- Validator는 해당 release에 `sourceDir`이 없는 manifest module에서 snapshot-only manual을
  도출한다. Release tree에 존재하는 module에 대해 manual skip list를 추가하거나 link check를
  약화하지 않는다.
- Public-site snapshot publisher는 repository-relative link를 immutable release tag 또는 commit으로
  rewrite해야 한다. Deployment 전에 대표 generated URL을 검증한다. `develop` link는 release
  provenance가 아니다.
- Generated consumer example에는 central `bluetape4k-dependencies` BOM만 사용한다. Repository-local
  BOM coordinate는 implementation metadata이며 사용자가 선택할 version이 아니다.

## 검증 명령

변경에 대한 checklist를 증명하는 가장 작은 command set을 실행한다.

```bash
./gradlew projects --no-configuration-cache
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/export_manifest.rb --check
ruby scripts/manual/validate_release_manuals.rb 1.11.0 6187173b58e8b4c5c435c145e00e94708f31ef75
git diff --check
rg -n "<old-module-name>|<old-artifact-name>|<old-path>"
rg -n "<new-module-name>|<new-artifact-name>" README.md README.ko.md .codex/references/module-groups.md AGENTS.md
```

Compile/test evidence에는 affected module task를 우선한다.

```bash
./gradlew :<module>:compileKotlin :<module>:compileTestKotlin --no-configuration-cache
./gradlew :<module>:test --no-configuration-cache
```

README link/path check에는 수정한 relative link를 직접 검증한다.

```bash
test -f <path-from-link-target>
test -d <module-directory>
```

## PR에 필요한 Evidence

- Gradle project path가 `./gradlew projects`에 나타난다.
- Root README locale pair가 module을 나열하거나 의도적으로 생략한다.
- Module README locale pair가 존재하고 동기화되어 있다.
- 변경이 필요 없더라도 CI/Nightly/example workflow impact를 기록한다.
- Automatic aggregation이 cover하더라도 BOM/catalog impact를 기록한다.
- Generated consumer example은 `bluetape4k-dependencies`를 사용하고, published source link는
  `develop`이 아니라 문서화된 release로 resolve된다.
- Stale old module name은 없거나 historical로 명시되어 있다.

## 일반적인 Skip 사유

- Module lifecycle impact가 없는 documentation-only wording change.
- Old module path를 의도적으로 기록하는 `docs/superpowers` 아래 historical plan/spec file.
- `docs/security-review` 아래 archived security review evidence.
