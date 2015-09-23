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

import android.app.AlertDialog;
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
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
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
import com.ibuildapp.romanblack.AudioPlugin.utils.JSONParser;
import com.ibuildapp.romanblack.AudioPlugin.utils.PositionResolver;
import com.restfb.Facebook;
import org.apache.http.util.ByteArrayBuffer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

/**
 * This activity represents audio preview page.
 */
public class AudioPreviewActivity extends AppBuilderModuleMain implements OnClickListener,
        OnCommentPushedListener, ServiceCallback, OnAuthListener,
        AdapterView.OnItemClickListener, TextWatcher {

    private final int INITIALIZATION_FAILED = 0;
    private final int LOADING_ABORTED = 1;
    private final int SHOW_PROGRESS_DIALOG = 2;
    private final int HIDE_PROGRESS_DIALOG = 3;
    private final int SHOW_COMMENTS_LIST = 4;
    private final int NEED_INTERNET_CONNECTION = 5;
    private final int REFRESH_LIST = 6;
    private final int CONNECT_TO_SERVICE = 7;
    private final int HIDE_LIKE_BUTTON = 8;
    private final int UPDATE_LIKE_COUNTER = 9;
    private final int GET_OG_LIKES = 10;
    private final int REFRESH_LIKE_BUTTON = 11;
    private final int CLEAN_COMMENT_EDIT_TEXT = 12;
    private final int UNABLE_TO_WRITE_POST = 13;
    private final int AUTH_FOR_LIKE = 14;
    private final int REFRESH_PLAY_BUTTON = 15;
    private final int SET_LIKE_COUNT = 16;
    private final int FACEBOOK_AUTH = 10000;
    private final int TWITTER_AUTH = 10001;
    private final int AUTHORIZATION_ACTIVITY = 10002;
    private final int SEND_COMMENT_ACTIVITY = 10003;
    private final int SHARING_FACEBOOK = 10004;
    private final int SHARING_TWITTER = 10005;
    private final int PROGRESS_SIZE = 20;
    private ACTIONS action = ACTIONS.ACTION_NONE;
    private boolean needMenu = false;
    private boolean keyboardShown = false;
    private int position = 0;
    private int childPosition = 0;
    private int audioPosition = 0;
    private int startPosition = 0;
    private int endPosition = 0;
    private float density = 1;
    private String cachePath = "";
    private BasicItem item = null;
    private Widget widget = null;
    private PositionResolver resolver = null;
    private CommentsAdapter adapter = null;
    private Intent actionIntent = null;
    private LinearLayout rootLayout = null;
    private TextView audioTitleTextView = null;
    private TextView audioDescriptionTextView = null;
    private ListView listView = null;
    private RelativeLayout videoPreview = null;
    private ImageView videoPreviewImageView = null;
    private ProgressDialog progressDialog = null;
    private View mainHeaderView = null;
    private ImageView shareButton = null;
    private ImageView prevButton = null;
    private ImageView playButton = null;
    private ImageView nextButton = null;
    private TextView likesCountTextView = null;
    private LinearLayout likeButton = null;
    private LinearLayout bottomPanel = null;
    private ProgressBar progress = null;
    private TextView trackNameTextView = null;
    private TextView trackDurationTextView = null;
    private TextView tracksCountTextView = null;
    private TextView postCommentButton = null;
    private EditText commentEditText = null;
    private LinearLayout noCommentsLayout = null;
    private ArrayList<BasicItem> items = null;
    private ArrayList<CommentItem> comments = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case INITIALIZATION_FAILED: {
                    Toast.makeText(AudioPreviewActivity.this,
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
                case SHOW_COMMENTS_LIST: {
                    showCommentsList();
                }
                break;
                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(AudioPreviewActivity.this,
                            getResources().getString(
                                    R.string.romanblack_audio_alert_no_internet),
                            Toast.LENGTH_LONG).show();
                }
                break;
                case REFRESH_LIST: {
                    refreshList();
                }
                break;
                case SET_LIKE_COUNT: {
                    String likeCount = (String) message.obj;
                    if (!TextUtils.isEmpty(likeCount))
                        likesCountTextView.setText(likeCount);
                }
                break;
                case CONNECT_TO_SERVICE: {
                    connectToService();
                }
                break;
                case HIDE_LIKE_BUTTON: {
                    hideLikeButton();
                }
                break;
                case UPDATE_LIKE_COUNTER: {
                    updateLikeCounter();
                }
                break;
                case GET_OG_LIKES: {
                    getOgLikes();
                }
                break;
                case REFRESH_LIKE_BUTTON: {
                    refreshLikeButton();
                }
                break;
                case CLEAN_COMMENT_EDIT_TEXT: {
                    cleanCommentEditText();
                }
                break;
                case UNABLE_TO_WRITE_POST: {
                    Toast.makeText(AudioPreviewActivity.this,
                            R.string.romanblack_audio_alert_sending_failed,
                            Toast.LENGTH_LONG).show();
                }
                break;
                case AUTH_FOR_LIKE: {
                    action = ACTIONS.FACEBOOK_LIKE;
                    Authorization.authorize(AudioPreviewActivity.this, FACEBOOK_AUTH, Authorization.AUTHORIZATION_TYPE_FACEBOOK);
                }
                break;
                case REFRESH_PLAY_BUTTON: {
                    refreshPlayButton();
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
            if (item instanceof AudioItem) {
                startPosition = resolver.getAudioPosition(position, childPosition);
                endPosition = startPosition;

                Statics.serviceManageInterface.setPositionsInterval(startPosition, endPosition);
            } else if (item instanceof SetItem) {
                SetItem setItem = (SetItem) item;

                startPosition = resolver.getAudioPosition(position, 0);
                endPosition = startPosition + setItem.getTracksCount() - 1;

                Statics.serviceManageInterface.setPositionsInterval(startPosition, endPosition);
            }

            audioPosition = startPosition;

            checkPlayerButtons();

            boolean needToClick = true;

            if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY &&
                    widget.getOrder() == Statics.closedOrder) {
                try{
                    if (item.getUrl().equals(Statics.serviceManageInterface.getCurrentTrack().getUrl())) {
                        needToClick = false;
                        playButton.setImageResource(R.drawable.romanblack_audio_pause);
                    }
                }catch(Exception ex){
                }
            } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PAUSE) {
                Statics.serviceManageInterface.stop();
            } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_STOP) {
            }

            if (needToClick) {
                playButton.performClick();
            }
        }

        public void onServiceDisconnected(ComponentName cn) {
        }
    };

    @Override
    public void create() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.romanblack_audio_preview);

        density = getResources().getDisplayMetrics().density;

        setTopBarTitle(getResources().getString(R.string.romanblack_audio_preview_capture));
        swipeBlock();
        setTopBarLeftButtonText(getResources().getString(R.string.back), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeActivity();
            }
        });

        mainHeaderView = getLayoutInflater().inflate(R.layout.romanblack_audio_preview_header, null);
        shareButton = (ImageView) getLayoutInflater().inflate(R.layout.romanblack_audio_share_btn, null);
        shareButton.setLayoutParams(new LinearLayout.LayoutParams((int) (29 * density), (int) (39 * density)));
        shareButton.setColorFilter(navBarDesign.itemDesign.textColor);
