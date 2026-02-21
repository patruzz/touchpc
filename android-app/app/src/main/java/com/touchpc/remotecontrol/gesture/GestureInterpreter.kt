package com.touchpc.remotecontrol.gesture

import android.content.Context
import android.view.MotionEvent
import com.touchpc.remotecontrol.protocol.Command
import com.touchpc.remotecontrol.protocol.ProtocolConstants
import kotlin.math.abs
import kotlin.math.sqrt

class GestureInterpreter(context: Context) {

    enum class State {
        IDLE,
        TRACKING_SINGLE,
        MOVING_CURSOR,
        TAP_DETECTED,
        LONG_PRESS,
        DRAGGING,
        TRACKING_TWO_FINGER,
        SCROLLING,
        PINCHING,
        TRACKING_THREE_FINGER
    }

    // Thresholds (in pixels, converted from dp)
    private val density = context.resources.displayMetrics.density
    private val moveThreshold = 10f * density
    private val scrollThreshold = 5f * density
    private val tapTimeoutMs = 200L
    private val longPressTimeoutMs = 500L
    private val doubleTapTimeoutMs = 300L

    private val tracker = MultiTouchTracker()
    private var state = State.IDLE

    // Cursor movement tracking
    private var lastMoveX = 0f
    private var lastMoveY = 0f

    // Tap detection
    private var lastTapTime = 0L
    private var tapCount = 0

    // Pinch tracking
    private var initialPinchDistance = 0f

    // Three-finger tracking
    private var threeFingerStartX = 0f
    private var threeFingerStartY = 0f

    // Long press detection
    private var longPressStartTime = 0L
    private var longPressDetected = false

