/**
 * Copyright 2013 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.curiouscreature.android.roadtrip

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

import java.util.ArrayList

SuppressWarnings("ForLoopReplaceableByForEach")
public class StateView(context: Context, attrs: AttributeSet, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mSvg = SvgHelper(mPaint)
    public var svgResource: Int = 0

    private val mSvgLock = Object()
    private var mPaths: List<SvgHelper.SvgPath> = ArrayList(0)
    private var mLoader: Thread? = null

    public var phase: Float = 0.toFloat()
        set(phase) {
            $phase = phase
            synchronized (mSvgLock) {
                updatePathsPhaseLocked()
            }
            invalidate()
        }
    private var mFadeFactor: Float = 0.toFloat()
    private var mDuration: Int = 0
    public var parallax: Float = 1.0.toFloat()
        set(parallax) {
            $parallax = parallax
            invalidate()
        }
    private var mOffsetY: Float = 0.toFloat()

    private var mSvgAnimator: ObjectAnimator? = null

    {

        mPaint.setStyle(Paint.Style.STROKE)

        val a = context.obtainStyledAttributes(attrs, R.styleable.IntroView, defStyle, 0)
        try {
            if (a != null) {
                mPaint.setStrokeWidth(a.getFloat(R.styleable.StateView_strokeWidth, 1.0.toFloat()))
                mPaint.setColor(a.getColor(R.styleable.StateView_strokeColor, -16777216))
                phase = a.getFloat(R.styleable.StateView_phase, 1.0.toFloat())
                mDuration = a.getInt(R.styleable.StateView_duration, 4000)
                mFadeFactor = a.getFloat(R.styleable.StateView_fadeFactor, 10.0.toFloat())
            }
        } finally {
            if (a != null) a.recycle()
        }
    }

    private fun updatePathsPhaseLocked() {
        val count = mPaths.size()
        for (i in 0..count - 1) {
            val svgPath = mPaths.get(i)
            svgPath.renderPath.reset()
            svgPath.measure.getSegment(0.0.toFloat(), svgPath.length * phase, svgPath.renderPath, true)
            // Required only for Android 4.4 and earlier
            svgPath.renderPath.rLineTo(0.0.toFloat(), 0.0.toFloat())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (mLoader != null) {
            try {
                mLoader!!.join()
            } catch (e: InterruptedException) {
                Log.e(LOG_TAG, "Unexpected error", e)
            }

        }

        mLoader = Thread(object : Runnable {
            override fun run() {
                mSvg.load(getContext(), svgResource)
                synchronized (mSvgLock) {
                    mPaths = mSvg.getPathsForViewport(w - getPaddingLeft() - getPaddingRight(), h - getPaddingTop() - getPaddingBottom())
                    updatePathsPhaseLocked()
                }
            }
        }, "SVG Loader")
        mLoader!!.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized (mSvgLock) {
            canvas.save()
            canvas.translate(getPaddingLeft().toFloat(), getPaddingTop().toFloat() + mOffsetY)
            val count = mPaths.size()
            for (i in 0..count - 1) {
                val svgPath = mPaths.get(i)

                // We use the fade factor to speed up the alpha animation
                val alpha = (Math.min(phase * mFadeFactor, 1.0.toFloat()) * 255.0.toFloat()).toInt()
                svgPath.paint.setAlpha((alpha.toFloat() * parallax).toInt())

                canvas.drawPath(svgPath.renderPath, svgPath.paint)
            }
            canvas.restore()
        }
    }

    public fun reveal(scroller: View, parentBottom: Int) {
        if (mSvgAnimator == null) {
            mSvgAnimator = ObjectAnimator.ofFloat(this, "phase", 0.0.toFloat(), 1.0.toFloat())
            mSvgAnimator!!.setDuration(mDuration.toLong())
            mSvgAnimator!!.start()
        }

        val previousOffset = mOffsetY
        mOffsetY = Math.min(0, scroller.getHeight() - (parentBottom - scroller.getScrollY())).toFloat()
        if (previousOffset != mOffsetY) invalidate()
    }

    class object {
        private val LOG_TAG = "StateView"
    }
}
