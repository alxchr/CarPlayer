package ru.abch.carplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class PlayService extends Service {
    static final String TAG = "PlayService";
    static String[] playList;
    boolean playing = false;
    static int nTrack = 0;
    File file;
    int fileLength;
    AFListener afListenerSound;
    EofHandler eofHandler;
    private MainActivity.JNIListener nlistener;
    public PlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(TAG, "onBind");
        return null;
    }
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        if (MainActivity.audioManager != null) {
            afListenerSound = new AFListener("Sound");
            MainActivity.audioManager.requestAudioFocus(afListenerSound, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        /*
        String samplerateString = null, buffersizeString = null;
        if (Build.VERSION.SDK_INT >= 17) {
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                afListenerSound = new AFListener("Sound");
                samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
                buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
                audioManager.requestAudioFocus(afListenerSound, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        }
        if (samplerateString == null) samplerateString = "48000";
        if (buffersizeString == null) buffersizeString = "480";
        int samplerate = Integer.parseInt(samplerateString);
        int buffersize = Integer.parseInt(buffersizeString);
        */
        eofHandler = new EofHandler();
        nlistener = new MainActivity.JNIListener() {
            @Override
            public void onAcceptMessage(String string) {
                Log.d(TAG, "From JNI "+ string);
                if (string.equals("EOF")){
                    eofHandler.sendEmptyMessage(0);
                }
            }

            @Override
            public void onAcceptMessageVal(int messVal) {

            }
        };
        MainActivity.nsubscribeListener(nlistener);
//        StartAudio(samplerate, buffersize);
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        String cmd =  intent.getStringExtra("command");
        String folder;
        Log.d(TAG, "onStartCommand " + cmd);
        if (cmd.equals("init")) {
            if (playing) TogglePlayback();
            nTrack = intent.getIntExtra("track_num", 0);
            folder = intent.getStringExtra("folder");
            playList = intent.getStringArrayExtra("playlist");
            Log.d(TAG, "Init folder = " + folder + " Track number = " + nTrack + " of " + playList.length + " tracks");
            playing = false;
        }
        if (cmd.equals("new_play")) {
            if (playing) TogglePlayback();
            nTrack = intent.getIntExtra("track_num", 0);
            folder = intent.getStringExtra("folder");
            playList = intent.getStringArrayExtra("playlist");
            Log.d(TAG, "Folder = " + folder + " Track number = " + nTrack + " of " + playList.length + " tracks");
            if (nTrack < 0 || nTrack >= playList.length) nTrack = 0;
            file = new File(playList[nTrack]);
            fileLength = (int)file.length();
            OpenFile(playList[nTrack],0, fileLength);
            TogglePlayback();
            playing = true;
            displayTrack(playList[nTrack]);
            nTrack++;
        }
        if (cmd.equals("play_track") && !(playList == null)) {
            if (playing) TogglePlayback();
            nTrack = intent.getIntExtra("track_num", 0);
            folder = intent.getStringExtra("folder");
            Log.d(TAG, "Folder = " + folder + " Track number = " + nTrack + " of " + playList.length + " tracks");
            if (nTrack < 0 || nTrack >= playList.length) nTrack = 0;
            file = new File(playList[nTrack]);
            fileLength = (int)file.length();
            OpenFile(playList[nTrack],0, fileLength);
            TogglePlayback();
            playing = true;
            displayTrack(playList[nTrack]);
            nTrack++;
        }
        if (cmd.equals("play") && ! playing) {
            playing = true;
            TogglePlayback();
        }
        if (cmd.equals("pause") && playing) {
            playing = false;
            TogglePlayback();
        }
        if (cmd.equals("rewind")) {
            if (playing) TogglePlayback();
            if (--nTrack < 0) nTrack = 0;
            file = new File(playList[nTrack]);
            fileLength = (int)file.length();
            OpenFile(playList[nTrack],0, fileLength);
            TogglePlayback();
            playing = true;
            displayTrack(playList[nTrack]);
            nTrack++;
        }
        if (cmd.equals("forward")) {
            if (playing) TogglePlayback();
            if (nTrack >= playList.length) nTrack = 0;
            file = new File(playList[nTrack]);
            fileLength = (int)file.length();
            OpenFile(playList[nTrack],0, fileLength);
            TogglePlayback();
            playing = true;
            displayTrack(playList[nTrack]);
            nTrack++;
        }

        return super.onStartCommand(intent, flags, startId);
    }
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        MainActivity.ndismissListener();
        Cleanup();
    }
    public static void playNextFile(){
        Log.d(TAG, "Track #" + nTrack);
        MainActivity.sPref.edit().putInt(MainActivity.spTrackNum, nTrack).apply();
        final String curTrack= playList[nTrack++];
        if (nTrack >= playList.length) nTrack = 0;
        Thread t = new Thread(new Runnable() {
            public void run() {
                String path = curTrack;
                File file=new File(path);
                int fileLength = (int)file.length();
                OpenFile(path, 0, fileLength);
                TogglePlayback();
//                playing = true;
            }
        });
        t.start();
        displayTrack(curTrack);
    }
    class AFListener implements AudioManager.OnAudioFocusChangeListener {

        String label = "";
//        MediaPlayer mp;

        public AFListener(String label) {
            this.label = label;
//            this.mp = mp;
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            String event = "";
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    event = "AUDIOFOCUS_LOSS";
                    if (playing) {
                        playing = false;
                        TogglePlayback();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    event = "AUDIOFOCUS_LOSS_TRANSIENT";
                    if (playing) {
                        playing = false;
                        TogglePlayback();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    event = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
                    if (playing) {
                        playing = false;
                        TogglePlayback();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    event = "AUDIOFOCUS_GAIN";
                    if (!playing) {
                        playing = true;
                        onForeground();
                        TogglePlayback();
                    }
                    break;
            }
            Log.d(TAG, label + " onAudioFocusChange: " + event);
        }
    }
    static void displayTrack(String path) {
        int lastIndex = path.lastIndexOf("/");
        MainActivity.tvPlayingTrack.setText(path.substring(lastIndex + 1));
    }
    /*
    public interface JNIListener {
        void onAcceptMessage(String string);

        void onAcceptMessageVal(int messVal);
    }
    */
    private static native void OpenFile(String path, int offset, int length);
    private native static void TogglePlayback();
//    private native void StartAudio(int samplerate, int buffersize);
//    private native void nsubscribeListener(JNIListener JNIListener);
//    private native void ndismissListener();
    private native void Cleanup();
    private native void onForeground();
//    private native void onBackground();

}
