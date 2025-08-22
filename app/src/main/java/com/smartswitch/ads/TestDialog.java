package com.smartswitch.ads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;

public class TestDialog {

    private static final String TAG = TestDialog.class.getSimpleName();

    private final WeakReference<Activity> mActivityRef;
    private AlertDialog alertDialog;

    public TestDialog(Activity activity) {
        this.mActivityRef = new WeakReference<>(activity);
    }

    public void showDialog() {
        Activity activity = mActivityRef.get();

        if (activity == null || activity.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
            Log.w(TAG, "Cannot show dialog: activity is null, finishing, or destroyed");
            return;
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            return; // Already showing
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Title");
        builder.setMessage("Message");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Dialog OK clicked");
            }
        });

        alertDialog = builder.create();
        try {
            alertDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + e.getMessage());
        }
    }

    public void dismissDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            try {
                alertDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Failed to dismiss dialog: " + e.getMessage());
            } finally {
                alertDialog = null;
            }
        }
    }
}
