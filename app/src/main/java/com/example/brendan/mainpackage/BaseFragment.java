package com.example.brendan.mainpackage;


import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;

/**
 * Base Fragment class for all application Fragments
 */
public class BaseFragment extends Fragment {


    private ProgressDialog dialog;
    Handler handler;
    Thread t;

    /**
     * Shows ProgressDialog while information is being loaded.
     */
    public void showDialog() {
        if(dialog == null){
            dialog = new ProgressDialog(getContext());
            dialog.setMessage("Loading Data...");
        }
        handler = new Handler(Looper.getMainLooper()){

            @Override
            public void handleMessage(Message msg) {
                t = new Thread(){

                    @Override
                    public void run() {
                        if(dialog != null && !dialog.isShowing()) {
                            dialog.show();
                        }
                    }
                };
                t.start();
            }
        };



    }

    /**
     * Hides ProgressDialog  when information is done loading.
     */
    public void hideDialog() {
        dialog.dismiss();
        t.stop();
    }
}
