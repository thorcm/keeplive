package com.keeplive.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.keeplive.ICallBack;
import com.keeplive.ext.CactusExtKt;

public class RemoteService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        CactusExtKt.startForegroundNt(this);
    }

    //状态栏通知
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLocalService();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new TheCallWarnInterface().asBinder();
    }

    class TheCallWarnInterface extends com.keeplive.ICactusInterface.Stub {

        @Override
        public void connect(ICallBack callback) throws RemoteException {
            //绑定链接成功
            callback.conntectTimes(0);
        }

        @Override
        public void wakeup() throws RemoteException {

        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            startLocalService();
        }
    };


    //开启本地服务
    private void startLocalService() {
        try {
            Intent intent=CactusExtKt.startLocalService(this);
            bindService(intent,mConnection, Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        unbindService(mConnection);
    }

}
