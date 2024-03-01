package com.bz.nfccirctest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 广播自启动
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d("Jim", "===BootReceiver 开机自启==" + intent.getAction());
            //自启动APP，参数为需要自动启动的应用包名
            Intent newIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
        }
    }
}
