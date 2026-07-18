#!/usr/bin/env python3
"""Fail-closed evidence lifecycle for the issue-757 Protobuf JMH benchmark."""

import argparse
import contextlib
import csv
import datetime
import hashlib
import importlib.util
import json
import os
import platform
import re
import secrets
import shutil
import stat
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path


SCHEMA_VERSION = 1
REQUIRED_RUN_FILES = ("jmh.json", "metadata.json", "argv.json", "run.log", "environment.json", "summary.csv", "validation.json")
METADATA_MAIN = "io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmarkMetadata"
JVM_ARGS = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"]
PROFILE_ARGS = {
    "smoke": ["-t", "1", "-f", "1", "-wi", "1", "-i", "1", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json"],
    "canonical": ["-t", "1", "-f", "2", "-wi", "3", "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json"],
}
DISPATCH_ORDER = ("serializer_encode", "serializer_decode", "redisson_contiguous")
DISPATCH_CELLS = {
    "serializer_encode": ("serializerEncodeHeapOptimized", "serializerEncodeDirectOptimized"),
    "serializer_decode": ("serializerDecodeHeapOptimized", "serializerDecodeDirectOptimized"),
    "redisson_contiguous": ("redissonDecodeContiguousOptimized",),
}
POSITIVE_PHRASE = "measured allocation reduction"
NON_POSITIVE = "No positive reduction claim"


def utc_now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat().replace("+00:00", "Z")


def error(path, detail, hint):
    return ValueError("{}: {}; remediation: {}".format(Path(path), detail, hint))


def sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical_json_bytes(value):
    return (json.dumps(value, sort_keys=True, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def payload_json_bytes(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def atomic_write_bytes(path, data, fail_if_exists=False):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    if fail_if_exists and path.exists():
        raise FileExistsError("{} exists; choose a new no-clobber path".format(path))
    fd, temporary = tempfile.mkstemp(prefix="." + path.name + ".", dir=str(path.parent))
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
        if fail_if_exists:
            try:
                os.link(temporary, str(path))
            except FileExistsError:
                raise FileExistsError("{} exists; choose a new no-clobber path".format(path))
            os.unlink(temporary)
        else:
            os.replace(temporary, str(path))
    except Exception:
        try:
            os.unlink(temporary)
        except FileNotFoundError:
            pass
        raise


def atomic_write_json(path, value, fail_if_exists=False):
    atomic_write_bytes(path, canonical_json_bytes(value), fail_if_exists=fail_if_exists)


def load_json(path):
    path = Path(path)
    try:
        with path.open("r", encoding="utf-8") as stream:
            return json.load(stream)
    except (OSError, ValueError) as exc:
        raise error(path, "invalid JSON ({})".format(exc), "restore or regenerate this artifact")


def _run(command_runner, argv, cwd=None, pass_fds=()):
    kwargs = {"cwd": str(cwd) if cwd else None, "stdout": subprocess.PIPE, "stderr": subprocess.PIPE}
    if pass_fds:
        kwargs["pass_fds"] = tuple(pass_fds)
    return command_runner(argv, **kwargs)


def _stdout(result):
    value = result.stdout or b""
    return value if isinstance(value, bytes) else value.encode("utf-8")


def _stderr(result):
    value = result.stderr or b""
    return value if isinstance(value, bytes) else value.encode("utf-8")


def command_text(command_runner, argv, cwd=None, allow_failure=False):
    result = _run(command_runner, argv, cwd=cwd)
    output = _stdout(result)
    failure = _stderr(result)
    if result.returncode and not allow_failure:
        raise error(cwd or argv[0], "command={} exit_code={} stderr={!r}".format(argv, result.returncode, failure.decode("utf-8", "replace")), "repair the command and retry")
    return output.decode("utf-8", "replace"), failure.decode("utf-8", "replace"), result.returncode


def find_repo_root(start, command_runner=subprocess.run):
    stdout, _, _ = command_text(command_runner, ["git", "rev-parse", "--show-toplevel"], cwd=start)
    return Path(stdout.strip()).resolve()


def require_clean_tree(repo_root, phase, command_runner=subprocess.run):
    result = _run(command_runner, ["git", "status", "--porcelain=v1", "--untracked-files=all"], cwd=repo_root)
    output = _stdout(result)
    if result.returncode:
        raise error(repo_root, "{} clean gate exit_code={} stderr={!r}".format(phase, result.returncode, _stderr(result).decode("utf-8", "replace")), "run git status and repair the repository")
    text = output.decode("utf-8", "replace")
    if text:
        raise error(repo_root, "{} clean gate observed {!r}".format(phase, text), "run git status --short and commit, move, or remove every tracked and untracked change")
    return {"phase": phase, "stdout": text, "sha256": sha256_bytes(output)}


def generate_run_id():
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    return "run-{}-{}".format(stamp, secrets.token_hex(4))


def validate_heavy_work(profile, value, path):
    if value not in ("absent", "present", "unknown"):
        raise error(path, "concurrent_heavy_work={!r}".format(value), "supply absent, present, or unknown")
    if profile == "canonical" and value != "absent":
        raise error(path, "canonical concurrent_heavy_work={} != absent".format(value), "stop concurrent heavy work and rerun with --concurrent-heavy-work absent")


def capture_power_state(system=None, command_runner=subprocess.run):
    system = system or platform.system()
    argv = ["pmset", "-g", "batt"]
    if system != "Darwin":
        return {"normalized": "unknown", "command": argv, "stdout": "", "stderr": "unsupported platform", "exit_code": 127}
    try:
        result = _run(command_runner, argv)
    except OSError as exc:
        return {"normalized": "unknown", "command": argv, "stdout": "", "stderr": str(exc), "exit_code": 127}
    stdout = _stdout(result).decode("utf-8", "replace")
    stderr = _stderr(result).decode("utf-8", "replace")
    normalized = "battery" if "Battery Power" in stdout else "ac" if "AC Power" in stdout else "unknown"
    if result.returncode:
        normalized = "unknown"
    return {"normalized": normalized, "command": argv, "stdout": stdout, "stderr": stderr, "exit_code": result.returncode}


def capture_metadata(jar, expected=None, command_runner=subprocess.run, pass_fds=()):
    jar = Path(jar)
    argv = ["java", "-cp", str(jar), METADATA_MAIN, "--json"]
    result = _run(command_runner, argv, pass_fds=pass_fds)
    stdout = _stdout(result)
    stderr = _stderr(result)
    if result.returncode:
        raise error(jar, "metadata command exit_code={} stderr={!r}".format(result.returncode, stderr.decode("utf-8", "replace")), "rebuild the pinned benchmark JAR")
    try:
        value = json.loads(stdout.decode("utf-8"))
    except (UnicodeDecodeError, ValueError) as exc:
        raise error(jar, "metadata stdout is not one JSON object ({})".format(exc), "rebuild the pinned benchmark JAR")
    if not isinstance(value, dict):
        raise error(jar, "metadata stdout type={} != object".format(type(value).__name__), "rebuild the pinned benchmark JAR")
    result_value = {"value": value, "stdout": stdout, "stdout_sha256": sha256_bytes(stdout), "stderr": stderr.decode("utf-8", "replace"), "argv": argv}
    if expected is not None and stdout != expected["stdout"]:
        raise error(jar, "metadata changed between capture and launch: first={!r} second={!r}".format(expected["stdout"].decode("utf-8", "replace"), stdout.decode("utf-8", "replace")), "rebuild the pinned benchmark JAR and start a fresh run")
    return result_value


def resolve_jar(jar_dir, state_path, rollback_bundle=None, command_runner=subprocess.run, repo_root=None):
    jar_dir = Path(jar_dir)
    state_path = Path(state_path)
    if state_path.exists():
        raise error(state_path, "state exists", "choose a fresh state path; state files are no-clobber")
    jars = sorted(path for path in jar_dir.glob("*-JMH.jar") if path.is_file())
    if len(jars) != 1:
        raise error(jar_dir, "expected exactly one *-JMH.jar, observed {}: {}".format(len(jars), [str(p) for p in jars]), "clean the JAR directory and rebuild once")
    jar = jars[0].resolve()
    state = {
        "schema_version": SCHEMA_VERSION,
        "benchmark_jar_path": str(jar),
        "benchmark_jar_sha256": sha256_file(jar),
        "benchmark_jar_stat": [jar.stat().st_dev, jar.stat().st_ino, jar.stat().st_size],
        "canonical_runs": [],
        "promotable": True,
    }
    if rollback_bundle:
        bundle_path = Path(rollback_bundle).resolve()
        chain = authenticate_rollback_bundle_chain(bundle_path)
        bundle = chain[-1][1]
        state["rollback_bundle_path"] = str(bundle_path)
        state["rollback_bundle_sha256"] = sha256_file(bundle_path)
        state["rollback_bundle"] = bundle
        state["rollback_bundle_generations"] = [{"path": str(path), "sha256": sha256_file(path), "generation": value["generation"]} for path, value in chain]
        repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(Path.cwd(), command_runner)
        head, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
        tree, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD^{tree}"], cwd=repo_root)
        latest = bundle["decisions"][-1]
        post_commit = latest.get("post_rollback_commit")
        old_commits = {decision.get("old_commit") for decision in bundle["decisions"]}
        if head.strip() in old_commits:
            raise error(bundle_path, "current HEAD={} is an archived pre-removal head".format(head.strip()), "checkout the authenticated post-removal commit before resolve-jar")
        descent = _run(command_runner, ["git", "merge-base", "--is-ancestor", post_commit, head.strip()], cwd=repo_root)
        if descent.returncode != 0:
            raise error(bundle_path, "current source head={} does not descend from post_rollback_commit={}".format(head.strip(), post_commit), "checkout the authenticated rollback lineage before resolving a fresh JAR")
        if head.strip() == post_commit and tree.strip() != latest.get("post_rollback_tree"):
            raise error(bundle_path, "post-removal tree observed={} expected={}".format(tree.strip(), latest.get("post_rollback_tree")), "restore the authenticated post-removal tree")
        state["source_commit"] = head.strip(); state["source_tree"] = tree.strip()
    atomic_write_json(state_path, state, fail_if_exists=True)
    return state


def verify_pinned_jar(state, state_path):
    jar = Path(state.get("benchmark_jar_path", ""))
    try:
        jar_stat = jar.lstat()
    except OSError:
        jar_stat = None
    if not jar.is_absolute() or jar_stat is None or not stat.S_ISREG(jar_stat.st_mode):
        raise error(state_path, "benchmark_jar_path={} is not an absolute regular file".format(jar), "run resolve-jar again from a fresh state path")
    observed_stat = [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size]
    if observed_stat != state.get("benchmark_jar_stat"):
        raise error(state_path, "benchmark JAR stat observed={} expected={}".format(observed_stat, state.get("benchmark_jar_stat")), "discard the state and resolve the exact rebuilt JAR")
    observed = sha256_file(jar)
    expected = state.get("benchmark_jar_sha256")
    if observed != expected:
        raise error(state_path, "benchmark JAR sha256 observed={} expected={}".format(observed, expected), "discard the state and resolve the rebuilt JAR")
    return jar


def prepare_private_execution_jar(state, state_path, run_dir):
    source = verify_pinned_jar(state, state_path)
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    source_fd = os.open(source, flags)
    try:
        before = os.fstat(source_fd)
        observed_stat = [before.st_dev, before.st_ino, before.st_size]
        if observed_stat != state.get("benchmark_jar_stat"):
            raise error(source, "opened JAR stat observed={} expected={}".format(observed_stat, state.get("benchmark_jar_stat")), "resolve a fresh state after any JAR replacement")
        chunks = []
        while True:
            chunk = os.read(source_fd, 1024 * 1024)
            if not chunk:
                break
            chunks.append(chunk)
        payload = b"".join(chunks)
        after = os.fstat(source_fd)
        after_stat = [after.st_dev, after.st_ino, after.st_size]
        observed_hash = sha256_bytes(payload)
        if after_stat != observed_stat or observed_hash != state.get("benchmark_jar_sha256"):
            raise error(source, "opened JAR changed stat {} -> {} or sha256 observed={} expected={}".format(observed_stat, after_stat, observed_hash, state.get("benchmark_jar_sha256")), "discard the state and rebuild once")
    finally:
        os.close(source_fd)
    private_dir = Path(run_dir) / (".pinned-execution-" + secrets.token_hex(8))
    private = private_dir / "benchmark-JMH.jar"
    try:
        private_dir.mkdir(mode=0o700)
        atomic_write_bytes(private, payload, fail_if_exists=True)
        directory_fd = os.open(private_dir, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0))
        try:
            os.fsync(directory_fd)
        finally:
            os.close(directory_fd)
        os.chmod(private, 0o400)
        os.chmod(private_dir, 0o500)
        directory_stat = private_dir.lstat()
        jar_stat = private.lstat()
        identity = {
            "directory_path": str(private_dir.resolve()),
            "directory_stat": [directory_stat.st_dev, directory_stat.st_ino],
            "jar_path": str(private.resolve()),
            "jar_stat": [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size],
            "sha256": state["benchmark_jar_sha256"],
        }
        verify_private_execution_jar(identity, state_path)
        return private.resolve(), identity
    except Exception:
        cleanup_private_execution_jar({"directory_path": str(private_dir), "jar_path": str(private)})
        raise


@contextlib.contextmanager
def private_execution_jar(state, state_path, run_dir):
    identity = None
    try:
        private, identity = prepare_private_execution_jar(state, state_path, run_dir)
        yield private, identity
    finally:
        if identity is not None:
            cleanup_private_execution_jar(identity)


def verify_private_execution_jar(identity, state_path):
    directory = Path(identity["directory_path"])
    jar = Path(identity["jar_path"])
    try:
        directory_stat = directory.lstat()
        jar_stat = jar.lstat()
    except OSError as exc:
        raise error(jar, "private execution JAR lstat failed ({})".format(exc), "discard the run; the isolated execution path changed")
    observed_directory = [directory_stat.st_dev, directory_stat.st_ino]
    observed_jar = [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size]
    if stat.S_ISLNK(directory_stat.st_mode) or not stat.S_ISDIR(directory_stat.st_mode):
        raise error(directory, "private execution directory is not a non-symlink directory", "discard the run; the isolated execution directory changed")
    if stat.S_IMODE(directory_stat.st_mode) != 0o500:
        raise error(directory, "private execution directory mode={:o} expected=500".format(stat.S_IMODE(directory_stat.st_mode)), "discard the run; restore isolated permissions")
    if stat.S_ISLNK(jar_stat.st_mode) or not stat.S_ISREG(jar_stat.st_mode):
        raise error(jar, "private execution JAR is not a non-symlink regular file", "discard the run; the isolated execution path changed")
    if stat.S_IMODE(jar_stat.st_mode) != 0o400:
        raise error(jar, "private execution JAR mode={:o} expected=400".format(stat.S_IMODE(jar_stat.st_mode)), "discard the run; restore isolated permissions")
    if observed_directory != identity.get("directory_stat") or observed_jar != identity.get("jar_stat"):
        raise error(jar, "private execution identity observed={}/{} expected={}/{}".format(observed_directory, observed_jar, identity.get("directory_stat"), identity.get("jar_stat")), "discard the run; the isolated execution path was replaced")
    if jar.parent != directory or jar.resolve().parent != directory.resolve():
        raise error(jar, "private execution JAR escaped directory {}".format(directory), "discard the run; restore the isolated pathname")
    observed_hash = sha256_file(jar)
    if observed_hash != identity.get("sha256"):
        raise error(jar, "private execution JAR sha256 observed={} expected={}".format(observed_hash, identity.get("sha256")), "discard the run; executed bytes were not pinned")
    return jar


def cleanup_private_execution_jar(identity):
    directory = Path(identity.get("directory_path", ""))
    jar = Path(identity.get("jar_path", ""))
    try:
        directory_stat = directory.lstat()
    except OSError:
        return
    expected_directory = identity.get("directory_stat")
    observed_directory = [directory_stat.st_dev, directory_stat.st_ino]
    if stat.S_ISLNK(directory_stat.st_mode) or not stat.S_ISDIR(directory_stat.st_mode):
        return
    if expected_directory is not None and observed_directory != expected_directory:
        return
    try:
        os.chmod(directory, 0o700)
    except OSError:
        return
    try:
        jar_stat = jar.lstat()
    except OSError:
        jar_stat = None
    if jar_stat is not None:
        try:
            if stat.S_ISLNK(jar_stat.st_mode) or stat.S_ISREG(jar_stat.st_mode):
                jar.unlink()
        except OSError:
            pass
    try:
        directory.rmdir()
    except OSError:
        pass


def append_canonical_run(state, run, state_path="<state>"):
    runs = state.setdefault("canonical_runs", [])
    if any(item.get("run_id") == run.get("run_id") or item.get("absolute_path") == run.get("absolute_path") for item in runs):
        raise error(state_path, "duplicate canonical run id/path", "choose a fresh --run-id and directory")
    if len(runs) >= 2:
        raise error(state_path, "canonical state already has exactly two runs", "create a fresh state for another measurement")
    runs.append(run)


def execute_logged(argv, run_dir, command_runner=subprocess.run, pass_fds=()):
    run_dir = Path(run_dir)
    run_dir.mkdir(parents=True, exist_ok=True)
    started = utc_now()
    try:
        result = _run(command_runner, argv, pass_fds=pass_fds)
        stdout, stderr, exit_code = _stdout(result), _stderr(result), result.returncode
    except OSError as exc:
        stdout, stderr, exit_code = b"", str(exc).encode("utf-8"), 127
    ended = utc_now()
    record = {"schema_version": SCHEMA_VERSION, "argv": list(argv), "started_at": started, "ended_at": ended, "exit_code": exit_code}
    atomic_write_json(run_dir / "argv.json", record, fail_if_exists=True)
    atomic_write_bytes(run_dir / "run.log", stdout + stderr + "exit_code={}\n".format(exit_code).encode("ascii"), fail_if_exists=True)
    if exit_code:
        raise error(run_dir / "run.log", "benchmark process exit_code={}".format(exit_code), "inspect run.log, repair the benchmark, and use a fresh run ID")
    return record


def build_jmh_argv(jar, profile, result_path):
    return ["java", "-jar", str(jar)] + PROFILE_ARGS[profile] + ["-rff", str(result_path), "-jvmArgsAppend", " ".join(JVM_ARGS)]


def _identity_capture(repo_root, command_runner):
    commit, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
    tree, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD^{tree}"], cwd=repo_root)
    java, java_err, java_code = command_text(command_runner, ["java", "-XshowSettings:properties", "-version"], cwd=repo_root, allow_failure=True)
    gradle, gradle_err, gradle_code = command_text(command_runner, [str(repo_root / "gradlew"), "--version"], cwd=repo_root, allow_failure=True)
    java_properties = {}
    for line in (java + "\n" + java_err).splitlines():
        match = re.match(r"\s*([A-Za-z0-9_.-]+)\s*=\s*(.*?)\s*$", line)
        if match:
            java_properties[match.group(1)] = match.group(2)
    gradle_match = re.search(r"^Gradle\s+([^\s]+)", gradle, re.MULTILINE)
    required_java = ("java.vendor", "java.version", "java.vm.name", "java.vm.version")
    if java_code or any(key not in java_properties for key in required_java):
        raise error(repo_root, "JVM identity exit_code={} properties={}".format(java_code, {key: java_properties.get(key) for key in required_java}), "repair Java 21 and rerun environment capture")
    if gradle_code or not gradle_match:
        raise error(repo_root / "gradlew", "Gradle identity exit_code={} version={!r}".format(gradle_code, gradle_match.group(1) if gradle_match else None), "repair the Gradle wrapper and rerun environment capture")
    return {
        "git_commit": commit.strip(), "tree_hash": tree.strip(),
        "uname": platform.platform(), "os": platform.system(), "arch": platform.machine(), "cpu": platform.processor() or "unknown",
        "java_version_stdout": java, "java_version_stderr": java_err, "java_version_exit_code": java_code,
        "gradle_version_stdout": gradle, "gradle_version_stderr": gradle_err, "gradle_version_exit_code": gradle_code,
        "jvm_vendor": java_properties.get("java.vendor", "unknown"),
        "jvm_version": java_properties.get("java.version", "unknown"),
        "jdk_version": java_properties.get("java.version", "unknown"),
        "vm_name": java_properties.get("java.vm.name", "unknown"),
        "vm_version": java_properties.get("java.vm.version", "unknown"),
        "gradle_version": gradle_match.group(1) if gradle_match else "unknown",
    }


def jmh_version_from_jar(jar):
    try:
        with zipfile.ZipFile(jar) as archive:
            candidates = (
                "META-INF/maven/org.openjdk.jmh/jmh-core/pom.properties",
                "META-INF/MANIFEST.MF",
            )
            for name in candidates:
                try:
                    text = archive.read(name).decode("utf-8", "replace")
                except KeyError:
                    continue
                for pattern in (r"(?m)^version\s*=\s*([^\s]+)", r"(?mi)^JMH-Version:\s*([^\s]+)", r"(?mi)^Implementation-Version:\s*([^\s]+)"):
                    match = re.search(pattern, text)
                    if match:
                        return match.group(1)
    except (OSError, zipfile.BadZipFile):
        pass
    return "unknown"


def capture_jmh_identity(jar, command_runner=subprocess.run, pass_fds=()):
    version = jmh_version_from_jar(jar)
    if version != "unknown":
        return {"normalized": version, "source": "jar-metadata", "command": [], "stdout": "", "stderr": "", "exit_code": 0}
    argv = ["java", "-cp", str(Path(jar).resolve()), "org.openjdk.jmh.Main", "-h"]
    result = _run(command_runner, argv, pass_fds=pass_fds)
    stdout = _stdout(result).decode("utf-8", "replace")
    stderr = _stderr(result).decode("utf-8", "replace")
    match = re.search(r"(?mi)JMH\s+version\s*[:=]\s*([^\s]+)", stdout + "\n" + stderr)
    if result.returncode or not match:
        raise error(jar, "JMH identity exit_code={} stdout={!r} stderr={!r}".format(result.returncode, stdout, stderr), "rebuild the pinned JMH JAR with an observable JMH version")
    return {"normalized": match.group(1), "source": "jmh-help", "command": argv, "stdout": stdout, "stderr": stderr, "exit_code": result.returncode}


def normalized_profile(profile):
    if profile == "canonical":
        return {"mode": "thrpt", "threads": 1, "forks": 2, "warmups": 3, "measurements": 5, "warmup_time": "1 s", "measurement_time": "1 s", "profiler": "gc", "jvm_args": list(JVM_ARGS)}
    return {"mode": "thrpt", "threads": 1, "forks": 1, "warmups": 1, "measurements": 1, "warmup_time": "1 s", "measurement_time": "1 s", "profiler": "gc", "jvm_args": list(JVM_ARGS)}


def run_benchmark(state_path, profile, output_root, run_id=None, concurrent_heavy_work=None, command_runner=subprocess.run, validator_path=None, repo_root=None):
    state_path = Path(state_path).resolve(); state = load_json(state_path)
    if profile not in PROFILE_ARGS:
        raise error(state_path, "profile={!r}".format(profile), "select smoke or canonical")
    validate_heavy_work(profile, concurrent_heavy_work, state_path)
    verify_pinned_jar(state, state_path)
    repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(Path.cwd(), command_runner)
    initial = require_clean_tree(repo_root, "initial", command_runner)
    run_id = run_id or generate_run_id()
    if not re.match(r"^[A-Za-z0-9._-]+$", run_id):
        raise error(output_root, "unsafe run_id={!r}".format(run_id), "use letters, numbers, dot, underscore, or hyphen")
    run_dir = Path(output_root).resolve() / run_id
    if run_dir.exists():
        raise error(run_dir, "run directory exists", "choose a unique run ID")
    run_dir.mkdir(parents=True)
    canonical_jar = verify_pinned_jar(state, state_path)
    with private_execution_jar(state, state_path, run_dir) as (executed_jar, execution_identity):
        verify_private_execution_jar(execution_identity, state_path)
        first = capture_metadata(executed_jar, command_runner=command_runner)
        verify_private_execution_jar(execution_identity, state_path)
        atomic_write_json(run_dir / "metadata.json", first["value"], fail_if_exists=True)
        identity = _identity_capture(repo_root, command_runner)
        result_path = run_dir / "jmh.json"
        argv = build_jmh_argv(executed_jar, profile, result_path)
        environment = dict(identity)
        metadata = first["value"]
        power = capture_power_state(identity["os"], command_runner)
        verify_private_execution_jar(execution_identity, state_path)
        jmh_identity = capture_jmh_identity(executed_jar, command_runner)
        verify_private_execution_jar(execution_identity, state_path)
        environment.update({
            "schema_version": SCHEMA_VERSION, "run_id": run_id, "profile": profile,
            "benchmark_jar_path": str(canonical_jar), "benchmark_jar_sha256": state["benchmark_jar_sha256"],
            "benchmark_jar_stat": state["benchmark_jar_stat"], "executed_jar_path": str(executed_jar), "executed_jar_stat": execution_identity["jar_stat"],
            "metadata": metadata, "metadata_stdout": first["stdout"].decode("utf-8"),
            "metadata_stdout_sha256": first["stdout_sha256"], "metadata_stderr": first["stderr"],
            "clean_status": "clean", "initial_clean_status": initial,
            "concurrent_heavy_work": concurrent_heavy_work,
            "power_state": power["normalized"], "power_state_capture": power, "jmh_argv": argv,
            "jmh_version": jmh_identity["normalized"], "jmh_version_capture": jmh_identity,
            "payload_size": metadata.get("payload_size"), "payload_sha256": metadata.get("payload_sha256"),
            "config_json": metadata.get("config_json"), "config_sha256": metadata.get("config_sha256"),
            "matrix_version": metadata.get("matrix_version"), "target_headroom": metadata.get("target_headroom"),
            "target_start": metadata.get("target_start"), "rollback_bundle_sha256": state.get("rollback_bundle_sha256"),
        })
        environment.update(normalized_profile(profile))
        prelaunch = require_clean_tree(repo_root, "pre-launch", command_runner)
        verify_private_execution_jar(execution_identity, state_path)
        second = capture_metadata(executed_jar, expected=first, command_runner=command_runner)
        verify_private_execution_jar(execution_identity, state_path)
        environment["prelaunch_clean_status"] = prelaunch
        environment["metadata_prelaunch_stdout_sha256"] = second["stdout_sha256"]
        atomic_write_json(run_dir / "environment.json", environment, fail_if_exists=True)
        verify_private_execution_jar(execution_identity, state_path)
        execute_logged(argv, run_dir, command_runner=command_runner)
        verify_private_execution_jar(execution_identity, state_path)
        verify_pinned_jar(state, state_path)
        validator_path = Path(validator_path or Path(__file__).with_name("validate-jmh.py")).resolve()
        validation_argv = [sys.executable, str(validator_path), "run", "--jar", str(canonical_jar), "--input", str(result_path), "--environment", str(run_dir / "environment.json"), "--summary", str(run_dir / "summary.csv"), "--validation", str(run_dir / "validation.json")]
        verify_private_execution_jar(execution_identity, state_path)
        validator_result = _run(command_runner, validation_argv, cwd=repo_root)
        verify_private_execution_jar(execution_identity, state_path)
        if validator_result.returncode:
            raise error(run_dir, "validator exit_code={} stdout={!r} stderr={!r}".format(validator_result.returncode, _stdout(validator_result).decode("utf-8", "replace"), _stderr(validator_result).decode("utf-8", "replace")), "inspect artifacts and rerun with a fresh run ID")
    if profile == "canonical":
        file_hashes = {name: sha256_file(run_dir / name) for name in REQUIRED_RUN_FILES}
        record = {"run_id": run_id, "absolute_path": str(run_dir), "files": file_hashes,
                  "environment_sha256": file_hashes["environment.json"], "summary_sha256": file_hashes["summary.csv"],
                  "validation_sha256": file_hashes["validation.json"]}
        append_canonical_run(state, record, state_path)
        atomic_write_json(state_path, state)
    print("argv={}".format(json.dumps(argv)))
    print("artifact_dir={}".format(run_dir))
    return run_dir


def compare_state(state_path, output, validation, command_runner=subprocess.run, validator_path=None):
    state_path = Path(state_path).resolve(); state = load_json(state_path)
    runs = state.get("canonical_runs", [])
    if len(runs) != 2:
        raise error(state_path, "canonical run count={} != 2".format(len(runs)), "collect exactly two successful canonical runs")
    for item in runs:
        root = Path(item["absolute_path"])
        for name, key in (("environment.json", "environment_sha256"), ("summary.csv", "summary_sha256"), ("validation.json", "validation_sha256")):
            observed = sha256_file(root / name)
            if observed != item[key]:
                raise error(root / name, "sha256 observed={} expected={}".format(observed, item[key]), "discard tampered evidence and collect two fresh runs")
    validator_path = Path(validator_path or Path(__file__).with_name("validate-jmh.py")).resolve()
    argv = [sys.executable, str(validator_path), "compare"]
    for item in runs:
        argv += ["--run", str(Path(item["absolute_path"]) / "summary.csv")]
    for item in runs:
        argv += ["--environment", str(Path(item["absolute_path"]) / "environment.json")]
    argv += ["--output", str(Path(output).resolve()), "--validation", str(Path(validation).resolve())]
    if state.get("rollback_bundle_path"):
        argv += ["--rollback-bundle", state["rollback_bundle_path"]]
    result = _run(command_runner, argv)
    if result.returncode:
        raise error(state_path, "comparison validator exit_code={} stderr={!r}".format(result.returncode, _stderr(result).decode("utf-8", "replace")), "repair inputs or collect fresh runs")
    state.update({"comparison_path": str(Path(output).resolve()), "comparison_sha256": sha256_file(output), "comparison_validation_path": str(Path(validation).resolve()), "comparison_validation_sha256": sha256_file(validation)})
    atomic_write_json(state_path, state)
    return state


def atomic_promote(source, destination):
    source = Path(source); destination = Path(destination)
    if destination.exists():
        raise error(destination, "destination exists", "use replace-promoted only after verifying the tracked manifest")
    destination.parent.mkdir(parents=True, exist_ok=True)
    _rename(source, destination)


def _rename(source, destination):
    Path(source).rename(destination)


def atomic_replace_promoted(source, destination, backup_root):
    source = Path(source); destination = Path(destination); backup_root = Path(backup_root)
    if not source.is_dir() or not destination.is_dir():
        raise error(destination, "replacement requires existing source={} and destination={} directories".format(source, destination), "build and verify both directories first")
    backup_root.mkdir(parents=True, exist_ok=True)
    backup = backup_root / ("backup-" + generate_run_id())
    if backup.exists():
        raise error(backup, "backup collision", "retry to allocate another backup generation")
    _rename(destination, backup)
    try:
        _rename(source, destination)
    except Exception as primary:
        try:
            _rename(backup, destination)
        except Exception as restore:
            raise error(backup, "replacement failed={!r}; restore failed={!r}; backup preserved".format(primary, restore), "remove the unexpected destination and restore this unique backup manually") from primary
        raise
    return backup.resolve()


def cleanup_recorded_backup(state, requested, backup_root):
    requested_raw = Path(os.path.abspath(str(requested)))
    recorded_raw = Path(os.path.abspath(str(state.get("replacement_backup_path", "__missing__"))))
    requested = requested_raw.parent.resolve() / requested_raw.name
    recorded = recorded_raw.parent.resolve() / recorded_raw.name
    backup_root = Path(backup_root).resolve()
    if requested != recorded:
        raise error(requested, "requested path != recorded backup {}".format(recorded), "pass the exact state-recorded backup")
    if requested.parent != backup_root or not re.fullmatch(r"backup-run-\d{8}T\d{6}\.\d{6}Z-[0-9a-f]{8}", requested.name):
        raise error(requested, "recorded backup is not one generated direct child of {}".format(backup_root), "refuse cleanup and inspect state for tampering")
    try:
        mode = requested.lstat().st_mode
    except OSError as exc:
        raise error(requested, "recorded backup lstat failed ({})".format(exc), "inspect replacement state")
    if stat.S_ISLNK(mode) or not stat.S_ISDIR(mode):
        raise error(requested, "recorded backup is not a non-symlink directory", "inspect replacement state")
    shutil.rmtree(requested)


def validate_manifest(manifest, manifest_path):
    files = manifest.get("files")
    if not isinstance(files, list) or not files:
        raise error(manifest_path, "files must be a non-empty array", "rerun verify-promoted")
    seen = set()
    for item in files:
        value = item.get("path", "")
        path = Path(value)
        if path.is_absolute():
            raise error(manifest_path, "absolute promoted path={!r}".format(value), "record canonical repo-relative paths only")
        if ".." in path.parts or value in seen:
            raise error(manifest_path, "unsafe or duplicate path={!r}".format(value), "regenerate a canonical manifest")
        if not re.match(r"^[0-9a-f]{64}$", str(item.get("sha256", ""))):
            raise error(manifest_path, "invalid sha256 for {}: {!r}".format(value, item.get("sha256")), "regenerate the manifest from verified bytes")
        seen.add(value)
    if manifest.get("schema_version") != SCHEMA_VERSION:
        raise error(manifest_path, "schema_version={} != {}".format(manifest.get("schema_version"), SCHEMA_VERSION), "regenerate the delivery manifest")
    for key, value in _walk_strings(manifest):
        if isinstance(value, str):
            absolute = Path(value).is_absolute() or bool(re.search(r"(?:^|[= ])/(?:[^ ]+)", value))
            build_token = bool(re.search(r"(?:^|[/\\])build(?:[/\\]|$)", value))
            if absolute or build_token:
                raise error(manifest_path, "absolute/build token in {}={!r}".format(key, value), "record canonical repo-relative promoted paths and redacted pinned-JAR tokens only")
    return manifest


def _walk_strings(value, prefix=""):
    if isinstance(value, dict):
        for key, child in value.items():
            yield from _walk_strings(child, key)
    elif isinstance(value, list):
        for child in value:
            yield from _walk_strings(child, prefix)
    else:
        yield prefix, value


def verify_manifest_files(manifest, repo_root, manifest_path):
    validate_manifest(manifest, manifest_path)
    repo_root = Path(repo_root).resolve()
    for item in manifest["files"]:
        path = (repo_root / item["path"]).resolve()
        try:
            path.relative_to(repo_root)
        except ValueError:
            raise error(manifest_path, "path escapes repository: {}".format(item["path"]), "regenerate the manifest")
        if not path.is_file():
            raise error(path, "manifest file is missing", "restore committed evidence")
        observed = sha256_file(path)
        if observed != item["sha256"]:
            raise error(path, "sha256 observed={} expected={}".format(observed, item["sha256"]), "restore committed bytes or regenerate evidence")
    return True


def create_delivery_manifest(repo_root, destination, commit, tree_hash, final_verdicts=None, rollback=None, commands=None, results=None, delivery_commit=None, benchmark_jar_sha256=None, final_reasons=None):
    repo_root = Path(repo_root).resolve(); destination = Path(destination).resolve()
    files = []
    for path in sorted(destination.rglob("*")):
        if path.is_file() and path.name != "delivery-manifest.json":
            relative = path.relative_to(repo_root)
            files.append({"path": relative.as_posix(), "sha256": sha256_file(path)})
    manifest = {
        "schema_version": SCHEMA_VERSION,
        "files": files,
        "measurement": {"git_commit": commit, "tree_hash": tree_hash, "benchmark_jar_sha256": benchmark_jar_sha256},
        "delivery": {"git_commit": delivery_commit or commit},
        "final_verdicts": final_verdicts or [],
        "final_reasons": final_reasons or {},
        "rollback": rollback or {"decisions": []},
        "commands": commands or [],
        "results": results or [],
    }
    manifest["report_input_sha256"] = sha256_bytes(payload_json_bytes({key: manifest[key] for key in ("measurement", "delivery", "final_verdicts", "final_reasons", "rollback", "commands", "results")}))
    validate_manifest(manifest, destination / "delivery-manifest.json")
    return manifest


def validate_committed(manifest_path, repo_root=None, require_git_commit=True, command_runner=subprocess.run):
    manifest_path = Path(manifest_path).resolve()
    repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(manifest_path.parent, command_runner)
    manifest = load_json(manifest_path)
    verify_manifest_files(manifest, repo_root, manifest_path)
    report_hash = sha256_bytes(payload_json_bytes({key: manifest.get(key) for key in ("measurement", "delivery", "final_verdicts", "final_reasons", "rollback", "commands", "results")}))
    if report_hash != manifest.get("report_input_sha256"):
        raise error(manifest_path, "report_input_sha256 observed={} expected={}".format(report_hash, manifest.get("report_input_sha256")), "rerun verify-promoted")
    validate_committed_semantics(manifest, manifest_path, repo_root)
    if require_git_commit:
        relative = manifest_path.relative_to(repo_root).as_posix()
        result = _run(command_runner, ["git", "show", "HEAD:" + relative], cwd=repo_root)
        if result.returncode or _stdout(result) != manifest_path.read_bytes():
            raise error(manifest_path, "working manifest is not byte-identical to HEAD", "commit the verified manifest, then rerun validate-committed")
        head, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
        delivery = manifest.get("delivery", {}).get("git_commit")
        if not isinstance(delivery, str) or not delivery:
            raise error(manifest_path, "missing delivery git_commit", "rerun verify-promoted from a committed delivery head")
        ancestor = _run(command_runner, ["git", "merge-base", "--is-ancestor", delivery, head.strip()], cwd=repo_root)
        if ancestor.returncode:
            raise error(manifest_path, "delivery git_commit={} is not an ancestor of committed HEAD={}".format(delivery, head.strip()), "restore the verified delivery provenance")
    return manifest


def _load_validator():
    path = Path(__file__).with_name("validate-jmh.py")
    spec = importlib.util.spec_from_file_location("issue757_validate_jmh", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def validate_committed_semantics(manifest, manifest_path, repo_root):
    validator = _load_validator()
    paths = [(repo_root / item["path"]).resolve() for item in manifest["files"]]
    listed = set(paths)
    actual = {path.resolve() for path in manifest_path.parent.rglob("*") if path.is_file() and path.resolve() != manifest_path.resolve()}
    if listed != actual:
        raise error(manifest_path, "delivery file set observed={} expected={}".format(sorted(str(path) for path in listed), sorted(str(path) for path in actual)), "restore the complete manifest-bound delivery directory")
    for name in ("comparison.csv", "validation.json"):
        if (manifest_path.parent / name).resolve() not in listed:
            raise error(manifest_path, "missing top-level {}".format(name), "restore comparison and comparison-validation artifacts")
    environments = sorted(path for path in paths if path.name == "environment.json")
    if len(environments) != 2:
        raise error(manifest_path, "environment count={} != 2".format(len(environments)), "restore exactly two promoted canonical runs")
    reconstructed = []
    jar_hashes = set()
    with tempfile.TemporaryDirectory(prefix="issue-757-committed-") as td:
        scratch = Path(td)
        for environment_path in environments:
            run_root = environment_path.parent
            actual = sorted(path.name for path in run_root.iterdir() if path.is_file())
            if actual != sorted(REQUIRED_RUN_FILES):
                raise error(run_root, "committed run file set observed={} expected={}".format(actual, sorted(REQUIRED_RUN_FILES)), "restore the exact promoted run")
            environment = load_json(environment_path)
            measurement = manifest.get("measurement", {})
            if environment.get("git_commit") != measurement.get("git_commit") or environment.get("tree_hash") != measurement.get("tree_hash"):
                raise error(manifest_path, "measurement commit/tree={} differs from {} environment commit/tree={}".format({"git_commit": measurement.get("git_commit"), "tree_hash": measurement.get("tree_hash")}, environment_path, {"git_commit": environment.get("git_commit"), "tree_hash": environment.get("tree_hash")}), "bind delivery to both exact measurement environments")
            metadata = load_json(run_root / "metadata.json")
            if metadata != environment.get("metadata"):
                raise error(run_root / "metadata.json", "metadata file != environment metadata", "restore the runner-captured metadata")
            stdout = environment.get("metadata_stdout", "").encode("utf-8")
            if sha256_bytes(stdout) != environment.get("metadata_stdout_sha256") or environment.get("metadata_prelaunch_stdout_sha256") != environment.get("metadata_stdout_sha256"):
                raise error(environment_path, "metadata double-capture hash mismatch", "recollect evidence from the unchanged pinned JAR")
            argv = load_json(run_root / "argv.json")
            if argv.get("argv") != environment.get("jmh_argv") or argv.get("exit_code") != 0 or not (run_root / "run.log").read_text(encoding="utf-8").endswith("exit_code=0\n"):
                raise error(run_root / "argv.json", "argv/log/exit evidence mismatch", "restore the complete successful runner artifacts")
            if not validator.is_clean_environment(environment):
                raise error(environment_path, "committed clean gates are not clean", "restore both exact runner clean observations")
            records = validator._load_json(run_root / "jmh.json")
            parsed = validator.parse_jmh_records(records, str(run_root / "jmh.json"))
            validator.validate_manifest_observations(environment, parsed, str(environment_path))
            validator.validate_environment_identity(environment, str(environment_path))
            generated_summary = scratch / (environment["run_id"] + "-summary.csv")
            validator.write_summary(generated_summary, environment["run_id"], parsed)
            if generated_summary.read_bytes() != (run_root / "summary.csv").read_bytes():
                raise error(run_root / "summary.csv", "reconstructed summary differs", "restore raw JMH and derived summary parity")
            validation = load_json(run_root / "validation.json")
            expected_fields = {"status": "passed", "mode": "run", "run_id": environment["run_id"],
                               "benchmark_jar_sha256": environment["benchmark_jar_sha256"],
                               "config_sha256": environment["config_sha256"],
                               "observed_config": parsed["observed_config"],
                               "observed_config_sha256": parsed["observed_config_sha256"],
                               "method_count": len(parsed["rows"])}
            for key, expected in expected_fields.items():
                if validation.get(key) != expected:
                    raise error(run_root / "validation.json", "{} observed={} expected={}".format(key, validation.get(key), expected), "restore the per-run validation")
            jar_hashes.add(environment["benchmark_jar_sha256"])
            reconstructed.append((validator.read_summary(generated_summary), environment, generated_summary))
        if len(jar_hashes) != 1 or next(iter(jar_hashes)) != manifest.get("measurement", {}).get("benchmark_jar_sha256"):
            raise error(manifest_path, "pinned JAR hash chain run={} manifest={}".format(sorted(jar_hashes), manifest.get("measurement", {}).get("benchmark_jar_sha256")), "restore the verified pinned-JAR hash chain")
        comparison_validation_path = manifest_path.parent / "validation.json"
        comparison_validation = load_json(comparison_validation_path)
        run_order = comparison_validation.get("run_ids", [])
        by_run = {item[1]["run_id"]: item for item in reconstructed}
        if len(run_order) != 2 or set(run_order) != set(by_run):
            raise error(comparison_validation_path, "run_ids observed={} expected={}".format(run_order, sorted(by_run)), "restore the state-bound comparison run order")
        reconstructed = [by_run[run_id] for run_id in run_order]
        reconstructed_commands = [manifest_command(load_json(environment_path.parent / "argv.json")["argv"], load_json(environment_path), environment_path.parent, repo_root) for environment_path in [next(path for path in environments if load_json(path)["run_id"] == run_id) for run_id in run_order]]
        if manifest.get("commands") != reconstructed_commands:
            raise error(manifest_path, "normalized commands observed={} expected={}".format(manifest.get("commands"), reconstructed_commands), "rerun verify-promoted from both exact argv.json files")
        validator.validate_identity(reconstructed[0][1], reconstructed[1][1], str(environments[0]), str(environments[1]))
        bundle_paths = sorted(path for path in paths if path.name.startswith("rollback-bundle-g"))
        rollback = None
        if bundle_paths:
            chain = authenticate_rollback_bundle_chain(bundle_paths[-1])
            if {path for path, _ in chain} != set(bundle_paths):
                raise error(manifest_path, "committed rollback generation file set is incomplete or extra", "restore the exact immutable bundle chain")
            if chain[-1][1] != manifest.get("rollback"):
                raise error(manifest_path, "manifest rollback payload differs from latest committed bundle", "rerun verify-promoted with the authenticated bundle")
            rollback = validator.validate_rollback_bundle(bundle_paths[-1])
        elif manifest.get("rollback") != {"decisions": []}:
            raise error(manifest_path, "rollback payload exists without committed rollback bundle files", "restore the complete immutable rollback bundle chain")
        comparison = validator.compare_runs(reconstructed[0][0], reconstructed[1][0], rollback["ineligible_cells"] if rollback else None, str(manifest_path))
        generated_comparison = scratch / "comparison.csv"
        validator.write_comparison(generated_comparison, comparison)
        comparison_path = manifest_path.parent / "comparison.csv"
        if generated_comparison.read_bytes() != comparison_path.read_bytes():
            raise error(comparison_path, "reconstructed comparison differs", "restore both summaries and comparison")
        verdicts = {method: row["verdict"] for method, row in sorted(comparison.items())}
        reasons = {method: row["reason"] for method, row in sorted(comparison.items())}
        expected_comparison_validation = {
            "status": "passed", "mode": "compare", "run_ids": run_order,
            "comparison_sha256": sha256_file(comparison_path), "verdicts": verdicts, "reasons": reasons,
            "rollback_bundle_sha256": rollback["sha256"] if rollback else None,
        }
        for key, expected in expected_comparison_validation.items():
            if comparison_validation.get(key) != expected:
                raise error(comparison_validation_path, "{} observed={} expected={}".format(key, comparison_validation.get(key), expected), "restore the authoritative comparison validation")
        if comparison_validation.get("verdicts") != verdicts or comparison_validation.get("reasons") != reasons:
            raise error(manifest_path.parent / "validation.json", "comparison verdict/reason semantics differ", "restore the authoritative comparison validation")
        if manifest.get("final_verdicts") != verdicts or manifest.get("final_reasons") != reasons:
            raise error(manifest_path, "manifest final verdict/reason differs from reconstructed comparison", "rerun verify-promoted")
        result_keys = {(str(row["method"]), str(row["run"])) for row in manifest.get("results", [])}
        expected_keys = {(method, env["run_id"]) for method in comparison for _, env, _ in reconstructed}
        if result_keys != expected_keys:
            raise error(manifest_path, "report results observed={} expected={}".format(sorted(result_keys), sorted(expected_keys)), "rerun verify-promoted from exact summaries")
        result_map = {(row["method"], row["run"]): row for row in manifest.get("results", [])}
        for run_index, (summary, environment, _) in enumerate(reconstructed):
            for method, metrics in summary["rows"].items():
                row = result_map[(method, environment["run_id"])]
                expected_delta = comparison[method]["run_{}_delta_percent".format("a" if run_index == 0 else "b")]
                if float(row["allocation_b_per_op"]) != metrics["allocation"] or float(row["throughput_ops_per_s"]) != metrics["throughput"]:
                    raise error(manifest_path, "result metric mismatch for {}/{}".format(method, environment["run_id"]), "regenerate report inputs from summary.csv")
                if str(row["delta_percent"]) != str(expected_delta) or row["verdict"] != verdicts[method] or row["reason"] != reasons[method]:
                    raise error(manifest_path, "result verdict/delta/reason mismatch for {}/{}".format(method, environment["run_id"]), "regenerate report inputs from comparison.csv")
    return True


def _decision_payload(decision):
    return {key: decision[key] for key in sorted(decision) if key != "decision_sha256"}


def require_archive_artifact(path, archive_root):
    path = Path(path)
    archive_root = Path(archive_root).resolve()
    try:
        mode = path.lstat().st_mode
    except OSError as exc:
        raise error(path, "rollback artifact lstat failed ({})".format(exc), "restore the complete immutable archive")
    if stat.S_ISLNK(mode) or not stat.S_ISREG(mode):
        raise error(path, "rollback artifact is not a non-symlink regular file", "replace links with archive-owned regular files")
    resolved = path.resolve()
    try:
        resolved.relative_to(archive_root)
    except ValueError:
        raise error(path, "rollback artifact resolves outside archive {}".format(archive_root), "copy complete regular files into the archive first")
    return resolved


def require_symlink_free_tree(root):
    root = Path(root)
    try:
        root_mode = root.lstat().st_mode
    except OSError as exc:
        raise error(root, "rollback source lstat failed ({})".format(exc), "restore the complete state-bound source")
    if stat.S_ISLNK(root_mode) or not stat.S_ISDIR(root_mode):
        raise error(root, "rollback source is not a non-symlink directory", "restore a regular state-bound directory")
    resolved_root = root.resolve()
    for path in root.rglob("*"):
        mode = path.lstat().st_mode
        if stat.S_ISLNK(mode):
            raise error(path, "rollback source contains a symlink", "replace links with state-bound regular files")
        try:
            path.resolve().relative_to(resolved_root)
        except ValueError:
            raise error(path, "rollback source entry resolves outside {}".format(resolved_root), "restore the bounded source tree")


def make_rollback_decision(dispatch, cells, commit, tree_hash, archive_root, files, generation, timestamp, post_rollback_commit=None, lineage_parent_commit=None, post_rollback_tree=None):
    if dispatch not in DISPATCH_CELLS:
        raise error(archive_root, "unknown dispatch={!r}".format(dispatch), "select one of {}".format(DISPATCH_ORDER))
    archive_root = Path(archive_root).resolve()
    require_symlink_free_tree(archive_root)
    artifacts = []
    for item in sorted(Path(item) for item in files):
        path = require_archive_artifact(item, archive_root)
        try:
            relative = path.relative_to(archive_root).as_posix()
        except ValueError:
            raise error(path, "rollback artifact is outside archive {}".format(archive_root), "copy complete evidence into the archive first")
        artifacts.append({"path": relative, "sha256": sha256_file(path)})
    post_commit = post_rollback_commit or commit
    post_tree = post_rollback_tree or tree_hash
    decision = {"dispatch": dispatch, "regressed_cells": sorted(cells), "old_commit": commit, "old_tree": tree_hash,
                "post_rollback_commit": post_commit, "post_rollback_tree": post_tree, "lineage_parent_commit": lineage_parent_commit,
                "removal_evidence": {"dispatch": dispatch, "regressed_cells": sorted(cells), "old_commit": commit, "old_tree": tree_hash, "post_rollback_commit": post_commit, "post_rollback_tree": post_tree, "head_changed": post_commit != commit, "tree_changed": post_tree != tree_hash},
                "archive_root": archive_root.name, "artifacts": artifacts, "timestamp": timestamp, "generation": generation}
    decision["decision_sha256"] = sha256_bytes(payload_json_bytes(_decision_payload(decision)))
    return decision


def _bundle_payload(bundle):
    def strip_hashes(value):
        if isinstance(value, dict):
            return {key: strip_hashes(child) for key, child in value.items() if key not in ("bundle_sha256", "decision_sha256")}
        if isinstance(value, list):
            return [strip_hashes(child) for child in value]
        return value
    return strip_hashes(bundle)


def write_rollback_bundle(archive_root, decisions, predecessor=None):
    archive_root = Path(archive_root).resolve(); archive_root.mkdir(parents=True, exist_ok=True)
    dispatches = [item.get("dispatch") for item in decisions]
    if len(dispatches) != len(set(dispatches)):
        raise error(archive_root, "duplicate rollback dispatch decisions {}".format(dispatches), "record each dispatch once")
    if any(item not in DISPATCH_ORDER for item in dispatches):
        raise error(archive_root, "unknown dispatch in {}".format(dispatches), "use the fixed dispatch mapping")
    ordered = sorted(decisions, key=lambda item: DISPATCH_ORDER.index(item["dispatch"]))
    if predecessor:
        previous = authenticate_rollback_bundle(predecessor)
        generation = previous["generation"] + 1
        all_decisions = previous["decisions"] + ordered
        predecessor_hash = sha256_file(predecessor)
    else:
        generation = 1
        all_decisions = ordered
        predecessor_hash = None
    all_dispatches = [item["dispatch"] for item in all_decisions]
    if len(all_dispatches) != len(set(all_dispatches)):
        raise error(archive_root, "duplicate or conflicting dispatch lineage {}".format(all_dispatches), "do not decide the same dispatch twice")
    bundle = {"schema_version": SCHEMA_VERSION, "generation": generation, "predecessor_bundle_sha256": predecessor_hash, "decisions": all_decisions}
    bundle["bundle_sha256"] = sha256_bytes(payload_json_bytes(_bundle_payload(bundle)))
    path = archive_root / "rollback-bundle-g{}-{}.json".format(generation, bundle["bundle_sha256"])
    atomic_write_json(path, bundle, fail_if_exists=True)
    return path.resolve()


def authenticate_rollback_bundle(path):
    path = Path(path).resolve(); bundle = load_json(path)
    if bundle.get("schema_version") != SCHEMA_VERSION:
        raise error(path, "rollback schema_version={} != {}".format(bundle.get("schema_version"), SCHEMA_VERSION), "use a bundle generated by this runner")
    observed = sha256_bytes(payload_json_bytes(_bundle_payload(bundle)))
    expected = bundle.get("bundle_sha256")
    if observed != expected:
        raise error(path, "rollback bundle sha256 observed={} expected={}".format(observed, expected), "restore the immutable bundle")
    match = re.search(r"rollback-bundle-g(\d+)-([0-9a-f]{64})\.json$", path.name)
    if not match or int(match.group(1)) != bundle.get("generation") or match.group(2) != expected:
        raise error(path, "bundle filename generation/hash does not match payload", "restore the canonical immutable filename")
    decisions = bundle.get("decisions", [])
    dispatches = [item.get("dispatch") for item in decisions]
    if len(dispatches) != len(set(dispatches)):
        raise error(path, "duplicate or conflicting decisions {}".format(dispatches), "restore one decision per dispatch")
    generations = {item.get("generation") for item in decisions}
    if generations != set(range(1, bundle["generation"] + 1)):
        raise error(path, "missing/reordered generations observed={} expected={}".format(sorted(generations), list(range(1, bundle["generation"] + 1))), "restore every immutable bundle generation")
    for generation in sorted(generations):
        observed_order = [item.get("dispatch") for item in decisions if item.get("generation") == generation]
        expected_order = sorted(observed_order, key=lambda item: DISPATCH_ORDER.index(item) if item in DISPATCH_ORDER else 999)
        if observed_order != expected_order:
            raise error(path, "generation {} dispatch order observed={} expected={}".format(generation, observed_order, expected_order), "restore the fixed dispatch order")
    previous_generation = 0
    previous_post = None
    generation_post = {}
    for item in decisions:
        calculated = sha256_bytes(payload_json_bytes(_decision_payload(item)))
        if calculated != item.get("decision_sha256"):
            raise error(path, "decision {} sha256 observed={} expected={}".format(item.get("dispatch"), calculated, item.get("decision_sha256")), "restore the immutable decision")
        if item.get("generation", 0) > bundle["generation"] or item.get("generation", 0) < 1:
            raise error(path, "decision generation={} is outside bundle generation={}".format(item.get("generation"), bundle["generation"]), "restore complete generation lineage")
        generation = item["generation"]
        post = item.get("post_rollback_commit")
        if not isinstance(post, str) or not post:
            raise error(path, "decision {} missing post_rollback_commit".format(item.get("dispatch")), "restore the authenticated source lineage")
        post_tree = item.get("post_rollback_tree")
        if post == item.get("old_commit") or post_tree == item.get("old_tree"):
            raise error(path, "decision {} does not prove changed post-removal commit/tree".format(item.get("dispatch")), "finalize rollback only after the dispatch-removal commit")
        expected_removal = {"dispatch": item.get("dispatch"), "regressed_cells": item.get("regressed_cells"),
                            "old_commit": item.get("old_commit"), "old_tree": item.get("old_tree"),
                            "post_rollback_commit": post, "post_rollback_tree": post_tree,
                            "head_changed": True, "tree_changed": True}
        if item.get("removal_evidence") != expected_removal:
            raise error(path, "decision {} removal_evidence differs from authenticated lineage".format(item.get("dispatch")), "restore exact post-removal evidence")
        if generation > previous_generation and generation > 1 and item.get("lineage_parent_commit") != previous_post:
            raise error(path, "generation {} lineage_parent_commit={} expected={}".format(generation, item.get("lineage_parent_commit"), previous_post), "restore the exact descending source lineage")
        if generation == 1 and item.get("lineage_parent_commit") is not None:
            raise error(path, "generation 1 lineage_parent_commit={!r}".format(item.get("lineage_parent_commit")), "restore the lineage root")
        if generation in generation_post and generation_post[generation] != post:
            raise error(path, "generation {} post_rollback_commit values differ".format(generation), "bind simultaneous decisions to one post-rollback head")
        generation_post[generation] = post
        archive = path.parent / item.get("archive_root", "")
        require_symlink_free_tree(archive)
        comparison = None
        for artifact in item.get("artifacts", []):
            artifact_path = archive / artifact["path"]
            try:
                artifact_path = require_archive_artifact(artifact_path, archive)
            except ValueError:
                raise
            if sha256_file(artifact_path) != artifact["sha256"]:
                raise error(artifact_path, "rollback archive sha256/missing mismatch", "restore the immutable rollback archive")
            if Path(artifact["path"]).name == "comparison.csv":
                if comparison is not None:
                    raise error(path, "multiple comparison.csv artifacts for {}".format(item.get("dispatch")), "restore exactly one state-bound comparison")
                comparison = artifact_path
        actual_artifacts = sorted(candidate.relative_to(archive).as_posix() for candidate in archive.rglob("*") if candidate.is_file()) if archive.is_dir() else []
        declared_artifacts = sorted(artifact.get("path") for artifact in item.get("artifacts", []))
        if actual_artifacts != declared_artifacts:
            raise error(archive, "archive file set observed={} expected={}".format(actual_artifacts, declared_artifacts), "restore the complete immutable rollback archive")
        if comparison is None:
            raise error(path, "missing comparison.csv artifact for {}".format(item.get("dispatch")), "archive the complete state-bound comparison")
        verdicts = read_comparison_verdicts(comparison)
        expected_cells = sorted(DISPATCH_CELLS.get(item.get("dispatch"), ()))
        if item.get("regressed_cells") != expected_cells:
            raise error(path, "{} regressed_cells observed={} expected={}".format(item.get("dispatch"), item.get("regressed_cells"), expected_cells), "record the complete mapped dispatch cells")
        for cell in expected_cells:
            if verdicts.get(cell) != "regressed":
                raise error(comparison, "{} verdict={} != regressed".format(cell, verdicts.get(cell)), "record rollback only from a state-bound regressed comparison")
        previous_generation = generation
        previous_post = post
    return bundle


def authenticate_rollback_bundle_chain(path):
    path = Path(path).resolve()
    current_path = path
    reversed_chain = []
    while True:
        bundle = authenticate_rollback_bundle(current_path)
        reversed_chain.append((current_path, bundle))
        predecessor_hash = bundle.get("predecessor_bundle_sha256")
        if bundle["generation"] == 1:
            if predecessor_hash is not None:
                raise error(current_path, "generation 1 predecessor_bundle_sha256={!r}".format(predecessor_hash), "restore the root bundle generation")
            break
        if not predecessor_hash:
            raise error(current_path, "generation {} is missing predecessor hash".format(bundle["generation"]), "restore every immutable bundle generation")
        candidates = [candidate for candidate in current_path.parent.glob("rollback-bundle-g{}-*.json".format(bundle["generation"] - 1)) if sha256_file(candidate) == predecessor_hash]
        if len(candidates) != 1:
            raise error(current_path, "predecessor hash={} matched {} generation files".format(predecessor_hash, len(candidates)), "restore exactly one preceding immutable bundle")
        current_path = candidates[0].resolve()
    chain = list(reversed(reversed_chain))
    if [value["generation"] for _, value in chain] != list(range(1, len(chain) + 1)):
        raise error(path, "bundle generations are incomplete", "restore every immutable bundle generation")
    for index in range(1, len(chain)):
        predecessor_path, predecessor = chain[index - 1]
        current_path, current = chain[index]
        if current.get("predecessor_bundle_sha256") != sha256_file(predecessor_path):
            raise error(current_path, "predecessor hash differs from inherited generation", "restore the exact immutable predecessor")
        prefix = current.get("decisions", [])[:len(predecessor.get("decisions", []))]
        if prefix != predecessor.get("decisions", []):
            raise error(current_path, "inherited decision prefix differs from predecessor", "restore the exact authenticated decision prefix")
    return chain


def read_comparison_verdicts(path):
    with Path(path).open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    return {row.get("method") or row.get("candidate"): row.get("verdict") for row in rows}


def record_rollback(state_path, dispatches, archive_root, command_runner=subprocess.run, repo_root=None):
    state_path = Path(state_path).resolve(); state = load_json(state_path)
    if len(dispatches) != len(set(dispatches)):
        raise error(state_path, "duplicate dispatch arguments {}".format(dispatches), "pass each regressed dispatch once")
    verdicts = read_comparison_verdicts(state["comparison_path"])
    required = []
    for dispatch in DISPATCH_ORDER:
        if all(verdicts.get(cell) == "regressed" for cell in DISPATCH_CELLS[dispatch]):
            required.append(dispatch)
    if sorted(dispatches, key=DISPATCH_ORDER.index) != required:
        raise error(state["comparison_path"], "dispatches={} mapped simultaneous regressions={}".format(dispatches, required), "record every and only simultaneously regressed dispatch")
    first_environment = load_json(Path(state["canonical_runs"][0]["absolute_path"]) / "environment.json")
    old_commit = first_environment.get("git_commit"); old_tree = first_environment.get("tree_hash")
    repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(Path.cwd(), command_runner)
    post_commit, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
    post_tree, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD^{tree}"], cwd=repo_root)
    post_commit = post_commit.strip(); post_tree = post_tree.strip()
    if post_commit == old_commit or post_tree == old_tree:
        raise error(state_path, "rollback head/tree unchanged at commit={} tree={}".format(post_commit, post_tree), "remove the regressed dispatch, commit it, then record rollback")
    ancestor = _run(command_runner, ["git", "merge-base", "--is-ancestor", old_commit, post_commit], cwd=repo_root)
    if ancestor.returncode:
        raise error(state_path, "post-removal commit={} does not descend from measurement commit={}".format(post_commit, old_commit), "record rollback on the authenticated descendant branch")
    root = Path(archive_root).resolve(); root.mkdir(parents=True, exist_ok=True)
    generation = state.get("rollback_bundle", {}).get("generation", 0) + 1
    archive = root / "archive-g{}-{}".format(generation, generate_run_id())
    archive.mkdir()
    bundle_path = None
    try:
        files = []
        for run in state.get("canonical_runs", []):
            source = Path(run["absolute_path"])
            require_symlink_free_tree(source)
            target = archive / source.name
            shutil.copytree(source, target, symlinks=True)
            files.extend(path for path in target.rglob("*") if path.is_file())
        for source in (state_path, Path(state["comparison_path"]), Path(state["comparison_validation_path"])):
            source = require_archive_artifact(source, source.parent)
            target = archive / source.name
            shutil.copy2(source, target, follow_symlinks=False); files.append(target)
        predecessor_decisions = state.get("rollback_bundle", {}).get("decisions", [])
        lineage_parent = predecessor_decisions[-1].get("post_rollback_commit") if predecessor_decisions else None
        decisions = [make_rollback_decision(dispatch, list(DISPATCH_CELLS[dispatch]), old_commit, old_tree, archive, files, generation, utc_now(), post_commit, lineage_parent, post_tree) for dispatch in required]
        predecessor = state.get("rollback_bundle_path")
        if predecessor and Path(predecessor).resolve().parent != root:
            raise error(predecessor, "predecessor bundle root differs from archive root {}".format(root), "continue the immutable chain under the original archive root")
        bundle_path = write_rollback_bundle(root, decisions, predecessor=predecessor)
        chain = authenticate_rollback_bundle_chain(bundle_path)
    except Exception:
        if bundle_path and Path(bundle_path).exists():
            Path(bundle_path).unlink()
        shutil.rmtree(archive, ignore_errors=True)
        raise
    state.update({"promotable": False, "rollback_bundle_path": str(bundle_path), "rollback_bundle_sha256": sha256_file(bundle_path), "rollback_bundle": chain[-1][1], "rollback_bundle_generations": [{"path": str(path), "sha256": sha256_file(path), "generation": value["generation"]} for path, value in chain]})
    atomic_write_json(state_path, state)
    print(bundle_path)
    return bundle_path


def render_report_text(manifest):
    lines = [
        "# Protobuf Buffer Allocation Evidence", "",
        "## Scope", "", "Issue #757 allocation evidence generated from the committed delivery manifest.", "",
        "## Provenance", "",
        "- Measurement commit: `{}`".format(manifest.get("measurement", {}).get("git_commit", "unknown")),
        "- Measurement tree: `{}`".format(manifest.get("measurement", {}).get("tree_hash", "unknown")),
        "- Delivery commit: `{}`".format(manifest.get("delivery", {}).get("git_commit", "unknown")), "",
        "## Recorded commands", "",
    ]
    for command in manifest.get("commands", []):
        lines.append("- `" + " ".join(command) + "`")
    lines += ["", "## Measurements", "", "| Method | Run | B/op | ops/s | Delta | Verdict | Reason | Claim |", "|---|---|---:|---:|---:|---|---|---|"]
    for row in manifest.get("results", []):
        accepted = row.get("verdict") == "accepted"
        claim = POSITIVE_PHRASE if accepted else NON_POSITIVE
        lines.append("| {method} | {run} | {allocation_b_per_op} | {throughput_ops_per_s} | {delta_percent}% | {verdict} | {reason} | {claim} |".format(claim=claim, **row))
    lines += ["", "## Rollback decisions", ""]
    decisions = manifest.get("rollback", {}).get("decisions", [])
    if decisions:
        for decision in decisions:
            lines.append("- `{}` removed after regression; archived cells: {}.".format(decision["dispatch"], ", ".join(decision.get("regressed_cells", []))))
    else:
        lines.append("- No rollback decision is recorded.")
    lines += ["", "## Compatibility controls", "", "Fallback and composite controls remain claim-ineligible and are reported without a positive claim.", "", "## Limitations", "", "JMH GC allocation is environment-sensitive; throughput is diagnostic and not the allocation acceptance criterion.", ""]
    report = "\n".join(lines)
    validate_positive_language(report, manifest, Path("generated report"))
    return report


def validate_positive_language(report, manifest, path):
    removed = {cell for decision in manifest.get("rollback", {}).get("decisions", []) for cell in decision.get("regressed_cells", [])}
    verdicts = manifest.get("final_verdicts", {})
    reasons = manifest.get("final_reasons", {})
    accepted = set()
    for row in manifest.get("results", []):
        method = row.get("method")
        if row.get("verdict") != verdicts.get(method) or row.get("reason") != reasons.get(method):
            raise error(path, "result {} verdict/reason contradicts final maps".format(method), "regenerate the report inputs from committed comparison")
        if row.get("verdict") == "accepted":
            if method in removed or row.get("reason") in ("removed_after_regression", "baseline", "compatibility_control"):
                raise error(path, "accepted cell {} is removed/ineligible".format(method), "positive claims require retained accepted candidates only")
            accepted.add(method)
    for line in report.splitlines():
        if POSITIVE_PHRASE.lower() in line.lower() and not any(method and ("| {} |".format(method) in line or line.startswith(method + " ")) for method in accepted):
            raise error(path, "positive reduction language is not tied to a retained accepted cell: {!r}".format(line), "use fixed non-positive wording for inconclusive, regressed, removed, baseline, and compatibility cells")


def render_report(manifest_path, output):
    manifest = validate_committed(manifest_path)
    text = render_report_text(manifest)
    atomic_write_bytes(output, text.encode("utf-8"))


def validate_report(manifest_path, input_path):
    manifest = validate_committed(manifest_path)
    expected = render_report_text(manifest).encode("utf-8")
    observed = Path(input_path).read_bytes()
    if observed != expected:
        raise error(input_path, "tracked report bytes differ from deterministic render", "rerun render-report and commit the exact bytes")
    validate_positive_language(observed.decode("utf-8"), manifest, input_path)


def _copy_state_evidence(state, staging):
    for run in state.get("canonical_runs", []):
        source = Path(run["absolute_path"])
        shutil.copytree(source, staging / source.name)
    for key, name in (("comparison_path", "comparison.csv"), ("comparison_validation_path", "validation.json")):
        source = Path(state[key])
        shutil.copy2(source, staging / name)
    if state.get("rollback_bundle_path"):
        bundle = Path(state["rollback_bundle_path"])
        for generation_path, _ in authenticate_rollback_bundle_chain(bundle):
            shutil.copy2(generation_path, staging / generation_path.name)
        for decision in state["rollback_bundle"]["decisions"]:
            archive = bundle.parent / decision["archive_root"]
            if not (staging / archive.name).exists():
                shutil.copytree(archive, staging / archive.name)


def expected_promoted_files(state):
    expected = set()
    for run in state.get("canonical_runs", []):
        name = Path(run["absolute_path"]).name
        expected.update("{}/{}".format(name, filename) for filename in REQUIRED_RUN_FILES)
    expected.update(("comparison.csv", "validation.json"))
    if state.get("rollback_bundle_path"):
        for bundle_path, _ in authenticate_rollback_bundle_chain(state["rollback_bundle_path"]):
            expected.add(bundle_path.name)
        latest = state["rollback_bundle"]
        for decision in latest.get("decisions", []):
            expected.update("{}/{}".format(decision["archive_root"], artifact["path"]) for artifact in decision.get("artifacts", []))
    return expected


def verify_state_inputs(state, state_path):
    verify_pinned_jar(state, state_path)
    for run in state.get("canonical_runs", []):
        root = Path(run["absolute_path"])
        actual_names = sorted(path.name for path in root.iterdir() if path.is_file()) if root.is_dir() else []
        if actual_names != sorted(REQUIRED_RUN_FILES):
            raise error(root, "run file set observed={} expected={}".format(actual_names, sorted(REQUIRED_RUN_FILES)), "collect a fresh complete canonical run")
        recorded = run.get("files")
        if set(recorded or {}) != set(REQUIRED_RUN_FILES):
            raise error(state_path, "state run file set observed={} expected={}".format(sorted(recorded or {}), sorted(REQUIRED_RUN_FILES)), "resolve fresh state and recollect both runs")
        for name in REQUIRED_RUN_FILES:
            observed = sha256_file(root / name)
            if observed != recorded[name]:
                raise error(root / name, "state-bound sha256 observed={} expected={}".format(observed, recorded[name]), "collect fresh canonical evidence")
    for key, hash_key in (("comparison_path", "comparison_sha256"), ("comparison_validation_path", "comparison_validation_sha256")):
        path = Path(state.get(key, ""))
        observed = sha256_file(path) if path.is_file() else None
        if observed != state.get(hash_key):
            raise error(path, "state-bound sha256 observed={} expected={}".format(observed, state.get(hash_key)), "rerun comparison from the two state-bound runs")
    if state.get("rollback_bundle_path"):
        chain = authenticate_rollback_bundle_chain(state["rollback_bundle_path"])
        bundle = chain[-1][1]
        observed = sha256_file(state["rollback_bundle_path"])
        if observed != state.get("rollback_bundle_sha256") or bundle != state.get("rollback_bundle"):
            raise error(state["rollback_bundle_path"], "imported rollback bundle/hash differs from state", "resolve a fresh state with the authenticated bundle")
        recorded = state.get("rollback_bundle_generations", [])
        actual = [{"path": str(path), "sha256": sha256_file(path), "generation": value["generation"]} for path, value in chain]
        if recorded and recorded != actual:
            raise error(state["rollback_bundle_path"], "rollback generation set differs from state", "resolve a fresh state with the complete immutable chain")


def _semantic_json(value):
    if isinstance(value, dict):
        return {key: _semantic_json(child) for key, child in value.items() if not key.endswith("_path")}
    if isinstance(value, list):
        return [_semantic_json(child) for child in value]
    return value


def manifest_command(argv, environment, promoted_run, repo_root):
    normalized = []
    index = 0
    while index < len(argv):
        token = argv[index]
        if token in (environment.get("benchmark_jar_path"), environment.get("executed_jar_path")):
            normalized.append("<PINNED_JAR_SHA256:{}>".format(environment["benchmark_jar_sha256"]))
        elif index > 0 and argv[index - 1] == "-rff":
            normalized.append((Path(promoted_run) / "jmh.json").relative_to(repo_root).as_posix())
        elif Path(token).is_absolute() or re.search(r"(?:^|[/\\])build(?:[/\\]|$)", token):
            raise error(promoted_run / "argv.json", "unsafe command token={!r}".format(token), "normalize only the pinned JAR and promoted result path")
        else:
            normalized.append(token)
        index += 1
    return normalized


def semantic_verify_promoted(state, destination, command_runner=subprocess.run, validator_path=None):
    destination = Path(destination).resolve()
    validator_path = Path(validator_path or Path(__file__).with_name("validate-jmh.py")).resolve()
    jar = verify_pinned_jar(state, "promoted state")
    with tempfile.TemporaryDirectory(prefix="issue-757-verify-") as td:
        scratch = Path(td)
        generated_summaries = []
        environments = []
        for run in state["canonical_runs"]:
            source = destination / Path(run["absolute_path"]).name
            target = scratch / source.name; target.mkdir()
            summary = target / "summary.csv"; validation = target / "validation.json"
            argv = [sys.executable, str(validator_path), "run", "--jar", str(jar), "--input", str(source / "jmh.json"), "--environment", str(source / "environment.json"), "--summary", str(summary), "--validation", str(validation)]
            result = _run(command_runner, argv)
            if result.returncode:
                raise error(source, "promoted per-run validator exit_code={} stderr={!r}".format(result.returncode, _stderr(result).decode("utf-8", "replace")), "discard the destination and collect fresh evidence")
            if summary.read_bytes() != (source / "summary.csv").read_bytes():
                raise error(source / "summary.csv", "semantic rerun CSV differs from promoted bytes", "discard tampered or stale promoted evidence")
            if _semantic_json(load_json(validation)) != _semantic_json(load_json(source / "validation.json")):
                raise error(source / "validation.json", "semantic rerun validation differs from promoted validation", "discard tampered or stale promoted evidence")
            generated_summaries.append(summary); environments.append(source / "environment.json")
        comparison = scratch / "comparison.csv"; validation = scratch / "validation.json"
        argv = [sys.executable, str(validator_path), "compare"]
        for summary in generated_summaries:
            argv += ["--run", str(summary)]
        for environment in environments:
            argv += ["--environment", str(environment)]
        argv += ["--output", str(comparison), "--validation", str(validation)]
        if state.get("rollback_bundle_path"):
            argv += ["--rollback-bundle", str(destination / Path(state["rollback_bundle_path"]).name)]
        result = _run(command_runner, argv)
        if result.returncode:
            raise error(destination, "promoted comparison validator exit_code={} stderr={!r}".format(result.returncode, _stderr(result).decode("utf-8", "replace")), "discard the destination and collect fresh evidence")
        if comparison.read_bytes() != (destination / "comparison.csv").read_bytes():
            raise error(destination / "comparison.csv", "semantic rerun CSV differs from promoted bytes", "discard tampered or stale promoted evidence")
        if _semantic_json(load_json(validation)) != _semantic_json(load_json(destination / "validation.json")):
            raise error(destination / "validation.json", "semantic rerun comparison differs from promoted validation", "discard tampered or stale promoted evidence")


def promote_state(state_path, destination):
    state = load_json(state_path)
    if not state.get("promotable", True):
        raise error(state_path, "state is non-promotable after rollback", "resolve a fresh JAR with the rollback bundle and collect two new runs")
    if len(state.get("canonical_runs", [])) != 2:
        raise error(state_path, "canonical run count={} != 2".format(len(state.get("canonical_runs", []))), "collect and compare exactly two canonical runs")
    verify_state_inputs(state, state_path)
    comparison_validation = load_json(state["comparison_validation_path"])
    regressions = sorted(method for method, verdict in comparison_validation.get("verdicts", {}).items() if verdict == "regressed")
    if regressions:
        raise error(state["comparison_validation_path"], "retained regressed verdicts={}".format(regressions), "record the mapped rollback bundle, remove the dispatch, and collect two fresh runs")
    destination = Path(destination).resolve()
    staging = destination.parent / ("." + destination.name + ".staging-" + secrets.token_hex(4))
    if destination.exists() or staging.exists():
        raise error(destination, "promotion destination or staging collision", "use a fresh destination or replace-promoted")
    staging.mkdir(parents=True)
    try:
        _copy_state_evidence(state, staging)
        atomic_promote(staging, destination)
    except Exception:
        shutil.rmtree(staging, ignore_errors=True)
        raise
    state["promoted_destination"] = str(destination)
    atomic_write_json(state_path, state)


def verify_promoted(state_path, destination, repo_root=None, command_runner=subprocess.run, validator_path=None):
    state_path = Path(state_path).resolve(); state = load_json(state_path); destination = Path(destination).resolve()
    if state.get("promoted_destination") != str(destination):
        raise error(destination, "destination differs from state {}".format(state.get("promoted_destination")), "verify the exact promoted destination")
    if state.get("promotion_status") not in (None, "verified"):
        raise error(state_path, "promotion_status={!r}".format(state.get("promotion_status")), "restore a pre-verification state")
    repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(destination)
    actual_files = {path.relative_to(destination).as_posix() for path in destination.rglob("*") if path.is_file() and path.name != "delivery-manifest.json"}
    expected_files = expected_promoted_files(state)
    if actual_files != expected_files:
        raise error(destination, "promoted file set observed={} expected={}".format(sorted(actual_files), sorted(expected_files)), "discard extra/missing files and promote exact state-bound evidence")
    # Reconstruct expected state hashes before emitting the commit-facing manifest.
    for run in state.get("canonical_runs", []):
        target = destination / Path(run["absolute_path"]).name
        actual_names = sorted(path.name for path in target.iterdir() if path.is_file()) if target.is_dir() else []
        if actual_names != sorted(REQUIRED_RUN_FILES):
            raise error(target, "promoted run file set observed={} expected={}".format(actual_names, sorted(REQUIRED_RUN_FILES)), "discard the destination and promote exact state inputs again")
        for name in REQUIRED_RUN_FILES:
            if sha256_file(target / name) != run["files"][name]:
                raise error(target / name, "promoted hash differs from state", "discard the destination and promote verified inputs again")
    for name, key in (("comparison.csv", "comparison_sha256"), ("validation.json", "comparison_validation_sha256")):
        observed = sha256_file(destination / name)
        if observed != state.get(key):
            raise error(destination / name, "promoted sha256 observed={} expected={}".format(observed, state.get(key)), "discard the destination and promote exact state-bound comparison artifacts")
    semantic_verify_promoted(state, destination, command_runner=command_runner, validator_path=validator_path)
    comparison_validation = load_json(destination / "validation.json")
    verdicts = comparison_validation.get("verdicts", {})
    reasons = comparison_validation.get("reasons", {})
    comparisons = {}
    with (destination / "comparison.csv").open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream):
            comparisons[row["method"]] = row
    commands = []
    results = []
    first_environment = None
    for index, run in enumerate(state["canonical_runs"]):
        run_root = destination / Path(run["absolute_path"]).name
        environment = load_json(run_root / "environment.json")
        first_environment = first_environment or environment
        commands.append(manifest_command(load_json(run_root / "argv.json")["argv"], environment, run_root, repo_root))
        with (run_root / "summary.csv").open(encoding="utf-8", newline="") as stream:
            for row in csv.DictReader(stream):
                method = row["method"]
                comparison = comparisons.get(method)
                if comparison:
                    delta = comparison["run_{}_delta_percent".format("a" if index == 0 else "b")]
                    verdict_value = verdicts.get(method, comparison.get("verdict"))
                    reason = reasons.get(method, comparison.get("reason"))
                else:
                    delta = "n/a"
                    verdict_value = "baseline" if row.get("eligibility_reason") == "baseline" else "compatibility"
                    reason = row.get("eligibility_reason", "control")
                results.append({
                    "method": method, "run": environment["run_id"],
                    "allocation_b_per_op": row["allocation_bytes_per_operation"],
                    "throughput_ops_per_s": row["throughput_ops_per_second"],
                    "delta_percent": delta, "verdict": verdict_value, "reason": reason,
                })
    delivery_commit, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
    manifest = create_delivery_manifest(
        repo_root, destination, first_environment.get("git_commit", "unknown"),
        first_environment.get("tree_hash", "unknown"), final_verdicts=verdicts,
        rollback=state.get("rollback_bundle", {"decisions": []}), commands=commands,
        results=results, delivery_commit=delivery_commit.strip(), benchmark_jar_sha256=state["benchmark_jar_sha256"],
        final_reasons=reasons,
    )
    manifest_path = destination / "delivery-manifest.json"
    if manifest_path.exists():
        if manifest_path.read_bytes() != canonical_json_bytes(manifest):
            raise error(manifest_path, "existing manifest differs from verified destination", "restore the identical destination or use replacement lifecycle")
    else:
        atomic_write_json(manifest_path, manifest, fail_if_exists=True)
    state["promotion_status"] = "verified"
    state["delivery_manifest_path"] = str(manifest_path)
    state["delivery_manifest_sha256"] = sha256_file(manifest_path)
    atomic_write_json(state_path, state)
    return manifest


def replace_promoted(state_path, expected_manifest, destination, backup_root):
    destination = Path(destination).resolve(); expected_manifest = Path(expected_manifest).resolve()
    repo_root = find_repo_root(destination)
    validate_committed(expected_manifest, repo_root=repo_root, require_git_commit=True)
    state = load_json(state_path)
    verify_state_inputs(state, state_path)
    regressions = sorted(method for method, verdict in load_json(state["comparison_validation_path"]).get("verdicts", {}).items() if verdict == "regressed")
    if regressions:
        raise error(state["comparison_validation_path"], "retained regressed verdicts={}".format(regressions), "complete rollback and two fresh runs before replacement")
    staging = destination.parent / ("." + destination.name + ".replacement-" + secrets.token_hex(4))
    staging.mkdir(parents=True)
    try:
        _copy_state_evidence(state, staging)
        semantic_verify_promoted(state, staging)
        backup = atomic_replace_promoted(staging, destination, backup_root)
    except Exception:
        shutil.rmtree(staging, ignore_errors=True)
        raise
    state["promoted_destination"] = str(destination)
    state["replacement_backup_path"] = str(backup)
    try:
        atomic_write_json(state_path, state)
    except Exception:
        failed_new = Path(backup_root).resolve() / ("failed-new-" + generate_run_id())
        _rename(destination, failed_new)
        _rename(backup, destination)
        shutil.rmtree(failed_new, ignore_errors=True)
        raise
    return backup


def cleanup_replacement_backup(state_path, manifest, expected_head, backup_root, command_runner=subprocess.run):
    state_path = Path(state_path).resolve(); state = load_json(state_path)
    manifest_path = Path(manifest).resolve(); repo_root = find_repo_root(manifest_path.parent, command_runner)
    if state.get("promotion_status") != "verified":
        raise error(state_path, "promotion_status={} != verified".format(state.get("promotion_status")), "run verify-promoted before cleanup")
    if state.get("delivery_manifest_path") != str(manifest_path) or state.get("delivery_manifest_sha256") != sha256_file(manifest_path):
        raise error(manifest_path, "manifest path/hash differs from verified state", "pass the exact state-bound verified manifest")
    validate_committed(manifest_path, repo_root=repo_root, require_git_commit=True, command_runner=command_runner)
    head, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
    expected, _, _ = command_text(command_runner, ["git", "rev-parse", expected_head], cwd=repo_root)
    if head.strip() != expected.strip():
        raise error(manifest_path, "current HEAD={} expected HEAD={}".format(head.strip(), expected.strip()), "checkout the exact commit containing the byte-identical manifest")
    cleanup_recorded_backup(state, Path(state["replacement_backup_path"]), backup_root)
    state["replacement_backup_cleaned"] = True
    atomic_write_json(state_path, state)


def parser():
    value = argparse.ArgumentParser(description=__doc__)
    commands = value.add_subparsers(dest="command", required=True)
    resolve = commands.add_parser("resolve-jar"); resolve.add_argument("--jar-dir", required=True); resolve.add_argument("--state", required=True); resolve.add_argument("--rollback-bundle")
    run = commands.add_parser("run"); run.add_argument("--state", required=True); run.add_argument("--profile", choices=sorted(PROFILE_ARGS), required=True); run.add_argument("--output-root", required=True); run.add_argument("--run-id"); run.add_argument("--concurrent-heavy-work", choices=("absent", "present", "unknown"), required=True)
    compare = commands.add_parser("compare"); compare.add_argument("--state", required=True); compare.add_argument("--output", required=True); compare.add_argument("--validation", required=True)
    rollback = commands.add_parser("record-rollback"); rollback.add_argument("--state", required=True); rollback.add_argument("--dispatch", action="append", required=True, choices=DISPATCH_ORDER); rollback.add_argument("--archive-root", required=True)
    promote = commands.add_parser("promote"); promote.add_argument("--state", required=True); promote.add_argument("--destination", required=True)
    replace = commands.add_parser("replace-promoted"); replace.add_argument("--state", required=True); replace.add_argument("--expected-manifest", required=True); replace.add_argument("--destination", required=True); replace.add_argument("--backup-root", required=True)
    cleanup = commands.add_parser("cleanup-replacement-backup"); cleanup.add_argument("--state", required=True); cleanup.add_argument("--manifest", required=True); cleanup.add_argument("--expected-head", required=True); cleanup.add_argument("--backup-root", required=True)
    verify = commands.add_parser("verify-promoted"); verify.add_argument("--state", required=True); verify.add_argument("--destination", required=True)
    committed = commands.add_parser("validate-committed"); committed.add_argument("--manifest", required=True)
    render = commands.add_parser("render-report"); render.add_argument("--manifest", required=True); render.add_argument("--output", required=True)
    report = commands.add_parser("validate-report"); report.add_argument("--manifest", required=True); report.add_argument("--input", required=True)
    return value


def main(argv=None):
    args = parser().parse_args(argv)
    if args.command == "resolve-jar": resolve_jar(args.jar_dir, args.state, args.rollback_bundle)
    elif args.command == "run": run_benchmark(args.state, args.profile, args.output_root, args.run_id, args.concurrent_heavy_work)
    elif args.command == "compare": compare_state(args.state, args.output, args.validation)
    elif args.command == "record-rollback": record_rollback(args.state, args.dispatch, args.archive_root)
    elif args.command == "promote": promote_state(args.state, args.destination)
    elif args.command == "replace-promoted": print(replace_promoted(args.state, args.expected_manifest, args.destination, args.backup_root))
    elif args.command == "cleanup-replacement-backup": cleanup_replacement_backup(args.state, args.manifest, args.expected_head, args.backup_root)
    elif args.command == "verify-promoted": verify_promoted(args.state, args.destination)
    elif args.command == "validate-committed": validate_committed(args.manifest)
    elif args.command == "render-report": render_report(args.manifest, args.output)
    elif args.command == "validate-report": validate_report(args.manifest, args.input)


if __name__ == "__main__":
    try:
        main()
    except ValueError as exc:
        print("run-evidence: {}".format(exc), file=sys.stderr)
        sys.exit(1)
    except (FileExistsError, OSError, KeyError) as exc:
        print("run-evidence: artifact/state error observed={!r}; remediation: inspect the named path, restore exact inputs, and retry with a fresh no-clobber target".format(exc), file=sys.stderr)
        sys.exit(1)
