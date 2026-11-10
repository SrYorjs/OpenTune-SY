package com.arturo254.opentune.ui.visual.studio

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LiftedPart { MIDDLE, BAR }
enum class StudioBottomBarStyle { STACK, VOLUME }
enum class StudioStage { PLAYER, SELECTOR, EDIT, COVER, MIDDLE, MENU, BAR }

enum class StudioSheet { CONTROLS, SEEKBAR, END_TIME, MENU_SLOT, COVER_OPTIONS, DISC_LABEL, POSTER, IPOD, BEDSIDE }

private const val PrepareFrames = 2

private const val HandoffFrames = 3

internal val StudioSpring: AnimationSpec<Float> = spring(dampingRatio = 0.88f, stiffness = 210f)

@Stable
class Motion internal constructor(private val scope: CoroutineScope, initial: Float = 0f) {
    private val state = mutableFloatStateOf(initial)
    private var job: Job? = null

    var value: Float
        get() = state.floatValue
        set(v) {
            job?.cancel()
            job = null
            state.floatValue = v
        }

    val settling: Boolean get() = job?.isActive == true

    fun stop() {
        job?.cancel()
        job = null
    }

    fun animateTo(target: Float, velocity: Float = 0f, spec: AnimationSpec<Float> = StudioSpring, onEnd: (() -> Unit)? = null) {
        job?.cancel()
        val from = state.floatValue
        if (abs(from - target) < 0.0005f && abs(velocity) < 0.01f) {
            state.floatValue = target
            onEnd?.invoke()
            return
        }
        job = scope.launch {
            animate(from, target, velocity, spec) { v, _ -> state.floatValue = v }
            onEnd?.invoke()
        }
    }
}

@Stable
class StudioAnchors {
    private val pending = HashMap<MutableState<Rect>, LayoutCoordinates>()

    
    var root: LayoutCoordinates? = null
        set(value) {
            field = value
            if (value != null && pending.isNotEmpty()) {
                val waiting = pending.toList()
                pending.clear()
                waiting.forEach { (slot, coordinates) -> capture(slot, coordinates) }
            }
        }
    val cover = mutableStateOf(Rect.Zero)
    val songInfo = mutableStateOf(Rect.Zero)
    val slider = mutableStateOf(Rect.Zero)
    val transport = mutableStateOf(Rect.Zero)
    val card = mutableStateOf(Rect.Zero)
    val sideButton = mutableStateOf(Rect.Zero)
    val chevron = mutableStateOf(Rect.Zero)
    val seekBar = mutableStateOf(Rect.Zero)
    val endTime = mutableStateOf(Rect.Zero)
    
    val volumeBar = mutableStateOf(Rect.Zero)

    
    fun bar(style: StudioBottomBarStyle): MutableState<Rect> = when (style) {
        StudioBottomBarStyle.STACK -> card
        StudioBottomBarStyle.VOLUME -> volumeBar
    }

    
    val middle: Rect
        get() {
            val a = songInfo.value
            val b = slider.value
            if (a == Rect.Zero) return b
            if (b == Rect.Zero) return a
            return Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
        }

    fun capture(slot: MutableState<Rect>, coordinates: LayoutCoordinates) {
        val base = root
        if (base == null) {
            pending[slot] = coordinates
            return
        }
        if (!base.isAttached || !coordinates.isAttached) return
        val rect = base.localBoundingBoxOf(coordinates, clipBounds = false)
        if (rect != slot.value) slot.value = rect
    }
}

fun Modifier.studioAnchor(anchors: StudioAnchors?, slot: (StudioAnchors) -> MutableState<Rect>): Modifier =
    if (anchors == null) this else onGloballyPositioned { anchors.capture(slot(anchors), it) }

