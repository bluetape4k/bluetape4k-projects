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
import selectors
import signal
import shutil
import stat
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path


SCHEMA_VERSION = 1
ROLLBACK_SCHEMA_VERSION = 2
MAX_RUN_LOG_BYTES = 16 * 1024 * 1024
RUNNER_FAILURE_EXIT_CODE = 125
RUNNER_PROTOCOL_FAILURE_EXIT_CODE = 126
RUN_LOG_LIMIT_MARKER = b"\n[runner] output truncated: log size limit exceeded\n"
RUN_LOG_PROTOCOL_MARKER = b"[runner] anchor protocol failure: "
RUN_LOG_EXECUTION_MARKER = b"[runner] anchor execution failure: "
REQUIRED_RUN_FILES = ("jmh.json", "metadata.json", "argv.json", "run.log", "environment.json", "summary.csv", "validation.json")
METADATA_MAIN = "io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmarkMetadata"
FALLBACK_DEBUG_MARKER = "Protobuf deserialization failed; delegating to the trusted fallback serializer."
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
DISPATCH_SOURCE_PATHS = {
    "serializer_encode": "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt",
    "serializer_decode": "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt",
    "redisson_contiguous": "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt",
}
POSITIVE_PHRASE = "measured allocation reduction"
NON_POSITIVE = "No positive reduction claim"


class AnchorProtocolError(ValueError):
    """The owned anchor failed to provide a trustworthy target exit status."""


class AnchorExecutionError(RuntimeError):
    """An OS operation owned by the anchor runner failed."""


def _anchor_os_call(operation, function, *args, **kwargs):
    try:
        return function(*args, **kwargs)
    except OSError as exc:
        raise AnchorExecutionError("anchor {} failed: {}".format(operation, exc)) from exc


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
    stderr_text = stderr.decode("utf-8", "replace")
    if re.search(r"\bDEBUG\b", stderr_text) or FALLBACK_DEBUG_MARKER in stderr_text:
        raise error(
            jar,
            "metadata stderr contains DEBUG/fallback diagnostic output={!r}".format(stderr_text),
            "rebuild the benchmark JAR with the benchmark logback.xml resource",
        )
    try:
        value = json.loads(stdout.decode("utf-8"))
    except (UnicodeDecodeError, ValueError) as exc:
        raise error(jar, "metadata stdout is not one JSON object ({})".format(exc), "rebuild the pinned benchmark JAR")
    if not isinstance(value, dict):
        raise error(jar, "metadata stdout type={} != object".format(type(value).__name__), "rebuild the pinned benchmark JAR")
    result_value = {"value": value, "stdout": stdout, "stdout_sha256": sha256_bytes(stdout), "stderr": stderr_text, "argv": argv}
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


def _signal_owned_group(process_group_id, requested_signal):
    try:
        os.killpg(process_group_id, requested_signal)
    except (ProcessLookupError, PermissionError):
        # macOS can report EPERM for a session whose only remaining member is
        # the already-signalled anchor zombie; anchor.wait() below still reaps it.
        pass
    except OSError as exc:
        raise AnchorExecutionError("anchor process-group signal failed: {}".format(exc)) from exc


def _terminate_owned_process(anchor, process_group_id=None, terminate_first=True, drain=None):
    process_group_id = process_group_id if process_group_id is not None else anchor.pid
    first_failure = None
    first_traceback = None

    def attempt(function, *args):
        nonlocal first_failure, first_traceback
        try:
            return function(*args)
        except Exception as exc:
            if first_failure is None:
                first_failure = exc
                first_traceback = exc.__traceback__
            return None

    if terminate_first:
        attempt(_signal_owned_group, process_group_id, signal.SIGTERM)
        if drain is not None:
            attempt(drain, 5.0)
    attempt(_signal_owned_group, process_group_id, signal.SIGKILL)
    exit_code = attempt(_anchor_os_call, "wait", anchor.wait)
    if first_failure is not None:
        raise first_failure.with_traceback(first_traceback)
    return exit_code


def _anchor_process(status_fd, pass_fds, target_argv):
    exit_code = 127
    try:
        target = subprocess.Popen(target_argv, pass_fds=tuple(pass_fds))
        exit_code = target.wait()
    except OSError as exc:
        print("anchor target launch failed: {}".format(exc), file=sys.stderr, flush=True)
    try:
        os.write(status_fd, "{}\n".format(exit_code).encode("ascii"))
    finally:
        os.close(status_fd)
    while True:
        signal.pause()


def _publish_temporary_no_clobber(temporary, target):
    try:
        os.link(str(temporary), str(target))
    except FileExistsError:
        raise FileExistsError("{} exists; choose a new no-clobber path".format(target))
    os.unlink(str(temporary))


def _write_bounded_output(stream, chunks, payload_limit):
    written = 0
    for chunk in chunks:
        if not chunk:
            continue
        remaining = payload_limit - written
        if len(chunk) > remaining:
            if remaining > 0:
                stream.write(chunk[:remaining])
                written += remaining
            return written, True
        stream.write(chunk)
        written += len(chunk)
    return written, False


def _consume_anchor_events(selector, log_stream, payload_limit, state, timeout=None, allow_missing_status=False):
    events = _anchor_os_call("selector wait", selector.select, timeout)
    for key, _ in events:
        try:
            chunk = os.read(key.fd, 64 * 1024)
        except BlockingIOError:
            continue
        except OSError as exc:
            raise AnchorExecutionError("anchor stream read failed: {}".format(exc)) from exc
        if key.data == "stdout":
            if not chunk:
                _anchor_os_call("selector unregister", selector.unregister, key.fileobj)
                continue
            remaining = payload_limit - state["written"]
            if remaining > 0:
                log_stream.write(chunk[:remaining])
                state["written"] += min(len(chunk), remaining)
            if len(chunk) > remaining:
                state["limit_exceeded"] = True
        else:
            if not chunk:
                _anchor_os_call("selector unregister", selector.unregister, key.fileobj)
                _anchor_os_call("status descriptor close", os.close, key.fd)
                state["status_fd_open"] = False
                if state["target_exit_code"] is None and not allow_missing_status:
                    raise AnchorProtocolError("anchor status pipe closed before target exit status")
                continue
            state["status"].extend(chunk)
            if len(state["status"]) > 32:
                raise AnchorProtocolError("anchor target exit status exceeded 32 bytes")
            if b"\n" in state["status"]:
                value = bytes(state["status"])
                if not re.fullmatch(rb"-?\d+\n", value):
                    raise AnchorProtocolError("anchor target exit status is malformed")
                state["target_exit_code"] = int(value[:-1])
                _anchor_os_call("selector unregister", selector.unregister, key.fileobj)
                _anchor_os_call("status descriptor close", os.close, key.fd)
                state["status_fd_open"] = False
    return bool(events)


