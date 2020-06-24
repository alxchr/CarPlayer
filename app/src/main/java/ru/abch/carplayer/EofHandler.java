package ru.abch.carplayer;

import android.os.Handler;
import android.util.Log;

import static ru.abch.carplayer.PlayService.playNextFile;

public class EofHandler extends Handler {
    private String TAG = "EOF Handler";
    public void handleMessage(android.os.Message msg) {
        if (msg.what == 0) {
            Log.d(TAG, "Handle eof");
            playNextFile();
        }
    }
}
