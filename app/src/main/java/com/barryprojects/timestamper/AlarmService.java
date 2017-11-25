package com.barryprojects.timestamper;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

public class AlarmService extends Service {
    // handles alarms and executes selected reminders

    private AlarmManager alarm;
    private PendingIntent alarmIntent;
    private BroadcastReceiver alarmReceiver;
    private MediaPlayer beep;
    private TextToSpeech talk;
    private Vibrator v;

    public AlarmService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // shouldn't be bound
        return null;
    } // end public IBinder onBind

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        beep = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
        if(talk != null) talk.shutdown();
        talk = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // what goes in here anyway?
            } // end public void onInit
        }); // end anonymous OnInitListener

        if(intent.getAction().equals(Constants.ACTION_START)) startReminders(intent.getLongExtra(Constants.PARAM_DELAY, 0), intent.getBooleanArrayExtra(Constants.PARAM_REMINDERS));
        else if(intent.getAction().equals(Constants.ACTION_STOP)) stopReminders();

        return START_STICKY;

    }  // end public int onStartCommand


    private void startReminders(final long delay, final  boolean[] reminders) {
        // starts all reminders

        // create notification required for foreground service
        Intent resultIntent = new Intent(this, TimerActivity.class);
        resultIntent.putExtra(Constants.PARAM_DELAY, delay);
        resultIntent.putExtra(Constants.PARAM_REMINDERS, reminders);

        PendingIntent onClickIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getString(R.string.notification_title));
        builder.setContentText(getNotificationText(reminders) + getTimeString(delay));
        builder.setContentIntent(onClickIntent);

        // add stop action to notification
        Intent stopIntent = new Intent(this, TimerActivity.class);
        stopIntent.setAction(Constants.ACTION_STOP);
        stopIntent.putExtra(Constants.PARAM_DELAY, delay);
        stopIntent.putExtra(Constants.PARAM_REMINDERS, reminders);

        PendingIntent stopPendingIntent = PendingIntent.getActivity(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Icon stopIcon = Icon.createWithResource(getApplicationContext(), android.R.drawable.ic_delete);
        Notification.Action stopAction = new Notification.Action.Builder(stopIcon, getString(R.string.stop), stopPendingIntent).build();

        builder.addAction(stopAction);

        Bundle extras = new Bundle();
        extras.putLong(Constants.PARAM_DELAY, delay);
        extras.putBooleanArray(Constants.PARAM_REMINDERS, reminders);
        builder.addExtras(extras);

        // build notification
        final Notification notification = builder.build();
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;


        // start activity in foreground
        startForeground(Constants.NOTIFICATION_ID, notification);
        ((NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(Constants.NOTIFICATION_ID, notification);

        // now schedule alarm
        alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_BEEP), 0);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, delay, alarmIntent);

        alarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), alarmIntent);

        // create broadcast receiver to receive alarm
        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // now make it notify
                if(reminders[0]) v.vibrate(Constants.VIBRATION_TIME); // first entry is for vibration
                if(reminders[1]) beep.start(); // second entry is for beep
                if (reminders[2]) talk.speak(getTalkingString(), TextToSpeech.QUEUE_FLUSH, null, String.valueOf(this.hashCode()));//Log.d("mymessage", "text to be spoken: " + getTalkingString());
                    // talk.speak(getTalkingString(), TextToSpeech.QUEUE_FLUSH, null, String.valueOf(this.hashCode()));

                // now recreate alarm
                alarm.cancel(alarmIntent);
                alarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, alarmIntent);

            } // end public void onReceive
        }; // end anonymous BroadcastReceiver
        IntentFilter filter = new IntentFilter(Constants.ACTION_BEEP);
        registerReceiver(alarmReceiver, filter);

    } // end private void startReminders

    private void stopReminders() {
        // stops the entire service

        stopForeground(true);
        unregisterReceiver(alarmReceiver);
        alarm.cancel(alarmIntent);
        if(talk != null) {
            talk.stop();
            talk.shutdown();
        } // end if talk
        stopSelf();

    } // end private void stopReminders

    private int[] getHourSecMin(long delay) {
        int delayInSeconds = (int) delay / Constants.MILLISECONDS_IN_SECOND;

        int[] separatedDelay = new int[3];
        separatedDelay[0] = delayInSeconds / (Constants.SECONDS_IN_HOUR);
        separatedDelay[1] = (delayInSeconds % Constants.SECONDS_IN_HOUR) / Constants.SECONDS_IN_MINUTE;
        separatedDelay[2] = delayInSeconds % Constants.SECONDS_IN_MINUTE;

        return separatedDelay;

    } // end private int[] getHourSecMin

    private String getTimeString(long delay) {

        String timeString;
        String hourString;
        String minuteString;
        String secondString;

        int[] separatedDelay = getHourSecMin(delay);
        int hours = separatedDelay[0];
        int minutes = separatedDelay[1];
        int seconds = separatedDelay[2];

        if(hours == 1) hourString = getString(R.string.hour);
        else hourString = getString(R.string.hours);

        if(minutes == 1) minuteString = getString(R.string.minute);
        else minuteString = getString(R.string.minutes);

        if(seconds == 1) secondString = getString(R.string.second);
        else secondString = getString(R.string.seconds);

        if(hours == 0 && minutes == 0) timeString = String.format(Locale.getDefault(), "%d %s", seconds, secondString);
        else if(hours == 0 && seconds == 0) timeString = String.format(Locale.getDefault(), "%d %s", minutes, minuteString);
        else if(hours == 0) timeString = String.format(Locale.getDefault(), "%d %s %d %s", minutes,minuteString, seconds, secondString);
        else if(minutes == 0 && seconds == 0) timeString = String.format(Locale.getDefault(), "%d %s", hours, hourString);
        else if(minutes == 0) timeString = String.format(Locale.getDefault(), "%d %s %d %s", hours, hourString, seconds, secondString);
        else if(seconds == 0) timeString = String.format(Locale.getDefault(), "%d %s %d %s", hours, hourString, minutes, minuteString);
        else timeString = String.format(Locale.getDefault(), "%d %s %d %s %d %s", hours, hourString, minutes, minuteString, seconds, secondString);

        return timeString;

    } // end private String getTimeString

    private String getNotificationText(boolean[] reminders) {
        // returns text to display in the notification

        String text = "";
        if(reminders[0]) {
            text += getString(R.string.notification_text_vibrate);
            if(!reminders[1] && !reminders[2]) text += getString(R.string.space);
            else text += getString(R.string.and);
        } // end if reminders
        if(reminders[1]){
            text += getString(R.string.notification_text_beep);
            if(!reminders[2]) text += getString(R.string.space);
            else text += getString(R.string.and) + getString(R.string.notification_text_talk) + getString(R.string.space);
        } // end if reminders

        text += getString(R.string.every);

        return text;

    } // end private String getNotificationText

    private String getTalkingString() {
        // generates string to be read aloud

        Calendar c = Calendar.getInstance();

        int hours = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);

        String talkString = String.format(Locale.getDefault(), "%s ", getString(R.string.talk_init));

        // minutes
        if(minutes == 1) talkString += String.format(Locale.getDefault(), "%s %s", String.valueOf(minutes), getString(R.string.talk_minute_past));
        else if(minutes == 59) {
            hours += 1;
            talkString += String.format(Locale.getDefault(), "%s %s", String.valueOf(1), getString(R.string.talk_minute_to));
        } // end if hours
        else if(minutes % 15 == 0) {
            if(minutes == 30) talkString += String.format(Locale.getDefault(), "%s %s ", getString(R.string.talk_half), getString(R.string.talk_past));
            else if(minutes != 0) {
                talkString += String.format(Locale.getDefault(), "%s ", getString(R.string.talk_quarter));
                if (minutes > 30) {
                    hours += 1;
                    talkString += String.format(Locale.getDefault(), "%s ", getString(R.string.talk_to));
                } // end if minutes
                else talkString += String.format(Locale.getDefault(), "%s ", getString(R.string.talk_past));
            }
        } // end if minutes
        else if(minutes > 30) {
            hours += 1;
            talkString += String.format(Locale.getDefault(), "%s %s ", String.valueOf(30 - (minutes % 30)), getString(R.string.talk_minutes_to));
        } // end else if minutes
        else talkString += String.format(Locale.getDefault(), "%s %s ", String.valueOf(minutes), getString(R.string.talk_minutes_past));

        // hours
        if(hours == 0) talkString += String.format(Locale.getDefault(), "%s%s%s", String.valueOf(12), getString(R.string.talk_comma), getString(R.string.talk_am));
        else if(hours > 12) {
            hours -= 12;
            talkString += String.format(Locale.getDefault(), "%s%s%s", String.valueOf(hours), getString(R.string.talk_comma), getString(R.string.talk_pm));
        } // end if hours
        else talkString += String.format(Locale.getDefault(), "%s%s%s", String.valueOf(hours), getString(R.string.talk_comma),  getString(R.string.talk_am));

        return talkString;

    } // end private String getTalkingString

} // end public class AlarmService
