package com.william.youtubevideodownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class DownloadCancelReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int notificationId = (int) intent.getExtras().get("notification_id");
        System.out.println(notificationId);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);
        Utility.isThreadRunning.put(notificationId,false);
    }

}
