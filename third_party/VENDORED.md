# Vendored source

- Chasm 2.0.0 (github.com/CharlieTap/chasm, Apache-2.0 OR MIT): 22 modules under `tool/src/main/kotlin/io/github/charlietap/chasm/`.
- kotlin-result 2.2.0 (github.com/michaelbull/kotlin-result, ISC): `tool/src/main/kotlin/com/github/michaelbull/result/`.
- Mood ecace54 (github.com/CharlieTap/mood, Apache-2.0 OR MIT): doom-runtime and its Chasm codegen output under `tool/src/main/kotlin/com/tap/mood/`; `doom.wasm` as `tool/src/main/assets/doom.bin`.
- doom.wasm is built from id Software's Doom source (GPL-2.0, github.com/CharlieTap/doom.wasm) and embeds the shareware DOOM1.WAD, which id permits redistributing unmodified.

Regenerate with `scripts/vendor.py <chasm> <kotlin-result> <mood>`; never edit the copies by hand.
