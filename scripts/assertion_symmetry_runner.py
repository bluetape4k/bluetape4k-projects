#!/usr/bin/env python3
"""Run the approved Assertions/Core checks with fixed commands and bounded capture."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Sequence


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_ROOT = Path("build/workflow/assertion-symmetry")
RUN_ID_PATTERN = re.compile(r"^[0-9]{8}T[0-9]{6}Z-[a-f0-9]{8}$")
STEP_PATTERN = re.compile(r"^[a-z0-9][a-z0-9-]*$")
GRADLE_OPTIONS = (
    "--no-daemon",
    "--no-configuration-cache",
    "--no-build-cache",
    "-Pkotlin.compiler.execution.strategy=in-process",
)
MODULES = {
    "assertions": {
        "project": ":bluetape4k-assertions",
        "directory": Path("testing/assertions"),
    },
    "core": {
        "project": ":bluetape4k-core",
        "directory": Path("bluetape4k/core"),
    },
}
ASSERTIONS_OWNERS = (
    "BasicKt",
    "ArraysKt",
    "CharSequencesKt",
    "CollectionsKt",
    "DateTimesInstantZonedKt",
    "DateTimesLegacyKt",
    "DateTimesLocalKt",
    "MapsKt",
    "NumericalKt",
    "ReflectionKt",
    "InvokingBlock",
    "CoInvokingBlock",
)
CORE_OWNERS = (
    "io.bluetape4k.javatimes.DurationSupportKt",
    "io.bluetape4k.support.KotlinDurationSupportKt",
    "io.bluetape4k.support.RequireSupportKt",
    "io.bluetape4k.support.CheckSupportKt",
)
FINAL_TEST_CLASSES = {
    "assertions": (
        "BasicTest",
        "ArraysTest",
        "CharSequencesTest",
        "CollectionsTest",
        "DateTimesTest$InstantAssertions",
        "DateTimesTest$ZonedDateTimeAssertions",
        "DateTimesTest$OffsetDateTimeAssertions",
        "DateTimesTest$LocalDateTimeAssertions",
        "DateTimesTest$LocalDateAssertions",
        "DateTimesTest$LocalTimeAssertions",
        "DateTimesTest$JavaUtilDateAssertions",
        "MapsTest",
        "NumericalTest",
        "DurationAssertionsTest",
    ),
    "core": (
        "RequireSupportTest",
        "CheckSupportTest",
        "DurationSupportTest",
        "KotlinDurationSupportTest",
    ),
}


@dataclass(frozen=True)
class CommandSpec:
    name: str
    argv: tuple[str, ...]
    timeout_seconds: float
    idle_timeout_seconds: float
    verifier: str | None = None
    module: str | None = None
    required_classes: tuple[str, ...] = ()
    artifact_globs: tuple[str, ...] = ()
    cleanup_paths: tuple[str, ...] = ()


@dataclass
class CommandResult:
    exit_code: int
    metadata: dict
    attempt_directory: Path

    @property
    def metadata_path(self) -> Path:
        return self.attempt_directory / "metadata.json"

    @property
    def stdout_path(self) -> Path:
        return self.attempt_directory / "stdout.log"

    @property
    def stderr_path(self) -> Path:
        return self.attempt_directory / "stderr.log"


ALIAS_PHASES = {
    "baseline-environment": {"baseline"},
    "baseline-assertions": {"baseline"},
    "baseline-core": {"baseline"},
    "baseline-detekt-assertions": {"baseline"},
    "baseline-detekt-core": {"baseline"},
    "compile-assertions": {"final"},
    "compile-core": {"final"},
    "test-assertions": {"final"},
    "test-core": {"final"},
    "detekt-assertions": {"final"},
    "detekt-core": {"final"},
    "javap-assertions": {"baseline", "final"},
    "javap-core": {"baseline", "final"},
}


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_json(path: Path, value: dict, *, replace: bool = False) -> None:
    temporary = path.with_name(path.name + ".tmp")
    if temporary.is_symlink() or path.is_symlink():
        raise ValueError("runner metadata path must not be a symlink")
    if temporary.exists():
        temporary.unlink()
    with temporary.open("x", encoding="utf-8") as output:
        json.dump(value, output, ensure_ascii=False, indent=2, sort_keys=True)
        output.write("\n")
        output.flush()
        os.fsync(output.fileno())
    if not replace and path.exists():
        temporary.unlink()
        raise ValueError(f"runner metadata already exists: {path}")
    os.replace(temporary, path)


def _fixed_alias_phase_is_valid(alias: str, phase: str) -> bool:
    return alias in ALIAS_PHASES and phase in ALIAS_PHASES[alias]


def validate_request(alias: str, run_id: str, phase: str) -> None:
    if not _fixed_alias_phase_is_valid(alias, phase):
        raise ValueError(f"unsupported alias/phase combination: {alias}/{phase}")
    if not RUN_ID_PATTERN.fullmatch(run_id):
        raise ValueError("run id must match YYYYMMDDTHHMMSSZ- followed by eight lowercase hex digits")


def _validate_phase_and_run_id(phase: str, run_id: str) -> None:
    if phase not in {"baseline", "final"}:
        raise ValueError(f"unsupported phase: {phase}")
    if not RUN_ID_PATTERN.fullmatch(run_id):
        raise ValueError("run id must match YYYYMMDDTHHMMSSZ- followed by eight lowercase hex digits")


def _gradle_step(
    name: str,
    module: str,
    tasks: tuple[str, ...],
    timeout_seconds: float,
    *,
    verifier: str | None = None,
) -> CommandSpec:
    project = MODULES[module]["project"]
    argv = (
        "./gradlew",
        *GRADLE_OPTIONS,
        *(task if task.startswith("-") else f"{project}:{task}" for task in tasks),
    )
    artifacts: tuple[str, ...] = ()
    cleanup: tuple[str, ...] = ()
    if verifier == "junit":
        artifacts = (str(MODULES[module]["directory"] / "build/test-results/test/**/*.xml"),)
    elif verifier == "detekt":
        report = MODULES[module]["directory"] / "build/reports/detekt/detekt.xml"
        artifacts = (str(report),)
        cleanup = (str(report),)
    return CommandSpec(
        name=name,
        argv=argv,
        timeout_seconds=timeout_seconds,
        idle_timeout_seconds=15 * 60,
        verifier=verifier,
        module=module,
        required_classes=FINAL_TEST_CLASSES[module] if verifier == "junit" else (),
        artifact_globs=artifacts,
        cleanup_paths=cleanup,
    )


def build_steps(alias: str, phase: str, repository_root: Path) -> tuple[CommandSpec, ...]:
    del repository_root  # Commands and outputs are intentionally repository-relative and fixed.
    if alias not in ALIAS_PHASES or phase not in ALIAS_PHASES[alias]:
        raise ValueError(f"unsupported alias/phase combination: {alias}/{phase}")
    if alias == "baseline-environment":
        commands = (
            ("java-version", ("java", "-version")),
            ("gradle-version", ("./gradlew", *GRADLE_OPTIONS, "--version")),
            ("java-toolchains", ("./gradlew", *GRADLE_OPTIONS, "-q", "javaToolchains")),
            ("locale", ("locale",)),
            ("timezone", ("date", "+%Z %z")),
        )
        return tuple(
            CommandSpec(name, argv, 5 * 60, 2 * 60)
            for name, argv in commands
        )
    if alias in {"baseline-assertions", "test-assertions"}:
        return (_gradle_step(alias, "assertions", ("clean", "test"), 45 * 60, verifier="junit"),)
    if alias in {"baseline-core", "test-core"}:
        return (_gradle_step(alias, "core", ("clean", "test"), 45 * 60, verifier="junit"),)
    if alias in {"baseline-detekt-assertions", "detekt-assertions"}:
        return (_gradle_step(alias, "assertions", ("detekt", "--rerun-tasks"), 20 * 60, verifier="detekt"),)
    if alias in {"baseline-detekt-core", "detekt-core"}:
        return (_gradle_step(alias, "core", ("detekt", "--rerun-tasks"), 20 * 60, verifier="detekt"),)
    if alias in {"compile-assertions", "compile-core"}:
        module = "assertions" if alias.endswith("assertions") else "core"
        return (
            _gradle_step(
                alias,
                module,
                ("clean", "compileKotlin", "compileTestKotlin"),
                30 * 60,
                verifier="compile",
            ),
        )
    if alias in {"javap-assertions", "javap-core"}:
        module = "assertions" if alias.endswith("assertions") else "core"
        owners = ASSERTIONS_OWNERS + (("DurationAssertionsKt",) if phase == "final" else ()) if module == "assertions" else CORE_OWNERS
        package_prefix = "io.bluetape4k.assertions." if module == "assertions" else ""
        classpath = str(MODULES[module]["directory"] / "build/classes/kotlin/main")
        steps = []
        for owner in owners:
            class_name = f"{package_prefix}{owner}"
            step_name = "javap-" + re.sub(r"[^a-z0-9]+", "-", owner.lower()).strip("-")
            steps.append(
                CommandSpec(
                    step_name,
                    ("javap", "-public", "-s", "-classpath", classpath, class_name),
                    5 * 60,
                    2 * 60,
                    verifier="javap",
                )
            )
            if module == "core" and owner in {
                "io.bluetape4k.support.RequireSupportKt",
                "io.bluetape4k.support.CheckSupportKt",
            }:
                steps.append(
                    CommandSpec(
                        step_name + "-metadata",
                        ("javap", "-v", "-public", "-classpath", classpath, class_name),
                        5 * 60,
                        2 * 60,
                        verifier="javap",
                    )
                )
        return tuple(steps)
    raise ValueError(f"unsupported alias: {alias}")


def _safe_repository_path(repository_root: Path, relative_path: Path) -> Path:
    root = repository_root.resolve()
    candidate = root
    for part in relative_path.parts:
        if part in {"", ".", ".."}:
            raise ValueError("runner output path contains an unsafe component")
        candidate = candidate / part
        if candidate.is_symlink():
            raise ValueError(f"runner output path contains a symlink: {candidate}")
    resolved = candidate.resolve(strict=False)
    if not resolved.is_relative_to(root):
        raise ValueError("runner output path escapes the repository root")
    return candidate


def _attempt_path(repository_root: Path, run_id: str, phase: str, step: str, attempt: int) -> Path:
    if not RUN_ID_PATTERN.fullmatch(run_id):
        raise ValueError("invalid run id")
    if phase not in {"baseline", "final"}:
        raise ValueError("invalid phase")
    if not STEP_PATTERN.fullmatch(step):
        raise ValueError("invalid step name")
    if not isinstance(attempt, int) or isinstance(attempt, bool) or attempt < 1 or attempt > 2:
        raise ValueError("attempt must be 1 or 2")
    relative = OUTPUT_ROOT / run_id / phase / step / f"attempt-{attempt}"
    return _safe_repository_path(repository_root, relative)


def create_attempt_directory(
    repository_root: Path,
    run_id: str,
    phase: str,
    step: str,
    attempt: int,
) -> Path:
    path = _attempt_path(repository_root, run_id, phase, step, attempt)
    if path.exists() or path.is_symlink():
        raise ValueError(f"runner attempt already exists: {path}")
    path.mkdir(parents=True, exist_ok=False)
    _safe_repository_path(repository_root, path.relative_to(repository_root.resolve()))
    return path


def _process_or_group_is_alive(pid: int, pgid: int) -> bool:
    for process_id, group in ((pid, False), (pgid, True)):
        try:
            os.killpg(process_id, 0) if group else os.kill(process_id, 0)
            return True
        except ProcessLookupError:
            continue
        except PermissionError:
            return True
    return False


def _artifact_paths(repository_root: Path, artifact_globs: Sequence[str]) -> list[Path]:
    root = repository_root.resolve()
    found: dict[str, Path] = {}
    for pattern in artifact_globs:
        for path in root.glob(pattern):
            if path.is_symlink():
                raise ValueError(f"generated report must not be a symlink: {path}")
            if path.is_file():
                resolved = path.resolve()
                if not resolved.is_relative_to(root):
                    raise ValueError(f"generated report escapes the repository: {path}")
                found[str(path.relative_to(root))] = path
    return [found[key] for key in sorted(found)]


def _capture_artifacts(
    repository_root: Path,
    attempt_directory: Path,
    artifact_globs: Sequence[str],
) -> list[dict]:
    root = repository_root.resolve()
    records = []
    for source in _artifact_paths(root, artifact_globs):
        relative = source.relative_to(root)
        destination_relative = attempt_directory.relative_to(root) / "artifacts" / relative
        destination = _safe_repository_path(root, destination_relative)
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.is_symlink():
            raise ValueError(f"captured artifact path must not be a symlink: {destination}")
        shutil.copy2(source, destination)
        records.append(
            {
                "source": str(relative),
                "captured": str(destination.relative_to(root)),
                "sha256": sha256_file(destination),
                "bytes": destination.stat().st_size,
            }
        )
    return records


def _group_exists(pgid: int) -> bool:
    try:
        os.killpg(pgid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True


def _wait_for_group_exit(process: subprocess.Popen, pgid: int, seconds: float) -> bool:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        process.poll()
        if not _group_exists(pgid):
            return True
        time.sleep(0.01)
    process.poll()
    return not _group_exists(pgid)


def _terminate_process_group(
    process: subprocess.Popen,
    pgid: int,
    grace_periods: Sequence[float],
) -> tuple[list[str], bool]:
    signals = (signal.SIGINT, signal.SIGTERM, signal.SIGKILL)
    names = {signal.SIGINT: "SIGINT", signal.SIGTERM: "SIGTERM", signal.SIGKILL: "SIGKILL"}
    sent = []
    terminated = not _group_exists(pgid)
    for current_signal, grace in zip(signals, grace_periods):
        if terminated:
            break
        sent.append(names[current_signal])
        try:
            os.killpg(pgid, current_signal)
        except ProcessLookupError:
            terminated = True
            break
        terminated = _wait_for_group_exit(process, pgid, grace)
    if not terminated:
        terminated = _wait_for_group_exit(process, pgid, max(grace_periods[-1], 0.1))
    return sent, terminated


def _write_metadata(attempt_directory: Path, metadata: dict, *, replace: bool = False) -> None:
    write_json(attempt_directory / "metadata.json", metadata, replace=replace)


def execute_command(
    argv: Sequence[str],
    repository_root: Path,
    attempt_directory: Path,
    *,
    timeout_seconds: float,
    idle_timeout_seconds: float,
    poll_interval: float = 0.1,
    grace_periods: Sequence[float] = (30.0, 30.0, 30.0),
    interrupt_event=None,
    artifact_globs: Sequence[str] = (),
    resumed_from_attempt: int | None = None,
) -> CommandResult:
    if not argv or any(not isinstance(argument, str) or not argument for argument in argv):
        raise ValueError("command argv must be a non-empty sequence of non-empty strings")
    if not attempt_directory.is_dir() or attempt_directory.is_symlink():
        raise ValueError("attempt directory must exist and must not be a symlink")
    if timeout_seconds <= 0 or idle_timeout_seconds <= 0 or poll_interval <= 0:
        raise ValueError("command timeouts and polling interval must be positive")
    if len(grace_periods) != 3 or any(value < 0 for value in grace_periods):
        raise ValueError("process group cleanup requires three non-negative grace periods")

    stdout_path = attempt_directory / "stdout.log"
    stderr_path = attempt_directory / "stderr.log"
    if any(path.exists() or path.is_symlink() for path in (stdout_path, stderr_path)):
        raise ValueError("runner output logs already exist")
    started_at = utc_now()
    started_unix = time.time()
    started_monotonic = time.monotonic()
    metadata = {
        "argv": list(argv),
        "started_at_utc": started_at,
        "started_unix": started_unix,
        "started_monotonic": started_monotonic,
        "finished_at_utc": None,
        "finished_monotonic": None,
        "pid": None,
        "pgid": None,
        "process_return_code": None,
        "exit_code": None,
        "terminating_signal": None,
        "termination_reason": None,
        "signals_sent": [],
        "process_group_terminated": None,
        "idle_warnings": [],
        "stdout_sha256": None,
        "stderr_sha256": None,
        "artifacts": [],
        "resumed_from_incomplete_attempt": resumed_from_attempt,
    }

    process = None
    termination_reason = "completed"
    artifact_capture_error = None
    try:
        with stdout_path.open("xb") as stdout_file, stderr_path.open("xb") as stderr_file:
            try:
                process = subprocess.Popen(
                    tuple(argv),
                    cwd=repository_root,
                    stdout=stdout_file,
                    stderr=stderr_file,
                    start_new_session=True,
                    close_fds=True,
                )
            except OSError as error:
                metadata["process_error"] = f"{type(error).__name__}: {error}"
                termination_reason = "spawn-failed"
            if process is not None:
                pgid = process.pid
                metadata["pid"] = process.pid
                metadata["pgid"] = pgid
                write_json(attempt_directory / "launch.json", metadata)
                last_output_size = 0
                last_output_change = time.monotonic()
                next_idle_warning = last_output_change + idle_timeout_seconds
                stop_reason = None
                while process.poll() is None:
                    now = time.monotonic()
                    if interrupt_event is not None and interrupt_event.is_set():
                        stop_reason = "interrupted"
                    elif now - started_monotonic >= timeout_seconds:
                        stop_reason = "timeout"
                    if stop_reason is not None:
                        termination_reason = stop_reason
                        try:
                            metadata["artifacts"] = _capture_artifacts(
                                repository_root, attempt_directory, artifact_globs
                            )
                        except (OSError, ValueError) as error:
                            artifact_capture_error = f"{type(error).__name__}: {error}"
                        signals_sent, group_terminated = _terminate_process_group(
                            process, pgid, grace_periods
                        )
                        metadata["signals_sent"] = signals_sent
                        metadata["process_group_terminated"] = group_terminated
                        break
                    current_size = 0
                    for log_path in (stdout_path, stderr_path):
                        try:
                            current_size += log_path.stat().st_size
                        except FileNotFoundError:
                            pass
                    if current_size != last_output_size:
                        last_output_size = current_size
                        last_output_change = now
                        next_idle_warning = now + idle_timeout_seconds
                    elif now >= next_idle_warning:
                        metadata["idle_warnings"].append(
                            {
                                "at_utc": utc_now(),
                                "elapsed_seconds": round(now - last_output_change, 3),
                                "bytes": current_size,
                            }
                        )
                        next_idle_warning = now + idle_timeout_seconds
                    time.sleep(poll_interval)
                process.wait()
                if termination_reason == "completed":
                    group_terminated = not _group_exists(pgid)
                    if not group_terminated:
                        signals_sent, group_terminated = _terminate_process_group(
                            process, pgid, grace_periods
                        )
                        metadata["signals_sent"] = signals_sent
                        termination_reason = (
                            "process-group-leak" if group_terminated else "process-group-cleanup-failed"
                        )
                    metadata["process_group_terminated"] = group_terminated
                    try:
                        metadata["artifacts"] = _capture_artifacts(
                            repository_root, attempt_directory, artifact_globs
                        )
                    except (OSError, ValueError) as error:
                        artifact_capture_error = f"{type(error).__name__}: {error}"
                        termination_reason = "artifact-capture-failed"
    except KeyboardInterrupt:
        termination_reason = "interrupted"
        if process is not None:
            try:
                metadata["artifacts"] = _capture_artifacts(
                    repository_root, attempt_directory, artifact_globs
                )
            except (OSError, ValueError) as error:
                artifact_capture_error = f"{type(error).__name__}: {error}"
            signals_sent, group_terminated = _terminate_process_group(
                process, process.pid, grace_periods
            )
            metadata["signals_sent"] = signals_sent
            metadata["process_group_terminated"] = group_terminated
            process.wait()

    finished_monotonic = time.monotonic()
    process_return_code = process.returncode if process is not None else None
    if termination_reason == "timeout":
        exit_code = 124
    elif termination_reason == "interrupted":
        exit_code = 130
    elif termination_reason in {
        "spawn-failed",
        "artifact-capture-failed",
        "process-group-leak",
        "process-group-cleanup-failed",
    }:
        exit_code = 1
    else:
        exit_code = process_return_code if process_return_code is not None else 1
    if process_return_code is not None and process_return_code < 0:
        try:
            metadata["terminating_signal"] = signal.Signals(-process_return_code).name
        except ValueError:
            metadata["terminating_signal"] = -process_return_code
    metadata.update(
        {
            "finished_at_utc": utc_now(),
            "finished_monotonic": finished_monotonic,
            "process_return_code": process_return_code,
            "exit_code": exit_code,
            "termination_reason": termination_reason,
            "stdout_sha256": sha256_file(stdout_path),
            "stderr_sha256": sha256_file(stderr_path),
        }
    )
    if artifact_capture_error is not None:
        metadata["artifact_capture_error"] = artifact_capture_error
    _write_metadata(attempt_directory, metadata)
    return CommandResult(exit_code, metadata, attempt_directory)


def _clean_stale_outputs(repository_root: Path, step: CommandSpec) -> None:
    root = repository_root.resolve()
    for relative in step.cleanup_paths:
        path = _safe_repository_path(root, Path(relative))
        if path.is_symlink():
            raise ValueError(f"stale generated output must not be a symlink: {path}")
        if path.exists():
            if not path.is_file():
                raise ValueError(f"stale generated output is not a regular file: {path}")
            path.unlink()


def verify_junit_reports(
    repository_root: Path,
    module: str,
    task_log: str,
    required_classes: Sequence[str] = (),
) -> dict:
    if module not in MODULES:
        raise ValueError(f"unsupported test module: {module}")
    project = MODULES[module]["project"]
    clean_task = f"> Task {project}:clean"
    test_task = f"> Task {project}:test"
    clean_position = task_log.find(clean_task)
    test_position = task_log.find(test_task)
    if clean_position < 0 or test_position < 0 or clean_position >= test_position:
        raise ValueError("Gradle log must show clean before the module test task")
    report_directory = repository_root / MODULES[module]["directory"] / "build/test-results/test"
    reports = sorted(report_directory.glob("**/*.xml")) if report_directory.is_dir() else []
    if not reports:
        raise ValueError("the current clean/test invocation produced no JUnit XML reports")
    total_cases = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    total_aborted = 0
    classes = set()
    report_records = []
    for report in reports:
        if report.is_symlink() or not report.is_file():
            raise ValueError(f"JUnit report must be a regular file: {report}")
        document = ET.parse(report).getroot()
        cases = list(document.iter("testcase"))
        total_cases += len(cases)
        for case in cases:
            if case.get("classname"):
                classes.add(case.get("classname"))
            total_failures += len(list(case.iter("failure")))
            total_errors += len(list(case.iter("error")))
            total_skipped += len(list(case.iter("skipped")))
            total_aborted += len(list(case.iter("aborted")))
        for suite in document.iter():
            if suite.tag in {"testsuite", "testsuites"}:
                total_failures += max(0, int(suite.get("failures", "0")) - sum(len(list(case.iter("failure"))) for case in cases)) if suite is document else 0
                total_errors += max(0, int(suite.get("errors", "0")) - sum(len(list(case.iter("error"))) for case in cases)) if suite is document else 0
                total_skipped += max(0, int(suite.get("skipped", "0")) - sum(len(list(case.iter("skipped"))) for case in cases)) if suite is document else 0
        relative = report.relative_to(repository_root)
        report_records.append(
            {
                "path": str(relative),
                "sha256": sha256_file(report),
                "bytes": report.stat().st_size,
                "testcases": len(cases),
            }
        )
    missing_classes = [
        name
        for name in required_classes
        if not any(actual == name or actual.endswith("." + name) for actual in classes)
    ]
    if total_cases < 1:
        raise ValueError("JUnit reports contain no testcases")
    if total_failures or total_errors or total_skipped or total_aborted:
        raise ValueError(
            "JUnit reports contain failures/errors/skips/aborts: "
            f"{total_failures}/{total_errors}/{total_skipped}/{total_aborted}"
        )
    if missing_classes:
        raise ValueError("required changed test classes have no testcase: " + ", ".join(missing_classes))
    return {
        "reports": report_records,
        "testcases": total_cases,
        "failures": total_failures,
        "errors": total_errors,
        "skipped": total_skipped,
        "aborted": total_aborted,
        "test_classes": sorted(classes),
    }


def _verify_detekt_report(repository_root: Path, module: str) -> dict:
    report = repository_root / MODULES[module]["directory"] / "build/reports/detekt/detekt.xml"
    if report.is_symlink() or not report.is_file():
        raise ValueError(f"current Detekt invocation produced no XML report: {report}")
    root = ET.parse(report).getroot()
    findings = len(list(root.iter("error")))
    return {
        "report": str(report.relative_to(repository_root)),
        "sha256": sha256_file(report),
        "bytes": report.stat().st_size,
        "findings": findings,
    }


def _verify_compile(repository_root: Path, module: str, task_log: str) -> dict:
    project = MODULES[module]["project"]
    required_tasks = (f"> Task {project}:clean", f"> Task {project}:compileKotlin", f"> Task {project}:compileTestKotlin")
    missing = [task for task in required_tasks if task not in task_log]
    if missing:
        raise ValueError("Gradle log does not show required clean/compile tasks: " + ", ".join(missing))
    module_root = repository_root / MODULES[module]["directory"] / "build/classes/kotlin"
    outputs = [path for path in module_root.glob("**/*.class") if path.is_file()]
    main_outputs = [path for path in outputs if "/main/" in path.as_posix()]
    test_outputs = [path for path in outputs if "/test/" in path.as_posix()]
    if not main_outputs or not test_outputs:
        raise ValueError("clean compile did not produce main and test Kotlin class files")
    return {"main_class_files": len(main_outputs), "test_class_files": len(test_outputs)}


def _verify_step(repository_root: Path, step: CommandSpec, result: CommandResult) -> None:
    log = result.stdout_path.read_text(encoding="utf-8", errors="replace")
    if step.verifier == "junit":
        validation = verify_junit_reports(repository_root, step.module or "", log, step.required_classes)
    elif step.verifier == "detekt":
        validation = _verify_detekt_report(repository_root, step.module or "")
    elif step.verifier == "compile":
        validation = _verify_compile(repository_root, step.module or "", log)
    elif step.verifier == "javap":
        if not log.strip():
            raise ValueError("javap produced an empty public API output")
        validation = {
            "sha256": sha256_file(result.stdout_path),
            "bytes": result.stdout_path.stat().st_size,
            "public_members": sum(
                1 for line in log.splitlines() if line.startswith("  public ") or line.startswith("public ")
            ),
        }
    else:
        return None
    return validation


def _select_attempt_number(
    repository_root: Path,
    run_id: str,
    phase: str,
    step: CommandSpec,
) -> tuple[int, int | None]:
    first = _attempt_path(repository_root, run_id, phase, step.name, 1)
    second = _attempt_path(repository_root, run_id, phase, step.name, 2)
    if not first.exists():
        return 1, None
    if second.exists():
        raise ValueError(f"maximum attempt count already reached for {step.name}")
    if (first / "metadata.json").is_file():
        raise ValueError(f"completed or failed result already exists for {step.name}; use a new run id")
    launch_path = first / "launch.json"
    if launch_path.is_symlink() or not launch_path.is_file():
        raise ValueError(f"incomplete attempt for {step.name} has no trustworthy launch record")
    launch = json.loads(launch_path.read_text(encoding="utf-8"))
    pid, pgid = launch.get("pid"), launch.get("pgid")
    if not isinstance(pid, int) or not isinstance(pgid, int) or _process_or_group_is_alive(pid, pgid):
        raise ValueError(f"incomplete attempt for {step.name} may still be running")
    if _artifact_paths(repository_root, step.artifact_globs):
        raise ValueError(f"incomplete attempt for {step.name} produced reports; do not retry it")
    return 2, 1


def run_steps(
    repository_root: Path,
    run_id: str,
    phase: str,
    steps: Sequence[CommandSpec],
    *,
    poll_interval: float = 0.1,
    grace_periods: Sequence[float] = (30.0, 30.0, 30.0),
) -> list[CommandResult]:
    _validate_phase_and_run_id(phase, run_id)
    results = []
    for step in steps:
        if not STEP_PATTERN.fullmatch(step.name):
            raise ValueError(f"invalid command step name: {step.name}")
        attempt_number, resumed_from = _select_attempt_number(repository_root, run_id, phase, step)
        _clean_stale_outputs(repository_root, step)
        attempt_directory = create_attempt_directory(
            repository_root, run_id, phase, step.name, attempt_number
        )
        result = execute_command(
            step.argv,
            repository_root,
            attempt_directory,
            timeout_seconds=step.timeout_seconds,
            idle_timeout_seconds=step.idle_timeout_seconds,
            poll_interval=poll_interval,
            grace_periods=grace_periods,
            artifact_globs=step.artifact_globs,
            resumed_from_attempt=resumed_from,
        )
        if result.exit_code == 0 and step.verifier is not None:
            try:
                result.metadata["post_validation"] = _verify_step(repository_root, step, result)
                result.metadata["validation_exit_code"] = 0
                _write_metadata(result.attempt_directory, result.metadata, replace=True)
            except (OSError, ValueError, ET.ParseError) as error:
                result.exit_code = 1
                result.metadata["exit_code"] = 1
                result.metadata["validation_exit_code"] = 1
                result.metadata["post_validation_error"] = f"{type(error).__name__}: {error}"
                _write_metadata(result.attempt_directory, result.metadata, replace=True)
        results.append(result)
        if result.exit_code != 0:
            break
    return results


def run_alias(alias: str, run_id: str, phase: str, repository_root: Path = REPOSITORY_ROOT) -> list[CommandResult]:
    validate_request(alias, run_id, phase)
    steps = build_steps(alias, phase, repository_root)
    return run_steps(repository_root, run_id, phase, steps)


def _result_summary(results: Sequence[CommandResult]) -> dict:
    return {
        "steps": [
            {
                "name": result.attempt_directory.parent.name,
                "exit_code": result.exit_code,
                "attempt_directory": str(result.attempt_directory),
                "metadata": str(result.metadata_path),
            }
            for result in results
        ],
        "exit_code": next((result.exit_code for result in results if result.exit_code != 0), 0),
    }


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("alias", choices=sorted(ALIAS_PHASES))
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--phase", required=True, choices=("baseline", "final"))
    arguments = parser.parse_args(argv)
    try:
        results = run_alias(arguments.alias, arguments.run_id, arguments.phase)
    except (OSError, ValueError, ET.ParseError) as error:
        print(f"runner error: {type(error).__name__}: {error}", file=sys.stderr)
        return 2
    summary = _result_summary(results)
    summary.update({"alias": arguments.alias, "phase": arguments.phase, "run_id": arguments.run_id})
    print(json.dumps(summary, ensure_ascii=False, sort_keys=True))
    return summary["exit_code"]


if __name__ == "__main__":
    raise SystemExit(main())
