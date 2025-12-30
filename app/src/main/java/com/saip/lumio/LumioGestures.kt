package com.saip.lumio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class LumioGestures(context: Context, private val listener: GestureListener) : View.OnTouchListener {

    interface GestureListener {
        fun onDoubleTap()        // Text (1-finger double)
        fun onTripleTap()        // Scene (1-finger triple)
        fun onTwoFingerSwipe()   // Object (2-finger swipe)
        fun onTwoFingerTap()     // Person (2-finger tap) OR Voice Input (in Add Mode)
        fun onLongPress()        // Touch Reader
    }

    private val handler = Handler(Looper.getMainLooper())

    // TAP STATE
    private var tapCount = 0
    private var lastTapTime = 0L
    private val TAP_DELAY = 400L // Time window to count taps

    // GESTURE STATE
    private var isTwoFingerMode = false
    private var startX = 0f
    private var startY = 0f
    private var hasSwiped = false
    private var isLongPress = false
    private val LONG_PRESS_TIMEOUT = 800L

    private val longPressRunnable = Runnable {
        if (!isTwoFingerMode) {
            isLongPress = true
            listener.onLongPress()
        }
    }

    // Runnable to decide what to do after tapping stops
    private val tapRunnable = Runnable {
        if (!isTwoFingerMode && !isLongPress) {
            when (tapCount) {
                2 -> listener.onDoubleTap()
                3 -> listener.onTripleTap()
            }
        }
        tapCount = 0
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Reset for new interaction
                isTwoFingerMode = false
                hasSwiped = false
                isLongPress = false

                tapCount++
                handler.removeCallbacks(tapRunnable) // Cancel pending decision
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT) // Start Long Press timer
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isTwoFingerMode = true
                    hasSwiped = false
                    startX = event.getX(0)
                    startY = event.getY(0)

                    // Cancel single finger logic
                    handler.removeCallbacks(tapRunnable)
                    handler.removeCallbacks(longPressRunnable)
                    tapCount = 0
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTwoFingerMode && event.pointerCount >= 2 && !hasSwiped) {
                    val dx = event.getX(0) - startX
                    val dy = event.getY(0) - startY
                    // Swipe Threshold
                    if (abs(dx) > 150 || abs(dy) > 150) {
                        hasSwiped = true
                        listener.onTwoFingerSwipe()
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // If 2 fingers were down, didn't swipe, and one lifts -> It's a 2-Finger Tap
                if (isTwoFingerMode && !hasSwiped && event.pointerCount == 2) {
                    listener.onTwoFingerTap()
                    hasSwiped = true // Lock it so lifting the 2nd finger doesn't trigger again
                }
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (!isTwoFingerMode && !isLongPress) {
                    // Schedule the decision (Double or Triple?)
                    handler.postDelayed(tapRunnable, TAP_DELAY)
                }
            }
        }
        return true
    }
}