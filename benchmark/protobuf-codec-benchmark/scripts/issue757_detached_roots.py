#!/usr/bin/env python3
"""Receipt-owned detached roots for issue #757 evidence collection."""

import hashlib
import json
import os
import shutil
import stat
import subprocess
import uuid
from pathlib import Path


PUBLIC_API_VERSION = 1
PUBLIC_API = (
    "begin_attempt",
    "materialize_worktree",
    "materialize_build_root",
    "seal_artifact",
    "cleanup_selected",
)
OWNER_MARKER = ".issue757-owner.json"


class OwnershipError(RuntimeError):
    pass


def _canonical_directory(path):
    path = Path(path)
    resolved = path.resolve(strict=True)
    mode = resolved.stat().st_mode
    if not stat.S_ISDIR(mode):
        raise OwnershipError("expected directory: {}".format(resolved))
    _reject_symlink_components(resolved)
    return resolved


def _reject_symlink_components(path):
    current = Path(path.anchor)
    for part in Path(path).parts[1:]:
        current = current / part
        if stat.S_ISLNK(current.lstat().st_mode):
            raise OwnershipError("symlink path component: {}".format(current))


def _fsync_directory(path):
    descriptor = os.open(str(path), os.O_RDONLY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)


def _json_bytes(value):
    return (json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n").encode(
        "utf-8"
    )


def _sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def _sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _write_no_clobber(path, data, mode=0o600):
    path = Path(path)
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    descriptor = os.open(str(path), flags, mode)
    try:
        with os.fdopen(descriptor, "wb", closefd=False) as stream:
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
    finally:
        os.close(descriptor)
    _fsync_directory(path.parent)
    return path