def _drain_anchor_events(selector, log_stream, payload_limit, state, seconds, allow_missing_status=False):
    deadline = time.monotonic() + seconds
    while selector.get_map() and time.monotonic() < deadline:
        remaining = max(0.0, deadline - time.monotonic())
        if not _consume_anchor_events(
            selector, log_stream, payload_limit, state, min(0.05, remaining), allow_missing_status,
        ):
            continue


def _execute_with_anchor(argv, log_stream, payload_limit, pass_fds):
    status_read, status_write = _anchor_os_call("status pipe creation", os.pipe)
    anchor = None
    anchor_stdout = None
    selector = None
    state = {
        "written": 0,
        "limit_exceeded": False,
        "status": bytearray(),
        "target_exit_code": None,
        "status_fd_open": True,
    }
    primary_failure = None
    cleanup_failure = None

    def remember_cleanup_failure(operation, function, *args):
        nonlocal cleanup_failure
        try:
            function(*args)
        except Exception as exc:
            if primary_failure is None and cleanup_failure is None:
                cleanup_failure = (operation, exc)

    try:
        selector = _anchor_os_call("selector creation", selectors.DefaultSelector)
        anchor_argv = [
            sys.executable, str(Path(__file__).resolve()), "_exec-anchor",
            "--status-fd", str(status_write),
        ]
        for inherited_fd in pass_fds:
            anchor_argv.extend(("--pass-fd", str(inherited_fd)))
        anchor_argv.extend(("--",))
        anchor_argv.extend(argv)
        inherited = tuple(dict.fromkeys((status_write,) + tuple(pass_fds)))
        anchor = _anchor_os_call(
            "launch",
            subprocess.Popen,
            anchor_argv,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            pass_fds=inherited,
            start_new_session=True,
        )
        anchor_stdout = anchor.stdout
        _anchor_os_call("parent status descriptor close", os.close, status_write)
        status_write = None
        _anchor_os_call("status nonblocking setup", os.set_blocking, status_read, False)
        _anchor_os_call("stdout nonblocking setup", os.set_blocking, anchor.stdout.fileno(), False)
        _anchor_os_call("stdout selector registration", selector.register, anchor.stdout, selectors.EVENT_READ, "stdout")
        _anchor_os_call("status selector registration", selector.register, status_read, selectors.EVENT_READ, "status")
        while state["target_exit_code"] is None and not state["limit_exceeded"]:
            _consume_anchor_events(selector, log_stream, payload_limit, state)
        if state["limit_exceeded"]:
            owned_anchor = anchor
            anchor = None
            _terminate_owned_process(
                owned_anchor,
                owned_anchor.pid,
                terminate_first=True,
                drain=lambda seconds: _drain_anchor_events(
                    selector, log_stream, payload_limit, state, seconds, allow_missing_status=True,
                ),
            )
            exit_code = RUNNER_FAILURE_EXIT_CODE
        else:
            exit_code = state["target_exit_code"]
            owned_anchor = anchor
            anchor = None
            _terminate_owned_process(owned_anchor, owned_anchor.pid, terminate_first=False)
            _drain_anchor_events(selector, log_stream, payload_limit, state, 1.0)
            if state["limit_exceeded"]:
                exit_code = RUNNER_FAILURE_EXIT_CODE
        return exit_code, state["limit_exceeded"]
    except Exception as exc:
        primary_failure = exc
        primary_traceback = exc.__traceback__
        if anchor is not None:
            owned_anchor = anchor
            anchor = None
            try:
                _terminate_owned_process(
                    owned_anchor,
                    owned_anchor.pid,
                    terminate_first=True,
                    drain=None if isinstance(exc, (OSError, AnchorExecutionError)) else lambda seconds: _drain_anchor_events(
                        selector, log_stream, payload_limit, state, seconds, allow_missing_status=True,
                    ),
                )
            except Exception:
                pass
        raise primary_failure.with_traceback(primary_traceback)
    finally:
        if selector is not None:
            remember_cleanup_failure("selector close", selector.close)
        if anchor is not None:
            owned_anchor = anchor
            anchor = None
            remember_cleanup_failure("final termination", _terminate_owned_process, owned_anchor, owned_anchor.pid, True)
        if status_write is not None:
            remember_cleanup_failure("status write descriptor close", os.close, status_write)
        if state["status_fd_open"]:
            remember_cleanup_failure("status read descriptor close", os.close, status_read)
        if anchor_stdout is not None:
            remember_cleanup_failure("anchor stdout close", anchor_stdout.close)
        if primary_failure is None and cleanup_failure is not None:
            operation, close_error = cleanup_failure
            if isinstance(close_error, AnchorExecutionError):
                raise close_error
            if isinstance(close_error, OSError):
                raise AnchorExecutionError("anchor {} failed: {}".format(operation, close_error)) from close_error
            raise close_error


