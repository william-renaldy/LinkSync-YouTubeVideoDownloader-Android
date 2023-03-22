package com.william.youtubevideodownloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private EditText ytUrl;

    private final PyObject downloadVideoFunction;


    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;


    private Context context;
    private Notification notification;


    public MainActivity(){
        Python python = Python.getInstance();
        downloadVideoFunction = python.getModule("downloader").get("download_video");
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();





        if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        }
        else {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
            }
        }


        ytUrl = findViewById(R.id.idURLlink);
        Button loginBtn = findViewById(R.id.idBtnLogin);


        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type) && intent.hasExtra(Intent.EXTRA_TEXT)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Extract the YouTube video URL from the shared text

                    // Set the video URL to the text field
                    ytUrl.setText(sharedText);
                }
            }
        }

        // adding on click listener for our button.
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // on below line we are getting data from our edit text.
                String url = ytUrl.getText().toString();



                // checking if the entered text is empty or not.
                if (url.trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please enter the url", Toast.LENGTH_SHORT).show();
                }
                else{
                    File downloadDir = getBaseContext().getExternalMediaDirs()[0];
                    System.out.println(downloadDir.getAbsolutePath());

                    downloadVideo(url, downloadDir + "/LinkSync");
                }


            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            ytUrl.setText(sharedText);
        }
    }


    private void downloadVideo(final String videoUrl, final String path) {


        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                int notificationId = Utility.notificationId;
                Utility.isThreadRunning.put(notificationId, true);

                Utility.notificationId += 1;

                System.out.println("Noti = "+notificationId);

                try {
                    // Download the video using PyTube



                    downloadVideoFunction.call(videoUrl, path, PyObject.fromJava(new GettingCallBack() {

                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void onSuccess() {

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Getting Video...", Toast.LENGTH_SHORT).show();
                                }
                            });

                            System.out.println(Utility.isThreadRunning);

                            if(!Utility.isThreadRunning.get(notificationId)){
                                throw new CancelException();
                            }

                            context = getApplicationContext();
                            PendingIntent cancelPendingIntent = createCancelPendingIntent(context, notificationId);

                            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            String channelId = "download_started";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel(channelId, "Getting Video", NotificationManager.IMPORTANCE_HIGH);
                                notificationManager.createNotificationChannel(channel);
                            }
                            builder = new NotificationCompat.Builder(context, channelId)
                                    .setContentTitle("Getting Video")
                                    .setSmallIcon(R.drawable.logo)
                                    .setProgress(100, 0, true)
                                    .setOngoing(true)
                                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel",cancelPendingIntent)
                                    .setOnlyAlertOnce(true);
                            notification = builder.build();
                            notificationManager.notify(notificationId, notification);
                        }
                    }), PyObject.fromJava(new DownloadCallBack() {

                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void onSuccess(PyObject stream, PyObject chunk, PyObject bytes_remaining) {
                            System.out.println(Utility.isThreadRunning);
                            if(!Utility.isThreadRunning.get(notificationId)){
                                throw new CancelException();
                            }
                            try {

                                chunk.toBoolean();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Starting Download...", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                buildDownloadNotification(Objects.requireNonNull(stream.get("title")).toString(),notificationId);

                                Utility.videomapper.put(stream.get("title").toString(),notificationId);
                                System.out.println(Utility.videomapper);
                            } catch (ClassCastException e) {

                                long filesize = Objects.requireNonNull(stream.get("filesize")).toLong();

                                long rem = bytes_remaining.toLong();

                                float total_mb = (float) filesize / 1048576;

                                float downloaded_mb = (float) (filesize - rem) / 1048576;

                                int progress = (int) (((float) (filesize - rem) / (float) filesize * 100));

                                System.out.println("File size = " + progress);

                                int notiId = Utility.videomapper.get(stream.get("title").toString());

                                updateDownloadNotification(total_mb, downloaded_mb, progress, notiId);
                            }
                        }
                    }));






                    Context context = getApplicationContext();
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    String channelId = "download_complete";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(channelId, "Download completed", NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);
                    }

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                            .setContentTitle("Download Completed")
                            .setSmallIcon(R.drawable.ic_baseline_check_24)
                            .setContentText("Video Downloaded Successfully")
                            .setAutoCancel(true);
                    Notification notification = builder.build();
                    notificationManager.notify(notificationId, notification);


                }
                catch (CancelException e) {
                    e.printStackTrace();
                }
                catch (PyException e) {
                    // Update the notification with an error message
                    Context context = getApplicationContext();
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    String channelId = "download_failed";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(channelId, "Download Failed", NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);
                    }
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                            .setContentTitle("Download Failed")
                            .setSmallIcon(R.drawable.logo)
                            .setContentText("Video Downloading Failed")
                            .setAutoCancel(true);
                    Notification notification = builder.build();
                    notificationManager.notify(notificationId, notification);


                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Download Failed", Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Print an error message to the console
                    Log.e("DOWNLOAD", "Error downloading video: " + e.getMessage());
                }

            }
        });

        thread.start();

    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    private void buildDownloadNotification(String title,int notificationId) {
        context = getApplicationContext();
        PendingIntent cancelPendingIntent = createCancelPendingIntent(context, notificationId);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "download_started";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Downloading..", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText("Starting Download")
                .setProgress(100,0,true)
                .setOngoing(true)
                .addAction(android.R.drawable.btn_default, "Cancel", cancelPendingIntent)
                .setOnlyAlertOnce(true);
        Utility.notificationMapper.put(notificationId,builder);
        notification = builder.build();
        notificationManager.notify(notificationId, notification);

    }


    private void updateDownloadNotification(float total_mb, float downloaded_mb, int progress, int notificationId) {
        NotificationCompat.Builder updateBuilder = Utility.notificationMapper.get(notificationId);
        updateBuilder.setContentText("Downloading " + progress + "% (" + downloaded_mb + " MB / " + total_mb + " MB)");
        updateBuilder.setProgress(100,progress,false);

        notificationManager.notify(notificationId,updateBuilder.build());
        Utility.notificationMapper.put(notificationId,updateBuilder);

    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private PendingIntent createCancelPendingIntent(Context context, int notificationId) {



        // Create an explicit intent for the cancel button callback
        Intent cancelIntent = new Intent(context, DownloadCancelReceiver.class);




        cancelIntent.setAction("CANCEL_NOTIFICATION");
        cancelIntent.putExtra("notification_id", notificationId);
        System.out.println("notification id = "+notificationId);

        // Create a PendingIntent for the cancel button callback


        return PendingIntent.getBroadcast(context, notificationId, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
    }



}

