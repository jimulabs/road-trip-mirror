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

import android.app.Activity
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast

import java.util.ArrayList

SuppressWarnings("ConstantConditions")
public class MainActivity : Activity() {

    private class State(var background: Int, var map: Int, var photos: IntArray) {
        val bitmaps: MutableList<Bitmap> = ArrayList()
    }

    private val mStates = array<State>(State(R.color.az, R.raw.map_az, intArray(R.drawable.photo_01_antelope, R.drawable.photo_09_horseshoe, R.drawable.photo_10_sky)), State(R.color.ut, R.raw.map_ut, intArray(R.drawable.photo_08_arches, R.drawable.photo_03_bryce, R.drawable.photo_04_powell)), State(R.color.ca, R.raw.map_ca, intArray(R.drawable.photo_07_san_francisco, R.drawable.photo_02_tahoe, R.drawable.photo_05_sierra, R.drawable.photo_06_rockaway)))

    private var mIntroView: IntroView? = null
    private var mActionBarDrawable: Drawable? = null
    private var mWindowBackground: Drawable? = null
    private var mAccentColor: Int = 0
    private var mAccentColor2: Int = 0

    private val mTempRect = Rect()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mActionBarDrawable = getResources().getDrawable(R.drawable.ab_solid)
        mActionBarDrawable!!.setAlpha(0)
        getActionBar()!!.setBackgroundDrawable(mActionBarDrawable)

        mAccentColor = getResources().getColor(R.color.accent)
        mAccentColor2 = getResources().getColor(R.color.accent2)

        mIntroView = findViewById(R.id.intro) as IntroView
        mIntroView!!.setSvgResource(R.raw.map_usa)
        mIntroView!!.setOnReadyListener(object : IntroView.OnReadyListener {
            override fun onReady() {
                loadPhotos()
            }
        })

