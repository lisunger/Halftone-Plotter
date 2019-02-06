package com.nikolay.halftoneplotter;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.nikolay.halftoneplotter.activities.ControlActivity;

public class TestService extends IntentService {

    private IBinder mBinder = new LocalBinder();

    public TestService() {
        super("TestService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Lisko", "testService onCreate()");
        startForeground(123, buildForegroundNotification());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("Lisko", "testService onHandleIntent()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Lisko", "testService onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public TestService getService() {
            return TestService.this;
        }
    }

    public void logNumber(int number) {
        Log.d("Lisko", "testService " + number);
    }

    private Notification buildForegroundNotification() {
        //TODO set activity
        Intent notificationIntent = new Intent(this, ControlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("TestService running")
                .setContentText("Notification text")
                .setSmallIcon(R.drawable.crosshairs_gps)
                .setContentIntent(pendingIntent)
                .setTicker("Ticker text")
                .build();
        return notification;
    }


}