def execute_logged(argv, run_dir, command_runner=subprocess.run, pass_fds=()):
    run_dir = Path(run_dir)
    run_dir.mkdir(parents=True, exist_ok=True)
    argv_path = run_dir / "argv.json"
    log_path = run_dir / "run.log"
    if argv_path.exists():
        raise FileExistsError("{} exists; choose a new no-clobber path".format(argv_path))
    if log_path.exists():
        raise FileExistsError("{} exists; choose a new no-clobber path".format(log_path))
    log_fd, log_temporary_name = tempfile.mkstemp(prefix=".run.log.", dir=str(run_dir))
    log_temporary = Path(log_temporary_name)
    started = utc_now()
    limit_exceeded = False
    exit_code = 127
    owned_failure = None
    owned_traceback = None
    payload_limit = MAX_RUN_LOG_BYTES - len(RUN_LOG_LIMIT_MARKER) - 64
    try:
        with os.fdopen(log_fd, "wb") as log_stream:
            log_fd = None
            if command_runner is subprocess.run:
                try:
                    exit_code, limit_exceeded = _execute_with_anchor(
                        argv, log_stream, payload_limit, tuple(pass_fds),
                    )
                except AnchorProtocolError as exc:
                    owned_failure = exc
                    owned_traceback = exc.__traceback__
                    exit_code = RUNNER_PROTOCOL_FAILURE_EXIT_CODE
                    diagnostic_marker = RUN_LOG_PROTOCOL_MARKER
                except AnchorExecutionError as exc:
                    owned_failure = exc
                    owned_traceback = exc.__traceback__
                    exit_code = 127
                    diagnostic_marker = RUN_LOG_EXECUTION_MARKER
                if owned_failure is not None:
                    diagnostic = diagnostic_marker + str(owned_failure).encode("utf-8", "replace") + b"\n"
                    remaining = max(0, payload_limit - log_stream.tell())
                    _, diagnostic_truncated = _write_bounded_output(log_stream, (diagnostic,), remaining)
                    limit_exceeded = limit_exceeded or diagnostic_truncated
            else:
                result = _run(command_runner, argv, pass_fds=pass_fds)
                exit_code = result.returncode
                _, limit_exceeded = _write_bounded_output(
                    log_stream, (_stdout(result), _stderr(result)), payload_limit,
                )
            if limit_exceeded:
                log_stream.write(RUN_LOG_LIMIT_MARKER)
            log_stream.write("exit_code={}\n".format(exit_code).encode("ascii"))
            log_stream.flush()
            os.fsync(log_stream.fileno())
        _publish_temporary_no_clobber(log_temporary, log_path)
    finally:
        if log_fd is not None:
            os.close(log_fd)
        try:
            log_temporary.unlink()
        except FileNotFoundError:
            pass
    ended = utc_now()
    record = {
        "schema_version": SCHEMA_VERSION,
        "argv": list(argv),
        "started_at": started,
        "ended_at": ended,
        "exit_code": exit_code,
        "log_limit_exceeded": limit_exceeded,
    }
    atomic_write_json(argv_path, record, fail_if_exists=True)
    if owned_failure is not None:
        raise owned_failure.with_traceback(owned_traceback)
    if limit_exceeded:
        raise error(log_path, "benchmark output exceeded log size limit={}".format(MAX_RUN_LOG_BYTES), "repair benchmark logging and use a fresh run ID")
    if exit_code:
        raise error(log_path, "benchmark process exit_code={}".format(exit_code), "inspect run.log, repair the benchmark, and use a fresh run ID")
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


def _canonical_manifest_path(repo_root, relative, manifest_path):
    repo_root = Path(repo_root).resolve()
    path = repo_root / relative
    path = path.parent.resolve() / path.name
    try:
        path.relative_to(repo_root)
    except ValueError:
        raise error(manifest_path, "path escapes repository: {}".format(relative), "regenerate the manifest")
    return path


def verify_manifest_files(manifest, repo_root, manifest_path, run_log_results=None):
    validate_manifest(manifest, manifest_path)
    repo_root = Path(repo_root).resolve()
    run_log_results = run_log_results if run_log_results is not None else preflight_manifest_run_logs(manifest, repo_root, manifest_path)
    for item in manifest["files"]:
        path = _canonical_manifest_path(repo_root, item["path"], manifest_path)
        if not path.is_file():
            raise error(path, "manifest file is missing", "restore committed evidence")
        observed = run_log_results[path]["sha256"] if path.name == "run.log" else sha256_file(path)
        if observed != item["sha256"]:
            raise error(path, "sha256 observed={} expected={}".format(observed, item["sha256"]), "restore committed bytes or regenerate evidence")
    return True


def create_delivery_manifest(repo_root, destination, commit, tree_hash, final_verdicts=None, rollback=None, commands=None, results=None, delivery_commit=None, benchmark_jar_sha256=None, final_reasons=None):
    repo_root = Path(repo_root).resolve(); destination = Path(destination).resolve()
    validator = _load_validator()
    run_log_results = {
        path: validator.validate_run_log(path)
        for path in sorted(destination.rglob("run.log"))
        if path.is_file()
    }
    files = []
    for path in sorted(destination.rglob("*")):
        if path.is_file() and path.name != "delivery-manifest.json":
            relative = path.relative_to(repo_root)
            observed = run_log_results[path]["sha256"] if path.name == "run.log" else sha256_file(path)
            files.append({"path": relative.as_posix(), "sha256": observed})
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
    run_log_results = preflight_manifest_run_logs(manifest, repo_root, manifest_path)
    verify_manifest_files(manifest, repo_root, manifest_path, run_log_results)
    report_hash = sha256_bytes(payload_json_bytes({key: manifest.get(key) for key in ("measurement", "delivery", "final_verdicts", "final_reasons", "rollback", "commands", "results")}))
    if report_hash != manifest.get("report_input_sha256"):
        raise error(manifest_path, "report_input_sha256 observed={} expected={}".format(report_hash, manifest.get("report_input_sha256")), "rerun verify-promoted")
    validate_committed_semantics(manifest, manifest_path, repo_root, run_log_results)
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


def preflight_manifest_run_logs(manifest, repo_root, manifest_path):
    validate_manifest(manifest, manifest_path)
    repo_root = Path(repo_root).resolve()
    validator = _load_validator()
    results = {}
    for item in manifest["files"]:
        relative = Path(item["path"])
        if relative.name != "run.log":
            continue
        path = _canonical_manifest_path(repo_root, relative, manifest_path)
        results[path] = validator.validate_run_log(path)
    return results


def validate_committed_semantics(manifest, manifest_path, repo_root, run_log_results=None):
    validator = _load_validator()
    run_log_results = run_log_results if run_log_results is not None else preflight_manifest_run_logs(manifest, repo_root, manifest_path)
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
            validator.validate_execution_artifacts(
                argv, environment, run_log_results[run_root / "run.log"], run_root / "argv.json",
            )
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
        preparation_paths = {path for path in paths if path.name.startswith("rollback-preparation-g")}
        rollback = None
        if bundle_paths:
            chain = authenticate_rollback_bundle_chain(bundle_paths[-1])
            if {path for path, _ in chain} != set(bundle_paths):
                raise error(manifest_path, "committed rollback generation file set is incomplete or extra", "restore the exact immutable bundle chain")
            if chain[-1][1] != manifest.get("rollback"):
                raise error(manifest_path, "manifest rollback payload differs from latest committed bundle", "rerun verify-promoted with the authenticated bundle")
            expected_preparations = {(path.parent / value["preparation_path"]).resolve() for path, value in chain}
            if preparation_paths != expected_preparations:
                raise error(manifest_path, "committed rollback preparation file set differs", "restore every and only referenced immutable preparation")
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


