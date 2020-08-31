package com.antonionicolaspina.textimageview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.graphics.contains
import androidx.core.graphics.toPoint
import androidx.core.graphics.withMatrix
import com.mapbox.android.gestures.*
import kotlin.math.max
import kotlin.math.min


class TextImageView
@JvmOverloads constructor(
  context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0
): ImageView(context, attributeSet, defStyleAttr) {
  interface Listener {
    fun textsChanged(texts: List<Text>)
  }

  var panEnabled = false
  var scaleEnabled = false
  var rotationEnabled = false
  var listener: Listener? = null
  var initialTextSize = 0f
  var minTextSize = 0f
  var maxTextSize = 0f

  private val texts = mutableListOf<TextProperties>()
  private var selectedText: TextProperties? = null

  init {
    context.theme.obtainStyledAttributes(attributeSet, R.styleable.TextImageView, 0, 0).apply {
      try {
        panEnabled = getBoolean(R.styleable.TextImageView_tiv_panEnabled, false)
        scaleEnabled = getBoolean(R.styleable.TextImageView_tiv_scaleEnabled, false)
        rotationEnabled = getBoolean(R.styleable.TextImageView_tiv_rotationEnabled, false)

        initialTextSize = getDimensionPixelSize(
          R.styleable.TextImageView_tiv_initialTextSize, resources.getDimensionPixelSize(
            R.dimen.default_text_size
          )
        ).toFloat()
        minTextSize = getDimensionPixelSize(
          R.styleable.TextImageView_tiv_minTextSize, resources.getDimensionPixelSize(
            R.dimen.default_min_text_size
          )
        ).toFloat()
        maxTextSize = getDimensionPixelSize(
          R.styleable.TextImageView_tiv_maxTextSize, resources.getDimensionPixelSize(
            R.dimen.default_max_text_size
          )
        ).toFloat()
      } finally {
        recycle()
      }
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (isInEditMode && texts.isEmpty()) {
      setText("sample text")
    }

    texts.forEach { tp ->
      canvas.withMatrix(tp.matrix) {
        canvas.drawText(tp.text, 0f, 0f, tp.paint)
      }
    }
  }

  /**
   * Set text to be drawn over the image.
   * @param text The text.
   */
  fun setText(text: String) {
    texts.clear()
    addText(text)
  }

  /**
   * Set the typeface to use for the text.
   * @param typeface The typeface to be used.
   */
  fun setTypeface(typeface: Typeface?) {
    selectedText?.let {
      it.paint.typeface = typeface
      textChanged(it)
    }
  }

  /**
   * Set the text color.
   * @param color Color in the format of <a href="http://developer.android.com/reference/android/graphics/Color.html">android.graphics.Color</a>.
   *
   * @see <a href="http://developer.android.com/reference/android/graphics/Color.html">android.graphics.Color</a>
   */
  fun setTextColor(@ColorInt color: Int) {
    selectedText?.let {
      it.paint.color = color
      textChanged(it)
    }
  }

  /**
   * Adds a text to be drawn over the image, above existing texts.
   * @param text The text.
   */
  fun addText(text: String) {
    val tp = TextProperties(text)
    tp.paint.textSize = initialTextSize
    texts.add(tp)
    selectedText = tp
    textChanged(tp)
  }

  //region Gestures
  private val androidGesturesManager = AndroidGesturesManager(context).apply {
    setMoveGestureListener(object : MoveGestureDetector.SimpleOnMoveGestureListener() {
      override fun onMove(
        detector: MoveGestureDetector,
        distanceX: Float,
        distanceY: Float
      ): Boolean {
        if (panEnabled) {
          selectedText?.let {
            it.position.x -= distanceX
            it.position.y -= distanceY
            textChanged(it)
          }
        }
        return true
      }
    })

    setStandardScaleGestureListener(object :
      StandardScaleGestureDetector.SimpleStandardOnScaleGestureListener() {
      override fun onScale(detector: StandardScaleGestureDetector): Boolean {
        if (scaleEnabled) {
          selectedText?.let {
            val s = it.scaleFactor * detector.scaleFactor
            val textSize = s * it.paint.textSize
            it.scaleFactor = max(minTextSize, min(textSize, maxTextSize)) / it.paint.textSize
            textChanged(it)
          }
        }
        return true
      }
    })

    setRotateGestureListener(object : RotateGestureDetector.SimpleOnRotateGestureListener() {
      override fun onRotate(
        detector: RotateGestureDetector,
        rotationDegreesSinceLast: Float,
        rotationDegreesSinceFirst: Float
      ): Boolean {
        if (rotationEnabled) {
          selectedText?.let {
            it.rotationDegress -= rotationDegreesSinceLast
            textChanged(it)
          }
        }
        return true
      }
    })

    setStandardGestureListener(object : StandardGestureDetector.SimpleStandardOnGestureListener() {
      override fun onDown(e: MotionEvent): Boolean {
        texts.forEach { tp ->
          val p = tp.inverseMap(e.x, e.y)

          if (tp.boundingRect.contains(p.toPoint())) {
            selectedText = tp
          }
        }
        invalidate()
        return true
      }
    })
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    androidGesturesManager.onTouchEvent(event)
    return true
  }
  //endregion

  private fun textChanged(tp: TextProperties) {
    invalidate()

    tp.paint.getTextBounds(tp.text, 0, tp.text.length, tp.boundingRect)

    with(tp.matrix) {
      reset()
      setScale(tp.scaleFactor, tp.scaleFactor)
      preTranslate(tp.position.x / tp.scaleFactor, tp.position.y / tp.scaleFactor)
      preRotate(
        tp.rotationDegress,
        tp.boundingRect.exactCenterX() / tp.scaleFactor,
        tp.boundingRect.exactCenterY() / tp.scaleFactor
      )
    }

    val w = measuredWidth
    val h = measuredHeight
    listener?.textsChanged(texts.map { it.toText(w, h) })
  }
}
