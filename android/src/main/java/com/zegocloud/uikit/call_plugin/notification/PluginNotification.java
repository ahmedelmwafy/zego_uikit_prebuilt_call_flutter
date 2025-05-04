package com.zegocloud.uikit.call_plugin.notification;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.zegocloud.uikit.call_plugin.Defines;
import com.zegocloud.uikit.call_plugin.R;
import com.zegocloud.uikit.call_plugin.StringUtils;

public class PluginNotification {
    public static String TAG = "ZEGO_Notification";

    public void addLocalIMNotification(Context context, String title, String body,
                                       String channelID, String soundSource, String iconSource,
                                       String notificationIdString, Boolean isVibrate) {
        Log.i("call plugin", "add IM Notification, title:" + title + ",body:" + body + ",channelId:" + channelID +
                ",soundSource:" + soundSource + ",iconSource:" + iconSource + "," +
                "notificationId:" + notificationIdString + ", isVibrate:" + isVibrate);

        int notificationId = parseNotificationId(notificationIdString);
        createNotificationChannel(context, channelID, channelID, soundSource, isVibrate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            wakeUpScreen(context);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        ClickReceiver clickReceiver = new ClickReceiver();
        Intent clickIntent = new Intent(context, clickReceiver.getClass());
        clickIntent.setAction(Defines.ACTION_CLICK_IM);
        clickIntent.putExtra(Defines.FLUTTER_PARAM_NOTIFICATION_ID, notificationId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, notificationId, clickIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(clickPendingIntent)
                .setSound(retrieveSoundResourceUri(context, soundSource))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        configureVibration(builder, isVibrate);
        setNotificationIcon(context, builder, iconSource);

        android.app.Notification notification = builder.build();
        postNotification(context, notificationId, notification);
    }

    public void addLocalCallNotification(Context context, String title, String body,
                                         String acceptButtonText, String rejectButtonText,
                                         String channelID, String soundSource, String iconSource,
                                         String notificationIdString, Boolean isVibrate, Boolean isVideo) {
        Log.i("call plugin", "add Notification, title:" + title + ",body:" + body +
                ",channelId:" + channelID + ",soundSource:" + soundSource + ",iconSource:" + iconSource +
                ",notificationId:" + notificationIdString + ",isVibrate:" + isVibrate + ",isVideo:" + isVideo);

        int notificationId = parseNotificationId(notificationIdString);
        createNotificationChannel(context, channelID, channelID, soundSource, isVibrate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            wakeUpScreen(context);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        // Create pending intents with unique request codes
        Intent fullscreenIntent = new Intent(context, IncomingCallActivity.class);
        fullscreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fullscreenIntent.putExtra("notification_id", notificationId);
        PendingIntent fullscreenPendingIntent = PendingIntent.getActivity(context, notificationId, fullscreenIntent, flags);

        CancelReceiver cancelReceiver = new CancelReceiver();
        Intent intentCancel = new Intent(context, cancelReceiver.getClass());
        intentCancel.setAction(Defines.ACTION_CANCEL);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, notificationId, intentCancel, flags);

        ClickReceiver clickReceiver = new ClickReceiver();
        Intent clickIntent = new Intent(context, clickReceiver.getClass());
        clickIntent.setAction(Defines.ACTION_CLICK);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, notificationId, clickIntent, flags);

        // Action buttons
        AcceptReceiver acceptReceiver = new AcceptReceiver();
        Intent acceptIntent = new Intent(context, acceptReceiver.getClass());
        acceptIntent.setAction(Defines.ACTION_ACCEPT);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, notificationId, acceptIntent, flags);

        RejectReceiver rejectReceiver = new RejectReceiver();
        Intent rejectIntent = new Intent(context, rejectReceiver.getClass());
        rejectIntent.setAction(Defines.ACTION_REJECT);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(context, notificationId, rejectIntent, flags);

        // Custom layout
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.layout_small_notification);
        contentView.setTextViewText(R.id.tvDecline, rejectButtonText);
        contentView.setTextViewText(R.id.tvAccept, acceptButtonText);
        contentView.setTextViewText(R.id.tvTitle, title);
        contentView.setTextViewText(R.id.tvBody, body);
        contentView.setOnClickPendingIntent(R.id.llAccept, acceptPendingIntent);
        contentView.setOnClickPendingIntent(R.id.llDecline, rejectPendingIntent);
        contentView.setImageViewResource(R.id.ivAccept, isVideo ? R.drawable.ic_video_accept : R.drawable.ic_audio_accept);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelID)
                .setContent(contentView)
                .setContentIntent(clickPendingIntent)
                .setDeleteIntent(cancelPendingIntent)
                .setFullScreenIntent(fullscreenPendingIntent, true)
                .setSound(retrieveSoundResourceUri(context, soundSource))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setOngoing(true)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        configureVibration(builder, isVibrate);
        setNotificationIcon(context, builder, iconSource);

        android.app.Notification notification = builder.build();
        notification.flags |= android.app.Notification.FLAG_INSISTENT;
        notification.flags |= android.app.Notification.FLAG_NO_CLEAR;

        postNotification(context, notificationId, notification);
    }

    // Helper methods
    private int parseNotificationId(String notificationIdString) {
        try {
            return Integer.parseInt(notificationIdString);
        } catch (NumberFormatException e) {
            Log.d("call plugin", "Invalid notification ID, using default");
            return 1;
        }
    }

    private void configureVibration(NotificationCompat.Builder builder, Boolean isVibrate) {
        if (isVibrate) {
            builder.setVibrate(new long[]{0, 1000, 500, 1000});
        } else {
            builder.setVibrate(new long[]{0});
        }
    }

    private void setNotificationIcon(Context context, NotificationCompat.Builder builder, String iconSource) {
        int iconResourceId = BitmapUtils.getDrawableResourceId(context, iconSource);
        if (iconResourceId != 0) {
            builder.setSmallIcon(iconResourceId);
        } else {
            Log.i("call plugin", "Using default notification icon");
            builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        }
    }

    private void postNotification(Context context, int notificationId, android.app.Notification notification) {
        String notificationTag = String.valueOf(notificationId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getNotificationManager(context).notify(notificationTag, notificationId, notification);
        } else {
            NotificationManagerCompat.from(context).notify(notificationTag, notificationId, notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void createNotificationChannel(Context context, String channelID, String channelName, 
                                         String soundSource, Boolean enableVibration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getNotificationManager(context);
            if (manager.getNotificationChannel(channelID) == null) {
                NotificationChannel channel = new NotificationChannel(channelID, channelName, 
                    NotificationManager.IMPORTANCE_HIGH);

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build();
                channel.setSound(retrieveSoundResourceUri(context, soundSource), audioAttributes);

                channel.enableVibration(enableVibration);
                if (enableVibration) {
                    channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                }

                manager.createNotificationChannel(channel);
            }
        }
    }

    public void dismissNotification(Context context, int notificationID) {
        String tag = String.valueOf(notificationID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getNotificationManager(context).cancel(tag, notificationID);
        } else {
            NotificationManagerCompat.from(context).cancel(tag, notificationID);
        }
    }

    public void dismissAllNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getNotificationManager(context).cancelAll();
        } else {
            NotificationManagerCompat.from(context).cancelAll();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void wakeUpScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (!pm.isInteractive()) {
            String appName = getAppName(context);
            
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE,
                appName + ":CallWakeLock");
            wakeLock.acquire(10_000);

            PowerManager.WakeLock cpuLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                appName + ":CallCpuLock");
            cpuLock.acquire(10_000);
        }
    }

    // Existing helper methods
    private Uri retrieveSoundResourceUri(Context context, String soundSource) {
        // ... existing implementation ...
    }

    private String getAppName(Context context) {
        // ... existing implementation ...
    }
}