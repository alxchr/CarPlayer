package ru.abch.carplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

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
    private static final int ID_SERVICE = 102;
    public PlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(TAG, "onBind");
        return null;
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "PlayService_channelid";
        String channelName = "Play Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(ID_SERVICE, notification);
        if (MainActivity.audioManager != null) {
            afListenerSound = new AFListener("Sound");
//            MainActivity.audioManager.requestAudioFocus(afListenerSound, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
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
            MainActivity.audioManager.requestAudioFocus(afListenerSound, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
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
            MainActivity.audioManager.requestAudioFocus(afListenerSound, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
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
            MainActivity.audioManager.requestAudioFocus(afListenerSound, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            TogglePlayback();
        }
        if (cmd.equals("pause") && playing) {
            playing = false;
            MainActivity.audioManager.abandonAudioFocus(afListenerSound);
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
        MainActivity.audioManager.abandonAudioFocus(afListenerSound);
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