    fun onTouchEvent(event: MotionEvent): List<Command> {
        val commands = mutableListOf<Command>()
        val previousPointerCount = tracker.pointerCount
        tracker.processEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event, commands)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event, commands)
            }

            MotionEvent.ACTION_MOVE -> {
                handleMove(event, commands)
            }

            MotionEvent.ACTION_UP -> {
                handleActionUp(event, commands)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                handlePointerUp(event, commands, previousPointerCount)
            }

            MotionEvent.ACTION_CANCEL -> {
                handleCancel(commands)
            }
        }

        return commands
    }

    private fun handleActionDown(event: MotionEvent, commands: MutableList<Command>) {
        lastMoveX = event.x
        lastMoveY = event.y
        longPressStartTime = System.currentTimeMillis()
        longPressDetected = false
        state = State.TRACKING_SINGLE
    }

    private fun handlePointerDown(event: MotionEvent, commands: MutableList<Command>) {
        when (tracker.pointerCount) {
            2 -> {
                if (state == State.DRAGGING) {
                    // End drag if second finger comes down
                    commands.add(Command.DragEnd)
                }
                state = State.TRACKING_TWO_FINGER
                initialPinchDistance = tracker.getDistanceBetweenPointers()
                val p0 = tracker.getPointer(0)
                val p1 = tracker.getPointer(1)
                if (p0 != null && p1 != null) {
                    lastMoveX = (p0.currentX + p1.currentX) / 2f
                    lastMoveY = (p0.currentY + p1.currentY) / 2f
                }
            }

            3 -> {
                state = State.TRACKING_THREE_FINGER
                threeFingerStartX = tracker.getAverageDeltaX()
                threeFingerStartY = tracker.getAverageDeltaY()
                val p0 = tracker.getPointer(0)
                if (p0 != null) {
                    threeFingerStartX = p0.currentX
                    threeFingerStartY = p0.currentY
                }
            }
        }
    }

    private fun handleMove(event: MotionEvent, commands: MutableList<Command>) {
        when (state) {
            State.TRACKING_SINGLE, State.MOVING_CURSOR -> {
                val dx = event.x - lastMoveX
                val dy = event.y - lastMoveY

                if (state == State.TRACKING_SINGLE) {
                    val totalDx = event.x - (tracker.getPointer(0)?.startX ?: event.x)
                    val totalDy = event.y - (tracker.getPointer(0)?.startY ?: event.y)
                    val distance = sqrt(totalDx * totalDx + totalDy * totalDy)

                    if (distance > moveThreshold) {
                        // Check for long press before transitioning to move
                        val elapsed = System.currentTimeMillis() - longPressStartTime
                        if (elapsed >= longPressTimeoutMs && !longPressDetected) {
                            longPressDetected = true
                            state = State.DRAGGING
                            commands.add(Command.DragStart(event.x.toInt(), event.y.toInt()))
                        } else {
                            state = State.MOVING_CURSOR
                        }
                    }
                }

                if (state == State.MOVING_CURSOR) {
                    if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                        commands.add(Command.MouseMoveRel(dx.toInt(), dy.toInt()))
                        lastMoveX = event.x
                        lastMoveY = event.y
                    }
                }

                // Check for long press transition while tracking
                if (state == State.TRACKING_SINGLE) {
                    val elapsed = System.currentTimeMillis() - longPressStartTime
                    if (elapsed >= longPressTimeoutMs && !longPressDetected) {
                        longPressDetected = true
                        state = State.DRAGGING
                        commands.add(Command.DragStart(event.x.toInt(), event.y.toInt()))
                    }
                }
            }

            State.DRAGGING -> {
                val dx = event.x - lastMoveX
                val dy = event.y - lastMoveY
                if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                    commands.add(Command.DragMove(dx.toInt(), dy.toInt()))
                    lastMoveX = event.x
                    lastMoveY = event.y
                }
            }

            State.TRACKING_TWO_FINGER, State.SCROLLING, State.PINCHING -> {
                handleTwoFingerMove(event, commands)
            }

            State.TRACKING_THREE_FINGER -> {
                handleThreeFingerMove(event, commands)
            }

            else -> {}
        }
    }

    private fun handleTwoFingerMove(event: MotionEvent, commands: MutableList<Command>) {
        if (tracker.pointerCount < 2) return

        val p0 = tracker.getPointer(0) ?: return
        val p1 = tracker.getPointer(1) ?: return

        val currentDistance = tracker.getDistanceBetweenPointers()
        val distanceChange = if (initialPinchDistance > 0f) {
            currentDistance / initialPinchDistance
        } else {
            1f
        }

        // Determine if it's a pinch or scroll
        val avgX = (p0.currentX + p1.currentX) / 2f
        val avgY = (p0.currentY + p1.currentY) / 2f
        val scrollDx = avgX - lastMoveX
        val scrollDy = avgY - lastMoveY

        // Check if fingers are converging/diverging (pinch) or moving together (scroll)
        val distanceRatio = abs(distanceChange - 1.0f)

        if (state == State.TRACKING_TWO_FINGER) {
            val scrollDistance = sqrt(scrollDx * scrollDx + scrollDy * scrollDy)
            if (distanceRatio > 0.05f) {
                state = State.PINCHING
            } else if (scrollDistance > scrollThreshold) {
                state = State.SCROLLING
            }
        }

        when (state) {
            State.SCROLLING -> {
                if (abs(scrollDx) > 1f || abs(scrollDy) > 1f) {
                    commands.add(Command.Scroll(scrollDx.toInt(), scrollDy.toInt()))
                    lastMoveX = avgX
                    lastMoveY = avgY
                }
            }

            State.PINCHING -> {
                val centerX = ((p0.currentX + p1.currentX) / 2f).toInt()
                val centerY = ((p0.currentY + p1.currentY) / 2f).toInt()
                commands.add(Command.GesturePinch(distanceChange, centerX, centerY))
                initialPinchDistance = currentDistance
                lastMoveX = avgX
                lastMoveY = avgY
            }

            else -> {
                lastMoveX = avgX
                lastMoveY = avgY
            }
        }
    }

    private fun handleThreeFingerMove(event: MotionEvent, commands: MutableList<Command>) {
        if (tracker.pointerCount < 3) return

        val p0 = tracker.getPointer(0) ?: return
        val avgX = p0.currentX
        val avgY = p0.currentY

        val dx = avgX - threeFingerStartX
        val dy = avgY - threeFingerStartY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > moveThreshold * 2) {
            val direction = if (abs(dx) > abs(dy)) {
                if (dx > 0) ProtocolConstants.SWIPE_RIGHT else ProtocolConstants.SWIPE_LEFT
            } else {
                if (dy > 0) ProtocolConstants.SWIPE_DOWN else ProtocolConstants.SWIPE_UP
            }
            commands.add(Command.GestureThreeFingerSwipe(direction))
            // Reset start position to avoid repeated swipes
            threeFingerStartX = avgX
            threeFingerStartY = avgY
        }
    }

    private fun handleActionUp(event: MotionEvent, commands: MutableList<Command>) {
        when (state) {
            State.TRACKING_SINGLE -> {
                val elapsed = System.currentTimeMillis() - longPressStartTime
                if (elapsed < tapTimeoutMs) {
                    // Check for double-tap
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < doubleTapTimeoutMs) {
                        tapCount++
                        if (tapCount >= 2) {
                            // Double tap = double click
                            commands.add(
                                Command.MouseButton(
                                    ProtocolConstants.MOUSE_BUTTON_LEFT,
                                    ProtocolConstants.ACTION_CLICK
                                )
                            )
                            commands.add(
                                Command.MouseButton(
                                    ProtocolConstants.MOUSE_BUTTON_LEFT,
                                    ProtocolConstants.ACTION_CLICK
                                )
                            )
                            tapCount = 0
                        }
                    } else {
                        // Single tap = left click
                        tapCount = 1
                        commands.add(
                            Command.MouseButton(
                                ProtocolConstants.MOUSE_BUTTON_LEFT,
                                ProtocolConstants.ACTION_CLICK
                            )
                        )
                    }
                    lastTapTime = now
                } else if (longPressDetected) {
                    // Long press was handled, just ignore tap
                }
            }

            State.DRAGGING -> {
                commands.add(Command.DragEnd)
            }

            State.LONG_PRESS -> {
                // Long press up
            }

            else -> {}
        }

        state = State.IDLE
        longPressDetected = false
    }

    private fun handlePointerUp(
        event: MotionEvent,
        commands: MutableList<Command>,
        previousPointerCount: Int
    ) {
        when {
            // Two-finger tap = right click (both fingers lifted quickly)
            previousPointerCount == 2 && state == State.TRACKING_TWO_FINGER -> {
                val elapsed = tracker.getElapsedTime(0)
                if (elapsed < tapTimeoutMs) {
                    commands.add(
                        Command.MouseButton(
                            ProtocolConstants.MOUSE_BUTTON_RIGHT,
                            ProtocolConstants.ACTION_CLICK
                        )
                    )
                }
                state = State.IDLE
            }

            // Three-finger tap = middle click
            previousPointerCount == 3 && state == State.TRACKING_THREE_FINGER -> {
                val p0 = tracker.getPointer(0)
                val elapsed = p0?.elapsedTime ?: 0L
                if (elapsed < tapTimeoutMs) {
                    commands.add(
                        Command.MouseButton(
                            ProtocolConstants.MOUSE_BUTTON_MIDDLE,
                            ProtocolConstants.ACTION_CLICK
                        )
                    )
                }
                state = State.IDLE
            }

            // Returning from scroll/pinch to single finger
            tracker.pointerCount == 1 -> {
                if (state == State.SCROLLING || state == State.PINCHING) {
                    state = State.IDLE
                }
                val p = tracker.getPointer(0)
                if (p != null) {
                    lastMoveX = p.currentX
                    lastMoveY = p.currentY
                }
            }
        }
    }

    private fun handleCancel(commands: MutableList<Command>) {
        if (state == State.DRAGGING) {
            commands.add(Command.DragEnd)
        }
        state = State.IDLE
        longPressDetected = false
    }

    fun reset() {
        state = State.IDLE
        tracker.reset()
        longPressDetected = false
        tapCount = 0
    }
}