//        shareButton.setOnClickListener(this);
        setTopBarRightButton(shareButton, getString(R.string.romanblack_audio_list_share), new OnClickListener() {
            @Override
            public void onClick(View v) {
                needMenu = true;

                showDialogSharing(new DialogSharing.Configuration.Builder()
                                .setFacebookSharingClickListener(new DialogSharing.Item.OnClickListener() {
                                    @Override
                                    public void onClick() {
                                        if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                                            shareFacebook();
                                        } else {
                                            action = ACTIONS.FACEBOOK_SHARE;
                                            Authorization.authorize(AudioPreviewActivity.this, FACEBOOK_AUTH, Authorization.AUTHORIZATION_TYPE_FACEBOOK);
                                        }

                                        needMenu = false;
                                    }
                                })
                                .setTwitterSharingClickListener(new DialogSharing.Item.OnClickListener() {
                                    @Override
                                    public void onClick() {
                                        if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER) != null) {
                                            shareTwitter();
                                        } else {
                                            Authorization.authorize(AudioPreviewActivity.this, TWITTER_AUTH, Authorization.AUTHORIZATION_TYPE_TWITTER);
                                        }

                                        needMenu = false;
                                    }
                                })
                                .setEmailSharingClickListener(new DialogSharing.Item.OnClickListener() {
                                    @Override
                                    public void onClick() {
                                        String text = item.getUrl();

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
                                        String text = item.getUrl();

                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
                                        intent.putExtra("sms_body", text);
                                        startActivity(intent);

                                        needMenu = false;
                                    }
                                })
                                .build()
                );
            }
        });

        Intent currentIntent = getIntent();
        items = (ArrayList<BasicItem>) currentIntent.getSerializableExtra("items");

        if (items == null || items.isEmpty()) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        position = currentIntent.getIntExtra("position", 0);
        childPosition = currentIntent.getIntExtra("childPosition", -1);

        if (childPosition < 0 || items.get(position) instanceof AudioItem) {
            try {
                item = items.get(position);
            } catch (IndexOutOfBoundsException iOOBEx) {
                handler.sendEmptyMessage(INITIALIZATION_FAILED);
                return;
            }
        } else {
            try {
                item = ((SetItem) items.get(position)).getTrack(childPosition);
            } catch (IndexOutOfBoundsException iOOBEx) {
                handler.sendEmptyMessage(INITIALIZATION_FAILED);
                return;
            }
        }

        if (item == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        widget = (Widget) currentIntent.getSerializableExtra("Widget");

        cachePath = currentIntent.getStringExtra("cachePath");

        rootLayout = (LinearLayout) findViewById(R.id.romanblack_audio_preview_root);
        rootLayout.setBackgroundColor(Statics.color1);

        postCommentButton = (TextView) findViewById(R.id.romanblack_audio_preview_postbtn);
        postCommentButton.setTextColor(bottomBarDesign.rightButtonDesign.textColor);
        postCommentButton.setOnClickListener(this);

        commentEditText = (EditText) findViewById(R.id.romanblack_audio_preview_edit);
        commentEditText.addTextChangedListener(this);

        noCommentsLayout = (LinearLayout) findViewById(R.id.nocomments_layout);

        audioTitleTextView = (TextView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_video_title);
        audioTitleTextView.setText(item.getTitle());
        audioTitleTextView.setTextColor(Statics.color3);

        audioDescriptionTextView = (TextView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_video_description);
        audioDescriptionTextView.setText(item.getDescription());
        audioDescriptionTextView.setTextColor(Statics.color4);

        likesCountTextView = (TextView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_comments_header_likes_count);
        likesCountTextView.setText(item.getLikesCount() + "");

        likeButton = (LinearLayout) mainHeaderView.findViewById(R.id.romanblack_audio_preview_comments_header_like_btn);
        likeButton.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.romanblack_audio_preview_listview);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setHeaderDividersEnabled(true);
        listView.addHeaderView(mainHeaderView);

        videoPreviewImageView = (ImageView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_preview_img);
        ImageDownloadTask idt = new ImageDownloadTask();
        idt.execute(item);

        playButton = (ImageView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_play);
        playButton.setOnClickListener(this);

        prevButton = (ImageView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_prev_track);
        prevButton.setOnClickListener(this);

        nextButton = (ImageView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_next_track);
        nextButton.setOnClickListener(this);

        progress = (ProgressBar) mainHeaderView.findViewById(R.id.romanblack_audio_preview_progress);
        progress.setVisibility(View.GONE);

        trackNameTextView = (TextView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_track_in_set_name);
        if (item instanceof AudioItem) {
            trackNameTextView.setVisibility(View.GONE);
        }

        trackDurationTextView = (TextView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_track_in_set_duration);
        trackDurationTextView.setVisibility(View.INVISIBLE);

        tracksCountTextView = (TextView) mainHeaderView.findViewById(R.id.romanblack_audio_preview_tracks_count);

        if (item instanceof SetItem) {
            tracksCountTextView.setText(((SetItem) item).getTracksCount() + "");
        } else {
            tracksCountTextView.setVisibility(View.INVISIBLE);
        }

        if (!Statics.isOnline) {
            shareButton.setAlpha(100);
            ImageView likeImage = (ImageView) findViewById(R.id.romanblack_audio_preview_comments_header_like_image);
            likeImage.setAlpha(100);
            TextView likeCaption = (TextView) findViewById(R.id.romanblack_audio_preview_comments_header_like_caption);
            likeCaption.setTextColor(Color.parseColor("#9bffffff"));
        }

        if (Statics.sharingOn.equalsIgnoreCase("off")
                && Statics.commentsOn.equalsIgnoreCase("off")) {
            bottomPanel.setVisibility(View.GONE);
        } else if (Statics.sharingOn.equalsIgnoreCase("off")) {
            shareButton.setVisibility(View.INVISIBLE);
        } else if (Statics.commentsOn.equalsIgnoreCase("off")) {
        }

        if (item.isLiked()) {
            hideLikeButton();
        }

        handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

        new Thread(new Runnable() {
            public void run() {

                boolean isOnline = false;
                if ( Utils.networkAvailable( AudioPreviewActivity.this) )
                    isOnline = true;

                if (isOnline) {
                    String commentsUrl = Statics.BASE_URL + "/getcomments/"
                            + com.appbuilder.sdk.android.Statics.appId + "/" + Statics.MODULE_ID + "/"
                            + item.getId() + "/0/" 
                            + com.appbuilder.sdk.android.Statics.appId + "/" 
                            + com.appbuilder.sdk.android.Statics.appToken;

                    comments = JSONParser.parseCommentsUrl(commentsUrl);

                    // так же обновим состояние лайка
                    if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                        List<String> link = new ArrayList<String>();
                        link.add(item.getPermalinkUrl());

                        // колво лайков
                        Map<String, String> likedMap = FacebookAuthorizationActivity.getLikesForUrls(link, Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK).getAccessToken());

                        // список моих
                        List<String> userLikes = null;
                        try {
                             userLikes = FacebookAuthorizationActivity.getUserOgLikes();
                        } catch (FacebookAuthorizationActivity.FacebookNotAuthorizedException e) {
                            e.printStackTrace();
                        }

                        boolean likedByMe = false;
                        String[] ar = item.getPermalinkUrl().split("/");
                        String trackName = ar[ar.length-1];
                        for (String likeUrl : userLikes)
                        {
                            if ( likeUrl.contains(trackName) ) {
                                likedByMe = true;
                                break;
                            }
                        }

                        if ( likedMap != null )
                            handler.sendMessage( handler.obtainMessage(SET_LIKE_COUNT, likedMap.get(item.getPermalinkUrl())));

                        if ( likedByMe )
                            handler.sendEmptyMessage(HIDE_LIKE_BUTTON);
                    }

                } else {
                    try {
                        FileInputStream fis = new FileInputStream(
                                cachePath + "/" + "ca-" + item.getId() + "-0");
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        comments = (ArrayList<CommentItem>) ois.readObject();
                        ois.close();
                        fis.close();
                    } catch (FileNotFoundException fNFEx) {
                        Log.d("", "");
                    } catch (IOException iOEx) {
                        Log.d("", "");
                    } catch (ClassNotFoundException cNFEx) {
                        Log.d("", "");
                    }
                }

                try {
                    Collections.reverse(comments);
                } catch (Exception ex) {
                }

                resolver = new PositionResolver(items);

                handler.sendEmptyMessage(CONNECT_TO_SERVICE);

                handler.sendEmptyMessage(SHOW_COMMENTS_LIST);
            }
        }).start();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                View v = (View) rootLayout.getParent().getParent().getParent().getParent();

                int rootHeight = v.getRootView().getHeight();
                int viewHeight = v.getHeight();

                int diff = Math.abs(rootHeight - viewHeight);
                if (diff > 100) {
                    keyboardShown = true;

                    if (commentEditText.getText().toString().length() == 0) {
                        disablePostComment();
                    }

                    try {
                        listView.setOnItemClickListener(AudioPreviewActivity.this);
                    } catch (NullPointerException nPEx) {
                    }
                } else {
                    keyboardShown = false;

                    enablePostComment();

                    try {
                        listView.setOnItemClickListener(null);
                    } catch (NullPointerException nPEx) {
                    }
                }
            }
        });

        Statics.onCommentPushedListeners.add(this);

        Statics.serviceCallbacks.add(this);

        Statics.onAuthListeners.add(this);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void destroy() {
        Statics.onCommentPushedListeners.remove(this);

        Statics.serviceCallbacks.remove(this);

        Statics.onAuthListeners.remove(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FACEBOOK_AUTH) {
            if (resultCode == RESULT_OK) {
                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                    if (action == ACTIONS.FACEBOOK_LIKE) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    boolean res = FacebookAuthorizationActivity.like(item.getPermalinkUrl());
                                    if ( res )
                                    {
                                        handler.sendEmptyMessage(HIDE_LIKE_BUTTON);
                                        handler.sendEmptyMessage(UPDATE_LIKE_COUNTER);
                                    }
                                } catch (FacebookAuthorizationActivity.FacebookNotAuthorizedException e) {
                                    showLikeAuthDialog();
                                } catch (FacebookAuthorizationActivity.FacebookAlreadyLiked facebookAlreadyLiked) {
                                    handler.sendEmptyMessage(HIDE_LIKE_BUTTON);
                                }
                            }
                        }).start();
                    } else if (action == ACTIONS.FACEBOOK_SHARE) {
                        shareFacebook();
                    }
                }
            }
        } else if (requestCode == TWITTER_AUTH) {
            if (resultCode == RESULT_OK) {
                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER) != null) {
                    shareTwitter();
                }
            }
        } else if (requestCode == AUTHORIZATION_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                postComment();
            } else {
            }
        } else if (requestCode == SEND_COMMENT_ACTIVITY) {
            if (resultCode == RESULT_OK) {
            }
        }  else if ( requestCode == SHARING_FACEBOOK )
        {
            if ( resultCode == RESULT_OK )
                Toast.makeText(AudioPreviewActivity.this, getString(R.string.directoryplugin_facebook_posted_success), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(AudioPreviewActivity.this, getString(R.string.directoryplugin_facebook_posted_error), Toast.LENGTH_SHORT).show();
        } else if ( requestCode == SHARING_TWITTER )
        {
            if ( resultCode == RESULT_OK )
                Toast.makeText(AudioPreviewActivity.this, getString(R.string.directoryplugin_twitter_posted_success), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(AudioPreviewActivity.this, getString(R.string.directoryplugin_twitter_posted_error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This menu contains share via Facebook, Twitter, Email, SMS buttons.
     * Also it contains "cancel" button.
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add("Facebook").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
//                    shareFacebook();
//                } else {
//                    action = ACTIONS.FACEBOOK_SHARE;
//                    Authorization.authorize(AudioPreviewActivity.this, FACEBOOK_AUTH, Authorization.AUTHORIZATION_TYPE_FACEBOOK);
//                }
//
//                needMenu = false;
//
//                return true;
//            }
//        });
//        menu.add("Twitter").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER) != null) {
//                    shareTwitter();
//                } else {
//                    Authorization.authorize(AudioPreviewActivity.this, TWITTER_AUTH, Authorization.AUTHORIZATION_TYPE_TWITTER);
//                }
//
//                needMenu = false;
//
//                return true;
//            }
//        });
//        menu.add(getString(R.string.romanblack_audio_email)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                String text = item.getUrl();
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
//        menu.add("SMS").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                String text = item.getUrl();
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
//        menu.add(getString(R.string.romanblack_audio_cancel)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem arg0) {
//                needMenu = false;
//
//                return true;
//            }
//        });

        return false;
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
     * Sets the audio cover image when it was downloaded.
     */
    private void setThumb() {
        if (videoPreviewImageView != null) {
            if (item.getCoverUrl().length() == 0) {
                videoPreviewImageView.setImageResource(R.drawable.romanblack_audio_placeholder);
                return;
            }

            if (item.getCoverPath().length() > 0) {
                Bitmap bitmap = null;

                try {
                    bitmap = decodeImageFile(item.getCoverPath());
                } catch (Exception e) {
                    Log.d("", "");
                }

                if (bitmap != null) {
                    BitmapDrawable bDrw = new BitmapDrawable(bitmap);
                    videoPreviewImageView.setImageDrawable(bDrw);
                }
            }
        }
    }

    /**
     * Hides like button when if this audio was liked by user.
     */
    private void refreshLikeButton() {
        if (item.isLiked()) {
            hideLikeButton();
        }
    }

    /**
     * Prepares audio open graph likes.
     */
    private void getOgLikes() {
        if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
            new Thread(new Runnable() {
                public void run() {
                    ArrayList<String> urls = null;
                    try {
                        urls = FacebookAuthorizationActivity.getUserOgLikes();

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

                        handler.sendEmptyMessage(REFRESH_LIKE_BUTTON);
                    } catch (FacebookAuthorizationActivity.FacebookNotAuthorizedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * Decodes image file to bitmap from device external storage.
     * @param imagePath image file path
     * @return decoded image bitmap
     */
    private Bitmap decodeImageFile(String imagePath) {
        try {
            int imageWidth = (int) (70 * getResources().getDisplayMetrics().density);
            int imageHeight = (int) (70 * getResources().getDisplayMetrics().density);

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

    /**
     * Refreshes comments list.
     */
    private void refreshList() {
        adapter.setComments(comments);
        adapter.notifyDataSetChanged();

        if (comments.isEmpty()) {
            noCommentsLayout.setVisibility(View.VISIBLE);
        } else {
            noCommentsLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Shows comments if the comments list is not empty.
     * Shows comments placeholder if the comments list is empty.
     */
    private void showCommentsList() {
        if (comments == null) {
            comments = new ArrayList<CommentItem>();
        }

        adapter = new CommentsAdapter(this, comments, item, widget);
        adapter.setCachePath(cachePath);
        listView.setAdapter(adapter);

        if (comments.isEmpty()) {
            noCommentsLayout.setVisibility(View.VISIBLE);
        } else {
            noCommentsLayout.setVisibility(View.GONE);
        }

        handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

        cacheMessages();
    }

    /**
     * Unhides "previous" button.
     */
    private void showPrevButton() {
        prevButton.setVisibility(View.VISIBLE);
        prevButton.setAlpha(500);
        prevButton.setClickable(true);
    }

    /**
     * Unhides "next" button.
     */
    private void showNextButton() {
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setAlpha(500);
        nextButton.setClickable(true);
    }

    /**
     * Makes "previous" button semi-transparent.
     */
    private void alphaPrevButton() {
        prevButton.setVisibility(View.VISIBLE);
        prevButton.setAlpha(400);
        prevButton.setClickable(false);
    }

    /**
     * Makes "next" button semi-transparent.
     */
    private void alphaNextButton() {
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setAlpha(400);
        nextButton.setClickable(false);
    }

    /**
     * Hides "previous" button.
     */
    private void hidePrevButton() {
        prevButton.setVisibility(View.GONE);
        prevButton.setClickable(false);
    }

    /**
     * Hides "next" button.
     */
    private void hideNextButton() {
        nextButton.setVisibility(View.GONE);
        nextButton.setClickable(false);
    }

    /**
     * Updates likes counter.
     */
    private void updateLikeCounter() {
        item.setLikesCount(item.getLikesCount() + 1);
        likesCountTextView.setText(item.getLikesCount() + "");
    }

    /**
     * Makes "like" button semi-transparent.
     */
    private void hideLikeButton() {
        ImageView likeImage = (ImageView) findViewById(R.id.romanblack_audio_preview_comments_header_like_image);
        likeImage.setAlpha(100);
        TextView likeCaption = (TextView) findViewById(R.id.romanblack_audio_preview_comments_header_like_caption);
        likeCaption.setTextColor(Color.parseColor("#9bffffff"));
        likeButton.getBackground().setAlpha(100);
    }

    /**
     * Binds this activity to bacground service.
     */
    private void connectToService() {
        bindService(new Intent(this, BackGroundMusicService.class), serviceConnection, 0);
    }

    /**
     * Caches comments array to device external storage.
     */
    private void cacheMessages() {
        File cacheFile = new File(cachePath);
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }

        File cache = new File(cachePath + "/" + "ca-" + item.getId() + "-0");
        if (cache.exists()) {
            cache.delete();
        }

        try {
            cache.createNewFile();
        } catch (IOException iOEx) {
        }

        ArrayList<CommentItem> cMessages = new ArrayList<CommentItem>();

        if ((comments.size()) <= 20 && (!comments.isEmpty())) {
            cMessages = comments;
        } else if (comments.size() > 20) {
            for (int i = 0; i < 20; i++) {
                cMessages.add(comments.get(i));
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(cache);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cMessages);
            oos.close();
            fos.close();
        } catch (IOException iOEx) {
        }
    }

    /**
     * Shows auth dialog if user is not authorized in Facebook.
     */
    private void showLikeAuthDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getString(R.string.romanblack_audio_dialog_must_be_logged_in));
//        builder.setPositiveButton(getString(R.string.romanblack_audio_yes),
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        handler.sendEmptyMessage(AUTH_FOR_LIKE);
//                    }
//                });
//        builder.setNegativeButton(getString(R.string.romanblack_audio_cancel),
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface arg0, int arg1) {
//                    }
//                });
//        builder.create().show();
        handler.sendEmptyMessage(AUTH_FOR_LIKE);
    }

    /**
     * Posts a new comment to this audio.
     */
    private void postComment() {
        if (commentEditText.getText().length() < 1) {
            Toast.makeText(this, R.string.romanblack_audio_alert_empty_message,
                    Toast.LENGTH_LONG).show();
        }

        if (commentEditText.getText().length() > 150) {
            Toast.makeText(this, R.string.romanblack_audio_alert_big_text,
                    Toast.LENGTH_LONG).show();

            return;
        }

        if ((commentEditText.getText().length() == 0)) {
            Toast.makeText(this, R.string.romanblack_audio_alert_empty_message,
                    Toast.LENGTH_LONG).show();

            return;
        }

        handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

        new Thread(new Runnable() {
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

                    MultipartEntity multipartEntity = new MultipartEntity();
                    multipartEntity.addPart("action", new StringBody("postcomment", Charset.forName("UTF-8")));
                    multipartEntity.addPart("app_id", new StringBody(com.appbuilder.sdk.android.Statics.appId, Charset.forName("UTF-8")));
                    multipartEntity.addPart("token", new StringBody(com.appbuilder.sdk.android.Statics.appToken, Charset.forName("UTF-8")));
                    multipartEntity.addPart("module_id", new StringBody(Statics.MODULE_ID, Charset.forName("UTF-8")));
                    multipartEntity.addPart("parent_id", new StringBody(item.getId() + "", Charset.forName("UTF-8")));

                    if (Authorization.getAuthorizedUser().getAccountType() == User.ACCOUNT_TYPES.FACEBOOK) {
                        multipartEntity.addPart("account_type", new StringBody("facebook", Charset.forName("UTF-8")));
                    } else if (Authorization.getAuthorizedUser().getAccountType() == User.ACCOUNT_TYPES.TWITTER) {
                        multipartEntity.addPart("account_type", new StringBody("twitter", Charset.forName("UTF-8")));
                    } else {
                        multipartEntity.addPart("account_type", new StringBody("ibuildapp", Charset.forName("UTF-8")));
                    }
                    multipartEntity.addPart("account_id", new StringBody(Authorization.getAuthorizedUser().getAccountId(), Charset.forName("UTF-8")));
                    multipartEntity.addPart("username", new StringBody(Authorization.getAuthorizedUser().getUserName(), Charset.forName("UTF-8")));
                    multipartEntity.addPart("avatar", new StringBody(Authorization.getAuthorizedUser().getAvatarUrl(), Charset.forName("UTF-8")));

                    multipartEntity.addPart("text", new StringBody(commentEditText.getText().toString(), Charset.forName("UTF-8")));

                    httpPost.setEntity(multipartEntity);

                    Statics.onPost();

                    String resp = httpClient.execute(httpPost, new BasicResponseHandler());

                    CommentItem recievedMessage = JSONParser.parseCommentsString(resp).get(0);

                    handler.sendEmptyMessage(CLEAN_COMMENT_EDIT_TEXT);

                    String commentsUrl = Statics.BASE_URL + "/getcomments/"
                            + com.appbuilder.sdk.android.Statics.appId + "/" + Statics.MODULE_ID + "/"
                            + item.getId() + "/0/"
                            + com.appbuilder.sdk.android.Statics.appId + "/"
                            + com.appbuilder.sdk.android.Statics.appToken;

                    ArrayList<CommentItem> tmpComments = JSONParser.parseCommentsUrl(commentsUrl);

                    if (tmpComments != null && tmpComments.size() > 0) {
                        Collections.reverse(tmpComments);

                        comments = tmpComments;

                        Statics.onCommentsUpdate(item, comments.size(), 0, comments);

                        handler.sendEmptyMessage(REFRESH_LIST);
                    }

                    Log.d("", "");

                } catch (Exception e) {
                    Log.d("", "");
                }

                handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

            }
        }).start();
    }

    /**
     * Starts share on facebook activity.
     */
    private void shareFacebook() {
        String url = "";

        if (childPosition == -1) {
            url = items.get(position).getPermalinkUrl();
        } else if (items.get(position) instanceof SetItem) {
            url = ((SetItem) items.get(position)).getTrack(childPosition).getPermalinkUrl();
        } else {
            url = items.get(position).getPermalinkUrl();
        }

        Intent it = new Intent(this, SharingActivity.class);
        it.putExtra("item", item);
        it.putExtra("type", "facebook");
        it.putExtra("link", url);
        startActivityForResult(it, SHARING_FACEBOOK);
    }

    /**
     * Starts share on twitter activity.
     */
    private void shareTwitter() {
        String url = "";

        if (childPosition == -1) {
            url = items.get(position).getPermalinkUrl();
        } else if (items.get(position) instanceof SetItem) {
            url = ((SetItem) items.get(position)).getTrack(childPosition).getPermalinkUrl();
        } else {
            url = items.get(position).getPermalinkUrl();
        }

        Intent it = new Intent(this, SharingActivity.class);
        it.putExtra("item", item);
        it.putExtra("type", "twitter");
        it.putExtra("link", url);
        startActivityForResult(it, SHARING_TWITTER);
    }

    /**
     * Checks if need to hide player buttons.
     */
    private void checkPlayerButtons() {
        /*audioPosition*/
        int tmpPosition = Statics.serviceManageInterface.getPosition();
        if (tmpPosition >= startPosition && tmpPosition <= endPosition) {
            audioPosition = tmpPosition;
        } else {
            audioPosition = startPosition;
        }

        if (item instanceof AudioItem) {
            hideNextButton();
            hidePrevButton();
        } else if (item instanceof SetItem) {
            if (audioPosition == startPosition) {
                alphaPrevButton();
                showNextButton();
            } else if (audioPosition == endPosition) {
                alphaNextButton();
                showPrevButton();
            } else {
                showNextButton();
                showPrevButton();
            }
        }

        try{
            trackNameTextView.setText(Statics.serviceManageInterface.getCurrentTrack().getTitle());
        }catch(NullPointerException ex){
        }
        
        try {
            trackDurationTextView.setText(new SimpleDateFormat("mm:ss").format
                    (Statics.serviceManageInterface./*getCurrentTrack().*/getDuration()));

            if (Statics.serviceManageInterface./*getCurrentTrack().*/getDuration()/*.getTime()*/ > 0) {
                trackDurationTextView.setVisibility(View.VISIBLE);
            } else {
                trackDurationTextView.setVisibility(View.INVISIBLE);
            }
        } catch (NullPointerException nPEx) {
        }

        refreshPlayButton();
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

    /**
     * Cleans comment EditText and closes soft keyboard.
     */
    private void cleanCommentEditText() {
        commentEditText.setText("");

        hideKeyboard();
    }

    /**
     * Hides soft keyboard.
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);
    }

    private void closeActivity() {
        hideProgressDialog();
        finish();
    }

    private void authForLike() {
    }

    public void onServiceStarted() {
    }

    /*Service Callback*/
    public void initializing() {
        playButton.setClickable(false);

        handler.sendEmptyMessage(REFRESH_PLAY_BUTTON);
    }

    public void musicStarted() {
        playButton.setClickable(true);

        try {
            trackDurationTextView.setText(new SimpleDateFormat("mm:ss").format(Statics.serviceManageInterface.getDuration()));
            if (Statics.serviceManageInterface.getDuration() > 0) {
                trackDurationTextView.setVisibility(View.VISIBLE);
            } else {
                trackDurationTextView.setVisibility(View.INVISIBLE);
            }
        } catch (NullPointerException nPEx) {
        }

        handler.sendEmptyMessage(REFRESH_PLAY_BUTTON);
    }

    public void error() {
        playButton.setClickable(true);

        handler.sendEmptyMessage(REFRESH_PLAY_BUTTON);
    }

    public void positionChanged(int position) {
        runOnUiThread(new Runnable() {

            public void run() {
                trackDurationTextView.setText("00:00");
                trackDurationTextView.setVisibility(View.INVISIBLE);
                checkPlayerButtons();
            }
        });
    }

    public void musicPaused(AudioItem item) {
        handler.sendEmptyMessage(REFRESH_PLAY_BUTTON);
    }

    public void musicUnpaused(AudioItem item) {
        handler.sendEmptyMessage(REFRESH_PLAY_BUTTON);
    }

    public void onClick(View arg0) {
        if (arg0 == videoPreview) {
        } else if (arg0 == likeButton) {
            if (Utils.networkAvailable( AudioPreviewActivity.this )) {
                if (Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK) != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                boolean res = FacebookAuthorizationActivity.like(item.getPermalinkUrl());
                                if ( res )
                                {
                                    handler.sendEmptyMessage(HIDE_LIKE_BUTTON);
                                    handler.sendEmptyMessage(UPDATE_LIKE_COUNTER);
                                }
                            } catch (FacebookAuthorizationActivity.FacebookNotAuthorizedException e) {
                                showLikeAuthDialog();
                            } catch (FacebookAuthorizationActivity.FacebookAlreadyLiked facebookAlreadyLiked) {
                                handler.sendEmptyMessage(HIDE_LIKE_BUTTON);
                            }
                        }
                    }).start();
                } else {
                    showLikeAuthDialog();
                }
            } else {
                Toast.makeText(this, this.getResources().getString(R.string.romanblack_audio_alert_like_need_internet),
                        Toast.LENGTH_LONG).show();
            }
        } else if (arg0 == postCommentButton) {
            if (commentEditText.getText().length() < 1) {
                Toast.makeText(this, R.string.romanblack_audio_alert_empty_message,
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (Utils.networkAvailable(AudioPreviewActivity.this)) {
                if (!Authorization.isAuthorized()) {
                    actionIntent = new Intent(this, SendMessageActivity.class);
                    actionIntent.putExtra("Widget", widget);
                    actionIntent.putExtra("item", item);

                    action = ACTIONS.SEND_MESSAGE;

                    Intent it = new Intent(this, AuthorizationActivity.class);
                    it.putExtra("Widget", widget);
                    startActivityForResult(it, AUTHORIZATION_ACTIVITY);
                } else {
                    postComment();
                }
            } else {
                handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
            }

        } else if (arg0 == prevButton) {
            Statics.serviceManageInterface.prev();
            checkPlayerButtons();
        } else if (arg0 == nextButton) {
            Statics.serviceManageInterface.next();
            checkPlayerButtons();
        } else if (arg0 == playButton) {
            if (Statics.isOnline) {
                if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PAUSE
                        || Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_STOP) {
                    Statics.serviceManageInterface.play();
                    playButton.setImageResource(R.drawable.romanblack_audio_pause);
                } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY) {
                    if (Statics.serviceManageInterface.getPosition() == audioPosition) {
                        Statics.serviceManageInterface.pause();
                        playButton.setImageResource(R.drawable.romanblack_audio_play);
                    } else {
                        Statics.serviceManageInterface.stop();
                        Statics.serviceManageInterface.setPosition(audioPosition);
                        Statics.serviceManageInterface.play();
                        playButton.setImageResource(R.drawable.romanblack_audio_pause);
                    }
                }

                checkPlayerButtons();
            } else {
                handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
            }
        } else if (arg0 == listView) {
        }
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (arg0 == listView) {
            if (keyboardShown) {
                hideKeyboard();
            }
        }
    }

    public void onAuth() {
        handler.sendEmptyMessage(GET_OG_LIKES);
    }

    public void onCommentPushed(CommentItem comment) {
                }

    public void onCommentsUpdate(BasicItem item, int count, int newCommentsCount, ArrayList<CommentItem> comments) {
    }

    private void downloadComplete() {
        setThumb();
    }

    private void downloadRegistration(String value) {
        item.setCoverPath(value);
    }

    /**
     * Refreshes "play" button.
     */
    private void refreshPlayButton() {
        try{
            if (item.getUrl().equals(Statics.serviceManageInterface.getCurrentTrack().getUrl())) {
                if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_INIT) {
                    progress.setVisibility(View.VISIBLE);

                    playButton.setImageResource(R.drawable.romanblack_audio_pause);
                } else if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_PLAY) {
                    progress.setVisibility(View.GONE);

                    playButton.setImageResource(R.drawable.romanblack_audio_pause);
                } else {
                    progress.setVisibility(View.GONE);

                    playButton.setImageResource(R.drawable.romanblack_audio_play);
                }
            } else {
                progress.setVisibility(View.GONE);
                playButton.setImageResource(R.drawable.romanblack_audio_play);
            }
        }catch(Exception ex){
        }
    }

    /**
     * Enables "post comment" button.
     */
    private void enablePostComment() {
        postCommentButton.setClickable(true);
        postCommentButton.setTextColor(bottomBarDesign.rightButtonDesign.textColor);
    }

    /**
     * Disables "post comment" button.
     */
    private void disablePostComment() {
        postCommentButton.setClickable(false);
        postCommentButton.setTextColor(Color.parseColor("#50000000"));
    }

    /*TextWatcher implementation start*/
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    public void afterTextChanged(Editable arg0) {
        if (keyboardShown) {
            if (arg0.toString().length() == 0) {
                disablePostComment();
            } else {
                enablePostComment();
            }
        }
    }
    /*TextWatcher implementation ends*/

    private enum ACTIONS {

        FACEBOOK_LIKE, FACEBOOK_SHARE, ACTION_NONE,
        SEND_MESSAGE
    };

    /**
     * This class creates a background thread to download audio list cover.
     */
    private class ImageDownloadTask extends AsyncTask<BasicItem, String, Void> {

        @Override
        protected Void doInBackground(BasicItem... items) {
            try {//ErrorLogging
                if (isCancelled()) {
                    downloadComplete();
                    return null;
                }

                items[0].setCoverPath(cachePath + "/images/" + Utils.md5(items[0].getCoverUrl()));

                if (items[0].getCoverPath().length() > 0) {
                    File file = new File(items[0].getCoverPath());
                    if (file.exists()) {
                        downloadComplete();
                        return null;
                    }
                }

                if (items[0].getCoverUrl().length() == 0) {
                    downloadComplete();
                    return null;
                }

                SystemClock.sleep(10);
                try {
                    URL imageUrl = new URL(URLDecoder.decode(items[0].getCoverUrl()));
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
                    String filename = cachePath + "/images/" + Utils.md5(items[0].getCoverUrl());
                    FileOutputStream fos = new FileOutputStream(new File(filename));
                    fos.write(baf.toByteArray());
                    fos.close();

                    downloadRegistration(filename);
                } catch (Exception e) {
                    Log.e("", "");
                }
                publishProgress();

                return null;

            } catch (Exception e) {//ErrorLogging
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... param) {
        }

        @Override
        protected void onPostExecute(Void unused) {
            downloadComplete();
        }
    }
}
