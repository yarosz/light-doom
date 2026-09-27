package com.yarosz.doom

import android.graphics.Bitmap
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.tap.mood.doom.runtime.engine.Binary
import com.tap.mood.doom.runtime.engine.EngineFactory
import com.tap.mood.doom.runtime.engine.Frame
import com.tap.mood.doom.runtime.input.InputMode
import com.tap.mood.doom.runtime.input.Key
import com.tap.mood.doom.runtime.instance.Instance
import com.tap.mood.doom.runtime.settings.EngineSettings
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import java.io.File
import kotlin.math.abs
import com.tap.mood.doom.runtime.instance.InstanceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** LP3 hardware keycodes (LightOS Generic.kl; Light's LightDeviceKeys). */
private object Lp3Keys {
    const val VOLUME_UP = 24
    const val VOLUME_DOWN = 25
    const val SHUTTER = 27
    const val SHUTTER_HALF = 80
}

/** How palette indices become pixels: Doom's colors, or grays tuned for LightOS's grayscale panel. */
enum class Look { COLOR, GRAY }

/** Two ARGB frame buffers: the engine thread fills the back one, the UI draws [front]. */
class FrameBuffers(val width: Int, val height: Int) {
    @Volatile var front = IntArray(width * height)
    private var back = IntArray(width * height)
    private var paletteRevision = -1
    private var look: Look? = null
    private val argb = IntArray(256)

    /**
     * Weapon slots 2..7 the status bar shows as owned (bit n = slot n), or 0 when it can't tell (no status bar).
     * Doom draws an owned slot's digit in yellow (STYSNUM) and an unowned one in gray (STGNUM), in the ARMS box.
     */
    @Volatile var ownedSlots = 0
        private set

    fun fill(frame: Frame, look: Look) {
        if (frame.paletteRevision != paletteRevision || look != this.look) {
            paletteRevision = frame.paletteRevision
            this.look = look
            for (i in 0 until 256) argb[i] = shade(frame.palette[i], look)
        }
        val px = frame.indexedPixels
        val out = back
        for (i in out.indices) out[i] = argb[px[i].toInt() and 0xff]
        back = front
        front = out
        if (width == 320 && height == 200) ownedSlots = readArms(px, frame.palette)
    }

    /** Owned digits measured on the emulator: 0xffff80, 9 pixels in the cell; unowned ones are gray (0x8f8f8f). */
    private fun readArms(px: ByteArray, palette: IntArray): Int {
        var slots = 0
        for (cell in 0 until 6) {
            val x0 = ARMS_X + (cell % 3) * ARMS_DX
            val y0 = ARMS_Y + (cell / 3) * ARMS_DY
            var yellow = 0
            for (y in y0 - 1 until y0 + 7) for (x in x0 - 1 until x0 + 5) {
                val rgb = palette[px[y * width + x].toInt() and 0xff]
                val r = rgb shr 16 and 0xff
                val g = rgb shr 8 and 0xff
                val b = rgb and 0xff
                if (r >= 200 && g >= 200 && r - b >= 60) yellow++
            }
            if (yellow >= LIT_PIXELS) slots = slots or (1 shl (cell + 2))
        }
        return slots
    }

    private fun shade(rgb: Int, look: Look): Int {
        if (look == Look.COLOR) return rgb or ALPHA
        val luma = (0.299 * (rgb shr 16 and 0xff) + 0.587 * (rgb shr 8 and 0xff) + 0.114 * (rgb and 0xff)) / 255.0
        val v = (Math.pow(luma, GRAY_GAMMA) * 255).toInt().coerceIn(0, 255)
        return ALPHA or (v shl 16) or (v shl 8) or v
    }

    private companion object {
        const val ALPHA = 0xFF000000.toInt()
        /** Doom's palette is dark; lifting the midtones keeps corridors readable on the LP3's gray panel. */
        const val GRAY_GAMMA = 0.75

        /** st_stuff.c: ST_ARMSX, ST_ARMSY, ST_ARMSXSPACE, ST_ARMSYSPACE, in the 320x200 frame. */
        const val ARMS_X = 111
        const val ARMS_Y = 172
        const val ARMS_DX = 12
        const val ARMS_DY = 10
        const val LIT_PIXELS = 5
    }
}

/**
 * One running Doom per process. LightOS can relaunch the Tool's activity (and so build a new screen and view model)
 * while the old ones are never cleared, so the engine, its keys and its loops live here, not in a view model:
 * a relaunch resumes the same game instead of starting a second engine.
 */
class Game private constructor(filesDir: File, readAsset: (String) -> ByteArray) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dispatcher = engineDispatcher()
    val instance = Instance(
        engineFactory = EngineFactory(Binary { readAsset("doom.bin") }),
        saveStore = FileSaveStore(filesDir),
        audioSink = TrackAudioSink(),
        dispatcher = dispatcher,
        logger = DoomLogger,
        onExecutionFinished = { dispatcher.close() },
    )
    val keys = Keys(instance)
    val frames = MutableStateFlow<FrameBuffers?>(null)
    val frameCount = MutableStateFlow(0L)
    var look = Look.GRAY

    /** Horizontal displacement of the right thumb on the turn surface, -1..1. */
    val turn = MutableStateFlow(0f)
    private val turning = Turning()

    init {
        instance.setFrameSink { frame ->
            val buffers = frames.value?.takeIf { it.width == frame.width && it.height == frame.height }
                ?: FrameBuffers(frame.width, frame.height).also { frames.value = it }
            buffers.fill(frame, look)
            frameCount.value++
        }
        scope.launch {
            while (true) {
                delay(TIC_MILLIS)
                keys.hold("turn", if (gameplay()) turning.tick(turn.value) else emptySet())
            }
        }
        scope.launch { instance.state.distinctUntilChangedBy { it.inputMode }.collect { keys.releaseAll() } }
        scope.launch {
            while (true) {
                delay(1000)
                val s = instance.state.value
                Log.i(TAG, "perf status=${s.status} fps=${s.framesPerSecond ?: 0.0} mode=${s.inputMode}")
            }
        }
    }

    fun gameplay() = instance.state.value.inputMode == InputMode.Gameplay

    /** Stops the engine for good; the next [obtain] starts a fresh game. */
    fun end() {
        scope.cancel()
        instance.close()
        if (current === this) current = null
    }

    companion object {
        private var current: Game? = null

        fun obtain(filesDir: File, readAsset: (String) -> ByteArray): Game =
            current ?: Game(filesDir, readAsset).also { current = it }
    }
}

