package desu.inugram.helpers.chat

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.view.MotionEvent
import android.view.SoundEffectConstants
import androidx.core.graphics.ColorUtils
import desu.inugram.InuConfig
import org.telegram.messenger.AndroidUtilities.dpf2
import org.telegram.messenger.LiteMode
import org.telegram.messenger.MessageObject
import org.telegram.messenger.Utilities
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.ChatMessageCell
import org.telegram.ui.Components.CubicBezierInterpolator
import org.telegram.ui.Components.spoilers.SpoilerEffect
import org.telegram.ui.Components.spoilers.SpoilerEffect2
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object LinkPreviewSpoilerHelper {
    private class State {
        var spoilered = false
        var revealProgress = 0f
        var revealX = 0f
        var revealY = 0f
        var revealMaxRadius = 0f
        var effect2: SpoilerEffect2? = null
        var effect2Index: Int? = null
        var fallback: SpoilerEffect? = null
        val cardRect = RectF()
        var cardRadius = 0f
        var blurBitmap: Bitmap? = null
        var blurCanvas: Canvas? = null
        var blurW = 0
        var blurH = 0
        var blurDirty = true
        var blurSrc: Bitmap? = null
    }

    private const val BLUR_SCALE = 0.2f

    private val states = WeakHashMap<ChatMessageCell, State>()
    private fun stateOf(cell: ChatMessageCell) = states.getOrPut(cell) { State() }

    private val backingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val clipPath = Path()

    @JvmStatic
    fun isSpoilered(cell: ChatMessageCell): Boolean =
        InuConfig.LINK_PREVIEW_SPOILER.value && states[cell]?.spoilered == true

    @JvmStatic
    fun onMessageContent(cell: ChatMessageCell, hasLinkPreview: Boolean) {
        val state = stateOf(cell)
        val msg = cell.messageObject
        val spoilered = InuConfig.LINK_PREVIEW_SPOILER.value && hasLinkPreview &&
            msg != null && isLinkCoveredBySpoiler(msg)
        state.spoilered = spoilered
        if (!spoilered) {
            state.revealProgress = 0f
            detachEffect2(cell, state)
            state.blurBitmap?.recycle()
            state.blurBitmap = null
            state.blurCanvas = null
            state.blurSrc = null
            return
        }
        if (!msg!!.isSpoilersRevealed) {
            state.revealProgress = 0f
            state.blurDirty = true
            if (cell.isAttachedToWindow) ensureEffect2(cell, state)
        }
    }

    @JvmStatic
    fun onAttached(cell: ChatMessageCell) {
        val state = states[cell] ?: return
        if (!state.spoilered || cell.messageObject?.isSpoilersRevealed != false) return
        ensureEffect2(cell, state)
    }

    @JvmStatic
    fun onDetached(cell: ChatMessageCell) {
        val state = states[cell] ?: return
        val e = state.effect2 ?: return
        state.effect2Index = e.getAttachIndex(cell)
        e.detach(cell)
    }

    @JvmStatic
    fun setCardBounds(cell: ChatMessageCell, left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        val state = states[cell] ?: return
        if (!state.spoilered) return
        state.cardRect.set(left, top, right, bottom)
        state.cardRadius = radius
    }

    @JvmStatic
    fun startCardReveal(cell: ChatMessageCell, x: Float, y: Float) {
        val state = states[cell] ?: return
        if (!state.spoilered || state.revealProgress != 0f || state.cardRect.isEmpty) return
        state.revealX = x
        state.revealY = y
        state.revealMaxRadius = maxReachFromPoint(state.cardRect, x, y)
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = (state.revealMaxRadius * 0.3f).coerceIn(250f, 550f).toLong()
        animator.interpolator = CubicBezierInterpolator.EASE_BOTH
        animator.addUpdateListener {
            state.revealProgress = it.animatedValue as Float
            cell.invalidate()
        }
        animator.start()
    }

    @JvmStatic
    fun revealFromCardTap(cell: ChatMessageCell, x: Float, y: Float): Boolean {
        val state = states[cell] ?: return false
        if (!isSpoilered(cell) || state.revealProgress > 0f) return false
        val msg = cell.messageObject ?: return false
        if (msg.isSpoilersRevealed) return false
        val trigger = findFirstTextSpoiler(cell, msg)
        if (trigger != null) {
            cell.spoilerPressed = trigger
            val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, x, y, 0)
            event.offsetLocation(0f, y - cell.getEventY(event))
            val handled = cell.checkSpoilersMotionEvent(event, 0)
            event.recycle()
            if (!handled) cell.spoilerPressed = null
        } else {
            msg.isSpoilersRevealed = true
            startCardReveal(cell, x, y)
            cell.playSoundEffect(SoundEffectConstants.CLICK)
            cell.invalidate()
        }
        return true
    }

    private fun findFirstTextSpoiler(cell: ChatMessageCell, msg: MessageObject): SpoilerEffect? {
        cell.explanationLayout?.textLayoutBlocks?.forEach { block ->
            if (block.spoilers.isNotEmpty()) return block.spoilers[0]
        }
        val caption = cell.captionLayout
        if (caption != null) {
            caption.textLayoutBlocks?.forEach { block ->
                if (block.spoilers.isNotEmpty()) return block.spoilers[0]
            }
        } else {
            msg.textLayoutBlocks?.forEach { block ->
                if (block.spoilers.isNotEmpty()) return block.spoilers[0]
            }
        }
        return null
    }

    @JvmStatic
    fun drawOverlay(canvas: Canvas, cell: ChatMessageCell) {
        if (cell.inu_capturingLinkPreviewBlur) return
        val state = states[cell] ?: return
        if (!state.spoilered || !InuConfig.LINK_PREVIEW_SPOILER.value) return
        val msg = cell.messageObject ?: return
        if (msg.isSpoilersRevealed && state.revealProgress >= 1f) return
        if (msg.isSpoilersRevealed && state.revealProgress == 0f) return
        val r = state.cardRect
        if (r.isEmpty) return

        val blur = ensureBlur(cell, state, r)

        canvas.save()
        clipPath.rewind()
        clipPath.addRoundRect(r, state.cardRadius, state.cardRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        clipPath.rewind()
        clipPath.addRect(r.left, r.top, r.left + dpf2(3f), r.bottom, Path.Direction.CW)
        canvas.clipPath(clipPath, Region.Op.DIFFERENCE)
        if (state.revealProgress > 0f) {
            clipPath.rewind()
            clipPath.addCircle(state.revealX, state.revealY, state.revealMaxRadius * state.revealProgress, Path.Direction.CW)
            canvas.clipPath(clipPath, Region.Op.DIFFERENCE)
        }

        if (blur != null) {
            canvas.drawBitmap(blur, null, r, bitmapPaint)
        } else {
            backingPaint.color = Theme.getColor(Theme.key_chat_mediaLoaderPhoto, cell.resourcesProvider)
            backingPaint.alpha = 255
            canvas.drawRect(r, backingPaint)
        }

        val mode = InuConfig.MEDIA_SPOILER_MODE.value
        if (mode == InuConfig.MediaSpoilerModeItem.TELEGRAM) {
            drawParticles(canvas, cell, state, r)
        } else {
            val scrim = ColorUtils.blendARGB(getBubbleColor(cell), Color.BLACK, 0.4f)
            SpoilerHelper.drawMediaSpoilerStyle(canvas, mode, r.left, r.top, r.right, r.bottom, 1f, cell.resourcesProvider, true, scrim)
        }
        canvas.restore()
    }

    private fun getBubbleColor(cell: ChatMessageCell): Int {
        val key = if (cell.messageObject?.isOutOwner == true) Theme.key_chat_outBubble else Theme.key_chat_inBubble
        return Theme.getColor(key, cell.resourcesProvider)
    }

    private fun drawParticles(canvas: Canvas, cell: ChatMessageCell, state: State, r: RectF) {
        val effect2 = state.effect2
        if (effect2 != null && !effect2.destroyed) {
            canvas.save()
            canvas.translate(r.left, r.top)
            val w = r.width()
            val h = r.height()
            val scale = max(1f, max(w / effect2.width, h / effect2.height))
            canvas.scale(scale, scale)
            effect2.draw(canvas, cell, (w / scale).toInt(), (h / scale).toInt(), 1f, cell.drawingToBitmap)
            canvas.restore()
            cell.invalidate()
        } else if (!LiteMode.isEnabled(LiteMode.FLAG_CHAT_SPOILER)) {
            val eff = state.fallback ?: SpoilerEffect().also { state.fallback = it }
            eff.setColor(ColorUtils.setAlphaComponent(Color.WHITE, (255 * 0.5f).toInt()))
            eff.setBounds(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
            eff.draw(canvas)
        }
    }

    private fun ensureBlur(cell: ChatMessageCell, state: State, r: RectF): Bitmap? {
        val w = r.width().toInt()
        val h = r.height().toInt()
        if (w <= 0 || h <= 0) return null
        val src = cell.photoImage.bitmap
        val cached = state.blurBitmap
        val imageJustLoaded = state.blurSrc == null && src != null
        if (cached != null && !state.blurDirty && state.blurW == w && state.blurH == h && !imageJustLoaded) {
            return cached
        }

        val bw = max(1, (w * BLUR_SCALE).toInt())
        val bh = max(1, (h * BLUR_SCALE).toInt())
        var bmp = state.blurBitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            state.blurBitmap = bmp
            state.blurCanvas = Canvas(bmp)
        }
        val c = state.blurCanvas!!
        bmp.eraseColor(getBubbleColor(cell) or 0xFF000000.toInt())
        c.save()
        c.scale(bw / w.toFloat(), bh / h.toFloat())
        c.translate(-r.left, -r.top)
        cell.inu_capturingLinkPreviewBlur = true
        try {
            cell.drawLinkPreview(c, 1f)
        } finally {
            cell.inu_capturingLinkPreviewBlur = false
        }
        c.restore()
        Utilities.stackBlurBitmap(bmp, max(4, min(bw, bh) / 6))

        state.blurW = w
        state.blurH = h
        state.blurSrc = src
        state.blurDirty = false
        return bmp
    }

    private fun ensureEffect2(cell: ChatMessageCell, state: State) {
        if (!SpoilerEffect2.supports()) return
        val e = state.effect2
        if (e != null && !e.destroyed) {
            e.attach(cell)
            return
        }
        val created = SpoilerEffect2.getInstance(cell) ?: return
        state.effect2 = created
        state.effect2Index?.let { created.reassignAttach(cell, it) }
    }

    private fun detachEffect2(cell: ChatMessageCell, state: State) {
        val e = state.effect2 ?: return
        e.detach(cell)
        state.effect2 = null
        state.effect2Index = null
    }

    private fun maxReachFromPoint(r: RectF, x: Float, y: Float): Float {
        val dx = max(abs(x - r.left), abs(x - r.right))
        val dy = max(abs(y - r.top), abs(y - r.bottom))
        return sqrt(dx * dx + dy * dy)
    }

    private fun isLinkCoveredBySpoiler(msg: MessageObject): Boolean {
        val media = MessageObject.getMedia(msg.messageOwner)
        val webpage = media?.webpage as? TLRPC.TL_webPage ?: return false
        val target = normalizeUrl(webpage.url) ?: return false
        val entities = msg.messageOwner?.entities ?: return false
        val text = msg.messageOwner?.message ?: return false
        val spoilers = entities.filterIsInstance<TLRPC.TL_messageEntitySpoiler>()
        if (spoilers.isEmpty()) return false
        for (e in entities) {
            val url = when (e) {
                is TLRPC.TL_messageEntityTextUrl -> e.url
                is TLRPC.TL_messageEntityUrl -> {
                    val end = e.offset + e.length
                    if (e.offset < 0 || end > text.length) null else text.substring(e.offset, end)
                }
                else -> null
            } ?: continue
            if (normalizeUrl(url) != target) continue
            val end = e.offset + e.length
            for (s in spoilers) {
                if (e.offset >= s.offset && end <= s.offset + s.length) return true
            }
        }
        return false
    }

    private fun normalizeUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        var u = url.trim().lowercase()
        u = u.removePrefix("https://").removePrefix("http://")
        u = u.removeSuffix("/")
        return u.ifEmpty { null }
    }
}
