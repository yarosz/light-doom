#!/usr/bin/env python3
"""Vendor Chasm, kotlin-result and Mood's Doom runtime into tool/src/main/kotlin as plain Android Kotlin.

  scripts/vendor.py <chasm checkout> <kotlin-result checkout> <mood checkout, built once>

Light's plugin allows neither library as a dependency and the tool module cannot be Kotlin
Multiplatform, so their Android-relevant source sets are copied in and flattened:

  * each module contributes commonMain plus the source sets Android compiles (androidMain, and
    vm's nonJsTargetsMain; kotlin-result's jvmMain, the variant Android resolves);
  * every `expect` declaration is deleted and the matching `actual` keyword is stripped;
  * Kotlin 2.4 collection literals (`= []`) are rewritten to the stdlib builder their declared
    type implies, since the SDK compiles with Kotlin 2.3.

Idempotent: the output directories are recreated on every run. Upstream commits are written to
third_party/VENDORED.md.
"""
import pathlib
import re
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "tool/src/main/kotlin"
ANDROID = ["commonMain", "androidMain"]

CHASM_MODULES = {m: ANDROID for m in [
    "ast", "chasm", "chasm-coroutines", "config", "decoder", "executor/instantiator", "executor/invoker",
    "host", "gc", "libs/stack", "libs/parallel", "memory", "compiler", "runtime/address",
    "runtime/core", "runtime/type", "runtime/value", "stream", "type-system", "validator",
]} | {"vm": ANDROID + ["nonJsTargetsMain"], "libs/sse2": ANDROID + ["nonWindowsMain"]}
RESULT_SETS = ["commonMain", "jvmMain"]

EXPECT = re.compile(r"^[ \t]*(?:@[\w.]+(?:\([^)]*\))?\s+)*(?:(?:public|internal|private)\s+)?expect\s", re.M)
ACTUAL = re.compile(r"\bactual\s+(?=(?:(?:public|internal|private|inline|value|data|annotation|override)\s+)*(?:fun|class|object|val|var|typealias|interface|constructor)\b)")
LITERAL = re.compile(r"(:\s*(Mutable)?(List|Set|Map|Collection|Iterable)<[^=\n]*?>\??\s*=\s*)\[\s*([^\[\]]*?)\s*\]")
BUILDERS = {
    (True, "List"): "mutableListOf", (True, "Set"): "mutableSetOf", (True, "Map"): "mutableMapOf",
    (True, "Collection"): "mutableListOf",
    (False, "List"): "listOf", (False, "Set"): "setOf", (False, "Map"): "mapOf",
    (False, "Collection"): "listOf", (False, "Iterable"): "listOf",
}
EMPTY = {"listOf": "emptyList", "setOf": "emptySet", "mapOf": "emptyMap"}


DEFAULTS: dict[str, tuple[dict[str, str], list[str]]] = {}
FUN_NAME = re.compile(r"\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(")
PARAM_DEFAULT = re.compile(r"(\w+)\s*:\s*[^=,()]+(?:\([^)]*\))?[^=,()]*=\s*([^,\n]+?)\s*,?\s*$", re.M)


def remember_defaults(declaration: str, text: str) -> None:
    """Kotlin puts default arguments on the expect; keep them to move onto the actual."""
    name = FUN_NAME.search(declaration)
    defaults = dict(PARAM_DEFAULT.findall(declaration))
    if name and defaults:
        DEFAULTS[name.group(1)] = (defaults, re.findall(r"(?m)^import .*$", text))


def apply_defaults(text: str) -> str:
    for name, (defaults, imports) in DEFAULTS.items():
        m = re.search(rf"\bfun\s+(?:<[^>]*>\s*)?{name}\s*\(", text)
        if not m:
            continue
        depth, i = 1, m.end()
        while depth:
            depth += {"(": 1, ")": -1}.get(text[i], 0)
            i += 1
        params = text[m.end():i - 1]
        for p, default in defaults.items():
            params = re.sub(rf"(\b{p}\s*:\s*[^,=]+?)(\s*(,|$))", rf"\1 = {default}\2", params, count=1)
        text = text[:m.end()] + params + text[i - 1:]
        have = set(re.findall(r"(?m)^import .*$", text))
        missing = [imp for imp in imports if imp not in have]
        if missing:
            text = re.sub(r"(?m)^(package .*)$", lambda pm: pm.group(1) + "\n\n" + "\n".join(missing), text, count=1)
    return text


def strip_expects(text: str) -> str:
    """Delete each expect declaration: its signature, and its body if it is a class with braces."""
    out, pos = [], 0
    for m in EXPECT.finditer(text):
        if m.start() < pos:
            continue
        out.append(text[pos:m.start()])
        i, depth = m.end(), 0
        while i < len(text):
            c = text[i]
            if c in "({":
                depth += 1
            elif c in ")}":
                depth -= 1
                if depth == 0 and c == "}":
                    i += 1
                    break
            elif c == "\n" and depth == 0:
                rest = text[i + 1:].lstrip(" \t")
                if not rest.startswith(("{", ":", ".", "where", ")")):
                    break
            i += 1
        remember_defaults(text[m.start():i], text)
        pos = i
    out.append(text[pos:])
    return "".join(out)