class DoomViewModel(private val filesDir: File, readAsset: (String) -> ByteArray) : LightViewModel<Unit>() {
    val game = Game.obtain(filesDir, readAsset)
    val instance = game.instance
    val keys = game.keys
    val look = MutableStateFlow(if (File(filesDir, "look-color").exists()) Look.COLOR else Look.GRAY)
    val gamma = MutableStateFlow(File(filesDir, "gamma").takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 2)

    /** Set once the player quit Doom; the screen then closes the Tool. */
    val ended = MutableStateFlow(false)

    /** Set when the engine stopped without the player quitting; the screen shows it with a CLOSE button. */
    val failure = MutableStateFlow<String?>(null)
    private var shutterHalf = false
    private var shutterFull = false
    private var yesAtMillis = 0L
    private var weaponSlot = 2

    init {
        game.look = look.value
        instance.configure(EngineSettings(gammaLevel = gamma.value))
        viewModelScope.launch {
            instance.state.first { it.status == InstanceState.Status.Error }
            val message = instance.state.value.errorMessage ?: "unknown error"
            DisplayColor.restore()
            game.end()
            // Doom's quit path traps in the interpreter (IndirectCallHasIncorrectFunctionType), so a stop right
            // after YES on a prompt is the player quitting; any other stop is a real failure.
            if (android.os.SystemClock.uptimeMillis() - yesAtMillis < QUIT_WINDOW_MILLIS) {
                Log.i(TAG, "doom quit; closing the Tool")
                ended.value = true
            } else {
                Log.e(TAG, "engine stopped: $message")
                failure.value = message
            }
        }
    }

    /** YES on a Doom prompt; remembered so the engine stop that follows Quit Game reads as a quit. */
    fun confirm() {
        yesAtMillis = android.os.SystemClock.uptimeMillis()
        keys.tap(Key.character('y'))
    }

    fun close() {
        ended.value = true
    }

    /**
     * Doom 1 has no working next-weapon key here (doomgeneric drops key code 0, and key_nextweapon is unbound), so
     * the Tool steps through the slot digits itself, skipping slots the status bar shows as not owned. Slot 1 (fist
     * or chainsaw) is always owned. With no status bar (full-screen size) every slot is tried.
     */
    private fun nextWeapon() {
        val owned = game.frames.value?.ownedSlots ?: 0
        val slots = if (owned == 0) (1..7).toList() else listOf(1) + (2..7).filter { owned and (1 shl it) != 0 }
        weaponSlot = slots.firstOrNull { it > weaponSlot } ?: slots.first()
        Log.i(TAG, "weapon slot $weaponSlot (owned ${slots.joinToString(",")})")
        keys.tap(Key.character('0' + weaponSlot))
    }