def require_safe_relative_path(container, value, field):
    if not isinstance(value, str) or not value or Path(value).is_absolute() or ".." in Path(value).parts:
        raise error(container, "unsafe {}={!r}".format(field, value), "restore a non-empty bundle-relative path without traversal")
    return value


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
    decision = {"dispatch": dispatch, "regressed_cells": sorted(cells),
                "removed_cells": sorted(DISPATCH_CELLS[dispatch]), "old_commit": commit, "old_tree": tree_hash,
                "archive_root": archive_root.name, "artifacts": artifacts, "timestamp": timestamp, "generation": generation}
    if post_rollback_commit is not None:
        decision.update({"post_rollback_commit": post_rollback_commit, "post_rollback_tree": post_rollback_tree,
                         "lineage_parent_commit": lineage_parent_commit,
                         "removal_evidence": {"dispatch": dispatch, "removed_cells": sorted(DISPATCH_CELLS[dispatch]),
                                              "old_commit": commit, "old_tree": tree_hash,
                                              "post_rollback_commit": post_rollback_commit, "post_rollback_tree": post_rollback_tree,
                                              "head_changed": post_rollback_commit != commit, "tree_changed": post_rollback_tree != tree_hash}})
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


def _preparation_payload(preparation):
    return {key: value for key, value in _bundle_payload(preparation).items() if key != "preparation_sha256"}


def write_rollback_preparation(archive_root, decisions, generation, predecessor=None):
    root = Path(archive_root).resolve(); root.mkdir(parents=True, exist_ok=True)
    ordered = sorted(decisions, key=lambda item: DISPATCH_ORDER.index(item["dispatch"]))
    preparation = {"schema_version": ROLLBACK_SCHEMA_VERSION, "kind": "rollback_preparation",
                   "generation": generation,
                   "predecessor_bundle_sha256": sha256_file(predecessor) if predecessor else None,
                   "decisions": ordered}
    preparation["preparation_sha256"] = sha256_bytes(payload_json_bytes(_preparation_payload(preparation)))
    path = root / "rollback-preparation-g{}-{}.json".format(generation, preparation["preparation_sha256"])
    atomic_write_json(path, preparation, fail_if_exists=True)
    return path.resolve()


def _authenticate_decision_archive(path, item, require_finalized):
    if item.get("removed_cells") != sorted(DISPATCH_CELLS.get(item.get("dispatch"), ())):
        raise error(path, "decision {} removed_cells differ from fixed mapping".format(item.get("dispatch")), "restore the v2 rollback artifact")
    calculated = sha256_bytes(payload_json_bytes(_decision_payload(item)))
    if calculated != item.get("decision_sha256"):
        raise error(path, "decision {} sha256 mismatch".format(item.get("dispatch")), "restore the immutable decision")
    archive_root = item.get("archive_root")
    require_safe_relative_path(path, archive_root, "archive_root")
    archive = path.parent / archive_root; require_symlink_free_tree(archive)
    comparison = None
    for artifact in item.get("artifacts", []):
        relative = artifact.get("path")
        require_safe_relative_path(path, relative, "archive artifact path")
        artifact_path = require_archive_artifact(archive / artifact["path"], archive)
        if sha256_file(artifact_path) != artifact["sha256"]:
            raise error(artifact_path, "rollback archive sha256 mismatch", "restore the immutable archive")
        if Path(artifact["path"]).name == "comparison.csv": comparison = artifact_path
    actual = sorted(p.relative_to(archive).as_posix() for p in archive.rglob("*") if p.is_file())
    declared = sorted(a["path"] for a in item.get("artifacts", []))
    if actual != declared or comparison is None:
        raise error(archive, "rollback archive file set/comparison mismatch", "restore the complete immutable archive")
    verdicts = read_comparison_verdicts(comparison)
    actual_regressed = sorted(cell for cell in DISPATCH_CELLS[item["dispatch"]] if verdicts.get(cell) == "regressed")
    if not actual_regressed or item.get("regressed_cells") != actual_regressed:
        raise error(comparison, "regressed_cells observed={} expected={}".format(item.get("regressed_cells"), actual_regressed), "restore actual state-bound regression evidence")
    if require_finalized:
        if not isinstance(item.get("post_rollback_commit"), str) or not item["post_rollback_commit"] or not isinstance(item.get("post_rollback_tree"), str) or not item["post_rollback_tree"]:
            raise error(path, "finalized decision missing post commit/tree", "restore complete finalized source lineage")
        if item.get("post_rollback_commit") == item.get("old_commit") or item.get("post_rollback_tree") == item.get("old_tree"):
            raise error(path, "finalized decision does not prove changed head/tree", "finalize after the removal commit")
    return archive_root, verdicts


def authenticate_rollback_preparation(path):
    path = Path(path).resolve(); preparation = load_json(path)
    if preparation.get("schema_version") != ROLLBACK_SCHEMA_VERSION or preparation.get("kind") != "rollback_preparation":
        raise error(path, "not a rollback v2 preparation", "run record-rollback before changing source")
    observed = sha256_bytes(payload_json_bytes(_preparation_payload(preparation)))
    if observed != preparation.get("preparation_sha256"):
        raise error(path, "rollback preparation sha256 mismatch", "restore the immutable preparation")
    expected = "rollback-preparation-g{}-{}.json".format(preparation.get("generation"), observed)
    if path.name != expected:
        raise error(path, "preparation filename differs from payload", "restore the canonical immutable filename")
    decisions = preparation.get("decisions")
    if not isinstance(decisions, list) or not decisions:
        raise error(path, "preparation decisions must be non-empty", "record every simultaneously regressed dispatch")
    evidence = [_authenticate_decision_archive(path, decision, False) for decision in decisions]
    if any(decision.get("generation") != preparation.get("generation") for decision in decisions):
        raise error(path, "preparation decision generation differs from container", "restore one exact preparation generation")
    old_identities = {(decision.get("old_commit"), decision.get("old_tree")) for decision in decisions}
    if len(old_identities) != 1 or any(not isinstance(value, str) or not value for identity in old_identities for value in identity):
        raise error(path, "preparation decisions do not share one non-empty old commit/tree", "bind every simultaneous decision to the exact measurement identity")
    if any(value != evidence[0] for value in evidence[1:]):
        raise error(path, "preparation decisions do not share one comparison/archive", "bind simultaneous decisions to one archived comparison")
    verdicts = evidence[0][1]
    required = [dispatch for dispatch in DISPATCH_ORDER if any(verdicts.get(cell) == "regressed" for cell in DISPATCH_CELLS[dispatch])]
    observed_dispatches = [decision.get("dispatch") for decision in decisions]
    if observed_dispatches != required:
        raise error(path, "simultaneous dispatches observed={} expected={}".format(observed_dispatches, required), "record every and only simultaneously regressed dispatch")
    predecessor_hash = preparation.get("predecessor_bundle_sha256")
    if preparation.get("generation") == 1:
        if predecessor_hash is not None:
            raise error(path, "generation 1 preparation has predecessor", "restore the root preparation")
    else:
        candidates = [candidate for candidate in path.parent.glob("rollback-bundle-g{}-*.json".format(preparation["generation"] - 1)) if sha256_file(candidate) == predecessor_hash]
        if len(candidates) != 1:
            raise error(path, "preparation predecessor match count={}".format(len(candidates)), "restore the exact finalized predecessor")
        if authenticate_rollback_bundle(candidates[0])["generation"] + 1 != preparation["generation"]:
            raise error(path, "preparation generation is not predecessor successor", "restore contiguous rollback generations")
    return preparation