        (findViewById(R.id.scroller) as TrackingScrollView).setOnScrollChangedListener(object : TrackingScrollView.OnScrollChangedListener {
            override fun onScrollChanged(source: TrackingScrollView, l: Int, t: Int, oldl: Int, oldt: Int) {
                handleScroll(source, t)
            }
        })
    }

    private fun handleScroll(source: ViewGroup, top: Int) {
        val actionBarHeight = getActionBar()!!.getHeight().toFloat()
        val firstItemHeight = findViewById(R.id.scroller).getHeight().toFloat() - actionBarHeight
        val alpha = Math.min(firstItemHeight, Math.max(0, top).toFloat()) / firstItemHeight

        mIntroView!!.setTranslationY(-firstItemHeight / 3.0.toFloat() * alpha)
        mActionBarDrawable!!.setAlpha((255 * alpha).toInt())

        val decorView = getWindow().getDecorView()
        removeOverdraw(decorView, alpha)
        if (ANIMATE_BACKGROUND) {
            changeBackgroundColor(decorView, alpha)
        }

        val container = source.findViewById(R.id.container) as ViewGroup
        val count = container.getChildCount()
        for (i in 0..count - 1) {
            val item = container.getChildAt(i)
            val v = item.findViewById(R.id.state)
            if (v != null && v.getGlobalVisibleRect(mTempRect)) {
                (v as StateView).reveal(source, item.getBottom())
            }
        }
    }

    SuppressWarnings("PointlessBitwiseExpression")
    private fun changeBackgroundColor(decorView: View, alpha: Float) {
        val srcR = ((mAccentColor shr 16) and 255).toFloat() / 255.0.toFloat()
        val srcG = ((mAccentColor shr 8) and 255).toFloat() / 255.0.toFloat()
        val srcB = ((mAccentColor shr 0) and 255).toFloat() / 255.0.toFloat()

        val dstR = ((mAccentColor2 shr 16) and 255).toFloat() / 255.0.toFloat()
        val dstG = ((mAccentColor2 shr 8) and 255).toFloat() / 255.0.toFloat()
        val dstB = ((mAccentColor2 shr 0) and 255).toFloat() / 255.0.toFloat()

        val r = ((srcR + ((dstR - srcR) * alpha)) * 255.0.toFloat()).toInt()
        val g = ((srcG + ((dstG - srcG) * alpha)) * 255.0.toFloat()).toInt()
        val b = ((srcB + ((dstB - srcB) * alpha)) * 255.0.toFloat()).toInt()

        val background = decorView.getBackground() as ColorDrawable
        background.setColor(-16777216 or r shl 16 or g shl 8 or b)
    }

    private fun removeOverdraw(decorView: View, alpha: Float) {
        if (alpha >= 1.0.toFloat()) {
            // Note: setting a large negative translation Y to move the View
            // outside of the screen is an optimization. We could make the view
            // invisible/visible instead but this would destroy/create its backing
            // layer every time we toggle visibility. Since creating a layer can
            // be expensive, especially software layers, we would introduce stutter
            // when the view is made visible again.
            mIntroView!!.setTranslationY((-mIntroView!!.getHeight()).toFloat() * 2.0.toFloat())
        }
        if (alpha >= 1.0.toFloat() && decorView.getBackground() != null) {
            mWindowBackground = decorView.getBackground()
            decorView.setBackground(null)
        } else if (alpha < 1.0.toFloat() && decorView.getBackground() == null) {
            decorView.setBackground(mWindowBackground)
            mWindowBackground = null
        }
    }

    private fun loadPhotos() {
        val resources = getResources()
        Thread(object : Runnable {
            override fun run() {
                for (s in mStates) {
                    for (resId in s.photos) {
                        s.bitmaps.add(BitmapFactory.decodeResource(resources, resId))
                    }
                }

                mIntroView!!.post(object : Runnable {
                    override fun run() {
                        finishLoadingPhotos()
                    }
                })
            }
        }, "Photos Loader").start()
    }

    private fun finishLoadingPhotos() {
        mIntroView!!.stopWaitAnimation()

        val container = findViewById(R.id.container) as LinearLayout
        val inflater = getLayoutInflater()

        val spacer = Space(this)
        spacer.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, findViewById(R.id.scroller).getHeight()))
        container.addView(spacer)

        for (s in mStates) {
            addState(inflater, container, s)
        }
    }

    private fun addState(inflater: LayoutInflater, container: LinearLayout, state: State) {
        val margin = getResources().getDimensionPixelSize(R.dimen.activity_peek_margin)

        val view = inflater.inflate(R.layout.item_state, container, false)
        val stateView = view.findViewById(R.id.state) as StateView
        stateView.svgResource = state.map
        view.setBackgroundResource(state.background)

        val subContainer = view.findViewById(R.id.sub_container) as LinearLayout
        val spacer = Space(this)
        spacer.setLayoutParams(LinearLayout.LayoutParams(container.getWidth() - margin, ViewGroup.LayoutParams.MATCH_PARENT))
        subContainer.addView(spacer)

        var first: ImageView? = null
        for (b in state.bitmaps) {
            val image = inflater.inflate(R.layout.item_photo, subContainer, false) as ImageView
            if (first == null) first = image
            image.setImageBitmap(b)
            subContainer.addView(image)
        }

        val cm = ColorMatrix()
        cm.setSaturation(0.0.toFloat())
        first!!.setTag(cm)
        first!!.setColorFilter(ColorMatrixColorFilter(cm))

        val bw = first

        val s = view.findViewById(R.id.scroller) as TrackingHorizontalScrollView
        s.setOnScrollChangedListener(object : TrackingHorizontalScrollView.OnScrollChangedListener {
            override fun onScrollChanged(source: TrackingHorizontalScrollView, l: Int, t: Int, oldl: Int, oldt: Int) {
                val width = (source.getWidth() - margin).toFloat()
                val alpha = Math.min(width, Math.max(0, l).toFloat()) / width

                stateView.setTranslationX(-width / 3.0.toFloat() * alpha)
                stateView.parallax = 1.0.toFloat() - alpha

                removeStateOverdraw(view, state, alpha)

                if (alpha < 1.0.toFloat()) {
                    val cm = bw!!.getTag() as ColorMatrix
                    cm.setSaturation(alpha)
                    bw.setColorFilter(ColorMatrixColorFilter(cm))
                } else {
                    bw!!.setColorFilter(null)
                }
            }
        })

        container.addView(view)
    }

    private fun removeStateOverdraw(stateView: View, state: State, alpha: Float) {
        if (alpha >= 1.0.toFloat() && stateView.getBackground() != null) {
            stateView.setBackground(null)
            stateView.findViewById(R.id.state).setVisibility(View.INVISIBLE)
        } else if (alpha < 1.0.toFloat() && stateView.getBackground() == null) {
            stateView.setBackgroundResource(state.background)
            stateView.findViewById(R.id.state).setVisibility(View.VISIBLE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.action_about) {
            Toast.makeText(this, R.string.text_about, Toast.LENGTH_LONG).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class object {
        private val ANIMATE_BACKGROUND = false
    }
}
