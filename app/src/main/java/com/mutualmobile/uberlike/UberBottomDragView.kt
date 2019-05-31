package com.mutualmobile.uberlike

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.customview.widget.ViewDragHelper.STATE_DRAGGING
import androidx.customview.widget.ViewDragHelper.STATE_IDLE
import kotlinx.android.synthetic.main.activity_maps.view.*

class UberBottomDragView : LinearLayout {
  private val AUTO_OPEN_SPEED_LIMIT = 800.0
  private var draggingBorder: Int = 0
  private var verticalRange: Int = 0
  private lateinit var dragHelper: ViewDragHelper
  private var draggingState = 0
  private var isOpen = false

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
      defStyleAttr)

  override fun onFinishInflate() {
    dragHelper = ViewDragHelper.create(this, 1.0F, DragHelperCallback())
    isOpen = false
    super.onFinishInflate()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    verticalRange = h.times(0.6F).toInt()
    super.onSizeChanged(w, h, oldw, oldh)
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    ev?.let {
      return dragHelper.shouldInterceptTouchEvent(ev)
    } ?: kotlin.run {
      return false
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    event?.let {
      return when {
        isMoving() -> {
          dragHelper.processTouchEvent(it)
          true
        }
        else -> super.onTouchEvent(event)
      }
    } ?: kotlin.run {
      return super.onTouchEvent(event)
    }
  }

  override fun computeScroll() {
    if (dragHelper.continueSettling(true)) {
      ViewCompat.postInvalidateOnAnimation(this)
    }
  }

  fun isMoving(): Boolean {
    return draggingState == STATE_DRAGGING || draggingState == ViewDragHelper.STATE_SETTLING
  }

  private fun onStopDraggingToClosed() {
    // To be implemented
  }

  inner class DragHelperCallback : ViewDragHelper.Callback() {
    override fun tryCaptureView(child: View, pointerId: Int): Boolean {
      return child.id == R.id.mainLayout
    }

    override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
      super.onViewPositionChanged(changedView, left, top, dx, dy)
      draggingBorder = top
    }

    override fun getViewVerticalDragRange(child: View): Int {
      return verticalRange
    }

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
      val topBound = paddingTop
      val bottomBound = verticalRange
      return Math.min(Math.max(top, topBound), bottomBound)
    }

    override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
      Log.e("draggingBorder $draggingBorder", draggingBorder.toString())
      val margin = resources.getDimension(R.dimen.size_16dp)

      if (draggingBorder == 0) {
        isOpen = false
        applyMargin(0)
        return
      }

      if (draggingBorder == verticalRange) {
        isOpen = true
        return
      }

      val rangeToCheck = verticalRange
      var settleToOpen = false
      when {
        yvel > AUTO_OPEN_SPEED_LIMIT -> settleToOpen = true
        yvel < -AUTO_OPEN_SPEED_LIMIT -> settleToOpen = false
        draggingBorder > rangeToCheck / 2 -> settleToOpen = true
        draggingBorder < rangeToCheck / 2 -> settleToOpen = false
      }

      val settleDestY = if (settleToOpen) verticalRange else 0
      val settleDestX = if (settleDestY < verticalRange) 0 else margin.toInt()

      if (dragHelper.smoothSlideViewTo(mainLayout,
              0, settleDestY)) {
        ViewCompat.postInvalidateOnAnimation(this@UberBottomDragView)
        applyMargin(settleDestX)
      }
    }

    private fun applyMargin(settleDestX: Int) {
      val anim = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
          super.applyTransformation(interpolatedTime, t)
          val params = mainContent.layoutParams as FrameLayout.LayoutParams
          params.leftMargin = settleDestX
          params.rightMargin = settleDestX
          mainContent.layoutParams = params
          mainLayout.smoothScrollTo(0,0)
        }
      }
      mainContent.startAnimation(anim)
    }

    override fun onViewDragStateChanged(state: Int) {
      if (state == draggingState) {
        return
      }

      if (viewStoppedFromMoving(state)) {
        when (draggingBorder) {
          0 -> onStopDraggingToClosed()
          verticalRange -> isOpen = true
        }
      }

      if (state == STATE_DRAGGING) {
        onStartDragging()
      }

      draggingState = state
      super.onViewDragStateChanged(state)
    }

    private fun viewStoppedFromMoving(state: Int): Boolean {
      return ((draggingState == STATE_DRAGGING || draggingState == ViewDragHelper.STATE_SETTLING) && state == STATE_IDLE)
    }

  }

  private fun onStartDragging() {

  }

  fun smoothSlideToBottom() {
    verticalRange = resources.displayMetrics.heightPixels.times(0.6F).toInt()
    if (dragHelper.smoothSlideViewTo(this@UberBottomDragView,
            0, verticalRange)) {
      ViewCompat.postInvalidateOnAnimation(this@UberBottomDragView)
    }
  }

}