def write_rollback_bundle(archive_root, decisions, predecessor=None):
    """Low-level fixture/helper writer; normal callers must use prepare then finalize."""
    root = Path(archive_root).resolve(); root.mkdir(parents=True, exist_ok=True)
    dispatches = [item.get("dispatch") for item in decisions]
    if len(dispatches) != len(set(dispatches)):
        raise error(root, "duplicate rollback dispatch decisions {}".format(dispatches), "record each dispatch once")
    previous = authenticate_rollback_bundle(predecessor) if predecessor else None
    generation = previous["generation"] + 1 if previous else 1
    prepared = []
    for source in decisions:
        item = {key: value for key, value in source.items() if key not in (
            "post_rollback_commit", "post_rollback_tree", "lineage_parent_commit", "removal_evidence", "decision_sha256"
        )}
        item["generation"] = generation
        item["decision_sha256"] = sha256_bytes(payload_json_bytes(_decision_payload(item)))
        prepared.append(item)
    preparation_path = write_rollback_preparation(root, prepared, generation, predecessor)
    ordered = sorted(decisions, key=lambda item: DISPATCH_ORDER.index(item["dispatch"]))
    all_decisions = (previous["decisions"] if previous else []) + ordered
    bundle = {"schema_version": ROLLBACK_SCHEMA_VERSION, "kind": "rollback_bundle", "generation": generation,
              "predecessor_bundle_sha256": sha256_file(predecessor) if predecessor else None,
              "preparation_path": preparation_path.name, "preparation_sha256": sha256_file(preparation_path),
              "preparation_payload_sha256": load_json(preparation_path)["preparation_sha256"], "decisions": all_decisions}
    bundle["bundle_sha256"] = sha256_bytes(payload_json_bytes(_bundle_payload(bundle)))
    path = root / "rollback-bundle-g{}-{}.json".format(generation, bundle["bundle_sha256"])
    atomic_write_json(path, bundle, fail_if_exists=True)
    authenticate_rollback_bundle(path)
    return path.resolve()


def authenticate_rollback_bundle(path):
    path = Path(path).resolve(); bundle = load_json(path)
    if bundle.get("schema_version") != ROLLBACK_SCHEMA_VERSION or bundle.get("kind") != "rollback_bundle":
        raise error(path, "rollback artifact is not a finalized v2 bundle (schema_version={}, kind={!r})".format(bundle.get("schema_version"), bundle.get("kind")), "run record-rollback, remove the dispatch, then finalize-rollback; v1 artifacts must be recreated")
    observed = sha256_bytes(payload_json_bytes(_bundle_payload(bundle)))
    expected = bundle.get("bundle_sha256")
    if observed != expected:
        raise error(path, "rollback bundle sha256 observed={} expected={}".format(observed, expected), "restore the immutable bundle")
    match = re.search(r"rollback-bundle-g(\d+)-([0-9a-f]{64})\.json$", path.name)
    if not match or int(match.group(1)) != bundle.get("generation") or match.group(2) != expected:
        raise error(path, "bundle filename generation/hash does not match payload", "restore the canonical immutable filename")
    preparation_name = bundle.get("preparation_path")
    require_safe_relative_path(path, preparation_name, "preparation_path")
    preparation_path = path.parent / preparation_name
    preparation = authenticate_rollback_preparation(preparation_path)
    if sha256_file(preparation_path) != bundle.get("preparation_sha256") or preparation.get("preparation_sha256") != bundle.get("preparation_payload_sha256"):
        raise error(path, "preparation hash differs from finalized bundle", "restore the authenticated preparation")
    decisions = bundle.get("decisions", [])
    if not isinstance(decisions, list) or not decisions:
        raise error(path, "finalized decisions must be non-empty", "finalize a non-empty authenticated preparation")
    if bundle.get("generation") == 1:
        predecessor_decisions = []
        if bundle.get("predecessor_bundle_sha256") is not None:
            raise error(path, "generation 1 has predecessor", "restore the root finalized bundle")
    else:
        predecessor_hash = bundle.get("predecessor_bundle_sha256")
        candidates = [candidate for candidate in path.parent.glob("rollback-bundle-g{}-*.json".format(bundle["generation"] - 1)) if sha256_file(candidate) == predecessor_hash]
        if len(candidates) != 1:
            raise error(path, "predecessor bundle match count={}".format(len(candidates)), "restore the exact finalized predecessor")
        predecessor_decisions = authenticate_rollback_bundle(candidates[0])["decisions"]
    if decisions[:len(predecessor_decisions)] != predecessor_decisions:
        raise error(path, "finalized predecessor prefix differs", "restore the exact inherited decision prefix")
    suffix = decisions[len(predecessor_decisions):]
    if len(suffix) != len(preparation["decisions"]):
        raise error(path, "finalized suffix count={} expected={}".format(len(suffix), len(preparation["decisions"])), "finalize each prepared decision exactly once")
    final_fields = {"post_rollback_commit", "post_rollback_tree", "lineage_parent_commit", "removal_evidence", "decision_sha256"}
    expected_parent = predecessor_decisions[-1]["post_rollback_commit"] if predecessor_decisions else None
    post_identities = {(decision.get("post_rollback_commit"), decision.get("post_rollback_tree"), decision.get("lineage_parent_commit")) for decision in suffix}
    if len(post_identities) != 1:
        raise error(path, "finalized generation does not share post commit/tree/lineage parent", "bind every finalized decision to one shared post-removal lineage")
    for prepared, finalized in zip(preparation["decisions"], suffix):
        prepared_core = {key: value for key, value in prepared.items() if key != "decision_sha256"}
        finalized_core = {key: value for key, value in finalized.items() if key not in final_fields}
        expected_removal = {"dispatch": prepared["dispatch"], "removed_cells": prepared["removed_cells"],
                            "old_commit": prepared["old_commit"], "old_tree": prepared["old_tree"],
                            "post_rollback_commit": finalized.get("post_rollback_commit"), "post_rollback_tree": finalized.get("post_rollback_tree"),
                            "head_changed": True, "tree_changed": True}
        if finalized_core != prepared_core or finalized.get("lineage_parent_commit") != expected_parent or finalized.get("removal_evidence") != expected_removal:
            raise error(path, "finalized decision differs from prepared decision/lineage", "restore the exact one-to-one finalized transformation")
    dispatches = [item.get("dispatch") for item in decisions]
    if len(dispatches) != len(set(dispatches)):
        raise error(path, "duplicate or conflicting decisions {}".format(dispatches), "restore one decision per dispatch")
    generations = {item.get("generation") for item in decisions}
    for generation in sorted(generations):
        observed_order = [item.get("dispatch") for item in decisions if item.get("generation") == generation]
        expected_order = sorted(observed_order, key=lambda item: DISPATCH_ORDER.index(item) if item in DISPATCH_ORDER else 999)
        if observed_order != expected_order:
            raise error(path, "generation {} dispatch order observed={} expected={}".format(generation, observed_order, expected_order), "restore the fixed dispatch order")
    for item in decisions:
        _authenticate_decision_archive(path, item, True)
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


