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
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.authorization.Authorization;
import com.appbuilder.sdk.android.authorization.FacebookAuthorizationActivity;
import com.appbuilder.sdk.android.sharing.Sharing;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.FacebookType;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/**
 * This activity provides share on Facebook or Twitter functionality.
 */
public class SharingActivity extends AppBuilderModuleMain implements OnClickListener {

    private final int NEED_INTERNET_CONNECTION = 0;
    private final int INITIALIZATION_FAILED = 1;
    private final int SHOW_PROGRESS_DIALOG = 2;
    private final int HIDE_PROGRESS_DIALOG = 3;
    private String text = "";
    private String sharingType = "";
    private String link = "";
    private Twitter twitter = null;
    private ImageView postImageView = null;
    private EditText mainEditText = null;
    private ProgressDialog progressDialog = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case INITIALIZATION_FAILED: {
                    finish();
                }
                break;
                case NEED_INTERNET_CONNECTION: {
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
            }
        }
    };

    @Override
    public void create() {
        setContentView(R.layout.romanblack_audio_sharing);

        float density = getResources().getDisplayMetrics().density;

        Intent currentIntent = getIntent();
        link = currentIntent.getStringExtra("link");
        if (TextUtils.isEmpty(link)) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        sharingType = currentIntent.getStringExtra("type");

        swipeBlock();
        setTopBarLeftButtonText(getResources().getString(R.string.back), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        postImageView = (ImageView) getLayoutInflater().inflate(R.layout.romanblack_audio_post_btn, null);
        postImageView.setLayoutParams(new LinearLayout.LayoutParams((int) (20 * density), (int) (20 * density)));
        postImageView.setColorFilter(navBarDesign.itemDesign.textColor);
//        postImageView.setOnClickListener(this);
        setTopBarRightButton(postImageView, getString(R.string.share_message), this);

        mainEditText = (EditText) findViewById(R.id.romanblack_audio_sharing_edittext);

        if (sharingType.equalsIgnoreCase("facebook")) {
            setTopBarTitle("Facebook");
        } else if (sharingType.equalsIgnoreCase("twitter")) {
            setTopBarTitle("Twitter");
        }
    }

    private void showProgressDialog() {
        try {
            if (progressDialog.isShowing()) {
                return;
            }
        } catch (NullPointerException nPEx) {
        }

        progressDialog = ProgressDialog.show(this, null, getString(R.string.romanblack_audio_loading));
        progressDialog.setCancelable(true);
    }

    private void hideProgressDialog() {
        try {
            progressDialog.dismiss();
        } catch (NullPointerException nPEx) {
        }

        finish();
    }

    public void onClick(View arg0) {
        if (arg0 == postImageView) {
            postImageView.setClickable(false);

            text = mainEditText.getText().toString();

            if ( !Utils.networkAvailable(SharingActivity.this) ){
                handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                return;
            }

            if (sharingType.equalsIgnoreCase("facebook")) {
                final FacebookClient fbClient = new DefaultFacebookClient(Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK).getAccessToken());

                handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

                text = Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK).getUserName() + " "
                        + getString(R.string.romanblack_audio_sharing_first_part) + " "
                        + link + " " + getString(R.string.romanblack_audio_sharing_second_part) + " "
                        + Statics.APP_NAME
                        + " app:\n\"" + mainEditText.getText() + "\"\n";

                if (com.appbuilder.sdk.android.Statics.showLink) {
                    text = text + getString(R.string.romanblack_audio_sharing_third_part)
                            + " app: http://" + com.appbuilder.sdk.android.Statics.BASE_DOMEN
                            + "/projects.php?action=info&projectid="
                            + Statics.APP_ID;
                }

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            boolean res = FacebookAuthorizationActivity.sharing(Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_FACEBOOK).getAccessToken(), text, null);
                            if ( res )
                            {
                                setResult(RESULT_OK);
                            }
                        } catch (FacebookAuthorizationActivity.FacebookNotAuthorizedException e) {
                        }
                        handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);
                    }
                }).start();

            } else if (sharingType.equalsIgnoreCase("twitter")) {
                handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

                text = Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getUserName() + " "
                        + getString(R.string.romanblack_audio_sharing_first_part) + " "
                        + link + " " + getString(R.string.romanblack_audio_sharing_second_part) + " "
                        + Statics.APP_NAME
                        + " app:\n\"" + mainEditText.getText() + "\"\n";

                if (com.appbuilder.sdk.android.Statics.showLink) {
                    text = text + getString(R.string.romanblack_audio_sharing_third_part)
                            + " app: http://" + com.appbuilder.sdk.android.Statics.BASE_DOMEN
                            + "/projects.php?action=info&projectid="
                            + Statics.APP_ID;
                }

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            twitter = new TwitterFactory().getInstance();
                            twitter.setOAuthConsumer(Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getConsumerKey(),
                                Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getConsumerSecret());
                            twitter.setOAuthAccessToken(new AccessToken(Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getAccessToken(),
                                    Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getAccessTokenSecret()));

                            Log.d("ROMAN", Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getAccessToken() + " " + Authorization.getAuthorizedUser(Authorization.AUTHORIZATION_TYPE_TWITTER).getAccessTokenSecret());
                            Log.d("ROMAN", com.appbuilder.sdk.android.Statics.TWITTER_CONSUMER_KEY + " " + com.appbuilder.sdk.android.Statics.TWITTER_CONSUMER_SECRET);

                            if (text.length() > 140) {
                                text = text.substring(0, 139);
                            }

                            twitter.updateStatus(text);

                            setResult(RESULT_OK);
                        } catch (TwitterException tEx) {
                            Log.d("ROMAN", "", tEx);
                        }

                        handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);
                    }
                }).start();
            }
        }
    }
}
