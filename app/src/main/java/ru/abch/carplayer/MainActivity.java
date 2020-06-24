package ru.abch.carplayer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import ru.abch.carplayer.dummy.DummyContent;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, PlayFragment.OnFragmentInteractionListener,
        ItemFragment.OnListFragmentInteractionListener, SettingsFragment.OnFragmentInteractionListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    final int MY_PERMISSIONS_REQUEST_USB_HOST = 2;
    final int SHOW_PLAYLIST = 0;
    final int SHOW_FILES = 1;
    final int SHOW_SETTINGS = 2;
    int showFragment = SHOW_PLAYLIST;
    public static String rootPlayList;
    static String TAG = "Main Activity";
    final String spFolder = "FOLDER";
    public static final String spTrackNum = "TRACK_NUM";
    public static final String spUsbDirect = "USB_DIRECT";
//    public static final String spUsbVID = "USB_VID";
//    public static final String spUsbPID = "USB_PID";
    public static boolean directMode;
    public static String musicRoot= Environment.getExternalStorageDirectory().getPath();
//    public static String musicRoot= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
    static PlayList pList = new PlayList();
    static int playListSize;
    boolean accessGranted = false, mute = false;
    static boolean playing = false;
    static ImageButton folder, playPause, bMute;
    ItemFragment fileBrowser;
    PlayFragment player;
    SettingsFragment settings;
    FragmentTransaction fTrans;
    public static SharedPreferences sPref;
    static int nTrack = 0;
    boolean newPlayList = true;
    static PlayFragment pFrag;
    String[] playListArray;
    Intent startIntent;
    static Context context;
    public static AudioManager audioManager;
    public static TextView tvPlayingTrack;

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    public void onClick(View v) {
        startIntent = new Intent(this, PlayService.class);
        fTrans = getSupportFragmentManager().beginTransaction();
        switch (v.getId()) {
/*
            case R.id.vol_mute:
                // кнопка mute
                Log.d(TAG, "Mute");
                mute = !mute;
                if (mute) {
                    bMute.setImageDrawable(getResources().getDrawable(R.mipmap.icons8_voice_96));
                } else {
                    bMute.setImageDrawable(getResources().getDrawable(R.mipmap.icons8_mute_96));
                }
                break;
*/
            case R.id.folder:
                // кнопка folder
                Log.d(TAG, "Folder");
                fTrans.replace(R.id.fragment_placeholder, fileBrowser).commit();
                newPlayList = true;
                if (playing) {
                    playing = false;
                    startIntent.putExtra("command", "pause");
                    playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_48dp));
                    nTrack = 0;
                    startService(startIntent);
                }
                showFragment = SHOW_FILES;
                break;

            case R.id.forward:
                // кнопка forward
                Log.d(TAG, "Forward");
                startIntent.putExtra("command", "forward");
                playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_48dp));
                startService(startIntent);
                break;