def sanitize_kotlin_lexically(source):
    chars = list(source); output = list(source); index = 0; state = "code"; depth = 0
    while index < len(chars):
        pair = "".join(chars[index:index + 2]); triple = "".join(chars[index:index + 3])
        if state == "code":
            if pair == "//": output[index:index + 2] = [" "] * 2; index += 2; state = "line"; continue
            if pair == "/*": output[index:index + 2] = [" "] * 2; index += 2; state = "block"; depth = 1; continue
            if triple == '\"\"\"': output[index:index + 3] = [" "] * 3; index += 3; state = "triple"; continue
            if chars[index] == '"': output[index] = " "; index += 1; state = "string"; continue
            if chars[index] == "'": output[index] = " "; index += 1; state = "char"; continue
            index += 1; continue
        if state == "line":
            if chars[index] == "\n": state = "code"; index += 1; continue
        elif state == "block":
            if pair == "/*": depth += 1
            elif pair == "*/": depth -= 1
            if depth == 0:
                output[index] = output[index + 1] = " "; index += 2; state = "code"; continue
        elif state == "triple" and triple == '\"\"\"' and index + 2 < len(chars):
            output[index:index + 3] = [" "] * 3; index += 3; state = "code"; continue
        elif state in ("string", "char"):
            quote = '"' if state == "string" else "'"
            if chars[index] == "\\" and index + 1 < len(chars):
                output[index] = output[index + 1] = " "; index += 2; continue
            if chars[index] == quote:
                output[index] = " "; index += 1; state = "code"; continue
        if chars[index] != "\n": output[index] = " "
        index += 1
    return "".join(output)


def kotlin_identifiers(source):
    sanitized = sanitize_kotlin_lexically(source)
    return [token[1:-1] if token.startswith("`") else token for token in re.findall(r"`[^`\n]+`|[A-Za-z_][A-Za-z0-9_]*", sanitized)]


def kotlin_function_declarations(source, expected_name):
    sanitized = sanitize_kotlin_lexically(source); declarations = []
    pattern = re.compile(r"\bfun\s+(?:<[^{}>]*>\s*)?(`[^`\n]+`|[A-Za-z_][A-Za-z0-9_]*)\s*\(")
    for match in pattern.finditer(sanitized):
        name = match.group(1); name = name[1:-1] if name.startswith("`") else name
        if name != expected_name: continue
        depth = 1; index = match.end()
        while index < len(sanitized) and depth:
            if sanitized[index] == "(": depth += 1
            elif sanitized[index] == ")": depth -= 1
            index += 1
        opening = sanitized.find("{", index); equals = sanitized.find("=", index)
        if equals >= 0 and (opening < 0 or equals < opening):
            end = sanitized.find("\n", equals + 1)
            declarations.append(("expression", sanitized[equals + 1:end if end >= 0 else len(sanitized)])); continue
        if opening < 0:
            declarations.append(("invalid", "")); continue
        brace_depth = 0
        for end in range(opening, len(sanitized)):
            if sanitized[end] == "{": brace_depth += 1
            elif sanitized[end] == "}":
                brace_depth -= 1
                if brace_depth == 0:
                    declarations.append(("block", sanitized[opening + 1:end])); break
        else: declarations.append(("invalid", ""))
    return declarations


def verify_dispatch_source_removals(repo_root, head, dispatches, command_runner=subprocess.run):
    cache = {}
    for dispatch in dispatches:
        source_path = DISPATCH_SOURCE_PATHS[dispatch]
        if source_path not in cache:
            source, _, _ = command_text(command_runner, ["git", "show", "{}:{}".format(head, source_path)], cwd=repo_root)
            cache[source_path] = source
        source = cache[source_path]; identifiers = kotlin_identifiers(source)
        if dispatch == "serializer_encode":
            valid = "serializeTo" not in identifiers
        elif dispatch == "serializer_decode":
            valid = "deserializeFrom" not in identifiers
        else:
            declarations = kotlin_function_declarations(source, "decodeProtobuf")
            body = declarations[0][1] if len(declarations) == 1 and declarations[0][0] == "block" else ""
            compact = re.sub(r"\s+", "", body)
            body_identifiers = kotlin_identifiers(body)
            valid = (len(declarations) == 1 and declarations[0][0] == "block" and
                     "AnyMessage.parseFrom(buf.getBytes(copy=true))" in compact and
                     not any("niobuffer" in identifier.lower() for identifier in body_identifiers))
        if not valid:
            raise error(source_path, "{} removal predicate failed; canonical expected form absent at committed head={}".format(dispatch, head), "restore inherited serializer compatibility or the single canonical copied decodeProtobuf block")
    return True


