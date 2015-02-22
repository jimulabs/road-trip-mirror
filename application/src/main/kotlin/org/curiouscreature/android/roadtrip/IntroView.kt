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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator

import java.util.ArrayList
import android.animation.ValueAnimator

SuppressWarnings("ForLoopReplaceableByForEach", "UnusedDeclaration")
public class IntroView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mSvg = SvgHelper(mPaint)
    private var mSvgResource: Int = 0

    private val mSvgLock = Object()
    private var mPaths: List<SvgHelper.SvgPath> = ArrayList(0)
    private var mLoader: Thread? = null

    private var mWaitPath: SvgHelper.SvgPath? = null
    private var mDragPath: SvgHelper.SvgPath? = null
    private var mArrowPaint: Paint? = null
    private var mArrowLength: Int = 0
    private var mArrowHeight: Int = 0

    public var phase: Float = 0.toFloat()
        set(phase) {
            $phase = phase
            synchronized (mSvgLock) {
                updatePathsPhaseLocked()
            }
            invalidate()
        }
    public var wait: Float = 0.toFloat()
        set(wait) {
            $wait = wait
            mWaitPath!!.paint.setPathEffect(createConcaveArrowPathEffect(mWaitPath!!.length, this.wait, 32.0f))
            invalidate()
        }
    public var drag: Float = 0.toFloat()
        set(drag) {
            $drag = drag

            mDragPath!!.paint.setPathEffect(createPathEffect(mDragPath!!.length, this.drag, mArrowLength.toFloat()))
            mArrowPaint!!.setPathEffect(createArrowPathEffect(mDragPath!!.length, this.drag, mArrowLength.toFloat()))

            val alpha = (Math.min((1.0f - this.drag) * mFadeFactor, 1.0f) * 255.0f).toInt()
            mDragPath!!.paint.setAlpha(alpha)
            mArrowPaint!!.setAlpha(alpha)
            invalidate()
        }

    private var mDuration: Int = 0
    private var mFadeFactor: Float = 0.toFloat()

    private var mRadius: Int = 0

    private var mSvgAnimator: ObjectAnimator? = null
    private var mWaitAnimator: ObjectAnimator? = null

    private var mListener: OnReadyListener? = null

    public trait OnReadyListener {
        public fun onReady()
    }

    {

        val a = context.obtainStyledAttributes(attrs, R.styleable.IntroView, 0, 0)
        try {
            if (a != null) {
                mPaint.setStrokeWidth(a.getFloat(R.styleable.IntroView_strokeWidth, 1.0f))
                mPaint.setColor(a.getColor(R.styleable.IntroView_strokeColor, -16777216))
                phase = a.getFloat(R.styleable.IntroView_phase, 1.0f)
                mDuration = a.getInt(R.styleable.IntroView_duration, 4000)
                mFadeFactor = a.getFloat(R.styleable.IntroView_fadeFactor, 10.0f)
                mRadius = a.getDimensionPixelSize(R.styleable.IntroView_waitRadius, 50)
                mArrowLength = a.getDimensionPixelSize(R.styleable.IntroView_arrowLength, 32)
                mArrowHeight = a.getDimensionPixelSize(R.styleable.IntroView_arrowHeight, 18)
            }
        } finally {
            if (a != null) a.recycle()
        }

        init()
    }

    private fun init() {
        mPaint.setStyle(Paint.Style.STROKE)

        createWaitPath()

        // Note: using a software layer here is an optimization. This view works with
        // hardware accelerated rendering but every time a path is modified (when the
        // dash path effect is modified), the graphics pipeline will rasterize the path
        // again in a new texture. Since we are dealing with dozens of paths, it is much
        // more efficient to rasterize the entire view into a single re-usable texture
        // instead. Ideally this should be toggled using a heuristic based on the number
        // and or dimensions of paths to render.
        // Note that PathDashPathEffects can lead to clipping issues with hardware rendering.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        mSvgAnimator = ObjectAnimator.ofFloat(this, "phase", 0.0f, 1.0f).setDuration(mDuration.toLong())

        mWaitAnimator = ObjectAnimator.ofFloat(this, "wait", 1.0f, 0.0f).setDuration(mDuration.toLong())
        mWaitAnimator!!.setRepeatMode(ValueAnimator.RESTART)
        mWaitAnimator!!.setRepeatCount(ValueAnimator.INFINITE)
        mWaitAnimator!!.setInterpolator(LinearInterpolator())
        mWaitAnimator!!.start()
    }

    private fun createWaitPath() {
        var paint = Paint(mPaint)
        paint.setStrokeWidth(paint.getStrokeWidth() * 4.0f)

        val p = Path()
        p.moveTo(0.0f, 0.0f)
        p.lineTo(mRadius.toFloat() * 6.0f, 0.0f)

        mWaitPath = SvgHelper.SvgPath(p, paint)
        mArrowPaint = Paint(mWaitPath!!.paint)

        paint = Paint(mWaitPath!!.paint)
        mDragPath = SvgHelper.SvgPath(makeDragPath(mRadius), paint)
    }

    public fun setSvgResource(resource: Int) {
        if (mSvgResource == 0) {
            mSvgResource = resource
        }
    }

    public fun stopWaitAnimation() {
        val alpha = ObjectAnimator.ofInt(mWaitPath!!.paint, "alpha", 0)
        alpha.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mWaitAnimator!!.cancel()
                ObjectAnimator.ofFloat(this@IntroView, "drag", 1.0f, 0.0f).setDuration((mDuration / 3).toLong()).start()
            }
        })
        alpha.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized (mSvgLock) {
            canvas.save()
            canvas.translate(getPaddingLeft().toFloat(), (getPaddingTop() - getPaddingBottom()).toFloat())
            val count = mPaths.size()
            for (i in 0..count - 1) {
                val svgPath = mPaths.get(i)

                // We use the fade factor to speed up the alpha animation
                val alpha = (Math.min(phase * mFadeFactor, 1.0f) * 255.0f).toInt()
                svgPath.paint.setAlpha(alpha)

                canvas.drawPath(svgPath.renderPath, svgPath.paint)
            }
            canvas.restore()
        }

        canvas.save()
        canvas.translate(0.0f, getHeight().toFloat() - getPaddingBottom().toFloat() - mRadius.toFloat() * 3.0f)
        if (mWaitPath!!.paint.getAlpha() > 0) {
            canvas.translate(getWidth().toFloat() / 2.0f - mRadius.toFloat() * 3.0f, mRadius.toFloat())
            canvas.drawPath(mWaitPath!!.path, mWaitPath!!.paint)
        } else {
            canvas.translate((getWidth() - mDragPath!!.bounds.width()).toFloat() / 2.0f, 0.0f)
            canvas.drawPath(mDragPath!!.path, mDragPath!!.paint)
            canvas.drawPath(mDragPath!!.path, mArrowPaint)
        }
        canvas.restore()
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
                mSvg.load(getContext(), mSvgResource)
                synchronized (mSvgLock) {
                    mPaths = mSvg.getPathsForViewport(w - getPaddingLeft() - getPaddingRight(), h - getPaddingTop() - getPaddingBottom())
                    updatePathsPhaseLocked()
                }
                post(object : Runnable {
                    override fun run() {
                        invokeReadyListener()
                        if (mSvgAnimator!!.isRunning()) mSvgAnimator!!.cancel()
                        mSvgAnimator!!.start()
                    }
                })
            }
        }, "SVG Loader")
        mLoader!!.start()
    }

    private fun invokeReadyListener() {
        if (mListener != null) mListener!!.onReady()
    }

    public fun setOnReadyListener(listener: OnReadyListener) {
        mListener = listener
    }

    private fun updatePathsPhaseLocked() {
        val count = mPaths.size()
        for (i in 0..count - 1) {
            val svgPath = mPaths.get(i)
            svgPath.renderPath.reset()
            svgPath.measure.getSegment(0.0f, svgPath.length * phase, svgPath.renderPath, true)
            // Required only for Android 4.4 and earlier
            svgPath.renderPath.rLineTo(0.0f, 0.0f)
        }
    }

    private fun createArrowPathEffect(pathLength: Float, phase: Float, offset: Float): PathEffect {
        return PathDashPathEffect(makeArrow(mArrowLength.toFloat(), mArrowHeight.toFloat()), pathLength, Math.max(phase * pathLength, offset), PathDashPathEffect.Style.ROTATE)
    }

    private fun createConcaveArrowPathEffect(pathLength: Float, phase: Float, offset: Float): PathEffect {
        return PathDashPathEffect(makeConcaveArrow(mArrowLength.toFloat(), mArrowHeight.toFloat()), mArrowLength.toFloat() * 1.2f, Math.max(phase * pathLength, offset), PathDashPathEffect.Style.ROTATE)
    }

    class object {
        private val LOG_TAG = "IntroView"

        private fun createPathEffect(pathLength: Float, phase: Float, offset: Float): PathEffect {
            return DashPathEffect(floatArray(pathLength.toFloat(), pathLength.toFloat()), Math.max(phase * pathLength, offset))
        }

        private fun makeDragPath(radius: Int): Path {
            val p = Path()
            val oval = RectF(0.0f, 0.0f, radius.toFloat() * 2.0f, radius.toFloat() * 2.0f)

            val cx = oval.centerX()
            val cy = oval.centerY()
            val rx = oval.width() / 2.0f
            val ry = oval.height() / 2.0f

            val TAN_PI_OVER_8 = 0.414213562f
            val ROOT_2_OVER_2 = 0.707106781f

            val sx = rx * TAN_PI_OVER_8
            val sy = ry * TAN_PI_OVER_8
            val mx = rx * ROOT_2_OVER_2
            val my = ry * ROOT_2_OVER_2

            val L = oval.left
            val T = oval.top
            val R = oval.right
            val B = oval.bottom

            p.moveTo(R, cy)
            p.quadTo(R, cy + sy, cx + mx, cy + my)
            p.quadTo(cx + sx, B, cx, B)
            p.quadTo(cx - sx, B, cx - mx, cy + my)
            p.quadTo(L, cy + sy, L, cy)
            p.quadTo(L, cy - sy, cx - mx, cy - my)
            p.quadTo(cx - sx, T, cx, T)
            p.lineTo(cx, T - oval.height() * 1.3f)

            return p
        }

        private fun makeArrow(length: Float, height: Float): Path {
            val p = Path()
            p.moveTo(-2.0f, -height / 2.0f)
            p.lineTo(length, 0.0f)
            p.lineTo(-2.0f, height / 2.0f)
            p.close()
            return p
        }

        private fun makeConcaveArrow(length: Float, height: Float): Path {
            val p = Path()
            p.moveTo(-2.0f, -height / 2.0f)
            p.lineTo(length - height / 4.0f, -height / 2.0f)
            p.lineTo(length, 0.0f)
            p.lineTo(length - height / 4.0f, height / 2.0f)
            p.lineTo(-2.0f, height / 2.0f)
            p.lineTo(-2.0f + height / 4.0f, 0.0f)
            p.close()
            return p
        }
    }
}