/*
            case R.id.shuffle:
                // кнопка forward
                Log.d(TAG, "Shuffle");
                break;
*/
            case R.id.play_pause:
                // кнопка play_pause
                if (newPlayList) {
                    playListSize = pList.init(rootPlayList, nTrack);
                    playListArray = new String [playListSize];
                    playListArray = pList.get().toArray(playListArray);
                    Log.d(TAG, "Playing folder " + rootPlayList + " contains " + playListSize + " tracks. Start at " + nTrack);
                    startIntent.putExtra("folder",rootPlayList);
                    startIntent.putExtra("playlist", playListArray);
                    startIntent.putExtra("track_num", nTrack);
                    startIntent.putExtra("command", "new_play");
                    fTrans.replace(R.id.fragment_placeholder, player).commit();
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putString(spFolder, rootPlayList).apply();
                    newPlayList = false;
                    playing = true;
                    startService(startIntent);
                } else {
                    if (playing) {
                        startIntent.putExtra("command", "pause");
                        playing = false;
                    } else {
                        startIntent.putExtra("command", "play");
                        playing = true;
                    }
                    startService(startIntent);
                }
                if (playing) {
                    Log.d(TAG, "Play");
                    playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_48dp));
                } else {
                    Log.d(TAG, "Pause");
                    playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_48dp));
                }
                break;

            case R.id.rewind:
                // кнопка rewind
                Log.d(TAG, "Rewind");
                playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_48dp));
                startIntent.putExtra("command", "rewind");
                startService(startIntent);
                break;

            case R.id.settings:
                // кнопка settings
                Log.d(TAG, "Settings");
                fTrans.replace(R.id.fragment_placeholder, settings).commit();
                showFragment = SHOW_SETTINGS;
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    playListSize = pList.init(rootPlayList, 0);
                    fTrans = getSupportFragmentManager().beginTransaction();
                    fTrans.replace(R.id.fragment_placeholder, player).commitAllowingStateLoss();
                    accessGranted = true;
                } else {
                    finish();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        setContentView(R.layout.activity_main);
        folder = findViewById(R.id.folder);
        playPause = findViewById(R.id.play_pause);
//        bMute = findViewById(R.id.vol_mute);
        fileBrowser = new ItemFragment();
        player = new PlayFragment();
        settings = new SettingsFragment();
        sPref = getPreferences(MODE_PRIVATE);
        nTrack = sPref.getInt(spTrackNum,0);
        rootPlayList = sPref.getString(spFolder, musicRoot);
        Log.d(TAG, "Saved folder = " + rootPlayList + " track # " + nTrack);
        directMode = sPref.getBoolean(spUsbDirect,true);
        tvPlayingTrack = findViewById(R.id.tv_playing_track);

        String samplerateString = null, buffersizeString = null;
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString == null) samplerateString = "48000";
        if (buffersizeString == null) buffersizeString = "480";
        int samplerate = Integer.parseInt(samplerateString);
        int buffersize = Integer.parseInt(buffersizeString);
        StartAudio(samplerate, buffersize);
        if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            playListSize = pList.init(rootPlayList, 0);
            playListArray = new String [playListSize];
            playListArray = pList.get().toArray(playListArray);
            fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.fragment_placeholder, player).commit();
            pFrag = player;
            startIntent = new Intent(this, PlayService.class);
            startIntent.putExtra("folder",rootPlayList);
            startIntent.putExtra("playlist", playListArray);
            startIntent.putExtra("track_num", nTrack);
            startIntent.putExtra("command", "init");
            startService(startIntent);
        }

        if (directMode) {
            Log.d(TAG,"Use ALSA");

        } else {
            Log.d(TAG,"Use Android Audio Manager");
        }
    }

    @Override
    public void onPause() {

        super.onPause();
        onBackground();
    }

    @Override
    public void onResume() {
        super.onResume();
        onForeground();
    }

    @Override
    protected void onDestroy() {
//        unregisterReceiver(usbReceiver);
        super.onDestroy();
        Cleanup();

    }
    @Override
    public void onBackPressed() {
        switch (showFragment) {
            case SHOW_PLAYLIST:
                super.onBackPressed();
                break;
            case SHOW_FILES:
                fTrans = getSupportFragmentManager().beginTransaction();
                fTrans.replace(R.id.fragment_placeholder, player).commit();
                showFragment = SHOW_PLAYLIST;
                break;
            case SHOW_SETTINGS:
                fTrans = getSupportFragmentManager().beginTransaction();
                fTrans.replace(R.id.fragment_placeholder, player).commit();
                showFragment = SHOW_PLAYLIST;
                break;
            default:
                break;
        }
    }
    public static void playButton(){
        playPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play_arrow_black_48dp));
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private native void StartAudio(int samplerate, int buffersize);
//    private static native void OpenFile(String path, int offset, int length);
//    public native static void TogglePlayback();
    public static native void nsubscribeListener(JNIListener JNIListener);
    public static native void ndismissListener();
    private native void Cleanup();
    private native void onForeground();
    private native void onBackground();
//    private native int onConnect(int deviceID, int fd, byte[] rawDescriptor);

    public interface JNIListener {
        void onAcceptMessage(String string);

        void onAcceptMessageVal(int messVal);
    }
}
