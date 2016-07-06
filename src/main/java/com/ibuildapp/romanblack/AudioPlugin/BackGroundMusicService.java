/****************************************************************************
*                                                                           *
*  Copyright (C) 2014-2015 iBuildApp, Inc. ( http://ibuildapp.com )         *
*                                                                           *
*  This file is part of iBuildApp.                                          *
*                                                                           *
*  This Source Code Form is subject to the terms of the iBuildApp License.  *
*  You can obtain one at http://ibuildapp.com/license/                      *
*                                                                           *
****************************************************************************/
package com.ibuildapp.romanblack.AudioPlugin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.*;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
//import com.flurry.android.FlurryAgent;
import com.ibuildapp.romanblack.AudioPlugin.callback.ServiceManageInterface;
import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
import com.ibuildapp.romanblack.AudioPlugin.utils.SoundCloudLinkResolver;
import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements service that plays audio in background.
 */
public class BackGroundMusicService extends Service implements OnPreparedListener,
        OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
        OnInfoListener, PlayerCallback, ServiceManageInterface {

    public static final int POSITION_UNDEFINED = -1;
    static final int PROGRESS_UNDEFINED = -1;
    static final int STREAM_PROXY_ERROR = 2;
    static final int FOUND_STREAM_CONTENT_TYPE = 4;
    static final int SHOW_ERROR = 5;
    private static int release_count = 0;
    private final String logname = "BackGroundMusicService";
    private final String PENDING_EXTRA_ACTION = "action";
    private final String PENDING_PARAMETER_PLAY = "play";
    private final String PENDING_PARAMETER_PREV = "prev";
    private final String PENDING_PARAMETER_NEXT = "next";
    private MediaPlayerStates playerState = MediaPlayerStates.PLAYER_STOP;
    private int position = 0;
    private int startPosition = POSITION_UNDEFINED;
    private int endPosition = POSITION_UNDEFINED;
    private boolean isPrepared = false;
    private boolean notificationShown = false;
    private boolean buffered;
    private String userID = null;
    private ContentTypes contentType = ContentTypes.UNDEFINED;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private MultiPlayer multiPlayer = new MultiPlayer();
    private StreamProxy proxy = null;
    private volatile boolean isAwait = false;
    private TelephonyManager telephonyManager = null;
    private ArrayList<AudioItem> items = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STREAM_PROXY_ERROR: { // stay here
                    Log.d(logname, "STREAM_PROXY_ERROR");

                    if (notificationShown) {
                        removeNotification();
                    }

                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                handler.sendEmptyMessage(SHOW_ERROR);
                                playerState = MediaPlayerStates.PLAYER_STOP;
                                Statics.error();
                                if (proxy != null) {
                                    proxy.stop();
                                    proxy = null;
                                }
                                if (mediaPlayer != null) {
                                    mediaPlayer.release();
                                }
                            } catch (Exception ex) {
                            }

                            setUnplaying();
                            playerState = MediaPlayerStates.PLAYER_STOP;
                            Statics.error();
                        }
                    }.start();
                }
                break;
                case FOUND_STREAM_CONTENT_TYPE: { //stay here
                    Log.d(logname, "FOUND_STREAM_CONTENT_TYPE");
                    new Thread() {
                        @Override
                        public void run() {
                            checkStreamType();
                            if (proxy != null) {
                                proxy.stop();
                                proxy = null;
                            }
                        }
                    }.start();
                }
                break;
                case SHOW_ERROR: {
                    Toast.makeText(BackGroundMusicService.this,
                            R.string.romanblack_audio_alert_cannot_open_stream,
                            Toast.LENGTH_LONG).show();
                 }
                break;
            }

            super.handleMessage(msg);
        }
    };
    private MediaPlayer mediaPlayer1;

    @Override
    public IBinder onBind(Intent arg0) {
        Log.e("ROMAN", "onBind");
        return new BackGroundMusicAIDLImpl();
    }

    @Override
    public void onDestroy() {
        stopMusic();
        Statics.serviceManageInterface = null;
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    try {

                        switch (state) {
                            case TelephonyManager.CALL_STATE_IDLE:
                                if (playerState == MediaPlayerStates.PLAYER_PLAY) {
                                    if (contentType == ContentTypes.FILE || contentType == ContentTypes.STREAM_MP3) {
                                        System.gc();
                                        mediaPlayer.start();
                                        playerState = MediaPlayerStates.PLAYER_PLAY;
                                    } else if (contentType == ContentTypes.STREAM) {
                                        init();
                                        playerState = MediaPlayerStates.PLAYER_PLAY;
                                    }
                                }
                                break;
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                Log.d("DEBUG", "OFFHOOK");
                                break;
                            case TelephonyManager.CALL_STATE_RINGING:
                                if (playerState == MediaPlayerStates.PLAYER_PLAY) {
                                    if (contentType == ContentTypes.FILE || contentType == ContentTypes.STREAM_MP3) {
                                        mediaPlayer.pause();
                                    } else if (contentType == ContentTypes.STREAM) {
                                        stopMusic();
                                    }
                                }
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(logname, "", e);
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);

            Statics.serviceManageInterface = this;
            Statics.onServiceStarted();
        } catch (Throwable e) {
            Log.e(logname, "", e);
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String packageName = getPackageName();
            String lastPart = packageName.substring(packageName.lastIndexOf(".") + 1);
            String uId = lastPart.substring(lastPart.indexOf("u") + 1, lastPart.indexOf("p"));
            userID = uId;
        } catch (Throwable thr) {
        }

        String action = null;
        try {
            action = intent.getStringExtra(PENDING_EXTRA_ACTION);
        } catch (NullPointerException nPEx) {
        }

        if (action != null) {
            if (action.equals(PENDING_PARAMETER_PLAY)) {
                if (playerState == MediaPlayerStates.PLAYER_PLAY) {
                    pause();
                    setNotification();
                } else if (playerState == MediaPlayerStates.PLAYER_PAUSE) {
                    play();
                    setNotification();
                }
            } else if (action.equals(PENDING_PARAMETER_PREV)) {
                if (playerState != MediaPlayerStates.PLAYER_STOP) {
                    prev();
                    setNotification();
                }
            } else if (action.equals(PENDING_PARAMETER_NEXT)) {
                if (playerState != MediaPlayerStates.PLAYER_STOP) {
                    next();
                    setNotification();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Checks type of the audio stream and chose player depending on it.
     */
    private void checkStreamType() {
        Log.d(logname, "checkStreamType");
        try {
            if (items.get(position).getUrl().contains("soundcloud.com")) {
                Uri url = Uri.parse(items.get(position).getUrl());
                url.getQueryParameter("url");
                String streamUrl = "";
                try {
                    streamUrl = SoundCloudLinkResolver.resolveLink(items.get(position).getUrl());
                } catch (Exception e) {
                    handler.sendEmptyMessage(STREAM_PROXY_ERROR);
                    return;
                }

                contentType = ContentTypes.FILE;
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnBufferingUpdateListener(this);
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnInfoListener(this);
                mediaPlayer.setOnPreparedListener(this);

                mediaPlayer.reset();
                mediaPlayer.setDataSource(streamUrl);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepareAsync();
            } else if (proxy!= null && proxy.getContentLength() > 0) {
                contentType = ContentTypes.FILE;

                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnBufferingUpdateListener(this);
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnInfoListener(this);
                mediaPlayer.setOnPreparedListener(this);

                mediaPlayer.reset();
                mediaPlayer.setDataSource(items.get(position).getUrl());
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepareAsync();
            } else if (proxy!= null && (proxy.getContentType().contains("mpeg") || Build.VERSION.SDK_INT >= 16)) {
                contentType = ContentTypes.STREAM_MP3;//streamType = "file";

                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnBufferingUpdateListener(this);
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnInfoListener(this);
                mediaPlayer.setOnPreparedListener(this);

                mediaPlayer.reset();
                mediaPlayer.setDataSource(items.get(position).getUrl());
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepareAsync();
            } else {
                contentType = ContentTypes.STREAM;

                multiPlayer = new MultiPlayer();
                multiPlayer.setPlayerCallback(this);
                multiPlayer.playAsync(items.get(position).getUrl());
            }

        } catch (Exception e) {
            Log.e(logname, "", e);
            Statics.musicPaused(items.get(position));
            stop();
        }
    }

    /**
     * Starts checking audio stream type.
     */
    private void init() {
        try {
            playerState = MediaPlayerStates.PLAYER_INIT;
            setPlaying(position);
            Statics.initializing();

            if (items.get(position).getUrl() == null) {
                handler.sendEmptyMessage(STREAM_PROXY_ERROR);
                Statics.error();
                return;
            }

            if (items.get(position).getUrl().equals("")) {
                handler.sendEmptyMessage(STREAM_PROXY_ERROR);
                Statics.error();
                return;
            }

            if (items.get(position).getUrl().contains("soundcloud.com")) {
                handler.sendEmptyMessage(FOUND_STREAM_CONTENT_TYPE);
                return;
            }

            if (mediaPlayer1!= null){
                mediaPlayer1.stop();
                mediaPlayer1.release();
                mediaPlayer1 = null;
            }
            if (proxy != null){
                proxy.stop();
            }

            proxy = new StreamProxy();
            proxy.init(handler);
            proxy.start();
            String proxyUrl = String.format("http://127.0.0.1:%d/%s", proxy.getPort(), items.get(position).getUrl());
            mediaPlayer1 = new MediaPlayer();
            isAwait = false;
            mediaPlayer1.setOnPreparedListener(new OnPreparedListener() {
                public void onPrepared(MediaPlayer arg0) {
                    proxy.stop();
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                    mediaPlayer1.release();
                    mediaPlayer1.start();
                }
            });

            System.gc();
            mediaPlayer1.reset();
            mediaPlayer1.setDataSource(proxyUrl);
            mediaPlayer1.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer1.prepareAsync();
            // попробуем заблочить этот тред чтобы успел отработать другой
            // во избежании ошибки HeapWorker
            if (android.os.Build.VERSION.SDK_INT <= 10) {
                Thread.sleep(2000);
                System.gc();
                Log.e("BackGroundMusicService", "sleeeeeeeeeep!");
            }
        } catch (Exception e) {
            playerState = MediaPlayerStates.PLAYER_STOP;

            handler.sendEmptyMessage(STREAM_PROXY_ERROR);
        }
    }

    /**
     * Stops audio if it is playing now.
     */
    private void stopMusic() {
        try {
            if (userID != null && userID.equals("186589")) {
                //FlurryAgent.endTimedEvent("AudioPlugin");
            }
        } catch (Exception e) {
            Log.e("STOP", "mediaPlayer release error");
        }

        Log.d(logname, "stopMusic");
        try {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(logname, "mediaPlayer release error");
            }

            try {
                multiPlayer.stop();
            } catch (Exception e) {
                Log.e(logname, "multiPlayer stop error");
            }

            release_count++;
            Log.e(logname, "release_count = " + Integer.toString(release_count));
        } catch (Exception e) {
        }

        buffered = false;
    }

    /**
     * Starts mp3 audio player.
     */
    private void playMp3() {
        if (isAwait = false) {
            isAwait = true;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.gc();
        mediaPlayer.start();
        playerState = MediaPlayerStates.PLAYER_PLAY;
    }

    /**
     * Stops mp3 audio player.
     */
    private void stopMp3() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            buffered = false;
        }
    }

    /**
     * Marks all audios uplaying.
     */
    private void setUnplaying() {
        if(items != null)
            for (int i = 0; i < items.size(); i++)
                if(items.get(i) != null)
                    items.get(i).setPlaying(false);
    }

    /**
     * Marks audio at given position playing.
     * @param pos audio position
     */
    private void setPlaying(int pos) {
        items.get(pos).setPlaying(true);
    }

    /**
     * Decodes image file to bitmap from device external storage.
     * @param imagePath image file path
     * @return decoded image bitmap
     */
    private Bitmap decodeImageFile(String imagePath) {
        try {
            File file = new File(imagePath);
            //Decode image size
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(file), null, opts);

            //Find the correct scale value. It should be the power of 2.
            int width = opts.outWidth, height = opts.outHeight;
            int scale = 1;
            while (true) {
                if (width / 2 < 40 || height / 2 < 40) {
                    break;
                }
                width /= 2;
                height /= 2;
                scale *= 2;
            }

            //Decode with inSampleSize
            opts = new BitmapFactory.Options();
            opts.inSampleSize = scale;

            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, opts);

            int x = 0, y = 0, l = 0;
            if (width > height) {
                x = (int) (width - height) / 2;
                y = 1;
                l = height - 1;
            } else {
                x = 1;
                y = (int) (height - width) / 2;
                l = width - 1;
            }

            float matrixWidth = (float) (40) / (float) l;
            float matrixHeight = (float) (40) / (float) l;
            Matrix matrix = new Matrix();
            matrix.postScale(matrixWidth, matrixHeight);

            return Bitmap.createBitmap(bitmap, x, y, l, l, matrix, true);
        } catch (Exception e) {
            Log.d("", "");
        }

        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(logname, "onPrepared");
        
        if(startPosition != POSITION_UNDEFINED && endPosition != POSITION_UNDEFINED){
            isPrepared = true;
            playerPlay();
            playerState = MediaPlayerStates.PLAYER_PLAY;
            setPlaying(position);
            Statics.musicStarted();
            setPlaying(position);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(logname, "onError" + what + " " + extra);
        try {

            handler.sendEmptyMessage(SHOW_ERROR);
            stopMp3();
            Statics.error();

            playerState = MediaPlayerStates.PLAYER_STOP;
            setUnplaying();
            removeNotification();
            if (endPosition != POSITION_UNDEFINED) {
                if (position < endPosition) {
                    next();
                }
                return true;
            }

            if (position < items.size() - 1) {
                position++;
                Statics.positionChanged(position);
                play();
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int progress) {
        //-1638400000
        if((progress < Integer.MIN_VALUE / 2 || progress > 50 || progress == 0) && isPrepared && !buffered && playerState == MediaPlayerStates.PLAYER_INIT) {
            buffered = true;
            playerPlay();
            playerState = MediaPlayerStates.PLAYER_PLAY;
            setPlaying(position);
            Statics.musicStarted();
            setPlaying(position);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        System.gc();
        return false;
    }

    /* INTERFACE IMPLEMENTATION */
    /* MediaPlayer */
    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMp3();
        isPrepared = false;
        setUnplaying();

        if (endPosition != POSITION_UNDEFINED) {
            if (position < endPosition) {
                next();
            } else {
                Statics.musicPaused(items.get(position));
                stop();
            }
            return;
        }

        if (position < items.size() - 1) {
            play();
            next();
        } else {
            if (notificationShown) {
                removeNotification();
            }
        }
    }

    public void playerStarted() {
        setPlaying(position);
        Statics.musicStarted();
        playerState = MediaPlayerStates.PLAYER_PLAY;
        setPlaying(position);
    }

    public void playerPCMFeedBuffer(boolean bln, int i, int i1) {
    }

    public void playerStopped(int i) {
        stopMusic();
        isPrepared = false;
        setUnplaying();

        if (endPosition != POSITION_UNDEFINED) {
            if (position < endPosition) {
                next();
            }

            return;
        }
    }

    public void playerException(Throwable thrwbl) {
        try {
            handler.sendEmptyMessage(SHOW_ERROR);
            
            Log.e("ROMAN", "player exception", thrwbl);
            
            stopMusic();
            Statics.error();
            playerState = MediaPlayerStates.PLAYER_STOP;
            setUnplaying();
            removeNotification();

            if (endPosition != POSITION_UNDEFINED) {
                if (position < endPosition) {
                    next();
                }
                return;
            }
            if (position < items.size() - 1) {
                position++;
                Statics.positionChanged(position);
                play();
            }
        } catch (Exception ex) {
            Log.d("", "");
        }
    }

    /*MediaPlayer*/

    /*MultiPlayer*/
    public void playerMetadata(String string, String string1) {
        Log.d(logname, "playerMetadata");
    }

    public void play() {
        try {
            if (userID != null && userID.equals("186589")) {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Listen", items.get(position).getTitle());
                //FlurryAgent.logEvent("AudioPlugin", params, true);
                
            }
        } catch (Exception e) {
        }

        Log.d(logname, "play");
        try {
            // if start position was set
            if (startPosition != POSITION_UNDEFINED) {
                if (position < startPosition) {
                    position = startPosition;
                }
            }

            // if end position was set
            if (endPosition != POSITION_UNDEFINED) {
                if (position > endPosition) {
                    position = endPosition;
                }
            }

            // if state is stopped then start player
            if (playerState == MediaPlayerStates.PLAYER_STOP) {
                stopMusic();
                playerState = MediaPlayerStates.PLAYER_STOP;
                init();
                // if state is paused then unpause player
            } else if (playerState == MediaPlayerStates.PLAYER_PAUSE) {
                if (contentType == ContentTypes.FILE || contentType == ContentTypes.STREAM_MP3) {
                    System.gc();
                    mediaPlayer.start();
                    playerState = MediaPlayerStates.PLAYER_PLAY;
                } else if (contentType == ContentTypes.STREAM) {
                    multiPlayer.playAsync(items.get(position).getUrl());
                    playerState = MediaPlayerStates.PLAYER_PLAY;
                }
                items.get(position).setPlaying(true);
                Statics.musicUnpaused(items.get(position));
            }
        } catch (Exception e) {
            Log.e(logname, "play()", e);
        }
    }

    public void stop() {
        stopMusic();
        playerState = MediaPlayerStates.PLAYER_STOP;
        setUnplaying();

        if (notificationShown) {
            removeNotification();
        }
    }

    public void pause() {
        Log.d(logname, "pause");

        if(getState() == MediaPlayerStates.PLAYER_INIT) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (proxy!=null)
            proxy.stop();
            //proxy = null;
            playerState = MediaPlayerStates.PLAYER_STOP;
            items.get(position).setPlaying(false);
        } else {
            if (contentType == ContentTypes.FILE || contentType == ContentTypes.STREAM_MP3) {
                try {
                    mediaPlayer.pause();
                } catch (Exception ex) {
                    Log.d(logname, "", ex);
                }

                playerState = MediaPlayerStates.PLAYER_PAUSE;
                items.get(position).setPlaying(false);
            } else if (contentType == ContentTypes.STREAM) {
                stopMusic();
                playerState = MediaPlayerStates.PLAYER_PAUSE;
                items.get(position).setPlaying(false);
            }
        }
        setUnplaying();
        Statics.musicUnpaused(items.get(position));

        if (notificationShown) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    removeNotification();
                }
            }, 5 * 60 * 1000);
        }
    }

    /*MultiPlayer*/
    /*ServiceManageInterface*/
    public void next() {
        Log.d(logname, "next");
        if (position != (items.size() - 1)) {
            stop();
            position++;
            Statics.positionChanged(position);
            play();
        }
    }

    public void prev() {
        Log.d(logname, "prev");
        if (position != 0) {
            stop();
            position--;
            Statics.positionChanged(position);
            play();
        }
    }

    public void setItems(ArrayList<AudioItem> items) {
        this.items = items;
        
        this.position = -1;
    }

    public MediaPlayerStates getState() {
        return playerState;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getProgress() {
        if (contentType == ContentTypes.FILE) {
            if ((mediaPlayer != null) && isPrepared && getState() != MediaPlayerStates.PLAYER_INIT) {
                if ((mediaPlayer.getDuration() != 0) && mediaPlayer.isPlaying()) {
                    return (int) (mediaPlayer.getCurrentPosition());
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        } else {
            return PROGRESS_UNDEFINED;
        }
    }

    public ContentTypes getContentType() {
        return contentType;
    }

    public void seekTo(int pos) {
        try {

            if ((mediaPlayer != null) && isPrepared && getState() != MediaPlayerStates.PLAYER_INIT) {
                mediaPlayer.seekTo(position);
            }

        } catch (Exception e) {
            Log.e(logname, "", e);
        }
    }

    public int getDuration() {
        try {

            if ((mediaPlayer != null) && isPrepared && getState() != MediaPlayerStates.PLAYER_INIT) {
                return mediaPlayer.getDuration();
            } else {
                return 0;
            }

        } catch (Exception ex) {
            Log.e(logname, "", ex);
            return 0;
        }
    }

    public void stopService() {
        this.stopSelf();
    }

    public AudioItem getCurrentTrack() {
        try {
            return items.get(position);
        } catch (Exception ex) {
            return null;
        }
    }

    public void setNotification() {
        try {
            NotificationCompat.Builder ncBuilder = new NotificationCompat.Builder(getApplicationContext());
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.romanblack_audio_notification);
            if (playerState == MediaPlayerStates.PLAYER_PAUSE) {
                remoteViews.setBitmap(R.id.romanblack_audio_notification_play,
                        "setImageBitmap",
                        BitmapFactory.decodeResource(getResources(), R.drawable.romanblack_audio_play));
            } else if (playerState == MediaPlayerStates.PLAYER_PLAY) {
                remoteViews.setBitmap(R.id.romanblack_audio_notification_play,
                        "setImageBitmap",
                        BitmapFactory.decodeResource(getResources(), R.drawable.romanblack_audio_pause));
            }

            remoteViews.setTextViewText(R.id.romanblack_audio_notification_title, items.get(position).getTitle());
            Bitmap tmpBitmap = decodeImageFile(items.get(position).getCoverPath());
            if (tmpBitmap == null) {
                remoteViews.setImageViewResource(R.id.romanblack_audio_notification_thumb, R.drawable.romanblack_audio_placeholder);
            } else {
                remoteViews.setImageViewBitmap(R.id.romanblack_audio_notification_thumb, tmpBitmap);
            }

            Intent playIntent = new Intent(this, BackGroundMusicService.class);
            playIntent.putExtra(PENDING_EXTRA_ACTION, PENDING_PARAMETER_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);//PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.romanblack_audio_notification_play, playPendingIntent);

            Intent prevIntent = new Intent(this, BackGroundMusicService.class);

            prevIntent.putExtra(PENDING_EXTRA_ACTION, PENDING_PARAMETER_PREV);
            PendingIntent prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            remoteViews.setOnClickPendingIntent(R.id.romanblack_audio_notification_prev, prevPendingIntent);

            Intent nextIntent = new Intent(this, BackGroundMusicService.class);
            nextIntent.putExtra(PENDING_EXTRA_ACTION, PENDING_PARAMETER_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            remoteViews.setOnClickPendingIntent(R.id.romanblack_audio_notification_next, nextPendingIntent);

            ncBuilder.setOngoing(true);
            ncBuilder.setContent(remoteViews);
            ncBuilder.setSmallIcon(R.drawable.icon);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1000, ncBuilder.build());

            notificationShown = true;

        } catch (Throwable thr) {
        }
    }

    public void removeNotification() {
        if (notificationShown) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1000);
        }
    }

    public void setPositionsInterval(int startPosition, int endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public void cleanItems() {
        this.items = null;
        startPosition = POSITION_UNDEFINED;
        endPosition = POSITION_UNDEFINED;
    }

    public enum MediaPlayerStates {

        PLAYER_STOP, PLAYER_PLAY, PLAYER_PAUSE, PLAYER_INIT
    }

    public enum ContentTypes {

        FILE, STREAM, STREAM_MP3, UNDEFINED
    }

    /*ServiceManageInterface*/
    /*INTERFACE IMPLEMENTATION*/
    public class BackGroundMusicAIDLImpl extends BackGroundMusicAIDL.Stub {
    }

    /*Handler was here **start** */
    /**
     * If audio stream initialization was failed.
     */
    private void initializationFailed() {
        Toast.makeText(BackGroundMusicService.this,
                R.string.romanblack_audio_alert_cannot_init,
                Toast.LENGTH_LONG).show();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                stopSelf();
            }
        }, 5000);
    }

    /**
     * If device has not network connection.
     */
    private void needInternetConnection() {
        Log.d(logname, "NEED_INTERNET_CONNECTION");
        Toast.makeText(BackGroundMusicService.this,
                R.string.romanblack_audio_alert_no_internet, Toast.LENGTH_LONG).show();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                stopSelf();
            }
        }, 5000);
    }

    private void loadingAborted() {
        Log.d(logname, "LOADING_ABORTED");
        stopSelf();
    }

    /**
     * If audio stream type is m3u8.
     */
    private void foundStreamM3U8() {
        playerState = MediaPlayerStates.PLAYER_PLAY;
        items.get(position).setPlaying(true);
    }

    /**
     * Initializes audio strem.
     */
    private void playerInit() {
        Log.d(logname, "PLAYER_INIT");
        init();
    }

    /**
     * Starts mp3 audio stream playing.
     */
    private void playerPlay() {
        Log.d(logname, "PLAYER_PLAY");
        playMp3();
        playerState = MediaPlayerStates.PLAYER_PLAY;
    }
    /*Handler was here **end  ** */
}