package com.keeplive.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.keeplive.ICactusInterface;
import com.keeplive.R;
import com.keeplive.act.LockScreenActivity;
import com.keeplive.ext.CactusExtKt;


public class LocalService extends Service {

    /**
     * 本地服务 开启远程服务、广播跳转1像素界面、无声音乐、 回调给app
     */

    private ICactusInterface cactusIntface;//远程服务接口
    private ServiceReceiver mServiceReceiver;//开屏锁屏 广播
    private MediaPlayer mMediaPlayer;


    @Override
    public void onCreate() {
        super.onCreate();
        registerBroadcastReceiver();
        CactusExtKt.startForegroundNt(this);
        registerMedia();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bindRemoteService(LocalService.this,mConnection);
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }


    public class LocalBinder extends Binder {
        LocalService getService() { return LocalService.this; }
    }

    //-----------------------开启远程服务进行双方绑定
    /**
     * 绑定开启远程服务
     */
    public void bindRemoteService(Context context, ServiceConnection serviceConnection) {
        try {
            //通过Intent指定服务端的服务名称和所在包，与远程Service进行绑定
            Intent intent=CactusExtKt.startRemoteService(this);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }catch (Exception e){
        }
    }

    //
    ServiceConnection mConnection = new ServiceConnection() {

        //在Activity与Service建立关联时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            cactusIntface = ICactusInterface.Stub.asInterface(service);
            try {//连接成功
                cactusIntface.connect(new IcallWarnBackUI());
//                cactusIntface.wakeup();// 可以搞个心跳 去wakeup
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //在Activity与Service解除关联的时候调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bindRemoteService(LocalService.this,mConnection);
        }
    };

    /**
     * service回调
     */
    class IcallWarnBackUI extends com.keeplive.ICallBack.Stub {

        @Override
        public void conntectTimes(int time) throws RemoteException {
            Log.d("service","--"+time);
        }
    }

    //-------------------开屏 息屏广播 开启1像素
    public final class ServiceReceiver extends BroadcastReceiver {
        public void onReceive(@Nullable Context context, @Nullable Intent intent) {

            if(TextUtils.equals(intent.getAction(),Intent.ACTION_SCREEN_OFF)){//屏幕被关闭
                CactusExtKt.openOnePix(context);
//                CactusExtKt.openLockScreenAct(context);
            }else if(TextUtils.equals(intent.getAction(),Intent.ACTION_SCREEN_ON)){//屏幕被打开
                CactusExtKt.finishOnePix();
            }
        }
    }

    private final void registerBroadcastReceiver() {
        if (mServiceReceiver == null) {
            mServiceReceiver = new ServiceReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(mServiceReceiver, intentFilter);
    }

    private final void unregisterReceiver() {
        if (mServiceReceiver != null) {
            unregisterReceiver(mServiceReceiver);
            this.mServiceReceiver = (ServiceReceiver) null;
        }
    }

    //无声音乐
    private final void registerMedia() {
        if (CactusExtKt.musicEnable) {
            if (this.mMediaPlayer == null) {
                this.mMediaPlayer = MediaPlayer.create(this, R.raw.no_notice);
            }
            mMediaPlayer.setVolume(0f,0f);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            //设置监听回调
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    CactusExtKt.mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMediaPlayer.start();
                        }
                    }, 1*3600);//1个小时播放一次
                }
            });

        }
    }

    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        unbindService(mConnection);
        unregisterReceiver();
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
        }
    }

}
