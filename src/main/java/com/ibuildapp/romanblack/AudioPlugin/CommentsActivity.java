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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.appbuilder.sdk.android.AppBuilderModule;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import com.appbuilder.sdk.android.authorization.Authorization;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnCommentPushedListener;
import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
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
import java.util.ArrayList;
import org.apache.http.util.ByteArrayBuffer;

/**
 * This activity represents comments list to audio.
 */
public class CommentsActivity extends AppBuilderModule implements
        OnClickListener, OnItemClickListener, OnCommentPushedListener {

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
    private ACTIONS action = ACTIONS.ACTION_NO;
    private int imageWidth = 100;
    private int imageHeight = 70;
    private int position = 0;
    private String cachePath = "";
    private AudioItem item = null;
    private Widget widget = null;
    private CommentsAdapter adapter = null;
    private Intent actionIntent = null;
    private View headerView = null;
    private ImageView thumbImageView = null;
    private TextView titleTextView = null;
    private TextView descriptionTextView = null;
    private TextView homeBtn = null;
    private ListView listView = null;
    private ProgressDialog progressDialog = null;
    private LinearLayout hasCommentsLayout = null;
    private LinearLayout hasntCommentsLayout = null;
    private LinearLayout headerLayout = null;
    private Button postCommentButton = null;
    private ImageView postCommentButtonTop = null;
    private ArrayList<AudioItem> items = null;
    private ArrayList<CommentItem> comments = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case INITIALIZATION_FAILED: {
                    Toast.makeText(CommentsActivity.this,
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
                    Toast.makeText(CommentsActivity.this,
                            getResources().getString(
                            R.string.romanblack_audio_alert_no_internet),
                            Toast.LENGTH_LONG).show();
                }
                break;
                case REFRESH_LIST: {
                    refreshList();
                }
                break;
            }
        }
    };

    @Override
    public void create() {
        /* Removing page header 
         */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.romanblack_audio_comments_main);

        Intent currentIntent = getIntent();
        items = (ArrayList<AudioItem>) currentIntent.getSerializableExtra("items");

        if (items == null) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

        if (items.isEmpty()) {
            handler.sendEmptyMessage(INITIALIZATION_FAILED);
            return;
        }

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

        listView = (ListView) findViewById(R.id.romanblack_audio_comments_main_listview);
        listView.setCacheColorHint(Color.parseColor("#41464b"));
        listView.setHeaderDividersEnabled(false);

        headerView = LayoutInflater.from(this).inflate(R.layout.romanblack_audio_comments_list_header, null);

        thumbImageView = (ImageView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_thumb);
        ImageDownloadTask idt = new ImageDownloadTask();
        idt.execute(item);

        titleTextView = (TextView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_title);
        titleTextView.setText(item.getTitle());

        descriptionTextView = (TextView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_description);
        descriptionTextView.setText(item.getDescription());

        hasCommentsLayout = (LinearLayout) findViewById(R.id.romanblack_audio_comments_main_has_layout);
        hasCommentsLayout.setVisibility(View.GONE);

        if (!Statics.isOnline) {
            postCommentButton.getBackground().setAlpha(100);
            postCommentButton.setTextColor(Color.parseColor("#9bffffff"));
            postCommentButtonTop.setAlpha(100);
        }

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
                    String commentsUrl ="http://" + Statics.BASE_URL + "/getcomments/"
                            + com.appbuilder.sdk.android.Statics.appId + "/" + Statics.MODULE_ID + "/"
                            + item.getId() + "/0/"
                            + com.appbuilder.sdk.android.Statics.appId + "/"
                            + com.appbuilder.sdk.android.Statics.appToken;

                    comments = JSONParser.parseCommentsUrl(commentsUrl);
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
                    if (comments.isEmpty()) {
                        handler.sendEmptyMessage(SHOW_NO_COMMENTS);
                    } else {
                        handler.sendEmptyMessage(SHOW_COMMENTS_LIST);
                    }
                } catch (NullPointerException nPEx) {
                    handler.sendEmptyMessage(SHOW_NO_COMMENTS);
                }

                handler.sendEmptyMessage(HIDE_PROGRESS_DIALOG);
            }
        }).start();

        Statics.onCommentPushedListeners.add(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTHORIZATION_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                if (action == ACTIONS.SEND_MESSAGE) {
                    startActivityForResult(actionIntent, SEND_COMMENT_ACTIVITY);
                }
            } else {
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
     * Refreshes comments list.
     */
    private void refreshList() {
        adapter.notifyDataSetChanged();
    }

    /**
     * Shows comments list if it is not empty.
     */
    private void showCommentsList() {
        if (comments == null) {
            return;
        }

        if (comments.isEmpty()) {
            return;
        }

        try {
            headerLayout.removeAllViews();
        } catch (Exception ex) {
            Log.d("", "");
        }

        try {
            listView.removeHeaderView(headerView);
        } catch (Exception ex) {
            Log.d("", "");
        }

        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                100, getResources().getDisplayMetrics());
        int height = (int) (px);
        headerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,
                AbsListView.LayoutParams.WRAP_CONTENT));
        listView.addHeaderView(headerView, null, false);

        hasCommentsLayout.setVisibility(View.VISIBLE);
        hasntCommentsLayout.setVisibility(View.GONE);

        adapter = new CommentsAdapter(this, comments, item, widget);
        adapter.setCachePath(cachePath);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);

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

        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                100, getResources().getDisplayMetrics());
        int height = (int) (px);
        headerLayout.addView(headerView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

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

        File cache = new File(cachePath + "/" + "ca-" + item.getId() + "-0");
        if (cache.exists()) {
            cache.delete();
        }

        try {
            cache.createNewFile();
        } catch (IOException iOEx) {
            Log.d("", "");
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
            Log.d("", "");
        }
    }

    /**
     * Sets video thumbnail after it was loaded and decoded.
     */
    private void setThumb() {
        if (thumbImageView != null) {
            if (item.getCoverPath().length() > 0) {
                thumbImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Bitmap bitmap = null;

                try {
                    bitmap = decodeImageFile(item.getCoverPath());
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

    public void onClick(View arg0) {
        if (arg0 == homeBtn) {
            finish();
        } else if (arg0 == postCommentButton || arg0 == postCommentButtonTop) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni != null) {
                if (ni.isConnectedOrConnecting()) {
                    if (!Authorization.isAuthorized()) {
                        actionIntent = new Intent(this, SendMessageActivity.class);
                        actionIntent.putExtra("Widget", widget);
                        actionIntent.putExtra("video", item);

                        action = ACTIONS.SEND_MESSAGE;

                        Intent it = new Intent(this, AuthorizationActivity.class);
                        it.putExtra("Widget", widget);
                        startActivityForResult(it, AUTHORIZATION_ACTIVITY);
                    } else {
                        Intent it = new Intent(this, SendMessageActivity.class);
                        it.putExtra("Widget", widget);
                        it.putExtra("video", item);
                        startActivityForResult(it, SEND_COMMENT_ACTIVITY);
                    }
                } else {
                    handler.sendEmptyMessage(NEED_INTERNET_CONNECTION);
                }
            }
        }
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
    }

    /**
     * This callback is called when new comment was pushed.
     * @param comment new comment that was pushed
     */
    public void onCommentPushed(final CommentItem comment) {
        if (comment != null) {
            if ((comment.getReplyId() == 0)
                    && (comment.getTrackId() == item.getId())) {
                if (comments == null) {
                    comments = new ArrayList<CommentItem>();
                }

                comments.add(comment);

                if (comments.size() == 1) {
                    handler.sendEmptyMessage(SHOW_COMMENTS_LIST);
                } else {
                    handler.sendEmptyMessage(REFRESH_LIST);
                }
            } else if (comment.getTrackId() == item.getId()) {
                if (comments != null) {
                    if (!comments.isEmpty()) {
                        for (int i = 0; i < comments.size(); i++) {
                            if (comments.get(i).getId() == comment.getId()) {
                                comments.get(i).setCommentsCount(comments.get(i).getCommentsCount() + 1);

                                break;
                            }
                        }

                        handler.sendEmptyMessage(REFRESH_LIST);
                    }
                }
            }
        }
    }

    /**
     * This callback is called when video thumbnail was downloaded.
     */
    private void downloadComplete() {
        setThumb();
    }

    private void downloadRegistration(String value) {
        item.setCoverPath(value);
    }

    /**
     * This class creates a background thread to download video thumbnail.
     */
    private class ImageDownloadTask extends AsyncTask<AudioItem, String, Void> {

        @Override
        protected Void doInBackground(AudioItem... items) {
            try {//ErrorLogging

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;

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
