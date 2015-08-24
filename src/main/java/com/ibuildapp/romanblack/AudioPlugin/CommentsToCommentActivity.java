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
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import com.appbuilder.sdk.android.authorization.Authorization;
import com.appbuilder.sdk.android.authorization.entities.User;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnCommentPushedListener;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.CommentItem;
import com.ibuildapp.romanblack.AudioPlugin.utils.JSONParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

/**
 * This activity represents comments list to comment.
 */
public class CommentsToCommentActivity extends AppBuilderModuleMain implements
        OnClickListener, OnItemClickListener, OnCommentPushedListener {

    /**
     * This callback is calling when comment list was updated.
     * @param item audio or audio list item on which comments was made
     * @param commentItem comment item on which comments was made
     * @param count comments count
     * @param newCommentsCount new comments count
     * @param comments comments list
     */
    public void onCommentsUpdate(BasicItem item, int count, int newCommentsCount, ArrayList<CommentItem> comments) {
    }

    private enum ACTIONS {

        ACTION_NO, SEND_MESSAGE
    };
    private final int AUTHORIZATION_ACTIVITY = 10000;
    private final int MESSAGE_VIEW_ACTIVITY = 10001;
    private final int SEND_COMMENT_ACTIVITY = 10003;
    private final int INITIALIZATION_FAILED = 0;
    private final int LOADING_ABORTED = 1;
    private final int SHOW_PROGRESS_DIALOG = 2;
    private final int HIDE_PROGRESS_DIALOG = 3;
    private final int SHOW_COMMENTS_LIST = 4;
    private final int SHOW_NO_COMMENTS = 5;
    private final int NEED_INTERNET_CONNECTION = 6;
    private final int REFRESH_LIST = 7;
    private final int CLEAN_COMMENT_EDIT_TEXT = 8;
    private boolean keyboardShown = false;
    private int imageWidth = 50;
    private int imageHeight = 50;
    private int position = 0;
    private String cachePath = "";
    private BasicItem videoItem = null;
    private CommentItem item = null;
    private Widget widget = null;
    private CommentsAdapter adapter = null;
    private Intent actionIntent = null;
    private LinearLayout rootLayout = null;
    private View headerView = null;
    private ImageView thumbImageView = null;
    private TextView titleTextView = null;
    private TextView descriptionTextView = null;
    private ListView listView = null;
    private ProgressDialog progressDialog = null;
    private LinearLayout hasCommentsLayout = null;
    private LinearLayout hasntCommentsLayout = null;
    private TextView postCommentButton = null;
    private EditText commentEditText = null;
    private ArrayList<CommentItem> items = null;
    private ArrayList<CommentItem> comments = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case INITIALIZATION_FAILED: {
                    Toast.makeText(CommentsToCommentActivity.this,
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
                case SHOW_NO_COMMENTS: {
                    showNoComments();
                }
                break;
                case NEED_INTERNET_CONNECTION: {
                    Toast.makeText(CommentsToCommentActivity.this,
                            getResources().getString(
                            R.string.romanblack_audio_alert_no_internet),
                            Toast.LENGTH_LONG).show();
                }
                break;
                case REFRESH_LIST: {
                    refreshList();
                }
                break;
                case CLEAN_COMMENT_EDIT_TEXT: {
                    cleanCommentEditText();
                }
                break;
            }
        }
    };

    @Override
    public void create() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.romanblack_audio_comments_main);

        Intent currentIntent = getIntent();
        items = (ArrayList<CommentItem>) currentIntent.getSerializableExtra("items");

        if (items == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        if (items.isEmpty()) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        setTopBarTitle(getResources().getString(R.string.romanblack_audio_replies_capture));
        swipeBlock();
        setTopBarLeftButtonText(getResources().getString(R.string.back), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeActivity();
            }
        });

        position = currentIntent.getIntExtra("position", 0);

        try {
            item = items.get(position);
        } catch (IndexOutOfBoundsException iOOBEx) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        if (item == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        videoItem = (BasicItem) currentIntent.getSerializableExtra("item");

        if (videoItem == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            if (ni.isConnectedOrConnecting()) {
                Statics.isOnline = true;
            } else {
                Statics.isOnline = false;
            }
        } else {
            Statics.isOnline = false;
        }

        widget = (Widget) currentIntent.getSerializableExtra("Widget");

        cachePath = currentIntent.getStringExtra("cachePath");

        rootLayout = (LinearLayout) findViewById(R.id.romanblack_audio_comments_main_root);
        rootLayout.setBackgroundColor(Statics.color1);

        postCommentButton = (TextView) findViewById(R.id.postbtn);
        postCommentButton.setTextColor(bottomBarDesign.rightButtonDesign.textColor);
        postCommentButton.setOnClickListener(this);

        commentEditText = (EditText) findViewById(R.id.edit);

        listView = (ListView) findViewById(R.id.romanblack_audio_comments_main_listview);
        listView.setCacheColorHint(Color.parseColor("#41464b"));
        listView.setHeaderDividersEnabled(false);

        headerView = LayoutInflater.from(this).inflate(R.layout.romanblack_audio_commentstocomments_header, null);

        thumbImageView = (ImageView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_thumb);
        ImageDownloadTask idt = new ImageDownloadTask();
        idt.execute(item);

        titleTextView = (TextView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_name);
        titleTextView.setText(item.getAuthor());
        titleTextView.setTextColor(Statics.color3);

        descriptionTextView = (TextView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_text);
        descriptionTextView.setText(item.getText());
        descriptionTextView.setTextColor(Statics.color4);

        hasCommentsLayout = (LinearLayout) findViewById(R.id.romanblack_audio_comments_main_has_layout);
        hasntCommentsLayout = (LinearLayout) findViewById(R.id.nocomments_layout);

        handler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

        new Thread(new Runnable() {
            public void run() {

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();

                boolean isOnline = false;

                if (ni != null) {
                    isOnline = ni.isConnectedOrConnecting();
                }

                if (isOnline) {
                    String commentsUrl = Statics.BASE_URL + "/getcomments/"
                            + com.appbuilder.sdk.android.Statics.appId + "/" + Statics.MODULE_ID + "/"
                            + videoItem.getId() + "/" + item.getId() + "/"
                            + com.appbuilder.sdk.android.Statics.appId + "/"
                            + com.appbuilder.sdk.android.Statics.appToken;

                    comments = JSONParser.parseCommentsUrl(commentsUrl);
                } else {
                    try {
                        FileInputStream fis = new FileInputStream(
                                cachePath + "/" + "ca-" + videoItem.getId()
                                + "-" + item.getId());
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

                if (comments == null) {
                    comments = new ArrayList<CommentItem>();
                }

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

                    try {
                        listView.setOnItemClickListener(CommentsToCommentActivity.this);
                    } catch (NullPointerException nPEx) {
                        Log.d("", "");
                    }
                } else {
                    keyboardShown = false;

                    try {
                        listView.setOnItemClickListener(null);
                    } catch (NullPointerException nPEx) {
                        Log.d("", "");
                    }
                }
            }
        });

        Statics.onCommentPushedListeners.add(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTHORIZATION_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                postComment();
            }
        } else if (requestCode == MESSAGE_VIEW_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                ArrayList<CommentItem> tmp = (ArrayList<CommentItem>) data.getSerializableExtra("messages");
                try {
                    if (!tmp.isEmpty()) {
                        comments = tmp;
                    }
                } catch (NullPointerException nPEx) {
                }
            }

            handler.sendEmptyMessage(SHOW_COMMENTS_LIST);
        } else if (requestCode == SEND_COMMENT_ACTIVITY) {
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void destroy() {
        Statics.onCommentPushedListeners.remove(this);
    }

    /**
     * Refreshes the comments list.
     */
    private void refreshList() {
        adapter.notifyDataSetChanged();

        if (comments.isEmpty()) {
            hasntCommentsLayout.setVisibility(View.VISIBLE);
        } else {
            hasntCommentsLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Shows comments list if it is not empty.
     */
    private void showCommentsList() {
        if (comments == null) {
            return;
        }

        listView.addHeaderView(headerView);

        if (comments.isEmpty()) {
            hasntCommentsLayout.setVisibility(View.VISIBLE);
        }

        adapter = new CommentsAdapter(this, comments, videoItem, widget);
        adapter.setCachePath(cachePath);
        listView.setAdapter(adapter);


        handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

        cacheMessages();
    }

    /**
     * Shows the placeholder if comments list is empty.
     */
    private void showNoComments() {
        try {
            listView.removeHeaderView(headerView);
        } catch (Exception ex) {
            Log.d("", "");
        }

        hasntCommentsLayout.setVisibility(View.VISIBLE);
        hasCommentsLayout.setVisibility(View.GONE);
    }

    /**
     * Caches comments list to device external storage.
     */
    private void cacheMessages() {
        File cacheFile = new File(cachePath);
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }

        File cache = new File(cachePath + "/" + "ca-" + videoItem.getId()
                + "-" + item.getId());
        if (cache.exists()) {
            cache.delete();
            try {
                cache.createNewFile();
            } catch (IOException iOEx) {
            }
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
     * Sets comment avatar image after it was loaded and decoded.
     */
    private void setThumb() {
        if (thumbImageView != null) {
            if (item.getAvatarPath().length() > 0) {
                thumbImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Bitmap bitmap = null;

                try {
                    bitmap = decodeImageFile(item.getAvatarPath());
                } catch (Exception e) {
                    Log.d("", "");
                }

                if (bitmap != null) {
                    thumbImageView.setImageBitmap(bitmap);
                }
            }
        }
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

            float matrixWidth = (float) (imageWidth - 4) / (float) l;
            float matrixHeight = (float) (imageHeight - 4) / (float) l;
            Matrix matrix = new Matrix();
            matrix.postScale(matrixWidth, matrixHeight);

            return Bitmap.createBitmap(bitmap, x, y, l, l, matrix, true);
        } catch (Exception e) {
            Log.d("", "");
        }

        return null;
    }

    /**
     * Checks user authorization and posts new comment.
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
                    multipartEntity.addPart("parent_id", new StringBody(videoItem.getId() + "", Charset.forName("UTF-8")));
                    if (item != null) {
                        multipartEntity.addPart("reply_id", new StringBody(item.getId() + "", Charset.forName("UTF-8")));
                    }

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

                    Log.d("", "");

                } catch (Exception e) {
                    Log.d("", "");
                }

                handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);

            }
        }).start();
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

    /**
     * Cleans comment EditText and hides the soft keyboard.
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

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void closeActivity() {
        hideProgressDialog();
        finish();
    }

    public void onClick(View arg0) {
        if (arg0 == postCommentButton) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni != null) {
                if (ni.isConnectedOrConnecting()) {
                    if (!Authorization.isAuthorized()) {
                        actionIntent = new Intent(this, SendMessageActivity.class);
                        actionIntent.putExtra("Widget", widget);
                        actionIntent.putExtra("item", videoItem);
                        actionIntent.putExtra("message", item);

                        Intent it = new Intent(this, AuthorizationActivity.class);
                        it.putExtra("Widget", widget);
                        startActivityForResult(it, AUTHORIZATION_ACTIVITY);
                    } else {
                        postComment();
                    }
                } else {
                    handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                }
            }
        }
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (arg0 == listView) {
            if (keyboardShown) {
                hideKeyboard();
            }
        }
    }

    /**
     * This callback is called when new comment was pushed.
     * @param comment new comment that was pushed
     */
    public void onCommentPushed(final CommentItem comment) {
        if (comment != null) {
            if ((comment.getReplyId() == item.getId())
                    && (comment.getTrackId() == videoItem.getId())) {
                if (comments == null) {
                    comments = new ArrayList<CommentItem>();
                }

                comments.add(comment);

                if (comments.size() == 1) {
                    handler.sendEmptyMessage(SHOW_COMMENTS_LIST);
                } else {
                    handler.sendEmptyMessage(REFRESH_LIST);
                }
            }
        }
    }

    /**
     * This callback is called when comment avatar was downloaded.
     */
    private void downloadComplete() {
        setThumb();
    }

    private void downloadRegistration(String value) {
        item.setAvatarPath(value);
    }

    /**
     * This class creates a background thread to download video thumbnail.
     */
    private class ImageDownloadTask extends AsyncTask<CommentItem, String, Void> {

        @Override
        protected Void doInBackground(CommentItem... items) {
            try {//ErrorLogging

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;

                if (isCancelled()) {
                    downloadComplete();
                    return null;
                }

                items[0].setAvatarPath(cachePath + "/images/"
                        + Utils.md5(items[0].getAvatarUrl()));

                if (items[0].getAvatarPath().length() > 0) {
                    File file = new File(items[0].getAvatarPath());
                    if (file.exists()) {
                        downloadComplete();
                        return null;
                    }
                }

                if (items[0].getAvatarUrl().length() == 0) {
                    downloadComplete();
                    return null;
                }

                SystemClock.sleep(10);
                try {
                    URL imageUrl = new URL(URLDecoder.decode(items[0].getAvatarUrl()));
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
                    String filename = cachePath + "/images/" + Utils.md5(items[0].getAvatarUrl());
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
