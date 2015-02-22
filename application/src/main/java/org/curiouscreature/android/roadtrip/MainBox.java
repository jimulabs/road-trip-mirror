package org.curiouscreature.android.roadtrip;

import android.view.View;

import com.jimulabs.mirrorsandbox.MirrorSandbox;

/**
 * Created by lintonye on 15-02-21.
 */
public class MainBox implements MirrorSandbox {
    private final View mRootView;

    public MainBox(View rootView) {
        mRootView = rootView;
        IntroView introView = (IntroView) mRootView.findViewById(R.id.intro);
        introView.setSvgResource(R.raw.map_usa);
//        introView.stopWaitAnimation();
//        introView.setDrag(0.5f);
    }

    @Override
    public void enterSandbox() {
    }

    @Override
    public void destroySandbox() {

    }
}
