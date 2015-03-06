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
import android.util.AttributeSet
import android.widget.HorizontalScrollView

public class TrackingHorizontalScrollView(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {
    public trait OnScrollChangedListener {
        public fun onScrollChanged(source: TrackingHorizontalScrollView, l: Int, t: Int, oldl: Int, oldt: Int)
    }

    private var mOnScrollChangedListener: OnScrollChangedListener? = null

    public fun setOnScrollChangedListener(listener: OnScrollChangedListener) {
        mOnScrollChangedListener = listener
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener!!.onScrollChanged(this, l, t, oldl, oldt)
        }
    }
}