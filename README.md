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

Tested on a Light Phone III (TLP301) with LightOS 582, firmware `00WW_1_440000`. You can install over Wi-Fi with
no cable, or with adb. Color needs adb once either way.

### Once, on the phone

1. Turn on developer mode for your phone on Light's [user dashboard](https://dashboard.thelightphone.com), as in
   Light's [Installing Tools Locally](https://github.com/lightphone/light-sdk/blob/main/docs/sideloading/README.md).
2. In the phone's Settings > Developer, set Allowed tools to allow any Tool. Doom is dev-signed, not signed by
   Light, and needs it: with a stricter setting, LightOS's code refuses to let it talk to LightOS, so it installs
   but doesn't work. (That's from reading LightOS; only the "any" setting has been tried on a phone.)

### Over Wi-Fi

Use [lightphone-wifi-install](https://github.com/yarosz/lightphone-wifi-install), which uploads a Tool to the
Tool Inbox of the phone's File Manager and needs only Python on your computer. Light's own guide describes a
`Developer` section there that LightOS 582 doesn't show yet; the Tool Inbox works today.

1. Put the phone and your computer on the same Wi-Fi, and turn off any VPN on the computer.
2. Run `python3 lp3-install.py` from lightphone-wifi-install (on Windows, `py lp3-install.py`).
3. Open this install link, which fills in Doom and its published SHA-256:
   <http://localhost:54450/?apk=https%3A%2F%2Fgithub.com%2Fyarosz%2Flight-doom%2Freleases%2Fdownload%2Fv0.1.0%2Flight-doom-color-v0.1.0.apk&sha256=daaf4879211113ba2d4b1ac6af00a722689f25828fadcb57650023974f76fedb>
4. On the phone, open Settings > Debug > File Manager, and hold its QR code up to your computer's camera.
5. Click Install on the phone, then open Doom from LightOS's Tools list.

Updating Doom this way keeps an earlier color grant.

If the upload is refused, see [If the phone refuses the upload](https://github.com/yarosz/lightphone-wifi-install#if-the-phone-refuses-the-upload)
in lightphone-wifi-install; on the phone this was tested on, adding a new Tool with (+) first fixed it.

### With adb

Light doesn't document turning on USB debugging. The route LP3 owners use: open the Phone tool, dial `*7412369#`
to open the developer menu, and turn on "Android Dev Mode" (USB debugging). Change only that toggle. You also need
`adb` from Android's [platform-tools](https://developer.android.com/tools/releases/platform-tools) and a USB-C cable
that carries data, and the phone awake and unlocked (it drops off USB when it sleeps).

1. Check the phone shows up: `adb devices`.
2. Install (skip this if you installed over Wi-Fi) and grant color:
   ```
   adb install light-doom-color-v0.1.0.apk
   adb shell pm grant com.yarosz.doom android.permission.WRITE_SECURE_SETTINGS
   ```
3. Open Doom from LightOS's Tools list, and tap COLOR (see [Color](#color)).

### From source

You also need [mise](https://mise.jdx.dev), which installs Java 17 and the Android command-line tools from
`mise.toml`, and the Android SDK packages the build asks for (`sdkmanager` can install them). Only macOS has been
tried. Clone with the Light SDK submodule, then build, install and launch in one step:

```
git clone --recurse-submodules https://github.com/yarosz/light-doom.git
cd light-doom
mise trust && mise install
mise exec -- scripts/run.sh -v release        # add -c for the color build
```

### Removing it

With adb, `adb uninstall com.yarosz.doom`; otherwise as you remove any Tool. Uninstalling also drops the color
grant. If the grayscale filter was ever left off (see Color), open and leave a photo in LightOS's album to restore
it.

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