    /** A quick touch fires: fire is held briefly so a tap during the weapon's cooldown still shoots. */
    fun fireTap() {
        viewModelScope.launch {
            keys.hold("tap-fire", setOf(Key.FIRE))
            delay(FIRE_TAP_MILLIS)
            keys.hold("tap-fire", emptySet())
        }
    }

    fun gameplay() = game.gameplay()

    fun toggleLook() {
        look.value = if (look.value == Look.GRAY) Look.COLOR else Look.GRAY
        game.look = look.value
        DisplayColor.want(look.value == Look.COLOR)
        val flag = File(filesDir, "look-color")
        if (look.value == Look.COLOR) flag.writeText("1") else flag.delete()
    }

    fun cycleGamma() {
        gamma.value = (gamma.value + 1) % 5
        File(filesDir, "gamma").writeText(gamma.value.toString())
        instance.configure(EngineSettings(gammaLevel = gamma.value))
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        DisplayColor.want(look.value == Look.COLOR)
        instance.setActive(true)
    }

    /** Leaving, screen-off or home: grayscale comes back before anything else can happen. */
    override fun onAppPause() {
        DisplayColor.want(false)
        game.turn.value = 0f
        keys.releaseAll()
        instance.setActive(false)
    }

    override fun onScreenHide(screen: SimpleLightScreen<Unit>) = DisplayColor.want(false)

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = key(keyCode, event, pressed = true)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = key(keyCode, event, pressed = false)

    /**
     * Held sideways (top of the phone to the left), the right index finger rests on the shutter and the left one on
     * the volume keys. The wheel, home and power are left to LightOS: returning false forwards the wheel, so
     * brightness and the flashlight still work.
     */
    private fun key(code: Int, event: KeyEvent, pressed: Boolean): Boolean {
        if (code !in HANDLED) return false
        val repeat = pressed && event.repeatCount > 0
        val play = gameplay()
        when (code) {
            Lp3Keys.SHUTTER_HALF, Lp3Keys.SHUTTER -> {
                if (code == Lp3Keys.SHUTTER) shutterFull = pressed else shutterHalf = pressed
                if (play) {
                    keys.hold("shutter", if (shutterHalf || shutterFull) setOf(Key.FIRE) else emptySet())
                } else if (pressed && !repeat && code == Lp3Keys.SHUTTER_HALF) {
                    keys.tap(Key.ENTER)
                }
            }
            Lp3Keys.VOLUME_UP -> if (pressed && !repeat) keys.tap(if (play) Key.USE else Key.ENTER)
            Lp3Keys.VOLUME_DOWN -> if (pressed && !repeat) {
                if (play) nextWeapon() else keys.tap(Key.BACKSPACE)
            }
        }
        return true
    }

    private companion object {
        val HANDLED = setOf(Lp3Keys.VOLUME_UP, Lp3Keys.VOLUME_DOWN, Lp3Keys.SHUTTER, Lp3Keys.SHUTTER_HALF)
        const val QUIT_WINDOW_MILLIS = 3_000L
        const val FIRE_TAP_MILLIS = 200L
    }
}

/** Lays the content out and draws it a quarter turn clockwise, so it is upright with the phone's top to the left. */
private fun Modifier.sideways() = layout { measurable, constraints ->
    val w = constraints.maxWidth
    val h = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(h, w))
    layout(w, h) { placeable.placeWithLayer((w - h) / 2, (h - w) / 2) { rotationZ = 90f } }
}

private val LABEL = TextStyle(color = Color.White, fontSize = 11.sp)
private val BIG = TextStyle(color = Color.White, fontSize = 18.sp)
private val SMALL = TextStyle(color = Color(0xCCFFFFFF), fontSize = 10.sp)
private val RING = Color(0x66FFFFFF)

/** A floating stick: the first touch sets its center; reports displacement (x right, y down, -1..1). */
private class Stick {
    val origin = mutableStateOf<Offset?>(null)
    val knob = mutableStateOf(Offset.Zero)
}

@InitialScreen
class DoomScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, DoomViewModel>(sealedActivity) {
    override val viewModelClass: Class<DoomViewModel>
        get() = DoomViewModel::class.java

    override fun createViewModel() = DoomViewModel(lightContext.filesDir, lightContext::readAsset)

