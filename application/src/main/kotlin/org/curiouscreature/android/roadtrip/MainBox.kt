package org.curiouscreature.android.roadtrip

import android.view.View

import com.jimulabs.mirrorsandbox.MirrorSandbox

/**
 * Created by lintonye on 15-02-21.
 */
public class MainBox(private val mRootView: View) : MirrorSandbox {

    {
        val introView = mRootView.findViewById(R.id.intro) as IntroView
        introView.setSvgResource(R.raw.map_usa)
        //        introView.stopWaitAnimation();
        //        introView.setDrag(0.5f);
    }

    override fun enterSandbox() {
    }

    override fun destroySandbox() {
    }
}
