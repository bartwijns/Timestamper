package com.barryprojects.timestamper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class TimerActivity extends Activity {

    // UI elements
    private TextView timeTxt;
    private Button startStopButton;
    private CheckBox vibrateBox;
    private CheckBox beepBox;
    private CheckBox talkBox;

    // app variables
    private boolean isRunning;
    private int seconds;
    private int minutes;
    private int hours;


    // overridden methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        // get UI elements
        timeTxt = (TextView) findViewById(R.id.txt_time);
        startStopButton = (Button) findViewById(R.id.button_start_stop);
        vibrateBox = (CheckBox) findViewById(R.id.check_vibrate);
        beepBox = (CheckBox) findViewById(R.id.check_beep);
        talkBox = (CheckBox) findViewById(R.id.check_talk);

        // check if reminder notification is active
        StatusBarNotification[] notifications = ((NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications();

        for(StatusBarNotification notification : notifications) {
            if(notification.getId() == Constants.NOTIFICATION_ID) {

                Notification actualNotification = notification.getNotification();
                Bundle params = actualNotification.extras;

                long delay = params.getLong(Constants.PARAM_DELAY);
                boolean[] reminders = params.getBooleanArray(Constants.PARAM_REMINDERS);

                applyParams(delay, reminders);

                break;

            } // end if notification.getId

        } // end for notification

        // get calling intent
        Intent callingIntent = getIntent();

        // check if activity was started from notification
        if(callingIntent.getLongExtra(Constants.PARAM_DELAY, 0) != 0) {

            isRunning = true;
            startAnimation();

        } // end if callingIntent
        else {

            isRunning = false;
            hours = Constants.DEFAULT_HOURS;
            minutes = Constants.DEFAULT_MINUTES;
            seconds = Constants.DEFAULT_SECONDS;
        } // end else

        // check if stop was clicked from notification
        if(!(callingIntent.getAction() == null) && callingIntent.getAction().equals(Constants.ACTION_STOP)) {
            stopReminders();
            stopAnimation();
            isRunning = false;
        } // end if callingIntent

        timeTxt.setText(getTimeString());
        timeTxt.setOnClickListener(new TimeEditListener());
        startStopButton.setOnClickListener(new StartStopListener());
        beepBox.setOnClickListener(new CheckBoxListener());
        talkBox.setOnClickListener(new CheckBoxListener());

    } // end protected void onCreate

    private void startReminders(long delay, boolean[] reminders) {

        Intent serviceIntent = new Intent(getApplicationContext(), AlarmService.class);
        serviceIntent.putExtra(Constants.PARAM_DELAY, delay);
        serviceIntent.putExtra(Constants.PARAM_REMINDERS, reminders);
        serviceIntent.setAction(Constants.ACTION_START);

        startService(serviceIntent);

    } // end private void startReminders

    private void stopReminders() {

        Intent serviceIntent = new Intent(getApplicationContext(), AlarmService.class);
        serviceIntent.setAction(Constants.ACTION_STOP);

        startService(serviceIntent);

    } // end private void stopReminders

    private long getDelayInMillis() {

        long total = (long) seconds * Constants.MILLISECONDS_IN_SECOND; // milliseconds in a second
        total += minutes * Constants.MILLISECONDS_IN_MINUTE; // milliseconds in a minute
        total += hours * Constants.MILLISECONDS_IN_HOUR; // milliseconds in an hour

        return total;

    } // end private Long getDelayInMillis

    private String getTimeString() {

        String timeString;
        String hourString;
        String minuteString;
        String secondString;

        if(hours == 1) hourString = getString(R.string.hour);
        else hourString = getString(R.string.hours);

        if(minutes == 1) minuteString = getString(R.string.minute);
        else minuteString = getString(R.string.minutes);

        if(seconds == 1) secondString = getString(R.string.second);
        else secondString = getString(R.string.seconds);

        timeString = String.format(Locale.getDefault(), "%d %s \n %d %s \n %d %s", hours, hourString, minutes, minuteString, seconds, secondString);

        return timeString;

    } // end private String getTimeString

    private void applyParams(long delay, boolean[] reminders) {

        // set delay timers
        int delayInSeconds = (int) delay / Constants.MILLISECONDS_IN_SECOND;
        hours = delayInSeconds / (Constants.SECONDS_IN_HOUR);
        minutes = (delayInSeconds % Constants.SECONDS_IN_HOUR) / Constants.SECONDS_IN_MINUTE;
        seconds = delayInSeconds % Constants.SECONDS_IN_MINUTE;

        // reset checkboxes to their original values
        // only check/uncheck if their original values differ from the default
        if(!reminders[0]) vibrateBox.setChecked(false);
        if(reminders[1]) beepBox.setChecked(true);
        if(reminders[2]) talkBox.setChecked(true);

        // also make checkboxes unclickable
        vibrateBox.setEnabled(false);
        beepBox.setEnabled(false);
        talkBox.setEnabled(false);

        // also disable timeTxt
        timeTxt.setEnabled(false);
    } // end private void applyParams

    // dialogs
    private void timeSetDlg(int oldHours, int oldMinutes, int oldSeconds) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        @SuppressLint("InflateParams")
        LinearLayout root = (LinearLayout) getLayoutInflater().inflate(R.layout.time_picker_dlg, null);

        builder.setTitle(R.string.time_dlg_title);
        builder.setView(root);

        // initialize UI of dialog
        final NumberPicker hourPicker = (NumberPicker) root.findViewById(R.id.picker_hour);
        final NumberPicker minutePicker = (NumberPicker) root.findViewById(R.id.picker_minute);
        final NumberPicker secondPicker = (NumberPicker) root.findViewById(R.id.picker_second);

        hourPicker.setMinValue(Constants.MIN_HOURS);
        hourPicker.setMaxValue(Constants.MAX_HOURS);
        hourPicker.setValue(oldHours);

        minutePicker.setMinValue(Constants.MIN_MINUTES);
        minutePicker.setMaxValue(Constants.MAX_MINUTES);
        minutePicker.setValue(oldMinutes);

        secondPicker.setMinValue(Constants.MIN_SECONDS);
        secondPicker.setMaxValue(Constants.MAX_SECONDS);
        secondPicker.setValue(oldSeconds);

        // set dialog buttons
        builder.setPositiveButton(R.string.button_ok, new AlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int id) {

                int newSeconds = secondPicker.getValue();
                int newMinutes = minutePicker.getValue();
                int newHours = hourPicker.getValue();

                if(newSeconds == 0 && newMinutes == 0 && newHours == 0) {
                    Toast.makeText(getApplicationContext(), R.string.err_zero_delay, Toast.LENGTH_SHORT).show();
                    return;

                } // end if newSeconds

                hours = newHours;
                minutes = newMinutes;
                seconds = newSeconds;

                timeTxt.setText(getTimeString());
                dlg.dismiss();

            } // end public void onClick
        }); // end anonymous OnClickListener

        builder.setNegativeButton(R.string.button_cancel, new AlertDialog.OnClickListener(){
            @Override
            public void onClick(DialogInterface dlg, int id) {
                dlg.dismiss();

            } // end public void onClick
        }); // end anonymous OnClickListener

        builder.create().show();

    } // end private void timeSetDlg

    private void startAnimation() {
        // start clock animation
        timeTxt.setBackgroundResource(R.drawable.active_clock_anim);
        AnimationDrawable clockAnim = (AnimationDrawable) timeTxt.getBackground();
        clockAnim.start();

        startStopButton.setText(R.string.button_stop);

        // also disable checkboxes
        vibrateBox.setEnabled(false);
        beepBox.setEnabled(false);
        talkBox.setEnabled(false);

        // and the time dialog
        timeTxt.setEnabled(false);
    } // end private void startAnimation

    private void stopAnimation() {
        // stop clock animation

        if(timeTxt.getBackground() instanceof AnimationDrawable) {
            AnimationDrawable clockAnim = (AnimationDrawable) timeTxt.getBackground();
            clockAnim.stop();
        } // end if timeTxt
        timeTxt.setBackgroundResource(R.drawable.clock);

        startStopButton.setText(R.string.button_start);

        // also make checkboxes clickable again
        vibrateBox.setEnabled(true);
        beepBox.setEnabled(true);
        talkBox.setEnabled(true);

        // also re-enable time dialog
        timeTxt.setEnabled(true);

    } // end private void stopAnimation


    // listeners
    private class TimeEditListener implements View.OnClickListener {
        // listener for the txtTime
        @Override
        public void onClick(View view) {
            timeSetDlg(hours, minutes, seconds);

        } // end public void onClick

    } // end private class TimeEditListener


    private class StartStopListener implements View.OnClickListener {
        // listener for startStopButton
        @Override
        public void onClick(View view) {
            if(isRunning) stopCounting();
            else startCounting();

        } // end public void onClick

        private void stopCounting() {
            // stop the counting
            isRunning = false;

            // stop fancy animation
            stopAnimation();

            // stop reminders
            stopReminders();

        } // end private void stopCounting

        private void startCounting() {
            // start the counting, unless no boxes are checked
            if(!vibrateBox.isChecked() && !beepBox.isChecked() && !talkBox.isChecked()) return;
            isRunning = true;

            // start fancy animation
            startAnimation();

            // start reminders
            boolean[] reminders = {vibrateBox.isChecked(), beepBox.isChecked(), talkBox.isChecked() };
            startReminders(getDelayInMillis(), reminders);

        } // end private void startCounting

    } // end private class StartStopListener

    private class CheckBoxListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if(beepBox.isChecked() && talkBox.isChecked()) Toast.makeText(getApplicationContext(), R.string.err_beep_and_talk, Toast.LENGTH_SHORT).show();

        } // end public void onClick

    } // end private class CheckBoxListener


} // end public class TimerActivity
