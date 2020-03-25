package org.cocos2dx.lib;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

public class Cocos2dxView extends FrameLayout {

    // ===========================================================
    // Constants
    // ===========================================================

    private final static String TAG = Cocos2dxView.class.getSimpleName();

    private static Activity sContext = null;
    private static Cocos2dxActivityDelegate sCocos2dxActivityDelegate= null;

    protected FrameLayout mFrameLayout = null;

    private Cocos2dxGLSurfaceView mGLSurfaceView = null;
    private int[] mGLContextAttrs = null;
    private Cocos2dxVideoHelper mVideoHelper = null;
    private Cocos2dxWebViewHelper mWebViewHelper = null;
    private boolean hasFocus = false;
    private Cocos2dxEditBox mEditBox = null;
    private boolean gainAudioFocus = false;
    private boolean paused = true;

    // DEBUG VIEW BEGIN
    private LinearLayout mLinearLayoutForDebugView;
    private TextView mFPSTextView;
    private TextView mJSBInvocationTextView;
    private TextView mGLOptModeTextView;
    private TextView mGameInfoTextView_0;
    private TextView mGameInfoTextView_1;
    private TextView mGameInfoTextView_2;

    public class Cocos2dxEGLConfigChooser implements GLSurfaceView.EGLConfigChooser {
        protected int[] configAttribs;
        public Cocos2dxEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize)
        {
            configAttribs = new int[] {redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize};
        }
        public Cocos2dxEGLConfigChooser(int[] attribs)
        {
            configAttribs = attribs;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {
            int[] value = new int[1];
            if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                return value[0];
            }
            return defaultValue;
        }

        class ConfigValue implements Comparable<Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue> {
            public EGLConfig config = null;
            public int[] configAttribs = null;
            public int value = 0;
            private void calcValue() {
                // depth factor 29bit and [6,12)bit
                if (configAttribs[4] > 0) {
                    value = value + (1 << 29) + ((configAttribs[4]%64) << 6);
                }
                // stencil factor 28bit and [0, 6)bit
                if (configAttribs[5] > 0) {
                    value = value + (1 << 28) + ((configAttribs[5]%64));
                }
                // alpha factor 30bit and [24, 28)bit
                if (configAttribs[3] > 0) {
                    value = value + (1 << 30) + ((configAttribs[3]%16) << 24);
                }
                // green factor [20, 24)bit
                if (configAttribs[1] > 0) {
                    value = value + ((configAttribs[1]%16) << 20);
                }
                // blue factor [16, 20)bit
                if (configAttribs[2] > 0) {
                    value = value + ((configAttribs[2]%16) << 16);
                }
                // red factor [12, 16)bit
                if (configAttribs[0] > 0) {
                    value = value + ((configAttribs[0]%16) << 12);
                }
            }

            public ConfigValue(int[] attribs) {
                configAttribs = attribs;
                calcValue();
            }

            public ConfigValue(EGL10 egl, EGLDisplay display, EGLConfig config) {
                this.config = config;
                configAttribs = new int[6];
                configAttribs[0] = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                configAttribs[1] = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                configAttribs[2] = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                configAttribs[3] = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                configAttribs[4] = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                configAttribs[5] = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
                calcValue();
            }

            @Override
            public int compareTo(Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue another) {
                if (value < another.value) {
                    return -1;
                } else if (value > another.value) {
                    return 1;
                } else {
                    return 0;
                }
            }

