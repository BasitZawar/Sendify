package com.smartswitch.ads.inter_ads;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class AdTimerClass {
    static int counter = 0;
    static boolean isFirstTimeClicked = true;
    static TimerTask myTimer;

    public static boolean isEligibleForAd() {

        if (isFirstTimeClicked) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    myTimer = this;
                    if (counter >= 3) {
                        cancelTimer();
                    }else{
                        isFirstTimeClicked = false;
                    }
                    counter++;
                    Log.d("Ads_", ": Counter  1 second: " + counter);
                }
            }, 0, 500);
        }
        Log.d("Ads_", ": Counter 2 second: " + counter);

        return isFirstTimeClicked;
    }

    public static void cancelTimer(){
        counter = -1;
        isFirstTimeClicked = true;
        myTimer.cancel();
    }
}
