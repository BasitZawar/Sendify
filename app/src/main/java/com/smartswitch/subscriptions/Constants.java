package com.smartswitch.subscriptions;

import android.content.Context;
import android.net.ConnectivityManager;

public class Constants {
    public static boolean isSubscribed = false;
    public static String previousPurchaseToken = "";
    public static int fragmentPosition = 5;
    public static boolean isDashboardInterLoaded = false;
    public static boolean isBackPressInterLoaded = false;

    public static String subPriceMonthly = "";
    public static String subPriceQuarterly = "";
    public static String subPriceYearly = "";

    public static String comeFrom = "";
    public static boolean isMovedToNext = false;

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

}
