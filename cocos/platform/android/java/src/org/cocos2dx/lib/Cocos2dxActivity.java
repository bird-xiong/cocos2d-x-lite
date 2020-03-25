/****************************************************************************
Copyright (c) 2010-2013 cocos2d-x.org
Copyright (c) 2013-2016 Chukong Technologies Inc.
Copyright (c) 2017-2018 Xiamen Yaji Software Co., Ltd.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.lib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public abstract class Cocos2dxActivity extends Activity {
    // ===========================================================
    // Constants
    // ===========================================================

    private final static String TAG = Cocos2dxActivity.class.getSimpleName();
    private Cocos2dxView sCocos2dxView = null;

    public Cocos2dxGLSurfaceView getGLSurfaceView(){
        return  sCocos2dxView.getGLSurfaceView();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "Cocos2dxActivity onCreate: " + this + ", savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);

        Cocos2dxView cocos2dxView = new Cocos2dxView(this);
        sCocos2dxView = cocos2dxView;
        setContentView(cocos2dxView);
    }

    @Override
    protected void onResume() {
    	Log.d(TAG, "onResume()");
        super.onResume();
        sCocos2dxView.onHostResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	Log.d(TAG, "onWindowFocusChanged() hasFocus=" + hasFocus);
        super.onWindowFocusChanged(hasFocus);
        sCocos2dxView.onHostWindowFocusChanged(hasFocus);
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        sCocos2dxView.onHostPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sCocos2dxView.onHostDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        sCocos2dxView.onHostActivityResult(requestCode, resultCode, data);
    }
}