            @Override
            public String toString() {
                return "{ color: " + configAttribs[3] + configAttribs[2] + configAttribs[1] + configAttribs[0] +
                        "; depth: " + configAttribs[4] + "; stencil: " + configAttribs[5] + ";}";
            }
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
        {
            int[] EGLattribs = {
                    EGL10.EGL_RED_SIZE, configAttribs[0],
                    EGL10.EGL_GREEN_SIZE, configAttribs[1],
                    EGL10.EGL_BLUE_SIZE, configAttribs[2],
                    EGL10.EGL_ALPHA_SIZE, configAttribs[3],
                    EGL10.EGL_DEPTH_SIZE, configAttribs[4],
                    EGL10.EGL_STENCIL_SIZE,configAttribs[5],
                    EGL10.EGL_RENDERABLE_TYPE, 4, //EGL_OPENGL_ES2_BIT
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            boolean eglChooseResult = egl.eglChooseConfig(display, EGLattribs, configs, 1, numConfigs);
            if (eglChooseResult && numConfigs[0] > 0)
            {
                return configs[0];
            }

            // there's no config match the specific configAttribs, we should choose a closest one
            int[] EGLV2attribs = {
                    EGL10.EGL_RENDERABLE_TYPE, 4, //EGL_OPENGL_ES2_BIT
                    EGL10.EGL_NONE
            };
            eglChooseResult = egl.eglChooseConfig(display, EGLV2attribs, null, 0, numConfigs);
            if(eglChooseResult && numConfigs[0] > 0) {
                int num = numConfigs[0];
                Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue[] cfgVals = new Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue[num];

                // convert all config to ConfigValue
                configs = new EGLConfig[num];
                egl.eglChooseConfig(display, EGLV2attribs, configs, num, numConfigs);
                for (int i = 0; i < num; ++i) {
                    cfgVals[i] = new Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue(egl, display, configs[i]);
                }

                Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue e = new Cocos2dxView.Cocos2dxEGLConfigChooser.ConfigValue(configAttribs);
                // bin search
                int lo = 0;
                int hi = num;
                int mi;
                while (lo < hi - 1) {
                    mi = (lo + hi) / 2;
                    if (e.compareTo(cfgVals[mi]) < 0) {
                        hi = mi;
                    } else {
                        lo = mi;
                    }
                }
                if (lo != num - 1) {
                    lo = lo + 1;
                }
                Log.w("cocos2d", "Can't find EGLConfig match: " + e + ", instead of closest one:" + cfgVals[lo]);
                return cfgVals[lo].config;
            }
            Log.e(getContext().DEVICE_POLICY_SERVICE, "Can not select an EGLConfig for rendering.");
            return null;
        }

    }


    public Cocos2dxView(final Context context) {
        super(context);
        sContext = (Activity)context;
        this.createView();
    }
    private void createView() {
        Log.d(TAG, "Cocos2dxActivity onCreate: " + this);


        // Workaround in https://stackoverflow.com/questions/16283079/re-launch-of-activity-on-home-button-but-only-the-first-time/16447508
        if (!sContext.isTaskRoot()) {
            // Android launched another instance of the root activity into an existing task
            //  so just quietly finish and go away, dropping the user back into the activity
            //  at the top of the stack (ie: the last state of this task)
            sContext.finish();
            Log.w(TAG, "[Workaround] Ignore the activity started from icon!");
            return;
        }

        Utils.setActivity(sContext);


        Utils.hideVirtualButton();

        Cocos2dxHelper.registerBatteryLevelReceiver(sContext);

        onLoadNativeLibraries();

        CanvasRenderingContext2DImpl.init(sContext);

        Cocos2dxActivityDelegate cocos2dxActivityDelegate = new Cocos2dxActivityDelegate(sContext, null);
        Cocos2dxHelper.init(cocos2dxActivityDelegate);
        sCocos2dxActivityDelegate = cocos2dxActivityDelegate;

        this.mGLContextAttrs = getGLContextAttrs();
        this.init();


        if (mVideoHelper == null) {
            mVideoHelper = new Cocos2dxVideoHelper(cocos2dxActivityDelegate, mFrameLayout);
        }

        if(mWebViewHelper == null){
            mWebViewHelper = new Cocos2dxWebViewHelper(cocos2dxActivityDelegate, mFrameLayout);
        }

        Window window = sContext.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        sContext.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    // ===========================================================
    // Member methods
    // ===========================================================

    public Cocos2dxGLSurfaceView getGLSurfaceView(){
        return  mGLSurfaceView;
    }

    public void init() {
        ViewGroup.LayoutParams frameLayoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mFrameLayout = new FrameLayout(sContext);
        mFrameLayout.setLayoutParams(frameLayoutParams);

        Cocos2dxRenderer renderer = this.addSurfaceView();
        this.addDebugInfo(renderer);


        // Should create EditBox after adding SurfaceView, or EditBox will be hidden by SurfaceView.
        mEditBox = new Cocos2dxEditBox(sCocos2dxActivityDelegate, mFrameLayout);

        this.addView(mFrameLayout);
    }

    public void setEnableAudioFocusGain(boolean value) {
        if(gainAudioFocus != value) {
            if(!paused) {
                if (value)
                    Cocos2dxAudioFocusManager.registerAudioFocusListener(sContext);
                else
                    Cocos2dxAudioFocusManager.unregisterAudioFocusListener(sContext);
            }
            gainAudioFocus = value;
        }
    }

    public Cocos2dxGLSurfaceView onCreateView() {
//        Cocos2dxGLSurfaceView glSurfaceView = new Cocos2dxGLSurfaceView(sContext);
//        //this line is need on some device if we specify an alpha bits
//        if(this.mGLContextAttrs[3] > 0) glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//
//        Cocos2dxView.Cocos2dxEGLConfigChooser chooser = new Cocos2dxView.Cocos2dxEGLConfigChooser(this.mGLContextAttrs);
//        glSurfaceView.setEGLConfigChooser(chooser);
//
//        return glSurfaceView;

        Cocos2dxGLSurfaceView glSurfaceView = new Cocos2dxGLSurfaceView(sContext);
        // TestCpp should create stencil buffer
        glSurfaceView.setEGLConfigChooser(5, 6, 5, 0, 16, 8);
        return glSurfaceView;
    }




    public void onHostResume() {
        Log.d(TAG, "onResume()");
        paused = false;
        if(gainAudioFocus)
            Cocos2dxAudioFocusManager.registerAudioFocusListener(sContext);
        Utils.hideVirtualButton();
        resumeIfHasFocus();
    }

    public void onHostWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged() hasFocus=" + hasFocus);
        this.hasFocus = hasFocus;
        resumeIfHasFocus();
    }

