#!/usr/bin/env python3
"""Spring Boot 3/4 모듈 대칭성과 nightly 테스트 등록 상태를 검증한다."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
NIGHTLY_WORKFLOW = ROOT / ".github" / "workflows" / "nightly-tests.yml"


def module_dirs(base: str) -> set[str]:
    base_dir = ROOT / base
    return {
        child.name
        for child in base_dir.iterdir()
        if child.is_dir() and (child / "build.gradle.kts").is_file()
    }


def project_path(boot: str, module: str) -> str:
    return f":bluetape4k-spring-boot{boot}-{module}"


def main() -> int:
    errors: list[str] = []
    modules = {
        "3": module_dirs("spring-boot3"),
        "4": module_dirs("spring-boot4"),
    }

    only_boot3 = sorted(modules["3"] - modules["4"])
    only_boot4 = sorted(modules["4"] - modules["3"])

    if only_boot3:
        errors.append(f"spring-boot3에만 있는 모듈: {', '.join(only_boot3)}")
    if only_boot4:
        errors.append(f"spring-boot4에만 있는 모듈: {', '.join(only_boot4)}")

    for boot, module_names in modules.items():
        # 두 형식 모두 허용:
        # - 구 Libs.kt 방식: spring_boot{boot}_dependencies
        # - libs.versions.toml 방식: spring.boot{boot}.dependencies
        legacy_token = f"spring_boot{boot}_dependencies"
        catalog_token = f"spring.boot{boot}.dependencies"
        base = ROOT / f"spring-boot{boot}"
        for module in sorted(module_names):
            build_file = base / module / "build.gradle.kts"
            content = build_file.read_text(encoding="utf-8")
            if legacy_token not in content and catalog_token not in content:
                errors.append(f"{build_file.relative_to(ROOT)}: spring-boot{boot}-dependencies BOM 적용 누락")

    nightly = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
    for boot, module_names in modules.items():
        for module in sorted(module_names):
            project = project_path(boot, module)
            expected_test = f"{project}:test"
            if expected_test not in nightly:
                errors.append(f"{NIGHTLY_WORKFLOW.relative_to(ROOT)}: {expected_test} 등록 누락")

            if not module.endswith("-demo"):
                expected_kover = f"{project}:koverXmlReport"
                if expected_kover not in nightly:
                    errors.append(f"{NIGHTLY_WORKFLOW.relative_to(ROOT)}: {expected_kover} 등록 누락")

    if errors:
        print("Spring Boot 3/4 대칭성 검증 실패:")
        for error in errors:
            print(f"- {error}")
        return 1

    print(
        "Spring Boot 3/4 대칭성 검증 성공: "
        f"{len(modules['3'])}개 모듈 쌍, BOM, nightly test 등록 확인"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
