package com.william.youtubevideodownloader;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

public class Utility {

    public static int notificationId = 1;

    public static Map<Integer, Boolean> isThreadRunning = new HashMap<>();

    public static Map<String, Integer> videomapper = new HashMap<>();

    public static Map<Integer, NotificationCompat.Builder> notificationMapper = new HashMap<>();
}
