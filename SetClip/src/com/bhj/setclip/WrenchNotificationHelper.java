package com.bhj.setclip;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.app.Notification;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService.*;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;
import com.Wrench.Input;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class WrenchNotificationHelper extends NotificationListenerService {
    private LocalSocket notificationSocket;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;
    private Handler mHandler;
    private static final int gotCommandFromWrench = 1;
    private static final int gotNewNotification = 2;
    @Override
    public void onCreate()  {
        super.onCreate();
        mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case gotCommandFromWrench:
                    case gotNewNotification:
                        Log.e("bhj", String.format("%s:%d: ", "WrenchNotificationHelper.java", 36));
                        if (writer != null) {
                            StatusBarNotification[] notifications = getActiveNotifications();
                            try {
                                for (StatusBarNotification sn : notifications) {
                                    Notification n = sn.getNotification();
                                    Bundle extra = n.extras;
                                    CharSequence title = extra.getCharSequence(Notification.EXTRA_TITLE, "no title");
                                    CharSequence text = extra.getCharSequence(Notification.EXTRA_TEXT, "no text");
                                    writer.write("got a notification: title: " + title + ", text: " + text + "\n");
                                }
                                writer.flush();
                            } catch (IOException e) {
                                Log.e("bhj", String.format("%s:%d: ", "WrenchNotificationHelper.java", 52), e);
                            }
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                    }
                }
            };
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)  {
        Log.e("bhj", String.format("%s:%d: onNotificationRemoved()", "WrenchNotificationHelper.java", 22));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)  {
        Notification n = sbn.getNotification();
        Bundle extra = n.extras;
        CharSequence text = extra.getCharSequence(Notification.EXTRA_TEXT, "no text");
        CharSequence title = extra.getCharSequence(Notification.EXTRA_TITLE, "no title");
        Log.e("bhj", String.format("%s:%d: new notificaton: %s (%s) ", "WrenchNotificationHelper.java", 34, title, text));

        Message msg = new Message();
        msg.what = gotNewNotification;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onListenerConnected()
    {
        super.onListenerConnected();
        Log.e("bhj", String.format("%s:%d: onListenerConnected()", "WrenchNotificationHelper.java", 48));
        new Thread(new Runnable() {
                @Override
                public void run() {
                    LocalServerSocket t1WrenchServer;
                    try {
                        t1WrenchServer = new LocalServerSocket("WrenchNotifications");

                    } catch(Exception e) {
                        Log.e("bhj", String.format("%s:%d: ", "WrenchNotificationHelper.java", 34), e);
                        return;
                    }

                    while (true) {
                        notificationSocket = null;
                        try {
                            notificationSocket = t1WrenchServer.accept();
                            if (!Input.checkPerm(notificationSocket.getFileDescriptor())) {
                                Log.e("bhj", String.format("%s:%d: only socket from shell is allowed", "WrenchNotificationHelper.java", 47));
                                notificationSocket.close();
                                continue;
                            }
                            reader =
                                new BufferedReader(new InputStreamReader(notificationSocket.getInputStream()));
                            writer =
                                new BufferedWriter(new OutputStreamWriter(notificationSocket.getOutputStream()));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                // writer.write("got a line: " + line + "\n");

                                // StatusBarNotification[] notifications = getActiveNotifications();
                                // for (StatusBarNotification sn : notifications) {
                                //     Notification n = sn.getNotification();
                                //     Bundle extra = n.extras;
                                //     CharSequence title = extra.getCharSequence(Notification.EXTRA_TITLE, "no title");
                                //     CharSequence text = extra.getCharSequence(Notification.EXTRA_TEXT, "no text");
                                //     writer.write("got a notification: title: " + title + ", text: " + text + "\n");
                                // }
                                // writer.flush();

                                Message msg = new Message();
                                msg.what = gotCommandFromWrench;
                                mHandler.sendMessage(msg);
                            }
                            reader.close();
                            reader = null;
                            writer.close();
                            writer = null;
                            notificationSocket.close();
                            notificationSocket = null;
                        } catch(Exception e) {
                            Log.e("bhj", String.format("%s:%d: ", "WrenchNotificationHelper.java", 28), e);
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (Exception x) {
                                }
                            }
                            if (writer != null) {
                                try {
                                    writer.close();
                                } catch (Exception x) {

                                }
                            }
                            if (notificationSocket != null) {
                                try {
                                    notificationSocket.close();
                                } catch (Exception x) {

                                }
                            }
                            reader = null;
                            writer = null;
                            notificationSocket = null;
                        }
                    }

                }
            }).start();

    }
}