@Stable
class PlayerStudioState internal constructor(
    private val scope: CoroutineScope,
    val columns: List<List<Int>> = emptyList(),
) {
    fun columnOf(style: Int): Int = columns.indexOfFirst { style in it }.takeIf { it >= 0 } ?: style
    fun rowOf(style: Int): Int = columns.getOrNull(columnOf(style))?.indexOf(style)?.coerceAtLeast(0) ?: 0
    fun rowCount(column: Int): Int = columns.getOrNull(column)?.size ?: 1

    fun styleAt(column: Int, row: Int): Int =
        columns.getOrNull(column)?.let { it.getOrNull(row.coerceIn(0, it.lastIndex)) } ?: column

    private val rowMotions = HashMap<Int, Motion>()

    init {
        columns.indices.forEach { rows(it) }
    }

    fun rows(column: Int): Motion = rowMotions.getOrPut(column) { Motion(scope) }

    val liveColumn: Int get() = columnOf(livePage)
    val liveRow: Int get() = rowOf(livePage)

    var stage by mutableStateOf(StudioStage.PLAYER)
        private set

    
    val selector = Motion(scope)
    
    val edit = Motion(scope)
    
    val cover = Motion(scope)
    
    val backdrop = Motion(scope)
    
    val middle = Motion(scope)
    
    val menu = Motion(scope)
    
    val bar = Motion(scope)

    
    val page = Motion(scope)
    
    val handoff = Motion(scope)
    
    var livePage by mutableIntStateOf(0)

    
    val coverPage = Motion(scope)
    
    var coverCommitted by mutableIntStateOf(0)
    
    val backdropPage = Motion(scope)
    var backdropCommitted by mutableIntStateOf(0)
    
    val barPage = Motion(scope)
    var barCommitted by mutableIntStateOf(0)

    
    var sheetCovered by mutableStateOf(false)
        internal set

    var sheet by mutableStateOf<StudioSheet?>(null)
    
    var sheetSlot by mutableIntStateOf(0)

    val engaged: Boolean
        get() = stage != StudioStage.PLAYER || selector.value > 0.0005f || edit.value > 0.0005f

    
    val editing: Boolean
        get() = stage >= StudioStage.EDIT || edit.value > 0.0005f

    
    val cameraPart: LiftedPart?
        get() = when (stage) {
            StudioStage.MIDDLE, StudioStage.MENU -> LiftedPart.MIDDLE
            StudioStage.BAR -> LiftedPart.BAR
            else -> null
        }

    
    fun camera(part: LiftedPart): Motion = when (part) {
        LiftedPart.MIDDLE -> middle
        LiftedPart.BAR -> bar
    }

    fun open(livePage: Int) {
        if (stage != StudioStage.PLAYER) return
        this.livePage = livePage
        page.value = liveColumn.toFloat()
        rowMotions.forEach { (column, motion) -> motion.value = if (column == liveColumn) liveRow.toFloat() else 0f }
        rows(liveColumn).value = liveRow.toFloat()
        handoff.value = 0f
        stage = StudioStage.SELECTOR
        sheet = null
        scope.launch {
            repeat(PrepareFrames) { withFrameNanos { } }
            if (stage == StudioStage.SELECTOR) go(StudioStage.SELECTOR)
        }
    }

    
    fun settleOn(index: Int, restyled: Boolean, velocity: Float = 0f, after: () -> Unit = {}) {
        livePage = index
        if (!restyled) {
            go(StudioStage.PLAYER, velocity)
            after()
            return
        }
        handoff.value = 1f
        stage = StudioStage.PLAYER
        sheet = null
        page.animateTo(liveColumn.toFloat())
        rows(liveColumn).animateTo(liveRow.toFloat())
        scope.launch {
            repeat(HandoffFrames) { withFrameNanos { } }
            if (stage != StudioStage.PLAYER) return@launch
            go(StudioStage.PLAYER, velocity)
            after()
        }
    }

    fun go(target: StudioStage, selectorVelocity: Float = 0f) {
        stage = target
        sheet = null
        when (target) {
            StudioStage.PLAYER -> {
                if (handoff.value > 0f) handoff.animateTo(0f, spec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                selector.animateTo(0f, velocity = selectorVelocity); edit.animateTo(0f); cover.animateTo(0f)
                backdrop.animateTo(0f); middle.animateTo(0f); menu.animateTo(0f); bar.animateTo(0f)
                page.animateTo(liveColumn.toFloat())
                rows(liveColumn).animateTo(liveRow.toFloat())
            }
            StudioStage.SELECTOR -> {
                handoff.value = 0f
                selector.animateTo(1f); edit.animateTo(0f); cover.animateTo(0f)
                backdrop.animateTo(0f); middle.animateTo(0f); menu.animateTo(0f); bar.animateTo(0f)
            }
            StudioStage.EDIT -> {
                page.animateTo(liveColumn.toFloat())
                rows(liveColumn).animateTo(liveRow.toFloat())
                selector.animateTo(0f); edit.animateTo(1f); cover.animateTo(0f)
                backdrop.animateTo(0f); middle.animateTo(0f); menu.animateTo(0f); bar.animateTo(0f)
                coverPage.animateTo(coverCommitted.toFloat()); backdropPage.animateTo(backdropCommitted.toFloat())
                barPage.animateTo(barCommitted.toFloat())
            }
            StudioStage.COVER -> {
                coverPage.value = coverCommitted.toFloat()
                backdropPage.value = backdropCommitted.toFloat()
                edit.animateTo(1f)
                cover.animateTo(1f)
            }
            StudioStage.MIDDLE -> { edit.animateTo(1f); middle.animateTo(1f); menu.animateTo(0f) }
            StudioStage.MENU -> { menu.animateTo(1f) }
            StudioStage.BAR -> {
                barPage.value = barCommitted.toFloat()
                edit.animateTo(1f)
                bar.animateTo(1f)
            }
        }
    }

    
    fun back() {
        when (stage) {
            StudioStage.PLAYER -> Unit
            StudioStage.SELECTOR -> go(StudioStage.PLAYER)
            StudioStage.EDIT -> go(StudioStage.SELECTOR)
            StudioStage.COVER -> if (backdrop.value > 0.5f) backdrop.animateTo(0f) else go(StudioStage.EDIT)
            StudioStage.MIDDLE -> go(StudioStage.EDIT)
            StudioStage.MENU -> go(StudioStage.MIDDLE)
            StudioStage.BAR -> go(StudioStage.EDIT)
        }
    }

    
    fun reset() {
        stage = StudioStage.PLAYER
        sheet = null
        listOf(selector, edit, cover, backdrop, middle, menu, bar, handoff).forEach { it.value = 0f }
        page.value = liveColumn.toFloat()
        rows(liveColumn).value = liveRow.toFloat()
        coverPage.value = coverCommitted.toFloat()
        backdropPage.value = backdropCommitted.toFloat()
        barPage.value = barCommitted.toFloat()
    }

    fun openSheet(sheet: StudioSheet, slot: Int = 0) {
        sheetSlot = slot
        this.sheet = sheet
    }
}

@Composable
fun rememberPlayerStudioState(columns: List<List<Int>> = emptyList()): PlayerStudioState {
    val scope = rememberCoroutineScope()
    return remember(scope, columns) { PlayerStudioState(scope, columns) }
}

data class PartTransform(val scale: Float = 1f, val dx: Float = 0f, val dy: Float = 0f) {
    fun lerpTo(other: PartTransform, t: Float): PartTransform = PartTransform(
        scale + (other.scale - scale) * t,
        dx + (other.dx - dx) * t,
        dy + (other.dy - dy) * t,
    )

    
    fun apply(rect: Rect): Rect {
        val cx = rect.center.x + dx
        val cy = rect.center.y + dy
        val hw = rect.width * scale / 2f
        val hh = rect.height * scale / 2f
        return Rect(cx - hw, cy - hh, cx + hw, cy + hh)
    }

    companion object {
        val Identity = PartTransform()

        
        fun fit(from: Rect, to: Rect, scale: Float) = PartTransform(scale, to.center.x - from.center.x, to.center.y - from.center.y)
    }
}

internal const val CardScale = 0.72f
internal const val CardCenterY = 0.465f
internal val CardCorner: Dp = 34.dp
internal val CardGap: Dp = 18.dp

internal const val BoxScale = 0.6f
internal const val BoxCenterY = 0.43f
internal val BoxCorner: Dp = 30.dp

class TileLayout(
    val coverTile: Rect,
    val middleTile: Rect,
    val controlsTile: Rect,
    
    val barTile: Rect?,
    
    val fullCover: Boolean,
)

internal val TileMargin = 14.dp
internal val TilePadding = 10.dp
internal val TileCorner = 30.dp
internal val TileStroke = 3.5.dp

fun tileLayout(
    width: Float,
    height: Float,
    density: Density,
    anchors: StudioAnchors,
    fullCover: Boolean,
    barStyle: StudioBottomBarStyle,
): TileLayout = with(density) {
    val margin = TileMargin.toPx()
    val pad = TilePadding.toPx()
    val gap = 8.dp.toPx()
    val cover = anchors.cover.value.takeIf { it.width > 0f } ?: Rect(margin, height * 0.1f, width - margin, height * 0.5f)
    val middle = anchors.middle.takeIf { it.width > 0f } ?: Rect(0f, height * 0.62f, width, height * 0.74f)
    val transport = anchors.transport.value.takeIf { it.width > 0f } ?: Rect(0f, height * 0.78f, width, height * 0.86f)
    val bar = anchors.bar(barStyle).value.takeIf { it.width > 0f }

    val coverTile = if (fullCover) {
        Rect(margin, 0f, width - margin, cover.bottom.coerceAtMost(middle.top - gap * 2))
    } else {
        Rect(cover.left - pad, cover.top - pad, cover.right + pad, cover.bottom + pad)
    }
    var middleTile = Rect(margin, middle.top - pad, width - margin, middle.bottom + pad)
    var controlsTile = Rect(margin, transport.top - pad, width - margin, transport.bottom + pad)
    var barTile = bar?.let {
        val bottom = if (barStyle == StudioBottomBarStyle.STACK) height + TileCorner.toPx() * 2f else it.bottom + pad
        Rect(margin, it.top - pad, width - margin, bottom)
    }
    if (middleTile.bottom + gap > controlsTile.top) {
        val split = (middle.bottom + transport.top) / 2f
        middleTile = middleTile.copy(bottom = split - gap / 2f)
        controlsTile = controlsTile.copy(top = split + gap / 2f)
    }
    if (bar != null && barTile != null && controlsTile.bottom + gap > barTile.top) {
        val split = (transport.bottom + bar.top) / 2f
        controlsTile = controlsTile.copy(bottom = split - gap / 2f)
        barTile = barTile.copy(top = split + gap / 2f)
    }
    TileLayout(
        coverTile = coverTile,
        middleTile = middleTile,
        controlsTile = controlsTile,
        barTile = barTile,
        fullCover = fullCover,
    )
}
