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

import android.app.ProgressDialog;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.provider.Settings.Secure;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.*;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.DialogSharing;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import com.appbuilder.sdk.android.authorization.Authorization;
import com.appbuilder.sdk.android.authorization.FacebookAuthorizationActivity;
import com.appbuilder.sdk.android.authorization.entities.User;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnAuthListener;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnCommentPushedListener;
import com.ibuildapp.romanblack.AudioPlugin.callback.ServiceCallback;
import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.CommentItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.SetItem;
import com.ibuildapp.romanblack.AudioPlugin.utils.EntityParser;
import com.ibuildapp.romanblack.AudioPlugin.utils.JSONParser;
import com.ibuildapp.romanblack.AudioPlugin.utils.PositionResolver;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main module class. Module entry point.
 * Represents audio list, audio stream widgets.
 */
public class AudioPlugin extends AppBuilderModuleMain implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        MediaExpandableAdapter.SharePressedListener, MediaExpandableAdapter.OnThumbClickListener,
        OnCommentPushedListener, ServiceCallback, OnAuthListener {


    private static ExecutorService executorService = null;
    private final String TAG = "com.ibuildapp.romanblack.AudioPlugin.AudioPlugin";
    private final int INITIALIZATION_FAILED = 0;
    private final int LOADING_ABORTED = 1;
    private final int SHOW_PROGRESS_DIALOG = 2;
    private final int HIDE_PROGRESS_DIALOG = 3;
    private final int SHOW_MEDIA_LIST = 4;
    private final int REFRESH_LIST = 5;
    private final int PING = 6;
    private final int CONNECT_TO_SERVICE = 7;
    private final int NEED_INTERNET_CONNECTION = 8;
    private final int GET_OG_LIKES = 9;
    private final int PLAYER_INITIALIZING = 10;
    private final int PLAYER_MUSIC_STARTED = 11;
    private final int PLAYER_ERROR = 12;
    private final int COLORS_RECIEVED = 13;
    private final int FACEBOOK_AUTH = 10000;
    private final int TWITTER_AUTH = 10001;
    private String logname = "AudioPlugin";
    private ACTIONS action = ACTIONS.ACTION_NONE;
    private boolean destroyed = false;
    private boolean needMenu = false;
    private boolean serviceConnected = false;
    private int likePosition = 0;
    private int likeChildPosition = 0;
    private int sharingPosition = 0;
    private int sharingChildPosition = 0;
    private String cachePath = "";
    private String coverUrl = "";
    private String coverFileDir = "";
    private PositionResolver resolver = null;
    private Widget widget = null;
    private MediaExpandableAdapter adapter = null;
    private ExpandableListView listView = null;
    Bundle store;
    Intent currentIntent;
    private ProgressDialog progressDialog = null;
    private LinearLayout rootLayout = null;
    private View headerView = null;
    private ImageView headerImageView = null;
    private ArrayList<BasicItem> items = new ArrayList<BasicItem>();
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INITIALIZATION_FAILED: {
                    Toast.makeText(AudioPlugin.this,
                            getResources().getString(R.string.romanblack_audio_alert_cannot_init),
                            Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 2000);
                }
                break;
                case LOADING_ABORTED: {
                    closeActivity();
                }
                break;
                case SHOW_PROGRESS_DIALOG: {
                    showProgressDialog();
                }
                break;
                case HIDE_PROGRESS_DIALOG: {
                    hideProgressDialog();
                }
                break;
                case SHOW_MEDIA_LIST: {
                    showMediaList();
                }
                break;
                case REFRESH_LIST: {
                    refreshList();
                }
                break;
                case PING: {
                    ping();
                }
                break;
                case CONNECT_TO_SERVICE: {
                    connectToService();
                }
                break;
                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(
                            AudioPlugin.this,
                            getString(R.string.romanblack_audio_alert_no_internet),
                            Toast.LENGTH_LONG).show();
                }
                break;
                case GET_OG_LIKES: {
                    getOgLikes();
                }
                break;

                case PLAYER_INITIALIZING: {
                    playerInitializing();
                }
                break;
                case PLAYER_MUSIC_STARTED: {
                    playerMusicStarted();
                }
                break;
                case PLAYER_ERROR: {
                    playerError();
                }
                break;
                case COLORS_RECIEVED: {
                    colorsRecieved();
                }
                break;
            }
        }
    };
    /**
     * This field using to bind this activity with background service.
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName cn, IBinder ib) {
            serviceConnected = true;

            Statics.serviceManageInterface.removeNotification();
            ArrayList<AudioItem> tmpItems = resolver.getAudioItems();

            if(Statics.closedOrder != widget.getOrder()){
                Statics.serviceManageInterface.setItems(tmpItems);
                Statics.serviceManageInterface.setPositionsInterval(
                        0,
                        tmpItems.size() - 1);
            }

            if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY &&
                    Statics.closedOrder == widget.getOrder()) {
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) instanceof AudioItem) {
                        if (((AudioItem) items.get(i)).getUrl().equals(Statics.serviceManageInterface.getCurrentTrack().getUrl())) {
                            ((AudioItem) items.get(i)).setPlaying(true);
                        }
                    } else if (items.get(i) instanceof SetItem) {
                        SetItem setItem = (SetItem) items.get(i);
                        for (int j = 0; j < setItem.getTracksCount(); j++) {
                            AudioItem audioItem = setItem.getTrack(j);

                            if (audioItem.getUrl().equals(Statics.serviceManageInterface.getCurrentTrack().getUrl())) {
                                audioItem.setPlaying(true);
                            }
                        }
                    }
                }
            } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PAUSE) {
                Statics.serviceManageInterface.stop();
            } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_STOP) {
            }

            Statics.closedOrder = widget.getOrder();
        }

        public void onServiceDisconnected(ComponentName cn) {
        }
    };

    @Override
    public void create() {
        setContentView(R.layout.romanblack_audio_main);

        currentIntent = getIntent();
        store = currentIntent.getExtras();
        widget = (Widget) store.getSerializable("Widget");
        if (widget == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        if (widget.getPluginXmlData().length() == 0) {
            if (currentIntent.getStringExtra("WidgetFile").length() == 0) {
                handler.sendEmptyMessageDelayed(INITIALIZATION_FAILED, 1000);
                return;
            }
        }

        if (widget.getBackgroundColor() != Color.TRANSPARENT) {
            Statics.backgroundColor = widget.getBackgroundColor();
        }

        if ( !TextUtils.isEmpty(widget.getTitle()) ) {
            setTopBarTitle(widget.getTitle());
        } else {
            setTopBarTitle(getResources().getString(R.string.romanblack_audio_main_capture));
        }

        rootLayout = (LinearLayout) findViewById(R.id.romanblack_audio_main_root);

        // topbar initialization
        setTopBarLeftButtonText(getResources().getString(R.string.common_home_upper), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        listView = (ExpandableListView) findViewById(R.id.romanblack_audio_main_listview);
        listView.setGroupIndicator(null);
        listView.setDivider(null);
        listView.setBackgroundColor(Color.TRANSPARENT);
        listView.setCacheColorHint(Color.TRANSPARENT);

        headerView = LayoutInflater.from(this).inflate(R.layout.romanblack_audio_main_header, null);
        headerImageView = (ImageView) headerView.findViewById(R.id.romanblack_audio_main_header_image);

        cachePath = widget.getCachePath() + "/audio-" + widget.getOrder();
        File cache = new File(this.cachePath);
        if (!cache.exists()) {
            cache.mkdirs();
        }

        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(5);
        }

        handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
        handler.sendEmptyMessage(PING);

        executorService.execute(new Runnable() {
            public void run() {
                // парсинг даты
                EntityParser parser;
                if (widget.getPluginXmlData().length() > 0) {
                    parser = new EntityParser(widget.getPluginXmlData());
                } else {
                    String xmlData = readXmlFromFile(currentIntent.getStringExtra("WidgetFile"));
                    parser = new EntityParser(xmlData);
                }

                parser.parse();
                items = parser.getItems();
                Statics.APP_ID = parser.getAppId();
                Statics.APP_NAME = parser.getAppName();
                Statics.MODULE_ID = parser.getModuleId();
                Statics.sharingOn = parser.getSharingOn();
                Statics.commentsOn = parser.getCommentsOn();

                Statics.color1 = parser.getColor1();
                Statics.color2 = parser.getColor2();
                Statics.color3 = parser.getColor3();
                Statics.color4 = parser.getColor4();
                Statics.color5 = parser.getColor5();

                handler.sendEmptyMessage(COLORS_RECIEVED);

                // чистка кэш директории ... зачем???
                coverUrl = parser.getCoverUrl();
                File dir = new File(cachePath);
                String[] files = dir.list();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String filename = files[i];
                        boolean fl = false;

                        if (fl == false) {
                            File file = new File(cachePath + "/" + filename);
                            file.delete();
                        }
                    }
                }

                resolver = new PositionResolver(items);
                handler.sendEmptyMessage(CONNECT_TO_SERVICE);
                handler.sendEmptyMessage(SHOW_MEDIA_LIST);
            }
        });

        if (Utils.networkAvailable(AudioPlugin.this)) {
            Statics.isOnline = true;
        } else {
            Statics.isOnline = false;
        }

        Statics.serviceCallbacks.add(this);
        Statics.onAuthListeners.add(this);
    }

    @Override
    public void destroy() {
        destroyed = true;

        if(Statics.serviceManageInterface.getState() ==
                BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY){
            Statics.closedOrder = widget.getOrder();
        }else{
            Statics.closedOrder = -1;
        }

        try {
            Statics.onAuthListeners.remove(this);
        } catch (Exception ex) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FACEBOOK_AUTH) {
            if (resultCode == RESULT_OK) {
                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                    if (action == ACTIONS.FACEBOOK_SHARE) {
                        shareFacebook(sharingPosition, sharingChildPosition);
                    }
                }
            }
        } else if (requestCode == TWITTER_AUTH) {
            if (resultCode == RESULT_OK) {
                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER) != null) {
                    shareTwitter(sharingPosition, sharingChildPosition);
                }
            }
        }
    }

    /**
     * This menu contains share via Facebook, Twitter, Email, SMS buttons.
     * Also it contains "cancel" button.
     *
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add("Facebook").setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
//                    shareFacebook(sharingPosition, sharingChildPosition);
//                } else {
//                    action = ACTIONS.FACEBOOK_SHARE;
//                    Authorization.authorize(AudioPlugin.this, FACEBOOK_AUTH, Authorization.AUTHORIZATION_TYPE_FACEBOOK);
//                }
//
//                needMenu = false;
//
//                return true;
//            }
//        });
//        menu.add("Twitter").setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
//                    shareTwitter(sharingPosition, sharingChildPosition);
//                } else {
//                    Authorization.authorize(AudioPlugin.this, TWITTER_AUTH, Authorization.AUTHORIZATION_TYPE_TWITTER);
//                }
//
//                needMenu = false;
//
//                return true;
//            }
//        });
//        menu.add(getString(R.string.romanblack_audio_email)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                String text = items.get(sharingPosition).getUrl();
//
//                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
//                emailIntent.setType("text/html");
//                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(text));
//                startActivity(emailIntent);
//
//                needMenu = false;
//
//                return true;
//            }
//        });
//        menu.add("SMS").setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                String text = items.get(sharingPosition).getUrl();
//
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
//                intent.putExtra("sms_body", text);
//                startActivity(intent);
//
//                needMenu = false;
//
//                return true;
//            }
//        });
//        menu.add(R.string.romanblack_audio_cancel).setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                needMenu = false;
//                return true;
//            }
//        });

        return false;
    }

    @Override
    public void resume() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if ( items != null && items.size() >0)
                {
                    items = JSONParser.getVideoLikesCount(items);
                    handler.sendEmptyMessage(REFRESH_LIST);
                }
            }
        }).start();

        handler.sendEmptyMessageDelayed(REFRESH_LIST, 400);
        ArrayList<AudioItem> tmpItems = resolver != null ? resolver.getAudioItems() : new ArrayList<AudioItem>();

        if (serviceConnected) {
            Statics.serviceManageInterface.setPositionsInterval(
                    0,
                    tmpItems.size() - 1);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return needMenu;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        needMenu = false;
    }

    /**
     * Unbind service or set notification when activity is closing.
     */
    @Override
    public void onBackPressed() {
        try {

            if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_STOP
                    || Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PAUSE) {
                Statics.serviceManageInterface.cleanItems();
                unbindService(serviceConnection);
                AudioPlugin.super.onBackPressed();
            } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_INIT) {
                Statics.serviceManageInterface.cleanItems();
                Statics.serviceManageInterface.stop();
                unbindService(serviceConnection);
                AudioPlugin.super.onBackPressed();
            } else {
                Statics.serviceManageInterface.setNotification();
                AudioPlugin.super.onBackPressed();
            }

            Log.e("ROMAN", Statics.serviceManageInterface.getState() + "");
        } catch (NullPointerException nPEx) {
            super.onBackPressed();
        }
    }

    /**
     * Calling when module colors was recieved.
     */
    private void colorsRecieved() {
        rootLayout.setBackgroundColor(Statics.color1);
    }

    /**
     * Binds this activity to background music service.
     */
    private void connectToService() {
        ComponentName componentName = startService(new Intent(this, BackGroundMusicService.class));
        Log.d(logname, "componentName = " + componentName);
        boolean booleanBindService = bindService(new Intent(this, BackGroundMusicService.class), serviceConnection, 0);
        Log.d(logname, "booleanBindService = " + booleanBindService);
    }

    /**
     * Shows media list after parsing.
     */
    private void showMediaList() {
        if (items.isEmpty()) {
            handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);
            return;
        }

        adapter = new MediaExpandableAdapter(this, items, widget, listView);
        adapter.setCachePath(cachePath);
        adapter.setSharePressedListener(this);
        adapter.setOnThumbClickListener(this);
        if (coverUrl.length() > 0) {
            listView.addHeaderView(headerView);

            executorService.execute(new Runnable() {
                private void downloadComplete() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AudioPlugin.this.downloadComplete();
                        }
                    });
                }

                @Override
                public void run() {
                    String filePath = cachePath + "/images/" + Utils.md5(coverUrl);
                    coverFileDir = filePath;

                    if (filePath.length() > 0) {
                        File file = new File(filePath);
                        if (file.exists()) {
                            downloadComplete();
                            return;
                        }
                    }

                    if (coverUrl.length() == 0) {
                        coverFileDir = "";
                        downloadComplete();
                        return;
                    }

                    try {
                        URL imageUrl = new URL(URLDecoder.decode(coverUrl));
                        BufferedInputStream bis = new BufferedInputStream(imageUrl.openConnection().getInputStream());
                        ByteArrayBuffer baf = new ByteArrayBuffer(32);
                        int current = 0;
                        while ((current = bis.read()) != -1) {
                            baf.append((byte) current);
                        }
                        String fileImagesDir = cachePath + "/images/";
                        File fileImagesDirect = new File(fileImagesDir);
                        if (!fileImagesDirect.exists()) {
                            fileImagesDirect.mkdirs();
                        }
                        String filename = cachePath + "/images/" + Utils.md5(coverUrl);
                        FileOutputStream fos = new FileOutputStream(new File(filename));
                        fos.write(baf.toByteArray());
                        fos.close();
                    } catch (Exception e) {
                        Log.e("", "");
                    }

                    downloadComplete();
                }
            });
        }
        listView.setAdapter(adapter);

        Statics.onCommentPushedListeners.add(this);

        listView.setOnItemClickListener(this);


        executorService.execute(new Runnable() {
            public void run() {
                // получаем комментарии для дорожек
                HashMap<String, String> commentCounts =
                        JSONParser.getVideoCommentsCount();

                for (int i = 0; i < items.size(); i++) {
                    int count = 0;

                    try {
                        String key = items.get(i).getId() + "";

                        if (commentCounts.containsKey(key)) {
                            count = Integer.parseInt(commentCounts.get(key));
                        }

                        items.get(i).setTotalComments(count);

                        if (items.get(i) instanceof SetItem) {
                            SetItem setItem = (SetItem) items.get(i);

                            for (int j = 0; j < setItem.getTracksCount(); j++) {
                                try {
                                    String trackKey = setItem.getTrack(j).getId() + "";

                                    if (commentCounts.containsKey(trackKey)) {
                                        count = Integer.parseInt(commentCounts.get(trackKey));
                                    }

                                    setItem.getTrack(j).setTotalComments(count);
                                } catch (Exception ex) {
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.d("", "");
                    }
                }

                // get likes for links
                items = JSONParser.getVideoLikesCount(items);
                handler.sendEmptyMessage(REFRESH_LIST);
            }
        });

        handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);
    }

    /**
     * Refreshes media list.
     */
    private void refreshList() {
        try {
            adapter.notifyDataSetChanged();
        } catch (Exception ex) {
        }
    }

    /**
     * Polls service when module is open.
     */
    private void ping() {
        executorService.execute(new Runnable() {
            public void run() {
                HttpParams params = new BasicHttpParams();
                params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
                        HttpVersion.HTTP_1_1);
                HttpClient httpClient = new DefaultHttpClient(params);

                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(Statics.BASE_URL);

                    sb.append("/");

                    HttpPost httpPost = new HttpPost(sb.toString());

                    String UID = Utils.md5(Secure.getString(getContentResolver(), Secure.ANDROID_ID));

                    MultipartEntity multipartEntity = new MultipartEntity();
                    multipartEntity.addPart("action", new StringBody("ping", Charset.forName("UTF-8")));
                    multipartEntity.addPart("platform", new StringBody("android", Charset.forName("UTF-8")));
                    multipartEntity.addPart("app_id", new StringBody(Statics.APP_ID, Charset.forName("UTF-8")));
                    multipartEntity.addPart("module_id", new StringBody(Statics.MODULE_ID, Charset.forName("UTF-8")));
                    multipartEntity.addPart("device", new StringBody(UID, Charset.forName("UTF-8")));

                    if (Authorization.getAuthorizedUser() != null) {
                        multipartEntity.addPart("account_id", new StringBody(Authorization.getAuthorizedUser().getAccountId(), Charset.forName("UTF-8")));
                        if (Authorization.getAuthorizedUser().getAccountType() == User.ACCOUNT_TYPES.FACEBOOK) {
                            multipartEntity.addPart("account_type", new StringBody("facebook", Charset.forName("UTF-8")));
                        } else if (Authorization.getAuthorizedUser().getAccountType() == User.ACCOUNT_TYPES.TWITTER) {
                            multipartEntity.addPart("account_type", new StringBody("twitter", Charset.forName("UTF-8")));
                        } else {
                            multipartEntity.addPart("account_type", new StringBody("ibuildapp", Charset.forName("UTF-8")));
                        }
                    }

                    httpPost.setEntity(multipartEntity);

                    String resp = httpClient.execute(httpPost, new BasicResponseHandler());

                    Log.d("", "");

                } catch (Exception e) {
                    Log.d("", "");
                }

                if (!destroyed) {
                    handler.sendEmptyMessageDelayed(PING, 30 * 1000);
                }
            }
        });
    }

    /**
     * Prepares audios open graph likes.
     */
    private void getOgLikes() {
        if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        ArrayList<String> urls = FacebookAuthorizationActivity.getUserOgLikes();

                        for (int i = 0; i < items.size(); i++) {
                            for (int j = 0; j < urls.size(); j++) {
                                if (items.get(i).getPermalinkUrl().equalsIgnoreCase(urls.get(j))) {
                                    items.get(i).setLiked(true);

                                    break;
                                }
                            }

                            if (items.get(i) instanceof SetItem) {
                                SetItem setItem = (SetItem) items.get(i);

                                for (int k = 0; k < setItem.getTracksCount(); k++) {
                                    for (int j = 0; j < urls.size(); j++) {
                                        if (items.get(i).getPermalinkUrl().equalsIgnoreCase(urls.get(j))) {
                                            items.get(i).setLiked(true);

                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        handler.sendEmptyMessage(REFRESH_LIST);
                    } catch (FacebookAuthorizationActivity.FacebookNotAuthorizedException e) {
                        e.printStackTrace();
                    }


                }
            }).start();
        }
    }

    /**
     * Starts share on facebook activity.
     *
     * @param position video position
     */
    private void shareFacebook(int position, int childPosition) {
        String url = "";

        if (childPosition == -1) {
            url = items.get(position).getPermalinkUrl();
        } else if (items.get(position) instanceof SetItem) {
            url = ((SetItem) items.get(position)).getTrack(childPosition).getPermalinkUrl();
        } else {
            url = items.get(position).getPermalinkUrl();
        }

        Intent it = new Intent(this, SharingActivity.class);
        it.putExtra("type", "facebook");
        it.putExtra("link", url);
        startActivity(it);
    }

    /**
     * Starts share on twitter activity.
     *
     * @param position video position
     */
    private void shareTwitter(int position, int childPosition) {
        String url = "";

        if (childPosition == -1) {
            url = items.get(position).getPermalinkUrl();
        } else if (items.get(position) instanceof SetItem) {
            url = ((SetItem) items.get(position)).getTrack(childPosition).getPermalinkUrl();
        } else {
            url = items.get(position).getPermalinkUrl();
        }

        Intent it = new Intent(this, SharingActivity.class);
        it.putExtra("type", "twitter");
        it.putExtra("link", url);
        startActivity(it);
    }

    private void showProgressDialog() {
        try {
            if (progressDialog.isShowing()) {
                return;
            }
        } catch (NullPointerException nPEx) {
        }

        progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_audio_loading), true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                handler.sendEmptyMessage(LOADING_ABORTED);
            }
        });
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void closeActivity() {
        hideProgressDialog();
        finish();
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
    }

    public void onClick(View arg0) {
    }

    public void onSharePressed(int position, int childPosition) {
        sharingPosition = position;
        sharingChildPosition = childPosition;

        needMenu = true;

        showDialogSharing(new DialogSharing.Configuration.Builder()
                        .setFacebookSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                                    shareFacebook(sharingPosition, sharingChildPosition);
                                } else {
                                    action = ACTIONS.FACEBOOK_SHARE;
                                    Authorization.authorize(AudioPlugin.this, FACEBOOK_AUTH, Authorization.AUTHORIZATION_TYPE_FACEBOOK);
                                }

                                needMenu = false;
                            }
                        })
                        .setTwitterSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                                    shareTwitter(sharingPosition, sharingChildPosition);
                                } else {
                                    Authorization.authorize(AudioPlugin.this, TWITTER_AUTH, Authorization.AUTHORIZATION_TYPE_TWITTER);
                                }

                                needMenu = false;
                            }
                        })
                        .setEmailSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                String text = items.get(sharingPosition).getUrl();

                                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                                emailIntent.setType("text/html");
                                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(text));
                                startActivity(emailIntent);

                                needMenu = false;
                            }
                        })
                        .setSmsSharingClickListener(new DialogSharing.Item.OnClickListener() {
                            @Override
                            public void onClick() {
                                String text = items.get(sharingPosition).getUrl();

                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
                                intent.putExtra("sms_body", text);
                                startActivity(intent);

                                needMenu = false;
                            }
                        })
                        .build()
        );