def record_rollback(state_path, dispatches, archive_root, command_runner=subprocess.run, repo_root=None):
    state_path = Path(state_path).resolve(); state = load_json(state_path)
    if len(state.get("canonical_runs", [])) != 2:
        raise error(state_path, "canonical run count={} expected=2".format(len(state.get("canonical_runs", []))), "collect exactly two state-bound canonical runs")
    verify_state_inputs(state, state_path)
    environments = [load_json(Path(run["absolute_path"]) / "environment.json") for run in state["canonical_runs"]]
    identities = [(value.get("git_commit"), value.get("tree_hash")) for value in environments]
    if identities[0] != identities[1]:
        raise error(state_path, "measurement identity observed={} expected one shared commit/tree".format(identities), "recollect both canonical runs from the same clean head")
    if len(dispatches) != len(set(dispatches)):
        raise error(state_path, "duplicate dispatch arguments {}".format(dispatches), "pass each regressed dispatch once")
    verdicts = read_comparison_verdicts(state["comparison_path"])
    required = []
    for dispatch in DISPATCH_ORDER:
        if any(verdicts.get(cell) == "regressed" for cell in DISPATCH_CELLS[dispatch]):
            required.append(dispatch)
    if sorted(dispatches, key=DISPATCH_ORDER.index) != required:
        raise error(state["comparison_path"], "dispatches={} mapped simultaneous regressions={}".format(dispatches, required), "record every and only simultaneously regressed dispatch")
    if state.get("rollback_status") == "prepared":
        existing_path = Path(state.get("rollback_preparation_path", "")).resolve()
        existing = authenticate_rollback_preparation(existing_path)
        prepared_identity = {(item["old_commit"], item["old_tree"]) for item in existing["decisions"]}
        current_identity = identities[0]
        artifacts = {item["path"]: item["sha256"] for item in existing["decisions"][0]["artifacts"]}
        if (sha256_file(existing_path) != state.get("rollback_preparation_file_sha256") or
                existing_path.parent != Path(archive_root).resolve() or
                [item["dispatch"] for item in existing["decisions"]] != required or
                prepared_identity != {current_identity} or
                artifacts.get("comparison.csv") != state.get("comparison_sha256") or
                artifacts.get("validation.json") != state.get("comparison_validation_sha256")):
            if prepared_identity != {current_identity}:
                raise error(state_path, "stale preparation lineage={} current={}".format(sorted(prepared_identity), current_identity), "reuse only the exact original measurement state")
            raise error(state_path, "prepared rollback conflicts with requested dispatch/root", "reuse the exact recorded preparation or start from a fresh state")
        print(existing_path)
        return existing_path
    first_environment = environments[0]
    old_commit = first_environment.get("git_commit"); old_tree = first_environment.get("tree_hash")
    repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(Path.cwd(), command_runner)
    require_clean_tree(repo_root, "rollback preparation", command_runner)
    head, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
    tree, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD^{tree}"], cwd=repo_root)
    if (head.strip(), tree.strip()) != (old_commit, old_tree):
        raise error(state_path, "preparation head/tree observed={} expected={}".format((head.strip(), tree.strip()), (old_commit, old_tree)), "checkout the exact clean measurement head before record-rollback")
    predecessor = state.get("rollback_bundle_path")
    if predecessor:
        previous = authenticate_rollback_bundle(predecessor)
        parent = previous["decisions"][-1]["post_rollback_commit"]
        if _run(command_runner, ["git", "merge-base", "--is-ancestor", parent, old_commit], cwd=repo_root).returncode:
            raise error(state_path, "measurement commit={} does not descend from predecessor post={}".format(old_commit, parent), "collect the next generation on the authenticated rollback lineage")
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
        decisions = [make_rollback_decision(dispatch, [cell for cell in DISPATCH_CELLS[dispatch] if verdicts.get(cell) == "regressed"], old_commit, old_tree, archive, files, generation, utc_now()) for dispatch in required]
        if predecessor and Path(predecessor).resolve().parent != root:
            raise error(predecessor, "predecessor bundle root differs from archive root {}".format(root), "continue the immutable chain under the original archive root")
        bundle_path = write_rollback_preparation(root, decisions, generation, predecessor=predecessor)
        preparation = authenticate_rollback_preparation(bundle_path)
    except Exception:
        if bundle_path and Path(bundle_path).exists():
            Path(bundle_path).unlink()
        shutil.rmtree(archive, ignore_errors=True)
        raise
    state.update({"promotable": False, "rollback_status": "prepared",
                  "rollback_preparation_path": str(bundle_path), "rollback_preparation_file_sha256": sha256_file(bundle_path),
                  "rollback_preparation": preparation})
    try:
        atomic_write_json(state_path, state)
    except Exception:
        Path(bundle_path).unlink(missing_ok=True)
        shutil.rmtree(archive, ignore_errors=True)
        raise
    print(bundle_path)
    return bundle_path


