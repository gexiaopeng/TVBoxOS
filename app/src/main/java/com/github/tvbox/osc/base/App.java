package com.github.tvbox.osc.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;
import androidx.multidex.MultiDexApplication;

import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.*;
import com.github.tvbox.osc.util.js.JSEngine;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;

import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;
    private Handler mHandler = new Handler();
    private Context mContext=this;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initParams();
        // OKGo
        OkGoHelper.init(); //台标获取
        EpgUtil.init();
      //writeLog("--app ok--");
            // 初始化Web服务器
            ControlManager.init(this);
            //初始化数据库
            AppDataManager.init();
            LoadSir.beginBuilder()
                    .addCallback(new EmptyCallback())
                    .addCallback(new LoadingCallback())
                    .commit();
        AutoSizeConfig.getInstance().setCustomFragment(true).getUnitsManager()
                .setSupportDP(true)
                .setSupportSP(true)
                .setSupportSubunits(Subunits.MM);
        try {
            PlayerHelper.init();
            JSEngine.getInstance().create();//android 14 加载lib出错
            //showInfo("App ok!!");
        } catch (Throwable e) {
            showInfo("error:"+e.getMessage());
        }
        //Toast.makeText(this, "App OK:"+Environment.getExternalStorageDirectory().getAbsolutePath(), Toast.LENGTH_LONG).show();

        /**




         **/

    }
    private void showInfo(String str){
        try {
             Runnable info = new Runnable() {
                @SuppressLint({"DefaultLocale", "SetTextI18n"})
                @Override
                public void run() {
                    Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();
                }
            };
            mHandler.post(info);
        } catch (Exception e) {
            Toast.makeText(this, "info error:"+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);
        putDefault(HawkConfig.HOME_REC_STYLE,true);
        putDefault(HawkConfig.PLAY_TYPE,2);
        putDefault(HawkConfig.IJK_CODEC,"硬解码");
        putDefault(HawkConfig.HISTORY_NUM,HistoryHelper.getHistoryNumArraySize()-1);
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JSEngine.getInstance().destroy();
    }


    private VodInfo vodInfo;
    public void setVodInfo(VodInfo vodinfo){
        this.vodInfo = vodinfo;
    }
    public VodInfo getVodInfo(){
        return this.vodInfo;
    }

    public Activity getCurrentActivity() {
        return AppManager.getInstance().currentActivity();
    }
    private void putDefault(String key, Object value) {
        if (!Hawk.contains(key)) {
            Hawk.put(key, value);
        }
    }
}