    @Composable
    override fun Content() {
        val vm = viewModel
        val state by vm.instance.state.collectAsState()
        val ended by vm.ended.collectAsState()
        val failure by vm.failure.collectAsState()
        LaunchedEffect(ended) { if (ended) goBack() }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.fillMaxSize().sideways()) {
                Picture(vm.game)
                val stopped = failure
                when {
                    stopped != null -> Failure(stopped) { vm.close() }
                    state.inputMode == InputMode.Gameplay -> Gameplay(vm)
                    state.inputMode == InputMode.Menu -> Menu(vm)
                    state.inputMode == InputMode.Confirmation ->
                        Choice("YES" to { vm.confirm() }, "NO" to { vm.keys.tap(Key.character('n')) })
                    else -> Choice("SAVE" to { saveUnderFixedName(vm) }, "CANCEL" to { vm.keys.tap(Key.ESCAPE) })
                }
                if (stopped == null) Top(vm, "%.0f fps".format(state.framesPerSecond ?: 0.0))
            }
            val playing = state.inputMode == InputMode.Gameplay && state.status == InstanceState.Status.Running
            val recent by produceState(true, vm.keys) {
                while (true) {
                    value = android.os.SystemClock.uptimeMillis() - vm.keys.lastInputMillis < AWAKE_AFTER_INPUT_MILLIS
                    delay(5_000)
                }
            }
            if (playing && recent) KeepAwake()
            ColorBridge()
        }
    }

    /** Hands [DisplayColor] this build's [ColorBackend], which may need an attached View. */
    @Composable
    private fun ColorBridge() {
        AndroidView(factory = { View(it).also { v -> DisplayColor.attach(ColorBackend.attach(v)) } }, modifier = Modifier.size(1.dp))
    }

    /**
     * The screen stays on during play only. A Tool can't reach its window's flags, but any attached View can ask for
     * keep-screen-on; in menus and when paused the phone sleeps as usual.
     */
    @Composable
    private fun KeepAwake() {
        AndroidView(factory = { View(it).apply { keepScreenOn = true } }, modifier = Modifier.size(1.dp))
    }

    @Composable
    private fun Picture(game: Game) {
        val buffers by game.frames.collectAsState()
        val count by game.frameCount.collectAsState()
        val b = buffers
        if (b == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BasicText("loading doom…", style = LABEL) }
            return
        }
        val bitmap = remember(b) { Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888) }
        Canvas(Modifier.fillMaxSize()) {
            count
            bitmap.setPixels(b.front, 0, b.width, 0, 0, b.width, b.height)
            val fitWidth = size.height * 4f / 3f <= size.width
            val dw = if (fitWidth) size.height * 4f / 3f else size.width
            val dh = if (fitWidth) size.height else size.width * 3f / 4f
            drawImage(
                bitmap.asImageBitmap(),
                srcSize = IntSize(b.width, b.height),
                dstOffset = IntOffset(((size.width - dw) / 2).toInt(), ((size.height - dh) / 2).toInt()),
                dstSize = IntSize(dw.toInt(), dh.toInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }

    @Composable
    private fun Gameplay(vm: DoomViewModel) {
        val move = remember { Stick() }
        val aim = remember { Stick() }
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxHeight().stickRing(move).pointerInput(Unit) {
                    stick(move, onMove = { x, y -> vm.keys.hold("move", movementKeys(x, y)) }, onTap = {})
                },
            )
            Box(
                Modifier.weight(1f).fillMaxHeight().stickRing(aim).pointerInput(Unit) {
                    stick(aim, onMove = { x, _ -> vm.game.turn.value = x }, onTap = { vm.fireTap() })
                },
            )
        }
    }

    @Composable
    private fun Menu(vm: DoomViewModel) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                    val step = 36.dp.toPx()
                    awaitEachGesture {
                        var last = awaitFirstDown().position
                        while (true) {
                            val change = awaitPointerEvent().changes.first()
                            if (!change.pressed) break
                            val d = change.position - last
                            val key = when {
                                abs(d.y) >= step && abs(d.y) >= abs(d.x) -> if (d.y > 0) Key.DOWN else Key.UP
                                abs(d.x) >= step -> if (d.x > 0) Key.RIGHT else Key.LEFT
                                else -> null
                            }
                            if (key != null) {
                                vm.keys.tap(key)
                                last = change.position
                            }
                        }
                    }
                },
                contentAlignment = Alignment.BottomCenter,
            ) { BasicText("drag: move · sideways: sliders", style = SMALL, modifier = Modifier.padding(BAND_TEXT)) }
            Box(
                Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        if (waitForUpOrCancellation() != null) vm.keys.tap(Key.ENTER)
                    }
                },
                contentAlignment = Alignment.BottomCenter,
            ) { BasicText("tap: select", style = SMALL, modifier = Modifier.padding(BAND_TEXT)) }
        }
    }

    /** Doom's yes/no prompts and save-name entry, as two opaque buttons low on the screen. */
    @Composable
    private fun Choice(first: Pair<String, () -> Unit>, second: Pair<String, () -> Unit>) {
        Row(
            Modifier.fillMaxSize().padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            for ((label, action) in listOf(first, second)) {
                Pad(label, Modifier.size(128.dp, 56.dp), solid = true, onTap = action)
            }
        }
    }

    private fun saveUnderFixedName(vm: DoomViewModel) {
        repeat(SAVE_NAME_MAX) { vm.keys.tap(Key.BACKSPACE) }
        SAVE_NAME.forEach { vm.keys.tap(Key.character(it)) }
        vm.keys.tap(Key.ENTER)
    }

    /** The engine stopped without the player quitting: say so instead of vanishing. */
    @Composable
    private fun Failure(message: String, onClose: () -> Unit) {
        Box(Modifier.fillMaxSize().background(Color(0xE0000000)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BasicText("Doom stopped", style = LABEL)
                BasicText(message, style = SMALL, modifier = Modifier.padding(horizontal = 32.dp))
                Pad("CLOSE", Modifier.size(128.dp, 48.dp), solid = true, onTap = onClose)
            }
        }
    }

    /** The top band: the 4:3 picture leaves about 25 dp above it, so the bar sits there, clear of Doom's messages. */
    @Composable
    private fun Top(vm: DoomViewModel, fps: String) {
        val look by vm.look.collectAsState()
        val gamma by vm.gamma.collectAsState()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Pad("MENU", TOP_PAD) { vm.keys.tap(Key.ESCAPE) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(fps, style = SMALL)
                Pad("LIGHT $gamma", TOP_PAD) { vm.cycleGamma() }
                val support by DisplayColor.support.collectAsState()
                val noGrant = support == DisplayColor.Support.NOT_GRANTED
                val color = if (noGrant) "COLOR (no grant)" else "COLOR"
                Pad(if (look == Look.GRAY) "GRAY" else color, if (noGrant) Modifier.size(104.dp, 22.dp) else TOP_PAD) {
                    vm.toggleLook()
                }
            }
            Pad("MAP", TOP_PAD) { vm.keys.tap(Key.TAB) }
        }
    }

    @Composable
    private fun Pad(label: String, modifier: Modifier, solid: Boolean = false, onTap: () -> Unit) {
        Box(
            modifier
                .background(if (solid) Color(0xFF1A1A1A) else Color(0x99202020))
                .then(if (solid) Modifier.border(1.dp, Color.White) else Modifier)
                .pointerInput(label) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        if (waitForUpOrCancellation() != null) onTap()
                    }
                },
            contentAlignment = Alignment.Center,
        ) { BasicText(label, style = if (solid) BIG else LABEL) }
    }

    private fun Modifier.stickRing(s: Stick) = drawBehind {
        val o = s.origin.value ?: return@drawBehind
        val r = STICK_RADIUS.toPx()
        drawCircle(RING, radius = r, center = o, style = Stroke(2.dp.toPx()))
        drawCircle(RING, radius = r / 3, center = o + s.knob.value * r)
    }

    /** Floating stick: displacement from the touch-down point, normalized to [STICK_RADIUS]; a short, still touch taps. */
    private suspend fun PointerInputScope.stick(s: Stick, onMove: (Float, Float) -> Unit, onTap: () -> Unit) {
        val r = STICK_RADIUS.toPx()
        val slop = 10.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            s.origin.value = down.position
            var travelled = 0f
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (travelled < slop && change.uptimeMillis - down.uptimeMillis < TAP_MILLIS) onTap()
                    break
                }
                val d = change.position - down.position
                travelled = maxOf(travelled, d.getDistance())
                val k = Offset((d.x / r).coerceIn(-1f, 1f), (d.y / r).coerceIn(-1f, 1f))
                s.knob.value = k
                onMove(k.x, k.y)
                change.consume()
            }
            s.origin.value = null
            s.knob.value = Offset.Zero
            onMove(0f, 0f)
        }
    }

    private companion object {
        val STICK_RADIUS = 56.dp
        val TOP_PAD = Modifier.size(58.dp, 22.dp)
        val BAND_TEXT = 5.dp
        const val TAP_MILLIS = 220L
        const val SAVE_NAME = "lp3 save"
        const val SAVE_NAME_MAX = 24
        /** The attract demo also counts as gameplay; without recent input the phone may sleep. */
        const val AWAKE_AFTER_INPUT_MILLIS = 180_000L
    }
}