def finalize_rollback(preparation_path, command_runner=subprocess.run, repo_root=None, removal_verifier=None):
    preparation_path = Path(preparation_path).resolve(); preparation = authenticate_rollback_preparation(preparation_path)
    repo_root = Path(repo_root).resolve() if repo_root else find_repo_root(Path.cwd(), command_runner)
    require_clean_tree(repo_root, "rollback finalization", command_runner)
    head, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD"], cwd=repo_root)
    tree, _, _ = command_text(command_runner, ["git", "rev-parse", "HEAD^{tree}"], cwd=repo_root)
    head = head.strip(); tree = tree.strip(); decisions = preparation["decisions"]
    old_identities = {(decision["old_commit"], decision["old_tree"]) for decision in decisions}
    if len(old_identities) != 1:
        raise error(preparation_path, "prepared decisions have mixed old lineage", "restore the authenticated preparation")
    old_commit, old_tree = next(iter(old_identities)); first = decisions[0]
    if any(head == decision["old_commit"] or tree == decision["old_tree"] for decision in decisions):
        raise error(preparation_path, "finalization head/tree did not change", "commit the symbol-scoped removal before finalizing")
    if _run(command_runner, ["git", "merge-base", "--is-ancestor", old_commit, head], cwd=repo_root).returncode:
        raise error(preparation_path, "finalization head does not descend from measurement head", "finalize on the authenticated descendant branch")
    if preparation.get("predecessor_bundle_sha256"):
        predecessor_candidates = [candidate for candidate in preparation_path.parent.glob("rollback-bundle-g{}-*.json".format(preparation["generation"] - 1)) if sha256_file(candidate) == preparation["predecessor_bundle_sha256"]]
        predecessor_bundle = authenticate_rollback_bundle(predecessor_candidates[0])
        predecessor_post = predecessor_bundle["decisions"][-1]["post_rollback_commit"]
        if _run(command_runner, ["git", "merge-base", "--is-ancestor", predecessor_post, first["old_commit"]], cwd=repo_root).returncode:
            raise error(preparation_path, "measurement commit does not descend from predecessor post", "finalize the authenticated chained rollback lineage")
    verify_dispatch_source_removals(repo_root, head, [decision["dispatch"] for decision in decisions], command_runner)
    if removal_verifier and not removal_verifier(repo_root, preparation, head, tree):
        raise error(preparation_path, "dispatch removal predicate failed", "remove every prepared dispatch exactly")
    root = preparation_path.parent; generation = preparation["generation"]
    matching = []
    for candidate in root.glob("rollback-bundle-g{}-*.json".format(generation)):
        candidate_bundle = authenticate_rollback_bundle(candidate)
        if candidate_bundle.get("preparation_sha256") == sha256_file(preparation_path): matching.append(candidate.resolve())
        else: raise error(candidate, "conflicting finalized bundle generation", "use one preparation per generation")
    if matching: return matching[0]
    predecessor = None
    if preparation.get("predecessor_bundle_sha256"):
        candidates = [p for p in root.glob("rollback-bundle-g{}-*.json".format(generation - 1)) if sha256_file(p) == preparation["predecessor_bundle_sha256"]]
        if len(candidates) != 1: raise error(preparation_path, "predecessor bundle mismatch", "restore exact finalized predecessor")
        predecessor = candidates[0]; previous = authenticate_rollback_bundle(predecessor)
        inherited = previous["decisions"]; lineage_parent = inherited[-1]["post_rollback_commit"]
    else:
        inherited = []; lineage_parent = None
    finalized = []
    for value in preparation["decisions"]:
        decision = dict(value); decision.pop("decision_sha256", None)
        decision.update({"post_rollback_commit": head, "post_rollback_tree": tree, "lineage_parent_commit": lineage_parent,
                         "removal_evidence": {"dispatch": value["dispatch"], "removed_cells": value["removed_cells"],
                                              "old_commit": value["old_commit"], "old_tree": value["old_tree"],
                                              "post_rollback_commit": head, "post_rollback_tree": tree,
                                              "head_changed": True, "tree_changed": True}})
        decision["decision_sha256"] = sha256_bytes(payload_json_bytes(_decision_payload(decision))); finalized.append(decision)
    bundle = {"schema_version": ROLLBACK_SCHEMA_VERSION, "kind": "rollback_bundle", "generation": generation,
              "predecessor_bundle_sha256": preparation.get("predecessor_bundle_sha256"),
              "preparation_path": preparation_path.name, "preparation_sha256": sha256_file(preparation_path),
              "preparation_payload_sha256": preparation["preparation_sha256"], "decisions": inherited + finalized}
    bundle["bundle_sha256"] = sha256_bytes(payload_json_bytes(_bundle_payload(bundle)))
    path = root / "rollback-bundle-g{}-{}.json".format(generation, bundle["bundle_sha256"])
    atomic_write_json(path, bundle, fail_if_exists=True); authenticate_rollback_bundle(path)
    print(path); return path.resolve()


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
            lines.append("- `{}` removed after regression; triggering cells: {}; ineligible removed cells: {}.".format(
                decision["dispatch"], ", ".join(decision.get("regressed_cells", [])), ", ".join(decision.get("removed_cells", []))))
    else:
        lines.append("- No rollback decision is recorded.")
    lines += ["", "## Compatibility controls", "", "Fallback and composite controls remain claim-ineligible and are reported without a positive claim.", "", "## Limitations", "", "JMH GC allocation is environment-sensitive; throughput is diagnostic and not the allocation acceptance criterion.", ""]
    report = "\n".join(lines)
    validate_positive_language(report, manifest, Path("generated report"))
    return report


def validate_positive_language(report, manifest, path):
    removed = {cell for decision in manifest.get("rollback", {}).get("decisions", []) for cell in decision.get("removed_cells", [])}
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
        for generation_path, generation in authenticate_rollback_bundle_chain(bundle):
            preparation = generation_path.parent / generation["preparation_path"]
            authenticate_rollback_preparation(preparation)
            shutil.copy2(preparation, staging / preparation.name)
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
        for bundle_path, bundle in authenticate_rollback_bundle_chain(state["rollback_bundle_path"]):
            expected.add(bundle["preparation_path"])
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
    if state.get("rollback_bundle_path"):
        promoted_bundle = destination / Path(state["rollback_bundle_path"]).name
        promoted_chain = authenticate_rollback_bundle_chain(promoted_bundle)
        if promoted_chain[-1][1] != state.get("rollback_bundle"):
            raise error(promoted_bundle, "promoted rollback payload differs from state", "restore the exact bundles, preparations, and archives")
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
    anchor = commands.add_parser("_exec-anchor", help=argparse.SUPPRESS)
    anchor.add_argument("--status-fd", type=int, required=True)
    anchor.add_argument("--pass-fd", type=int, action="append", default=[])
    anchor.add_argument("target_argv", nargs=argparse.REMAINDER)
    resolve = commands.add_parser("resolve-jar"); resolve.add_argument("--jar-dir", required=True); resolve.add_argument("--state", required=True); resolve.add_argument("--rollback-bundle")
    run = commands.add_parser("run"); run.add_argument("--state", required=True); run.add_argument("--profile", choices=sorted(PROFILE_ARGS), required=True); run.add_argument("--output-root", required=True); run.add_argument("--run-id"); run.add_argument("--concurrent-heavy-work", choices=("absent", "present", "unknown"), required=True)
    compare = commands.add_parser("compare"); compare.add_argument("--state", required=True); compare.add_argument("--output", required=True); compare.add_argument("--validation", required=True)
    rollback = commands.add_parser("record-rollback"); rollback.add_argument("--state", required=True); rollback.add_argument("--dispatch", action="append", required=True, choices=DISPATCH_ORDER); rollback.add_argument("--archive-root", required=True)
    finalize = commands.add_parser("finalize-rollback"); finalize.add_argument("--preparation", required=True)
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
    if args.command == "_exec-anchor":
        target_argv = args.target_argv[1:] if args.target_argv[:1] == ["--"] else args.target_argv
        if not target_argv:
            raise ValueError("anchor target argv must not be empty")
        _anchor_process(args.status_fd, args.pass_fd, target_argv)
    elif args.command == "resolve-jar": resolve_jar(args.jar_dir, args.state, args.rollback_bundle)
    elif args.command == "run": run_benchmark(args.state, args.profile, args.output_root, args.run_id, args.concurrent_heavy_work)
    elif args.command == "compare": compare_state(args.state, args.output, args.validation)
    elif args.command == "record-rollback": record_rollback(args.state, args.dispatch, args.archive_root)
    elif args.command == "finalize-rollback": finalize_rollback(args.preparation)
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
