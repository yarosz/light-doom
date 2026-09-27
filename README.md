# light-doom

Doom running as a LightOS Tool on the Light Phone III: 35 frames per second, in pure Kotlin, inside Light's
SDK sandbox. It's a proof of concept for tinkerers, shared for what it shows about the platform (and the meme).
It isn't an official Tool, isn't submitted to Light's Tool directory, and isn't affiliated with Light or id
Software.

How it works: id's C Doom, compiled to WebAssembly ([CharlieTap/doom.wasm](https://github.com/CharlieTap/doom.wasm),
by way of [CharlieTap/mood](https://github.com/CharlieTap/mood)), runs on [Chasm](https://github.com/CharlieTap/chasm),
a WebAssembly interpreter written in Kotlin. The Tool draws Doom's frames with Compose Canvas. No native code,
no reflection, no extra dependencies: the default build passes the Light SDK plugin's checks and a local rehearsal
of Light's release builder.

## Playing

Hold the phone sideways with its top to the left. Light's plugin only accepts portrait Tools, so the Tool
stays portrait to Android and draws its screen a quarter turn clockwise.

| Input | In a game | In menus |
|---|---|---|
| Left thumb, left half | Floating stick: walk and strafe | Drag up/down to move, left/right for sliders |
| Right thumb, right half | Drag sideways to turn (small drags aim finely, far drags spin with run); quick tap fires | Tap to select |
| Shutter (right index finger) | Fire while held, either stage | Select |
| Volume up (left index finger) | Use: doors and switches | Select |
| Volume down | Next owned weapon | Back one menu |
| MENU, MAP | Doom's menu, automap | |
| LIGHT n | Doom's gamma, 0 to 4 | |
| GRAY / COLOR | Palette; with the color build, COLOR also shows real color (see below) | |
| Wheel, home, power | Left to LightOS: brightness, flashlight, home | |

The back gesture closes the Tool, and reopening resumes the same game. Quit Game closes it. The screen stays on
during play while there was input in the last three minutes.

## Installing

This is a dev-signed Tool, so the phone must accept Tools that Light hasn't signed: developer mode with USB
debugging on, and External Tools set to "All tools". Then, with the phone attached:

```
scripts/run.sh -v release        # build, install and launch; add -c for the color build
```

The toolchain comes from [mise](https://mise.jdx.dev) (`mise.toml`: Java 17, Android SDK). The Light SDK is a
submodule pinned to v0.1.2: clone with `--recurse-submodules`, or run `git submodule update --init`.

## Color

LightOS keeps the whole display grayscale with Android's color-correction filter, and lifts it only for its own
Photos, where `accessibility_display_daltonizer_enabled` goes 1 → 0 → 1. Tools can't ask for the same yet;
[lightphone/light-sdk#190](https://github.com/lightphone/light-sdk/issues/190) requests it.

The color build does what Photos does: it lifts the filter while Doom is on screen in COLOR mode, and puts back
the value it found on pause, screen-off, home, quit, engine stop and crash. It needs a permission Light's plugin
won't let a Tool declare, so `scripts/color-build.sh` (used by `run.sh -c`) applies
`color/light-sdk-color.patch` to the SDK plugin, swaps in `color/ColorBackend.kt` and adds the permission for
that one build, then restores all three. Grant the permission once per phone:

```
adb shell pm grant com.yarosz.doom android.permission.WRITE_SECURE_SETTINGS
```

Without the grant the pad reads "COLOR (no grant)" and nothing changes. One gap: a hard kill while Doom is on
screen (`am force-stop`) leaves the filter off, because nothing runs to restore it. Opening and leaving Doom, or a
photo in LightOS's album, puts it back.

## Measured

| | fps median | range |
|---|---:|---|
| LP3, release, 60 s | 34.9 | 34.0 to 35.9 |
| Emulator, debug | 35.0 | 34.9 to 35.8 |

Doom's tick rate is 35 Hz, so both run at full speed. The interpreter uses about one CPU core.

## Commands

- `scripts/run.sh [-s serial] [-d seconds] [-v debug|release] [-c]` builds, installs and launches on the attached
  LP3 (else the emulator) and reports the frame rate from the Tool's `perf` log lines.
- `scripts/touch-check.sh [serial]` checks that touches reach the right controls through the rotation.
- `mise run light-build` rehearses Light's release builder on the default build (`--tree` for the working tree).
- `scripts/vendor.py <chasm> <kotlin-result> <mood>` regenerates the vendored source (see below).

## Vendored code

Light's plugin allows neither Chasm nor kotlin-result as a dependency, and a Tool module can't be Kotlin
Multiplatform, so `scripts/vendor.py` copies their Android source sets and Mood's Doom runtime into
`tool/src/main/kotlin` as plain Android Kotlin: it resolves each `expect`/`actual` pair (moving default
arguments onto the `actual`) and rewrites Kotlin 2.4 collection literals for the SDK's Kotlin 2.3. Versions and
licenses are in `third_party/`. Don't edit the copies by hand.

## Known limits

- Doom's quit path traps in the interpreter (`IndirectCallHasIncorrectFunctionType`); the Tool reads a stop right
  after YES on a prompt as a quit and closes. Any other stop shows the error.
- The engine takes key presses only, so turning is proportional by pulsing Doom's slow-turn key.
- doomgeneric drops key code 0 and leaves the next-weapon keys unbound, so the Tool reads owned weapons off the
  status bar; with the status bar hidden, volume down steps through every slot.
- Mood's own Android app shows red and blue swapped on the LP3 (it has no WebGPU adapter, and the fallback
  backend swaps channels); this Tool draws from Doom's palette and isn't affected.

## Credits and licenses

- Doom © id Software, released under the GNU GPL v2; `tool/src/main/assets/doom.bin` is `doom.wasm` built from
  that source and embeds the shareware DOOM1.WAD, which id permits redistributing unmodified. DOOM is a trademark
  of id Software; this project isn't affiliated with or endorsed by id Software, ZeniMax or Microsoft.
- Chasm and Mood by CharlieTap (Apache-2.0 OR MIT); kotlin-result by Michael Bull (ISC). Their license files are
  in `third_party/`.
- The Light SDK is a submodule, under its own license.
- Everything else in this repository is under the GNU GPL, version 2 or (at your option) any later version; see
  `LICENSE`.
