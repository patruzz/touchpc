package com.touchpc.remotecontrol.ui.keyboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.protocol.ProtocolConstants

data class KeyDef(val label: String, val keycode: Int, val widthWeight: Float = 1f, val isModifier: Boolean = false)

class KeyboardLayoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onKeyCommandListener: ((Command) -> Unit)? = null
    private var currentLayout = 0
    private var activeModifiers = 0
    private val keyMargin = resources.getDimension(R.dimen.key_margin)
    private val cornerRadius = resources.getDimension(R.dimen.key_corner_radius)

    private val keyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.key_background); style = Paint.Style.FILL }
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.key_pressed); style = Paint.Style.FILL }
    private val modifierActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.key_modifier_active); style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.key_text); textSize = resources.getDimension(R.dimen.key_text_size); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val modifierTextPaint = Paint(textPaint).apply { color = context.getColor(R.color.key_modifier_text) }

    private var pressedKeyIndex = -1
    private var pressedRowIndex = -1

    private val qwertyLayout = listOf(
        listOf(KeyDef("Q",0x51),KeyDef("W",0x57),KeyDef("E",0x45),KeyDef("R",0x52),KeyDef("T",0x54),KeyDef("Y",0x59),KeyDef("U",0x55),KeyDef("I",0x49),KeyDef("O",0x4F),KeyDef("P",0x50)),
        listOf(KeyDef("A",0x41),KeyDef("S",0x53),KeyDef("D",0x44),KeyDef("F",0x46),KeyDef("G",0x47),KeyDef("H",0x48),KeyDef("J",0x4A),KeyDef("K",0x4B),KeyDef("L",0x4C)),
        listOf(KeyDef("Shift",0x10,1.5f,true),KeyDef("Z",0x5A),KeyDef("X",0x58),KeyDef("C",0x43),KeyDef("V",0x56),KeyDef("B",0x42),KeyDef("N",0x4E),KeyDef("M",0x4D),KeyDef("Del",0x08,1.5f)),
        listOf(KeyDef("Ctrl",0x11,1.5f,true),KeyDef("Alt",0x12,1f,true),KeyDef("Space",0x20,4f),KeyDef("Enter",0x0D,1.5f),KeyDef("Esc",0x1B,1f))
    )
    private val fKeysLayout = listOf(
        listOf(KeyDef("F1",0x70),KeyDef("F2",0x71),KeyDef("F3",0x72),KeyDef("F4",0x73)),
        listOf(KeyDef("F5",0x74),KeyDef("F6",0x75),KeyDef("F7",0x76),KeyDef("F8",0x77)),
        listOf(KeyDef("F9",0x78),KeyDef("F10",0x79),KeyDef("F11",0x7A),KeyDef("F12",0x7B)),
        listOf(KeyDef("PrtSc",0x2C),KeyDef("ScrLk",0x91),KeyDef("Pause",0x13),KeyDef("Ins",0x2D)),
        listOf(KeyDef("Home",0x24),KeyDef("End",0x23),KeyDef("PgUp",0x21),KeyDef("PgDn",0x22)),
        listOf(KeyDef("Up",0x26),KeyDef("Down",0x28),KeyDef("Left",0x25),KeyDef("Right",0x27),KeyDef("Del",0x2E))
    )
    private val numpadLayout = listOf(
        listOf(KeyDef("7",0x67),KeyDef("8",0x68),KeyDef("9",0x69),KeyDef("/",0x6F)),
        listOf(KeyDef("4",0x64),KeyDef("5",0x65),KeyDef("6",0x66),KeyDef("*",0x6A)),
        listOf(KeyDef("1",0x61),KeyDef("2",0x62),KeyDef("3",0x63),KeyDef("-",0x6D)),
        listOf(KeyDef("0",0x60,2f),KeyDef(".",0x6E),KeyDef("+",0x6B)),
        listOf(KeyDef("Tab",0x09),KeyDef("Enter",0x0D,2f),KeyDef("Bksp",0x08))
    )

    private fun getCurrentLayout() = when (currentLayout) { 1 -> fKeysLayout; 2 -> numpadLayout; else -> qwertyLayout }
    fun setLayout(index: Int) { currentLayout = index; invalidate() }
    fun setOnKeyCommandListener(listener: (Command) -> Unit) { onKeyCommandListener = listener }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = getCurrentLayout()
        val totalRows = layout.size
        val rowH = (height.toFloat() - keyMargin * (totalRows + 1)) / totalRows
        for ((rowIndex, row) in layout.withIndex()) {
            val totalWeight = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val keyW = (width.toFloat() - keyMargin * (row.size + 1)) / totalWeight
            var x = keyMargin
            for ((keyIndex, key) in row.withIndex()) {
                val w = keyW * key.widthWeight
                val y = keyMargin + rowIndex * (rowH + keyMargin)
                val rect = RectF(x, y, x + w, y + rowH)
                val isPressed = rowIndex == pressedRowIndex && keyIndex == pressedKeyIndex
                val isModActive = key.isModifier && isModifierActive(key.keycode)
                val paint = when { isModActive -> modifierActivePaint; isPressed -> keyPressedPaint; else -> keyBgPaint }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                val tp = if (isModActive) modifierTextPaint else textPaint
                canvas.drawText(key.label, rect.centerX(), rect.centerY() - (tp.descent() + tp.ascent()) / 2, tp)
                x += w + keyMargin
            }
        }
    }

    private fun isModifierActive(keycode: Int) = when (keycode) {
        0x11 -> (activeModifiers and ProtocolConstants.MOD_CTRL) != 0
        0x10 -> (activeModifiers and ProtocolConstants.MOD_SHIFT) != 0
        0x12 -> (activeModifiers and ProtocolConstants.MOD_ALT) != 0
        else -> false
    }
    private fun getModifierBit(keycode: Int) = when (keycode) {
        0x11 -> ProtocolConstants.MOD_CTRL; 0x10 -> ProtocolConstants.MOD_SHIFT; 0x12 -> ProtocolConstants.MOD_ALT; else -> 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                hitTest(event.x, event.y)?.let { pressedRowIndex = it.first; pressedKeyIndex = it.second; invalidate() }
            }
            MotionEvent.ACTION_UP -> {
                if (pressedRowIndex >= 0 && pressedKeyIndex >= 0) {
                    getCurrentLayout().getOrNull(pressedRowIndex)?.getOrNull(pressedKeyIndex)?.let { handleKeyPress(it) }
                }
                pressedRowIndex = -1; pressedKeyIndex = -1; invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { pressedRowIndex = -1; pressedKeyIndex = -1; invalidate() }
        }
        return true
    }

    private fun handleKeyPress(key: KeyDef) {
        if (key.isModifier) { activeModifiers = activeModifiers xor getModifierBit(key.keycode); invalidate() }
        else { onKeyCommandListener?.invoke(Command.KeyEvent(key.keycode, ProtocolConstants.ACTION_CLICK, activeModifiers)); activeModifiers = 0; invalidate() }
    }

    private fun hitTest(x: Float, y: Float): Pair<Int, Int>? {
        val layout = getCurrentLayout()
        val totalRows = layout.size
        val rowH = (height.toFloat() - keyMargin * (totalRows + 1)) / totalRows
        for ((rowIndex, row) in layout.withIndex()) {
            val totalWeight = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val keyW = (width.toFloat() - keyMargin * (row.size + 1)) / totalWeight
            var kx = keyMargin; val ky = keyMargin + rowIndex * (rowH + keyMargin)
            for ((keyIndex, key) in row.withIndex()) {
                val w = keyW * key.widthWeight
                if (x in kx..(kx + w) && y in ky..(ky + rowH)) return Pair(rowIndex, keyIndex)
                kx += w + keyMargin
            }
        }
        return null
    }
}