def _write_selection(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.parent / (".{}.{}.tmp".format(path.name, uuid.uuid4().hex))
    _write_no_clobber(temporary, _json_bytes(value))
    os.replace(str(temporary), str(path))
    _fsync_directory(path.parent)


def _read_json(path):
    path = Path(path)
    mode = path.lstat().st_mode
    if not stat.S_ISREG(mode) or path.stat().st_nlink != 1:
        raise OwnershipError("expected single-link regular file: {}".format(path))
    return json.loads(path.read_bytes().decode("utf-8"))


def _git(repository, *arguments):
    return subprocess.check_output(
        ["git", "-C", str(repository)] + list(arguments),
        text=True,
        stderr=subprocess.STDOUT,
    ).strip()


def _append_record(context, phase, event, fields=None):
    fields = dict(fields or {})
    journal = Path(context["journal"])
    existing = sorted(journal.glob("*.json"))
    sequence = len(existing) + 1
    previous_hash = _sha256_file(existing[-1]) if existing else None
    record = {
        "schema": "issue757-detached-roots-v1",
        "attempt_id": context["attempt_id"],
        "sequence": sequence,
        "phase": phase,
        "event": event,
        "previous_sha256": previous_hash,
    }
    record.update(fields)
    record_path = journal / ("{:04d}-{}.json".format(sequence, event))
    payload = _json_bytes(record)
    _write_no_clobber(record_path, payload)
    selection = {
        "schema": "issue757-detached-selection-v1",
        "attempt_id": context["attempt_id"],
        "evidence_root": context["evidence_root"],
        "journal_path": str(record_path),
        "journal_sha256": _sha256_bytes(payload),
    }
    _write_selection(context["selection_state"], selection)
    return record


def _load_selected(selection_state):
    selection_path = Path(selection_state).resolve(strict=True)
    selection = _read_json(selection_path)
    journal_path = Path(selection["journal_path"])
    if _sha256_file(journal_path) != selection["journal_sha256"]:
        raise OwnershipError("selected journal hash mismatch")
    record = _read_json(journal_path)
    evidence_root = _canonical_directory(selection["evidence_root"])
    if record["attempt_id"] != selection["attempt_id"]:
        raise OwnershipError("selection attempt mismatch")
    journal = journal_path.parent.resolve(strict=True)
    if evidence_root not in journal.parents:
        raise OwnershipError("journal escapes evidence root")
    context = {
        "attempt_id": selection["attempt_id"],
        "evidence_root": str(evidence_root),
        "journal": str(journal),
        "selection_state": str(selection_path),
    }
    return context, record


def _owner_payload(attempt_id, kind, path):
    return {
        "schema": "issue757-owner-v1",
        "attempt_id": attempt_id,
        "kind": kind,
        "path": str(path),
    }


def _write_owner(path, attempt_id, kind, recorded_path=None):
    marker = Path(path) / OWNER_MARKER
    payload = _json_bytes(
        _owner_payload(attempt_id, kind, Path(recorded_path or path))
    )
    _write_no_clobber(marker, payload)
    return {"path": str(marker), "sha256": _sha256_bytes(payload)}


def _root_receipt(path, marker, extra=None):
    status = Path(path).stat()
    receipt = {
        "path": str(Path(path)),
        "device": status.st_dev,
        "inode": status.st_ino,
        "marker": marker,
    }
    receipt.update(extra or {})
    return receipt


def _attempt_root(context):
    root = Path(context["evidence_root"]) / "attempts" / context["attempt_id"]
    root = root.resolve(strict=True)
    if Path(context["evidence_root"]) not in root.parents:
        raise OwnershipError("attempt root escapes evidence root")
    return root


def begin_attempt(
    *,
    repository_root,
    evidence_root,
    role,
    revision,
    tree,
    selection_state,
    attempt_id=None
):
    repository = _canonical_directory(repository_root)
    evidence = _canonical_directory(evidence_root)
    selection_state = Path(selection_state).absolute()
    if selection_state.exists() or selection_state.is_symlink():
        raise FileExistsError(str(selection_state))
    if selection_state.parent.resolve(strict=True) != evidence:
        raise OwnershipError("selection state must be directly under evidence root")
    resolved_revision = _git(repository, "rev-parse", "{}^{{commit}}".format(revision))
    resolved_tree = _git(repository, "rev-parse", "{}^{{tree}}".format(revision))
    if resolved_revision != revision or resolved_tree != tree:
        raise OwnershipError("revision or tree authority mismatch")
    attempt_id = attempt_id or uuid.uuid4().hex
    if not attempt_id or any(character not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_" for character in attempt_id):
        raise ValueError("unsafe attempt id")

    journals = evidence / "journals"
    attempts = evidence / "attempts"
    journals.mkdir(exist_ok=True)
    attempts.mkdir(exist_ok=True)
    journal = journals / attempt_id
    journal.mkdir()
    _fsync_directory(journals)
    context = {
        "attempt_id": attempt_id,
        "evidence_root": str(evidence),
        "journal": str(journal),
        "selection_state": str(selection_state),
    }
    repository_stat = repository.stat()
    evidence_stat = evidence.stat()
    intent = _append_record(
        context,
        -1,
        "intent",
        {
            "repository_root": str(repository),
            "repository_device": repository_stat.st_dev,
            "repository_inode": repository_stat.st_ino,
            "evidence_device": evidence_stat.st_dev,
            "evidence_inode": evidence_stat.st_ino,
            "role": role,
            "revision": resolved_revision,
            "tree": resolved_tree,
        },
    )

    staging = attempts / (".{}.staging".format(attempt_id))
    final = attempts / attempt_id
    staging.mkdir()
    _write_owner(staging, attempt_id, "attempt-parent", recorded_path=final)
    (staging / "artifacts").mkdir()
    (staging / "roots").mkdir()
    _fsync_directory(staging)
    if final.exists() or final.is_symlink():
        raise FileExistsError(str(final))
    os.rename(str(staging), str(final))
    _fsync_directory(attempts)
    marker = final / OWNER_MARKER
    _append_record(
        context,
        0,
        "parent-published",
        {"attempt_root": _root_receipt(final, {"path": str(marker), "sha256": _sha256_file(marker)})},
    )
    return intent


def materialize_worktree(*, selection_state):
    context, selected = _load_selected(selection_state)
    if "worktree" in selected and Path(selected["worktree"]["path"]).exists():
        return Path(selected["worktree"]["path"])
    intent = _read_json(Path(context["journal"]) / "0001-intent.json")
    repository = _canonical_directory(intent["repository_root"])
    root = _attempt_root(context) / "roots" / "worktree-{}".format(intent["role"])
    if root.exists() or root.is_symlink():
        raise FileExistsError(str(root))
    subprocess.run(
        ["git", "-C", str(repository), "worktree", "add", "--detach", str(root), intent["revision"]],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if _git(root, "rev-parse", "HEAD") != intent["revision"]:
        raise OwnershipError("worktree revision mismatch")
    if _git(root, "rev-parse", "HEAD^{tree}") != intent["tree"]:
        raise OwnershipError("worktree tree mismatch")
    marker = _write_owner(root, context["attempt_id"], "worktree")
    receipt = _root_receipt(
        root,
        marker,
        {
            "revision": intent["revision"],
            "tree": intent["tree"],
            "repository_root": str(repository),
        },
    )
    _append_record(context, 1, "worktree-materialized", {"worktree": receipt})
    return root


def materialize_build_root(*, selection_state):
    context, selected = _load_selected(selection_state)
    if "build_root" in selected and Path(selected["build_root"]["path"]).exists():
        return Path(selected["build_root"]["path"])
    root = _attempt_root(context) / "roots" / "build"
    root.mkdir()
    marker = _write_owner(root, context["attempt_id"], "build-root")
    receipt = _root_receipt(root, marker)
    fields = {"build_root": receipt}
    if "worktree" in selected:
        fields["worktree"] = selected["worktree"]
    _append_record(context, 2, "build-root-materialized", fields)
    return root


def seal_artifact(*, selection_state, relative_path, data):
    context, selected = _load_selected(selection_state)
    relative = Path(relative_path)
    if relative.is_absolute() or not relative.parts or any(part in {"", ".", ".."} for part in relative.parts):
        raise ValueError("unsafe artifact path")
    artifacts = _attempt_root(context) / "artifacts"
    current = artifacts
    for part in relative.parts[:-1]:
        current = current / part
        if current.exists() or current.is_symlink():
            if current.is_symlink() or not current.is_dir():
                raise OwnershipError("unsafe artifact ancestor")
        else:
            current.mkdir()
            _fsync_directory(current.parent)
    destination = artifacts / relative
    if artifacts not in destination.parent.resolve(strict=True).parents and destination.parent.resolve(strict=True) != artifacts:
        raise OwnershipError("artifact escapes attempt root")
    _write_no_clobber(destination, bytes(data), mode=0o444)
    sealed = dict(selected.get("sealed_artifacts", {}))
    sealed[str(relative)] = {
        "path": str(destination),
        "sha256": _sha256_file(destination),
        "size": destination.stat().st_size,
    }
    fields = {"sealed_artifacts": sealed}
    for name in ("worktree", "build_root"):
        if name in selected:
            fields[name] = selected[name]
    _append_record(context, 3, "artifact-sealed", fields)
    return destination


def _verify_owned_root(receipt, attempt_id, kind):
    root = Path(receipt["path"])
    mode = root.lstat().st_mode
    if not stat.S_ISDIR(mode):
        raise OwnershipError("owned root is not a directory")
    status = root.stat()
    if status.st_dev != receipt["device"] or status.st_ino != receipt["inode"]:
        raise OwnershipError("owned root inode drift")
    marker = Path(receipt["marker"]["path"])
    if marker.parent != root or _sha256_file(marker) != receipt["marker"]["sha256"]:
        raise OwnershipError("owner marker drift")
    if _read_json(marker) != _owner_payload(attempt_id, kind, root):
        raise OwnershipError("owner marker identity mismatch")
    return root


def cleanup_selected(*, selection_state):
    context, selected = _load_selected(selection_state)
    if selected.get("cleanup_status") == "cleaned":
        return selected
    worktree = selected.get("worktree")
    build_root = selected.get("build_root")
    if worktree and Path(worktree["path"]).exists():
        root = _verify_owned_root(worktree, context["attempt_id"], "worktree")
        if _git(root, "rev-parse", "HEAD") != worktree["revision"]:
            raise OwnershipError("worktree revision drift")
        if _git(root, "rev-parse", "HEAD^{tree}") != worktree["tree"]:
            raise OwnershipError("worktree tree drift")
        subprocess.run(
            ["git", "-C", worktree["repository_root"], "worktree", "remove", "--force", str(root)],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        if root.exists():
            raise OwnershipError("worktree cleanup incomplete")
    if build_root and Path(build_root["path"]).exists():
        root = _verify_owned_root(build_root, context["attempt_id"], "build-root")
        shutil.rmtree(str(root))
        if root.exists():
            raise OwnershipError("build root cleanup incomplete")
    fields = {"cleanup_status": "cleaned", "status": "cleaned"}
    if "sealed_artifacts" in selected:
        fields["sealed_artifacts"] = selected["sealed_artifacts"]
    return _append_record(context, 4, "cleanup-complete", fields)
