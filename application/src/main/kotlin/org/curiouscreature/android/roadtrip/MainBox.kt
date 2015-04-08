package org.curiouscreature.android.roadtrip

import android.view.View

import com.jimulabs.mirrorsandbox.MirrorSandbox
import com.jimulabs.mirrorsandbox.MirrorSandboxBase

/**
 * Created by lintonye on 15-02-21.
 */
public class MainBox(private val rootView: View) : MirrorSandboxBase(rootView) {

    override fun `$onLayoutDone`(rootView: View) {
    }

    override fun `$onCreate`(rootView: View?) {
        val introView = rootView?.findViewById(R.id.intro) as IntroView
        introView.setSvgResource(R.raw.map_usa)
    }
}