//        openOptionsMenu();
    }

    public void onPost() {
        handler.removeMessages(PING);
        handler.sendEmptyMessage(PING);
    }

    public void onAuth() {
        handler.removeMessages(PING);
        handler.sendEmptyMessage(PING);
        handler.sendEmptyMessage(GET_OG_LIKES);
    }

    public void onCommentPushed(CommentItem item) {
        if (item != null) {
            if (item.getReplyId() != 0) {
                return;
            }

            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId() == item.getTrackId()) {
                    items.get(i).setTotalComments(
                            items.get(i).getTotalComments() + 1);
                }
            }

            handler.sendEmptyMessage(REFRESH_LIST);
        }
    }

    public void onCommentsUpdate(BasicItem pItem, int count, int newCommentsCount, ArrayList<CommentItem> comments) {
        for (int i = 0; i < items.size(); i++) {
            BasicItem tmpItem = items.get(i);

            if (pItem.getId() == tmpItem.getId()) {
                tmpItem.setTotalComments(count);

                handler.sendEmptyMessage(REFRESH_LIST);

                return;
            } else if (tmpItem instanceof SetItem) {
                SetItem tmpSet = (SetItem) tmpItem;

                ArrayList<AudioItem> tmpAudios = tmpSet.getTracks();

                for (int j = 0; j < tmpAudios.size(); j++) {
                    AudioItem tmpAudio = tmpAudios.get(j);

                    if (pItem.getId() == tmpAudio.getId()) {
                        tmpAudio.setTotalComments(count);

                        handler.sendEmptyMessage(REFRESH_LIST);

                        return;
                    }
                }
            }
        }
    }

    public void onThumbClick(int position, int childPosition) {
        try {
            if (items.get(position) instanceof AudioItem) {
                if (Statics.isOnline) {
                    AudioItem audioItem = (AudioItem) items.get(position);

                    // If the audio is playing
                    if (audioItem.isPlaying()) {
                        Statics.serviceManageInterface.pause();
                    } else {
                        // If player returns state play, it handles invalid state
                        if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY) {
                            Statics.serviceManageInterface.stop();
                            Statics.serviceManageInterface.setPosition(resolver.getAudioPosition(position, childPosition));
                            Statics.serviceManageInterface.play();
                        } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PAUSE) {
                            // If chosen track is paused
                            if (resolver.getAudioPosition(position, childPosition) == Statics.serviceManageInterface.getPosition()) {
                                Statics.serviceManageInterface.play();
                            } else {
                                Statics.serviceManageInterface.stop();
                                Statics.serviceManageInterface.setPosition(resolver.getAudioPosition(position, childPosition));
                                Statics.serviceManageInterface.play();
                            }
                        } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_STOP) {
                            Statics.serviceManageInterface.stop();
                            Statics.serviceManageInterface.setPosition(resolver.getAudioPosition(position, childPosition));
                            Statics.serviceManageInterface.play();
                        }
                    }
                } else {
                    handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                }
            } else if (items.get(position) instanceof SetItem) {
                SetItem setItem = (SetItem) items.get(position);

                if (childPosition < 0) {
                    if (setItem.isExpanded()) {
                        listView.collapseGroup(position);
                        setItem.setExpanded(false);
                    } else {
                        listView.expandGroup(position);
                        setItem.setExpanded(true);
                    }

                    adapter.notifyDataSetChanged();

                    return;
                }

                if (Statics.isOnline) {
                    AudioItem audioItem = setItem.getTrack(childPosition);

                    if (audioItem.isPlaying()) {
                        Statics.serviceManageInterface.pause();
                    } else {
                        if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY) {
                            Statics.serviceManageInterface.stop();
                            Statics.serviceManageInterface.setPosition(resolver.getAudioPosition(position, childPosition));
                            Statics.serviceManageInterface.play();
                        } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PAUSE) {
                            if (resolver.getAudioPosition(position, childPosition) == Statics.serviceManageInterface.getPosition()) {
                                Statics.serviceManageInterface.play();
                            } else {
                                Statics.serviceManageInterface.stop();
                                Statics.serviceManageInterface.setPosition(resolver.getAudioPosition(position, childPosition));
                                Statics.serviceManageInterface.play();
                            }
                        } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_STOP) {
                            Statics.serviceManageInterface.stop();
                            Statics.serviceManageInterface.setPosition(resolver.getAudioPosition(position, childPosition));
                            Statics.serviceManageInterface.play();
                        }
                    }
                } else {
                    handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                }
            }
        } catch (NullPointerException nPEx) {
            Log.d("", "");
        } catch (Throwable thr) {
            Log.d("", "");
        }
    }

    /*ServiceCallback*/
    public void onServiceStarted() {
    }

    public void initializing() {
        Log.e(TAG, "initializing");
        handler.sendEmptyMessage(PLAYER_INITIALIZING);
    }

    private void playerInitializing() {
        adapter.notifyDataSetChanged();

        int pos = Statics.serviceManageInterface.getPosition();

        Log.d("", "");
    }

    public void musicStarted() {
        Log.e(TAG, "initializing");
        handler.sendEmptyMessage(PLAYER_MUSIC_STARTED);
    }

    public void playerMusicStarted() {
        adapter.notifyDataSetChanged();

        int pos = Statics.serviceManageInterface.getPosition();

        Log.d("", "");
    }

    public void error() {
        handler.sendEmptyMessage(PLAYER_ERROR);
    }

    private void playerError() {
        adapter.notifyDataSetChanged();
    }

    public void positionChanged(int position) {
        runOnUiThread(new Runnable() {

            public void run() {

                adapter.notifyDataSetChanged();
            }
        });
    }

    public void musicPaused(AudioItem item) {
        adapter.notifyDataSetChanged();
        Log.e(TAG, "musicPaused");
    }

    public void musicUnpaused(AudioItem item) {
        adapter.notifyDataSetInvalidated();
        Log.e(TAG, "musicUnpaused");
    }

    /*ServiceCallback*/

    /**
     * Sets audio list image when it's was downloaded.
     */
    private void setThumb() {
        if (headerImageView != null) {
            if (coverFileDir.length() > 0) {

                Bitmap bitmap = null;

                try {
                    bitmap = decodeImageFile(coverFileDir);
                } catch (Exception e) {
                    Log.d("", "");
                }

                if (bitmap != null) {
                    BitmapDrawable bDrw = new BitmapDrawable(bitmap);
                    headerImageView.setImageDrawable(bDrw);
                }
            }
        }
    }

    /**
     * Decodes image file to bitmap from device external storage.
     *
     * @param imagePath image file path
     * @return decoded image bitmap
     */
    private Bitmap decodeImageFile(String imagePath) {
        try {
            int imageWidth = getResources().getDisplayMetrics().widthPixels;
            int imageHeight = (int) (150 * getResources().getDisplayMetrics().density);

            File file = new File(imagePath);
            //Decode image size
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(file), null, opts);

            //Find the correct scale value. It should be the power of 2.
            int width = opts.outWidth, height = opts.outHeight;
            int scale = 1;
            while (true) {
                if (width / 2 < imageWidth || height / 2 < imageHeight) {
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

            return bitmap;
        } catch (Exception e) {
            Log.d("", "");
        }

        return null;
    }

    private void downloadComplete() {
        setThumb();
    }

    private enum ACTIONS {

        FACEBOOK_LIKE, FACEBOOK_SHARE, ACTION_NONE
    }

    /**
     * This method using when module data is too big to put in Intent
     *
     * @param fileName - xml module data file name
     * @return xml module data
     */
    protected String readXmlFromFile(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(new File(fileName)));
            while ((line = in.readLine()) != null) {
                stringBuilder.append(line);
            }

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        return stringBuilder.toString();
    }
}
