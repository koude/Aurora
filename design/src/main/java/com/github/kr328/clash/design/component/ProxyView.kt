package com.github.kr328.clash.design.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import com.github.kr328.clash.common.compat.getDrawableCompat
import com.github.kr328.clash.design.store.UiStore

class ProxyView(
    context: Context,
    config: ProxyViewConfig,
) : View(context) {

    init {
        background = context.getDrawableCompat(config.clickableBackground)
    }

    var state: ProxyViewState? = null
    constructor(context: Context) : this(context, ProxyViewConfig(context, 2))
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val state = state ?: return super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.UNSPECIFIED ->
                resources.displayMetrics.widthPixels
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY ->
                MeasureSpec.getSize(widthMeasureSpec)
            else ->
                throw IllegalArgumentException("invalid measure spec")
        }

        state.paint.apply {
            reset()
            textSize = state.config.titleTextSize
        }

        val titleMetrics = state.paint.fontMetrics
        val titleHeight = titleMetrics.descent - titleMetrics.ascent
        state.paint.textSize = state.config.subtitleTextSize
        val subtitleMetrics = state.paint.fontMetrics
        val subtitleHeight = subtitleMetrics.descent - subtitleMetrics.ascent
        val expectedHeight = (state.config.layoutPadding * 2 +
                state.config.contentPadding * 2 +
                titleHeight + subtitleHeight +
                state.config.textMargin)
            .coerceAtLeast(state.config.cardMinHeight)
            .toInt()

        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.UNSPECIFIED ->
                expectedHeight
            MeasureSpec.AT_MOST ->
                expectedHeight.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
            MeasureSpec.EXACTLY ->
                MeasureSpec.getSize(heightMeasureSpec)
            else ->
                throw IllegalArgumentException("invalid measure spec")
        }

        setMeasuredDimension(width, height)
    }

    override fun draw(canvas: Canvas) {
        val state = state ?: return super.draw(canvas)

        if (state.update(false))
            postInvalidate()

        val width = width.toFloat()
        val height = height.toFloat()

        val paint = state.paint

        paint.reset()

        paint.apply {
            isAntiAlias = true
            color = state.background
            style = Paint.Style.FILL
        }

        // draw background
        canvas.apply {
            val path = state.path

            path.reset()

            path.addRoundRect(
                state.config.layoutPadding,
                state.config.layoutPadding,
                width - state.config.layoutPadding,
                height - state.config.layoutPadding,
                state.config.cardRadius,
                state.config.cardRadius,
                Path.Direction.CW,
            )

            paint.setShadowLayer(
                state.config.cardRadius,
                state.config.cardOffset,
                state.config.cardOffset,
                state.config.shadow
            )

            drawPath(path, paint)

            paint.apply {
                clearShadowLayer()
                color = if (state.selected) {
                    state.config.selectedOutline
                } else {
                    state.config.unselectedOutline
                }
                style = Paint.Style.STROKE
                strokeWidth = state.config.cardStroke
            }
            drawPath(path, paint)

            clipPath(path)
        }

        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val state = state ?: return

        val paint = state.paint

        val width = width.toFloat()
        val height = height.toFloat()

        paint.apply {
            reset()
            isAntiAlias = true
            textSize = state.config.delayTextSize
            typeface = Typeface.DEFAULT_BOLD
        }

        val delayTextWidth = paint.measureText(state.delayText)
        val delayMetrics = paint.fontMetrics
        val delayTextHeight = delayMetrics.descent - delayMetrics.ascent
        val delayPillWidth = if (state.delayText.isEmpty()) 0f else {
            delayTextWidth + state.config.delayPaddingHorizontal * 2
        }
        val delayPillHeight = delayTextHeight + state.config.delayPaddingVertical * 2
        val contentLeft = state.config.layoutPadding + state.config.contentPadding
        val contentRight = width - state.config.layoutPadding - state.config.contentPadding
        val delayBlockWidth = if (state.delayText.isEmpty()) 0f else {
            delayPillWidth + state.config.textMargin
        }

        val mainTextWidth = (contentRight - contentLeft - delayBlockWidth)
            .coerceAtLeast(0f)

        paint.textSize = state.config.titleTextSize
        val titleCount = paint.breakText(
            state.title,
            false,
            mainTextWidth,
            null,
        )

        paint.textSize = state.config.subtitleTextSize
        val subtitleCount = paint.breakText(
            state.subtitle,
            false,
            mainTextWidth,
            null,
        )

        if (state.selected) {
            paint.apply {
                color = state.config.selectedOutline
                style = Paint.Style.FILL
            }
            val indicatorLeft = state.config.layoutPadding + state.config.cardStroke * 2
            val indicatorTop = height * 0.27f
            canvas.drawRoundRect(
                indicatorLeft,
                indicatorTop,
                indicatorLeft + state.config.selectedIndicatorWidth,
                height - indicatorTop,
                state.config.selectedIndicatorWidth / 2,
                state.config.selectedIndicatorWidth / 2,
                paint,
            )
        }

        if (state.delayText.isNotEmpty()) {
            val delayColor = when (state.delay) {
                in 1..399 -> state.config.delayGood
                in 400..799 -> state.config.delayMedium
                else -> state.config.delayBad
            }
            val pillLeft = contentRight - delayPillWidth
            val pillTop = (height - delayPillHeight) / 2f

            paint.apply {
                color = Color.argb(
                    if (state.selected) 48 else 28,
                    Color.red(delayColor),
                    Color.green(delayColor),
                    Color.blue(delayColor),
                )
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                pillLeft,
                pillTop,
                contentRight,
                pillTop + delayPillHeight,
                delayPillHeight / 2,
                delayPillHeight / 2,
                paint,
            )

            paint.apply {
                color = delayColor
                textSize = state.config.delayTextSize
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(
                state.delayText,
                pillLeft + state.config.delayPaddingHorizontal,
                height / 2f - (delayMetrics.ascent + delayMetrics.descent) / 2f,
                paint,
            )
        }

        paint.apply {
            textSize = state.config.titleTextSize
            typeface = Typeface.DEFAULT_BOLD
        }
        val titleMetrics = paint.fontMetrics
        val titleHeight = titleMetrics.descent - titleMetrics.ascent
        paint.textSize = state.config.subtitleTextSize
        val subtitleMetrics = paint.fontMetrics
        val subtitleHeight = subtitleMetrics.descent - subtitleMetrics.ascent
        val textHeight = titleHeight + state.config.textMargin + subtitleHeight
        val textTop = (height - textHeight) / 2f

        paint.apply {
            color = state.controls
            textSize = state.config.titleTextSize
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.apply {
            val y = textTop - titleMetrics.ascent

            drawText(state.title, 0, titleCount, contentLeft, y, paint)
        }

        paint.apply {
            color = if (state.selected) {
                state.config.selectedControl
            } else {
                state.config.unselectedSubtitle
            }
            textSize = state.config.subtitleTextSize
            typeface = Typeface.DEFAULT
        }
        canvas.apply {
            val y = textTop + titleHeight + state.config.textMargin - subtitleMetrics.ascent

            drawText(state.subtitle, 0, subtitleCount, contentLeft, y, paint)
        }
    }
}
