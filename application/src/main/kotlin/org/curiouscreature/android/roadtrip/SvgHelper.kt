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

import android.content.Context
import android.graphics.*
import android.util.Log
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException

import java.util.ArrayList

public class SvgHelper(private val mSourcePaint: Paint) {

    private val mPaths = ArrayList<SvgPath>()

    private var mSvg: SVG? = null

    public fun load(context: Context, svgResource: Int) {
        if (mSvg != null) return
        try {
            mSvg = SVG.getFromResource(context, svgResource)
            mSvg!!.setDocumentPreserveAspectRatio(PreserveAspectRatio.UNSCALED)
        } catch (e: SVGParseException) {
            Log.e(LOG_TAG, "Could not load specified SVG resource", e)
        }

    }

    public class SvgPath(val path: Path, val paint: Paint) {
        val renderPath = Path()
        val length: Float
        val bounds: Rect
        val measure: PathMeasure

        {

            measure = PathMeasure(path, false)
            this.length = measure.getLength()

            sRegion.setPath(path, sMaxClip)
            bounds = sRegion.getBounds()
        }

        class object {
            private val sRegion = Region()
            private val sMaxClip = Region(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
        }
    }

    public fun getPathsForViewport(width: Int, height: Int): List<SvgPath> {
        mPaths.clear()

        val canvas = object : Canvas() {
            private val mMatrix = Matrix()

            override fun getWidth(): Int {
                return width
            }

            override fun getHeight(): Int {
                return height
            }

            override fun drawPath(path: Path, paint: Paint) {
                val dst = Path()

                //noinspection deprecation
                getMatrix(mMatrix)
                path.transform(mMatrix, dst)

                mPaths.add(SvgPath(dst, Paint(mSourcePaint)))
            }
        }

        val viewBox = mSvg!!.getDocumentViewBox()
        val scale = Math.min(width.toFloat() / viewBox.width(), height.toFloat() / viewBox.height())

        canvas.translate((width.toFloat() - viewBox.width() * scale) / 2.0.toFloat(), (height.toFloat() - viewBox.height() * scale) / 2.0.toFloat())
        canvas.scale(scale, scale)

        mSvg!!.renderToCanvas(canvas)

        return mPaths
    }

    class object {
        private val LOG_TAG = "SVG"
    }
}
