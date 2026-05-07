#!/usr/bin/env python3
"""Migrate Kluent imports to bluetape4k-assertions imports across all .kt files."""

import os
import re
import sys
from pathlib import Path

# Worktree root
ROOT = Path(__file__).parent.parent

# Imports that become InvokingBlock/CoInvokingBlock methods — no longer top-level
REMOVE_IMPORTS = {
    "import org.amshove.kluent.shouldThrow",
    "import org.amshove.kluent.shouldNotThrow",
    "import org.amshove.kluent.withMessage",
}

# Exact line replacements (before the generic rule)
EXACT_REPLACEMENTS = {
    "import org.amshove.kluent.internal.assertFailsWith":
        "import io.bluetape4k.assertions.assertFailsWith",
    "import org.amshove.kluent.internal.assertFails":
        "import io.bluetape4k.assertions.assertFails",
    # backtick aliases → camelCase
    "import org.amshove.kluent.`should be in range`":
        "import io.bluetape4k.assertions.shouldBeInRange",
    "import org.amshove.kluent.`should be near`":
        "import io.bluetape4k.assertions.shouldBeNear",
}

GENERIC_PATTERN = re.compile(r"^import org\.amshove\.kluent\.(.+)$")
REPLACEMENT_PREFIX = "import io.bluetape4k.assertions."

changed_files = 0
changed_lines = 0


def migrate_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if "org.amshove.kluent" not in text:
        return False

    lines = text.splitlines(keepends=True)
    new_lines = []
    file_changed = False

    for line in lines:
        stripped = line.rstrip("\n").rstrip("\r")

        # Lines to remove entirely
        if stripped in REMOVE_IMPORTS:
            file_changed = True
            continue  # skip this line

        # Exact replacements
        if stripped in EXACT_REPLACEMENTS:
            new_line = EXACT_REPLACEMENTS[stripped] + line[len(stripped):]
            new_lines.append(new_line)
            file_changed = True
            continue

        # Generic: import org.amshove.kluent.XXX → import io.bluetape4k.assertions.XXX
        m = GENERIC_PATTERN.match(stripped)
        if m:
            new_import = REPLACEMENT_PREFIX + m.group(1)
            new_line = new_import + line[len(stripped):]
            new_lines.append(new_line)
            file_changed = True
            continue

        new_lines.append(line)

    if file_changed:
        path.write_text("".join(new_lines), encoding="utf-8")

    return file_changed


def main():
    global changed_files, changed_lines

    kt_files = list(ROOT.rglob("*.kt"))
    print(f"Scanning {len(kt_files)} .kt files...")

    for kt_file in kt_files:
        if migrate_file(kt_file):
            changed_files += 1

    print(f"Done. Modified {changed_files} files.")


if __name__ == "__main__":
    main()
