package com.smartswitch.ads.inter_ads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.smartswitch.R;

public class InterstitialClass {
    static InterstitialAd mInterstitialAd;
    static Context mContext;
    static Activity mActivity;
    static String mInterstitialID;
    static String logTag = "Ads_";
    static ActionOnAdClosedListener mActionOnAdClosedListener;
    static boolean isAdDecided = false;
    static int DELAY_TIME = 0;
    public static Boolean isInterstitalIsShowing = false;

    static boolean stopInterstitial = false;
    static boolean timerCalled = false;

    public static void request_interstitial(Context context, Activity activity, String interstitial_id, ActionOnAdClosedListener actionOnAdClosedListenersm) {
        mContext = context;
        mActivity = activity;
        mInterstitialID = interstitial_id;
        mActionOnAdClosedListener = actionOnAdClosedListenersm;
        isAdDecided = false;

        Log.d("awaiskhan","request_interstitial");


        if (!isInternetAvailable(mContext)) {
            performAction();
            return;
        }

        if (AdTimerClass.isEligibleForAd()) {
            load_interstitial();
        } else {
            Log.d("interstitial", "else Ad Timer Eligible : " + AdTimerClass.isEligibleForAd());
            performAction();
        }


//        if (AdTimerClass.isEligibleForAd()) {
//            load_interstitial();
//        } else {
//            performAction();
//        }
    }

    public static void load_interstitial() {
        if (mInterstitialAd == null) {
            showAdDialog();

            stopInterstitial = false;
            timerCalled = false;
            AdRequest adRequest_interstitial = new AdRequest.Builder().build();
            InterstitialAd.load(mContext, mInterstitialID, adRequest_interstitial,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            // The mInterstitialAd reference will be null until
                            // an ad is loaded.
                            mInterstitialAd = interstitialAd;
                            isAdDecided = true;
                            Log.d(logTag, "Insterstitial Loaded.");

                            if (!timerCalled) {
                                closeAdDialog();
                                show_interstitial();
                            }


                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error
                            Log.d(logTag, "Interstitial Failed to Load." + loadAdError.getMessage());
                            mInterstitialAd = null;
                            isAdDecided = true;
                            if (!timerCalled) {
                                closeAdDialog();
                                performAction();
                            }

                        }
                    });
            timerAdDecided();
        } else {
            Log.d(logTag, "Ad was already loaded.: ");
            stopInterstitial = false;
            showAdDialog();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    closeAdDialog();
                    show_interstitial();
                }
            }, 2000);
        }


    }

    static void timerAdDecided() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAdDecided) {
                    stopInterstitial = true;
                    timerCalled = true;
                    Log.d(logTag, "Handler Cancel.");
                    AdTimerClass.cancelTimer();
                    closeAdDialog();
                    show_interstitial();
                }
            }
        }, 5000);
    }

    static ProgressDialog progressDialog;
    static AlertDialog alertDialog;

    /**
     * The WindowLeaked exception you're encountering when we change mobile theme during dialog box
     */
    static void showAdDialog() {
        if (mActivity == null || mActivity.isFinishing() || mActivity.isDestroyed()) {
            return;
        }
        isInterstitalIsShowing = true;

        try {
            if (Build.VERSION.SDK_INT >= 21) {
                progressDialog = new ProgressDialog(mContext);
                progressDialog.setTitle(mContext.getString(R.string.please_wait));
                progressDialog.setMessage(mContext.getString(R.string.add_is_expected));
                progressDialog.setCancelable(false);
                progressDialog.create();
                Log.d("awais", "Show Dialog");
                progressDialog.show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(mContext.getString(R.string.please_wait));
                builder.setMessage(mContext.getString(R.string.add_is_expected));
                alertDialog = builder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        } catch (WindowManager.BadTokenException | IllegalStateException e) {
            Log.e(logTag, "Error showing dialog: " + e.getMessage());
        }
    }

    public static void closeAdDialog() {
        isInterstitalIsShowing = false;
        if (mActivity != null && !mActivity.isFinishing()) {
            try {
                if (Build.VERSION.SDK_INT >= 24) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        Log.d("awais", "Close Dialog");
                        progressDialog.dismiss();
                    }
                } else {
                    if (alertDialog != null && alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Log the error to avoid a crash
                Log.e("closeAdDialog", "Failed to dismiss dialog: " + e.getMessage());
            }
        }
    }


    static void show_interstitial() {
        if (mInterstitialAd != null && stopInterstitial == false) {
            isInterstitalIsShowing = true;
            mInterstitialAd.show(mActivity);
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    Log.d(logTag, "Insterstitial Failed to Show.");
                    mInterstitialAd = null;
                    isInterstitalIsShowing = false;
                    performAction();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    Log.d(logTag, "Insterstitial Shown.");
                    mInterstitialAd = null;
                    isInterstitalIsShowing = false;
                    performAction();
                }
            });
        } else {
            performAction();
        }
    }

    static void performAction() {
        isInterstitalIsShowing = false;
        mActionOnAdClosedListener.ActionAfterAd();
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        }
        return false;
    }

}
