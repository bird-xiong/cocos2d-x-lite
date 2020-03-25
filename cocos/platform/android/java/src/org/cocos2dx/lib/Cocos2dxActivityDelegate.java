package org.cocos2dx.lib;

import android.app.Activity;
import android.content.Context;
import android.os.Message;

interface Cocos2dxActivityInterface {
    public Context getContext();
    public Activity getActivity();
    public Cocos2dxGLSurfaceView getGLSurfaceView();
    public void setKeepScreenOn(boolean value);
}

public class Cocos2dxActivityDelegate implements Cocos2dxActivityInterface, Cocos2dxHelper.Cocos2dxHelperListener  {
    public static Context mContext = null;
    private Cocos2dxGLSurfaceView mGLSurfaceView = null;
    private Cocos2dxHandler mHandler = null;

    public Cocos2dxActivityDelegate(Context context, Cocos2dxGLSurfaceView glSurfaceView ) {
        mContext = context;
        mGLSurfaceView = glSurfaceView;
        mHandler = new Cocos2dxHandler((Activity) context);
    }

    public void setGLSurfaceView(Cocos2dxGLSurfaceView mGLSurfaceView) {
        this.mGLSurfaceView = mGLSurfaceView;
    }

    public Cocos2dxGLSurfaceView getGLSurfaceView(){
        return  mGLSurfaceView;
    }


    public Context getContext() {
        return mContext;
    }

    public Activity getActivity() {
        return (Activity) mContext;
    }

    public void showDialog(final String pTitle, final String pMessage) {
        Message msg = new Message();
        msg.what = Cocos2dxHandler.HANDLER_SHOW_DIALOG;
        msg.obj = new Cocos2dxHandler.DialogMessage(pTitle, pMessage);
        this.mHandler.sendMessage(msg);
    }

    public void runOnGLThread(final Runnable runnable) {
        this.mGLSurfaceView.queueEvent(runnable);
    }

    public void setKeepScreenOn(boolean value) {
        final boolean newValue = value;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGLSurfaceView.setKeepScreenOn(newValue);
            }
        });
    }
}
