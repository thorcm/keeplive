package com.keeplive.service;

import android.app.Notification;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.keeplive.ext.CactusExtKt;

//5.0系统 才有的  7.0以上失效
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CactusJobService extends JobService {
    private int mJobId = 100;
    private JobScheduler mJobScheduler;

    public void onCreate() {
        super.onCreate();
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        CactusExtKt.startForegroundNt(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 在服务启动时，直接将任务推到JobScheduler 的任务队列,然后在设定的时间条件到达时，便会直接吊起我们的服务，走onStartJob()方法
        startJobScheduler();
        CactusExtKt.registerWorker(this);
        return START_STICKY;
    }


    //任务队列到达了 开始任务
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        //本地 远程服务是否在运行
        if (!CactusExtKt.isKeepLiveRunning(this)) {
            Intent intentLocal = new Intent(this, LocalService.class);
            Intent intentRemote = new Intent(this, RemoteService.class);
            CactusExtKt.startInternService(this,intentLocal);
            CactusExtKt.startInternService(this,intentRemote);
            //workermanager
            CactusExtKt.registerWorker(this);
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        startJobScheduler();
        return true;
    }

    //执行任务操作
    public void startJobScheduler() {
        mJobScheduler.cancel(mJobId);
        JobInfo.Builder builder = new JobInfo.Builder(mJobId, new ComponentName(getPackageName(), CactusJobService.class.getName()));
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
                builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
                builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
            } else {
                builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
                builder.setRequiresDeviceIdle(true);
            }
        } catch (Exception e) {
        }
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setRequiresCharging(true); // 当插入充电器，执行该任务
        builder.setPersisted(true);  // 设置设备重启时，执行该任务
        JobInfo info = builder.build();
        mJobScheduler.schedule(info); //开始定时执行该系统任务
    }


    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

}