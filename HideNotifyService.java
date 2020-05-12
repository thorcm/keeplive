package com.keeplive.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.keeplive.ICactusInterface;
import com.keeplive.ext.CactusExtKt;
import com.keeplive.ext.NotificationHP;


public class HideNotifyService extends Service {


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startForeground(NotificationHP.NOTICE_ID, NotificationHP.getInstance().getNotification(this));//通知栏显示
        }else {
            startForeground(NotificationHP.NOTICE_ID, new Notification());//通知栏不显示
        }
        // 开启一条线程，去移除DaemonService弹出的通知
        CactusExtKt.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 取消CancelNoticeService的前台
                stopForeground(true);
                // 移除DaemonService弹出的通知
                NotificationHP.getInstance().cancleNotify();
                // 任务完成，终止自己
                stopSelf();
                Log.d("hsc","run-------------------------");
            }
        },6000);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }


    public class LocalBinder extends Binder {
        HideNotifyService getService() { return HideNotifyService.this; }
    }

}