    private void resumeIfHasFocus() {
        if(hasFocus && !paused) {
            Utils.hideVirtualButton();
            Cocos2dxHelper.onResume();
            mGLSurfaceView.onResume();
        }
    }

    public void onHostPause() {
        Log.d(TAG, "onPause()");
        paused = true;
        if(gainAudioFocus)
            Cocos2dxAudioFocusManager.unregisterAudioFocusListener(sContext);
        Cocos2dxHelper.onPause();
        mGLSurfaceView.onPause();
    }

    public void onHostDestroy() {
        if (!sContext.isTaskRoot()) {
            return;
        }

        if(gainAudioFocus)
            Cocos2dxAudioFocusManager.unregisterAudioFocusListener(sContext);
        Cocos2dxHelper.unregisterBatteryLevelReceiver(sContext);;
        CanvasRenderingContext2DImpl.destroy();

        Log.d(TAG, "Cocos2dxActivity onDestroy: " + this + ", mGLSurfaceView" + mGLSurfaceView);
        if (mGLSurfaceView != null) {
            Cocos2dxHelper.terminateProcess();
        }
    }

    public void onHostActivityResult(int requestCode, int resultCode, Intent data)
    {
        for (PreferenceManager.OnActivityResultListener listener : Cocos2dxHelper.getOnActivityResultListeners()) {
            listener.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Cocos2dxRenderer addSurfaceView() {
        this.mGLSurfaceView = this.onCreateView();
        this.mGLSurfaceView.setPreserveEGLContextOnPause(true);
        // Should set to transparent, or it will hide EditText
        // https://stackoverflow.com/questions/2978290/androids-edittext-is-hidden-when-the-virtual-keyboard-is-shown-and-a-surfacevie
        mGLSurfaceView.setBackgroundColor(Color.TRANSPARENT);
        // Switch to supported OpenGL (ARGB888) mode on emulator
        if (isAndroidEmulator())
            this.mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        Cocos2dxRenderer renderer = new Cocos2dxRenderer();
        this.mGLSurfaceView.setCocos2dxRenderer(renderer);

        mFrameLayout.addView(this.mGLSurfaceView);

        sCocos2dxActivityDelegate.setGLSurfaceView(this.mGLSurfaceView );

        return renderer;
    }

    private void addDebugInfo(final Cocos2dxRenderer renderer) {
        final LinearLayout.LayoutParams linearLayoutParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayoutParam.setMargins(30, 0, 0, 0);
        Cocos2dxHelper.setOnGameInfoUpdatedListener(new Cocos2dxHelper.OnGameInfoUpdatedListener() {
            @Override
            public void onFPSUpdated(final float fps) {
                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFPSTextView != null) {
                            mFPSTextView.setText("FPS: " + (int)Math.ceil(fps));
                        }
                    }
                });
            }

            @Override
            public void onJSBInvocationCountUpdated(final int count) {
                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mJSBInvocationTextView != null) {
                            mJSBInvocationTextView.setText("JSB: " + count);
                        }
                    }
                });
            }

            @Override
            public void onOpenDebugView() {
                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mLinearLayoutForDebugView != null || mFrameLayout == null) {
                            Log.e(TAG, "onOpenDebugView: failed!");
                            return;
                        }

                        mLinearLayoutForDebugView = new LinearLayout(sContext);
                        mLinearLayoutForDebugView.setOrientation(LinearLayout.VERTICAL);
                        mFrameLayout.addView(mLinearLayoutForDebugView);

                        mFPSTextView = new TextView(sContext);
                        mFPSTextView.setBackgroundColor(Color.RED);
                        mFPSTextView.setTextColor(Color.WHITE);
                        mLinearLayoutForDebugView.addView(mFPSTextView, linearLayoutParam);

                        mJSBInvocationTextView = new TextView(sContext);
                        mJSBInvocationTextView.setBackgroundColor(Color.GREEN);
                        mJSBInvocationTextView.setTextColor(Color.WHITE);
                        mLinearLayoutForDebugView.addView(mJSBInvocationTextView, linearLayoutParam);

                        mGLOptModeTextView = new TextView(sContext);
                        mGLOptModeTextView.setBackgroundColor(Color.BLUE);
                        mGLOptModeTextView.setTextColor(Color.WHITE);
                        mGLOptModeTextView.setText("GL Opt: Enabled");
                        mLinearLayoutForDebugView.addView(mGLOptModeTextView, linearLayoutParam);

                        mGameInfoTextView_0 = new TextView(sContext);
                        mGameInfoTextView_0.setBackgroundColor(Color.RED);
                        mGameInfoTextView_0.setTextColor(Color.WHITE);
                        mLinearLayoutForDebugView.addView(mGameInfoTextView_0, linearLayoutParam);

                        mGameInfoTextView_1 = new TextView(sContext);
                        mGameInfoTextView_1.setBackgroundColor(Color.GREEN);
                        mGameInfoTextView_1.setTextColor(Color.WHITE);
                        mLinearLayoutForDebugView.addView(mGameInfoTextView_1, linearLayoutParam);

                        mGameInfoTextView_2 = new TextView(sContext);
                        mGameInfoTextView_2.setBackgroundColor(Color.BLUE);
                        mGameInfoTextView_2.setTextColor(Color.WHITE);
                        mLinearLayoutForDebugView.addView(mGameInfoTextView_2, linearLayoutParam);
                    }
                });

                renderer.showFPS();
            }

            @Override
            public void onDisableBatchGLCommandsToNative() {
                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mGLOptModeTextView != null) {
                            mGLOptModeTextView.setText("GL Opt: Disabled");
                        }
                    }
                });
            }

            @Override
            public void onGameInfoUpdated_0(final String text) {

                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mGameInfoTextView_0 != null) {
                            mGameInfoTextView_0.setText(text);
                        }
                    }
                });
            }

            @Override
            public void onGameInfoUpdated_1(final String text) {

                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mGameInfoTextView_1 != null) {
                            mGameInfoTextView_1.setText(text);
                        }
                    }
                });
            }

            @Override
            public void onGameInfoUpdated_2(final String text) {

                sContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mGameInfoTextView_2 != null) {
                            mGameInfoTextView_2.setText(text);
                        }
                    }
                });
            }
        });
    }



    protected void onLoadNativeLibraries() {
        try {
            ApplicationInfo ai = sContext.getPackageManager().getApplicationInfo(sContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            String libName = bundle.getString("android.app.lib_name");
            System.loadLibrary(libName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static boolean isAndroidEmulator() {
        String model = Build.MODEL;
        Log.d(TAG, "model=" + model);
        String product = Build.PRODUCT;
        Log.d(TAG, "product=" + product);
        boolean isEmulator = false;
        if (product != null) {
            isEmulator = product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_");
        }
        Log.d(TAG, "isEmulator=" + isEmulator);
        return isEmulator;
    }
    private static native int[] getGLContextAttrs();
}
