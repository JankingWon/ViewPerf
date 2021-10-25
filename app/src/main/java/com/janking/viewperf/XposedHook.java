package com.janking.viewperf;

import android.graphics.Canvas;
import android.view.View;
import android.view.WindowManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author jankingwon@foxmail.com
 * @since 2021/10/22
 */
public class XposedHook implements IXposedHookLoadPackage {

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals(loadPackageParam.processName)) {
            // 仅操作主进程
            return;
        }
        XposedBridge.log("监测到" + loadPackageParam.packageName);

        // 对于单个View
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.View"), "measure", int.class, int.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.beginViewStep((View) param.thisObject, Step.Measure);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.endViewStep((View) param.thisObject, Step.Measure);
            }

        });
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.View"), "layout", int.class, int.class, int.class, int.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.beginViewStep((View) param.thisObject, Step.Layout);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.endViewStep((View) param.thisObject, Step.Layout);
            }

        });
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.View"), "draw", Canvas.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.beginViewStep((View) param.thisObject, Step.Draw);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.endViewStep((View) param.thisObject, Step.Draw);
            }

        });


        // 对于ViewRoomImpl
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.ViewRootImpl"), "performTraversals", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View decorView = (View) param.thisObject.getClass().getDeclaredMethod("getView").invoke(param.thisObject);
                ViewPerfMonitor.startTraversal(decorView);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.stopTraversal();
            }
        });
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.ViewRootImpl"), "performMeasure", int.class, int.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.beginViewRootImplStep(Step.Measure);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.endViewRootImplStep(Step.Measure);
            }
        });
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.ViewRootImpl"), "performLayout", WindowManager.LayoutParams.class, int.class, int.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.beginViewRootImplStep(Step.Layout);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.endViewRootImplStep(Step.Layout);
            }
        });
        XposedHelpers.findAndHookMethod(loadPackageParam.classLoader.loadClass("android.view.ViewRootImpl"), "performDraw", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.beginViewRootImplStep(Step.Draw);
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewPerfMonitor.endViewRootImplStep(Step.Draw);
            }
        });
    }

}