def rewrite_literals(text: str) -> str:
    def repl(m):
        builder = BUILDERS[(bool(m.group(2)), m.group(3))]
        if not m.group(4):
            builder = EMPTY.get(builder, builder)
        return f"{m.group(1)}{builder}({m.group(4)})"
    return LITERAL.sub(repl, text)


def transform(text: str) -> str:
    if EXPECT.search(text):
        text = strip_expects(text)
    if ACTUAL.search(text):
        text = apply_defaults(ACTUAL.sub("", text))
    return rewrite_literals(text)


def empty(text: str) -> bool:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return not re.sub(r"(?m)^[ \t]*(@file:.*|package\b.*|import\b.*)$|//.*$|\s", "", text)


def copy_sets(base: pathlib.Path, sets: list[str], dest: pathlib.Path) -> int:
    n = 0
    for s in sets:
        src = base / "src" / s / "kotlin"
        if not src.is_dir():
            continue
        for f in sorted(src.rglob("*.kt")):
            rel = f.relative_to(src)
            text = transform(f.read_text())
            if empty(text):
                continue
            name = rel.name if s == "commonMain" else f"{rel.stem}{s.removesuffix('Main').capitalize()}.kt"
            target = dest / rel.parent / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text)
            n += 1
    return n


def describe(repo: pathlib.Path) -> str:
    return subprocess.run(["git", "-C", str(repo), "describe", "--tags", "--always"],
                          capture_output=True, text=True).stdout.strip()


MOOD_SKIP = {"RuntimeModule.kt", "Resource.kt"}
METRO = re.compile(r"(?m)^import dev\.zacsweers\.metro\..*\n|^[ \t]*@(Inject|SingleIn\([^)]*\)|ContributesBinding\([^)]*\)|ContributesTo\([^)]*\)|Provides)[ \t]*\n")


def copy_mood(mood: pathlib.Path) -> int:
    """Mood's doom-runtime (Metro DI and Compose resources removed) plus its checked-out Chasm codegen output.

    The codegen output comes from Mood's own build (`./gradlew :android:assembleRelease` in the Mood checkout),
    because the Chasm Gradle plugin that generates it is not an allowed plugin here.
    """
    n = 0
    src = mood / "doom-runtime/src/commonMain/kotlin"
    gen = mood / "doom-runtime/build/generated/kotlin/commonMain/DoomWasmModule"
    for base in (src, gen):
        for f in sorted(base.rglob("*.kt")):
            if f.name in MOOD_SKIP:
                continue
            target = OUT / f.relative_to(base)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(METRO.sub("", f.read_text()))
            n += 1
    assets = ROOT / "tool/src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    shutil.copy(mood / "doom-runtime/src/commonMain/composeResources/files/doom/doom.wasm", assets / "doom.bin")
    return n


def main():
    chasm, result, mood = map(pathlib.Path, sys.argv[1:4])
    for pkg in ("io/github/charlietap/chasm", "com/github/michaelbull/result", "com/tap/mood"):
        shutil.rmtree(OUT / pkg, ignore_errors=True)
    total = sum(copy_sets(chasm / m, sets, OUT) for m, sets in CHASM_MODULES.items())
    total += copy_sets(result / "kotlin-result", RESULT_SETS, OUT)
    total += copy_mood(mood)
    left = [str(p.relative_to(OUT)) for p in OUT.rglob("*.kt")
            if re.search(r"^\s*(?:[\w@]+\s+)*expect\s+(fun|class|object|val)", p.read_text(), re.M)]
    third = ROOT / "third_party"
    third.mkdir(exist_ok=True)
    (third / "VENDORED.md").write_text(
        "# Vendored source\n\n"
        f"- Chasm {describe(chasm)} (github.com/CharlieTap/chasm, Apache-2.0 OR MIT): {len(CHASM_MODULES)} modules "
        "under `tool/src/main/kotlin/io/github/charlietap/chasm/`.\n"
        f"- kotlin-result {describe(result)} (github.com/michaelbull/kotlin-result, ISC): "
        "`tool/src/main/kotlin/com/github/michaelbull/result/`.\n"
        f"- Mood {describe(mood)} (github.com/CharlieTap/mood, Apache-2.0 OR MIT): doom-runtime and its Chasm codegen "
        "output under `tool/src/main/kotlin/com/tap/mood/`; `doom.wasm` as `tool/src/main/assets/doom.bin`.\n"
        "- doom.wasm is built from id Software's Doom source (GPL-2.0, github.com/CharlieTap/doom.wasm) and embeds the "
        "shareware DOOM1.WAD, which id permits redistributing unmodified.\n\n"
        "Regenerate with `scripts/vendor.py <chasm> <kotlin-result> <mood>`; never edit the copies by hand.\n")
    for lic, name in ((chasm / "LICENSE-APACHE", "chasm-LICENSE-APACHE"), (chasm / "LICENSE-MIT", "chasm-LICENSE-MIT"),
                      (result / "LICENSE", "kotlin-result-LICENSE"), (mood / "LICENSE-APACHE", "mood-LICENSE-APACHE"),
                      (mood / "LICENSE-MIT", "mood-LICENSE-MIT")):
        shutil.copy(lic, third / name)
    print(f"vendor: {total} files; expect declarations left: {len(left)}")
    for p in left:
        print(f"  {p}")


main()
