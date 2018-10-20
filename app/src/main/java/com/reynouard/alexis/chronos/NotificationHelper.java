package com.reynouard.alexis.chronos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    public static final int IMPORTANCE_LOW;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IMPORTANCE_LOW = NotificationManager.IMPORTANCE_LOW;
        }
        else {
            IMPORTANCE_LOW = 0;
        }
    }

    private NotificationHelper() {
    }

    public static void createNotificationChannel(Context context, String channelId, String channelName, String channelDescription, int importance) {
        // Create the NotificationChannel, but only on API 26+ because
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager == null) throw new AssertionError(); // TODO

            notificationManager.createNotificationChannel(channel);
        }
    }
}
