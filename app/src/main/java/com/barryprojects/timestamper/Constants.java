package com.barryprojects.timestamper;


class Constants {

    // contains all kinds of used constants

    static final int MIN_HOURS = 0;
    static final int MAX_HOURS = 10;
    static final int DEFAULT_HOURS = 0;
    static final int MIN_MINUTES = 0;
    static final int MAX_MINUTES = 59;
    static final int DEFAULT_MINUTES = 5;
    static final int MIN_SECONDS = 0;
    static final int MAX_SECONDS = 59;
    static final int DEFAULT_SECONDS = 0;

    static final int VIBRATION_TIME = 1000;

    private static final int MINUTES_IN_HOUR = 60;
    static final int SECONDS_IN_MINUTE = 60;
    static final int MILLISECONDS_IN_SECOND = 1000;
    static final int SECONDS_IN_HOUR = MINUTES_IN_HOUR * SECONDS_IN_MINUTE;
    static final int MILLISECONDS_IN_MINUTE = MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE;
    static final int MILLISECONDS_IN_HOUR = MILLISECONDS_IN_SECOND * SECONDS_IN_HOUR;

    static final String ACTION_START = "com.barryprojects.timestamper.action.START";
    static final String ACTION_STOP = "com.barryprojects.timestamper.action.STOP";
    static final String ACTION_BEEP = "com.barryprojects.timestamper.action_BEEP";

    static final String PARAM_DELAY = "com.barryprojects.timestamper.param_DELAY";
    static final String PARAM_REMINDERS = "com.barryprojects.timestamper.param_REMINDERS";

    static final int NOTIFICATION_ID = 1;


} //  end public class Constants
