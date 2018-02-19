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

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.widget.TextView.OnEditorActionListener;
import com.appbuilder.sdk.android.AppBuilderModuleMain;
import com.appbuilder.sdk.android.Widget;
import com.appbuilder.sdk.android.authorization.Authorization;
import com.appbuilder.sdk.android.authorization.entities.User;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.CommentItem;
import com.ibuildapp.romanblack.AudioPlugin.utils.JSONParser;
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

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This activity provides send comment functionality.
 */
public class SendMessageActivity extends AppBuilderModuleMain implements OnClickListener,
        OnEditorActionListener, TextWatcher, ViewTreeObserver.OnGlobalLayoutListener {

    private final int TAKE_A_PICTURE_ACTIVITY = 10000;
    private final int PICK_IMAGE_ACTIVITY = 10001;
    private final int CLOSE_ACTIVITY_OK = 0;
    private final int CLOSE_ACTIVITY_BAD = 1;
    private final int PROGRESS_SIZE = 20;
    private boolean uploading = false;
    private String imagePath = "";
    private CommentItem message = null;
    private CommentItem recievedMessage = null;
    private BasicItem video = null;
    private LinearLayout root = null;
    private LinearLayout bottomPanel = null;
    private LinearLayout editLayout = null;
    private LinearLayout header = null;
    private View headerView = null;
    private EditText messageEditText = null;
    private TextView clearButton = null;
    private TextView postButton = null;
    private TextView symbolCounter = null;
    private ProgressBar progressBar = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case CLOSE_ACTIVITY_OK: {
                    closeActivityOK();
                }
                break;
                case CLOSE_ACTIVITY_BAD: {
                    closeActivityBad();
                }
                break;
            }
        }
    };

    @Override
    public void create() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.romanblack_audio_send_message);

        // set topbar title
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.INVISIBLE);
        float density = getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams((int) (PROGRESS_SIZE * density), (int) (PROGRESS_SIZE * density));
        progressBar.setLayoutParams(params);

        if(!showSideBar)
            drawTopBarRightButton(progressBar);

        setTopBarTitle(getResources().getString(R.string.romanblack_audio_preview_capture));
        swipeBlock();
        setTopBarLeftButtonText(getResources().getString(R.string.back), true, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Intent currentIntent = getIntent();

        message = (CommentItem) currentIntent.getSerializableExtra("message");

        video = (BasicItem) currentIntent.getSerializableExtra("item");

        messageEditText = (EditText) findViewById(R.id.romanblack_audio_sendmessage_edittext);
        messageEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        messageEditText.addTextChangedListener(this);

        clearButton = (TextView) findViewById(R.id.romanblack_audio_sendmessage_clear_btn);
        clearButton.setOnClickListener(this);

        postButton = (TextView) findViewById(R.id.romanblack_audio_sendmessage_post_btn);
        postButton.setOnClickListener(this);
        designButton(postButton, bottomBarDesign.rightButtonDesign);

        symbolCounter = (TextView) findViewById(R.id.romanblack_audio_sendmessage_symbols_counter);

        bottomPanel = (LinearLayout) findViewById(R.id.romanblack_audio_sendmessage_bottom_panel);
        bottomPanel.setVisibility(View.GONE);

        root = (LinearLayout) findViewById(R.id.romanblack_audio_sendmessage_root);
        root.setBackgroundColor(Statics.backgroundColor);
        root.getViewTreeObserver().addOnGlobalLayoutListener(this);

        editLayout = (LinearLayout) findViewById(R.id.romanblack_audio_sendmessage_edittext_layout);

        header = (LinearLayout) findViewById(R.id.romanblack_audio_sendmessage_header);


        if (message == null) {
            setTopBarTitle(getResources().getString(R.string.romanblack_audio_preview_capture));

            try {
                headerView = LayoutInflater.from(this).inflate(R.layout.romanblack_audio_comments_list_header, null);
                ((TextView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_title)).setText(video.getTitle());
                ((TextView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_description)).setText(video.getDescription());
                ((ImageView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_thumb)).setImageBitmap(decodeImageFile(video.getCoverPath(), 70, 70));
                if (video.getCoverUrl() == null) {
                    ((ImageView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_thumb)).setImageResource(R.drawable.romanblack_audio_placeholder);
                }
                if (video.getCoverUrl().length() == 0) {
                    ((ImageView) headerView.findViewById(R.id.romanblack_audio_comments_listview_header_thumb)).setImageResource(R.drawable.romanblack_audio_placeholder);
                }
            } catch (Exception ex) {
                Log.d("", "");
            }
        } else {
            setTopBarTitle(getResources().getString(R.string.romanblack_audio_reply));
            try {
                headerView = LayoutInflater.from(this).inflate(R.layout.romanblack_audio_commentstocomments_header, null);
                ((TextView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_name)).setText(message.getAuthor());
                ((TextView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_text)).setText(message.getText());
                String dateText = "";
                try {
                    long msgDate = message.getDate().getTime();
                    long curDate = System.currentTimeMillis();
                    long interval = curDate - msgDate;

                    if (interval < 60 * 1000) {
                        dateText = getResources().getString(R.string.romanblack_audio_date_just_now);
                    } else if (interval < 2 * 60 * 1000) {
                        dateText = getResources().getString(R.string.romanblack_audio_date_one_minute);
                    } else if (interval < 60 * 60 * 1000) {
                        dateText = (interval / 60 / 1000) + " "
                                + getResources().getString(R.string.romanblack_audio_date_minutes);
                    } else if (interval < 2 * 60 * 60 * 1000) {
                        dateText = getResources().getString(R.string.romanblack_audio_date_one_hour);
                    } else if (interval < 24 * 60 * 60 * 1000) {
                        dateText = (interval / 60 / 60 / 1000) + " "
                                + getResources().getString(R.string.romanblack_audio_date_hours);
                    } else if (interval < 2 * 24 * 60 * 60 * 1000) {
                        dateText = getResources().getString(R.string.romanblack_audio_date_yesterday);
                    } else if (interval < 5 * 24 * 60 * 60 * 1000) {
                        dateText = (interval / 24 / 60 / 60 / 1000) + " "
                                + getResources().getString(R.string.romanblack_audio_date_days);
                    } else {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM, d");
                            dateText = sdf.format(new Date(interval));
                        } catch (Exception ex) {
                            Log.d("", "");
                        }
                    }
                } catch (Exception ex) {
                }
                ((TextView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_date)).setText(dateText);
                ((ImageView) headerView.findViewById(R.id.romanblack_audio_commentstocomment_listview_header_thumb)).setImageBitmap(decodeImageFile(message.getAvatarPath(), 70, 70));
            } catch (Exception ex) {
                Log.d("", "");
            }
        }

        header.addView(headerView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d("", "");
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_A_PICTURE_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                imagePath = data.getStringExtra("imagePath");

                if (imagePath == null) {
                    return;
                }

                if (imagePath.length() == 0) {
                    return;
                }
            }
        } else if (requestCode == PICK_IMAGE_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(
                        selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();

                imagePath = filePath;

                if (imagePath == null) {
                    return;
                }

                if (imagePath.length() == 0) {
                    return;
                }

                if (imagePath.startsWith("http")) {
                    Toast.makeText(this, "This image cannot be selected", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    /**
     * Decodes image file to bitmap from device external storage.
     * @param imagePath image file path
     * @return decoded image bitmap
     */
    private Bitmap decodeImageFile(String imagePath, int imageWidth, int imageHeight) {
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
     * Hides header view when keyboard is shown.
     */
    private void keyboardShown() {
        header.setVisibility(View.VISIBLE);
        bottomPanel.setVisibility(View.VISIBLE);

        int height = (int) getResources().getDisplayMetrics().density * 90;
        editLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, height));
    }

    /**
     * Shows header view when keyboard is hidden.
     */
    private void keyboardHidden() {
        header.setVisibility(View.VISIBLE);
        bottomPanel.setVisibility(View.GONE);

        int height = (int) getResources().getDisplayMetrics().density * 90;
        editLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, height));
    }

    /**
     * Closes this activity with "OK" result.
     */
    private void closeActivityOK() {
        Intent it = new Intent();
        it.putExtra("message", recievedMessage);
        setResult(RESULT_OK, it);

        finish();
    }

    /**
     * Closes this activity with "Canceled" result.
     */
    private void closeActivityBad() {
        setResult(RESULT_CANCELED);

        finish();
    }

    public void onClick(View arg0) {
        if (!uploading) {
            if (arg0.getId() == R.id.romanblack_audio_sendmessage_clear_btn) {
                messageEditText.setText("");
            } else if (arg0.getId() == R.id.romanblack_audio_sendmessage_post_btn) {
                if (messageEditText.getText().length() < 1) {
                    Toast.makeText(this, R.string.romanblack_audio_alert_empty_message,
                            Toast.LENGTH_LONG).show();
                }

                progressBar.setVisibility(View.VISIBLE);

                uploading = true;
                if (messageEditText.getText().length() > 150) {
                    Toast.makeText(this, R.string.romanblack_audio_alert_big_text,
                            Toast.LENGTH_LONG).show();
                    uploading = false;

                    progressBar.setVisibility(View.INVISIBLE);

                    return;
                }

                if ((messageEditText.getText().length() == 0)) {
                    Toast.makeText(this, R.string.romanblack_audio_alert_empty_message,
                            Toast.LENGTH_LONG).show();
                    uploading = false;

                    progressBar.setVisibility(View.INVISIBLE);

                    return;
                }

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
                            multipartEntity.addPart("parent_id", new StringBody(video.getId() + "", Charset.forName("UTF-8")));
                            if (message != null) {
                                multipartEntity.addPart("reply_id", new StringBody(message.getId() + "", Charset.forName("UTF-8")));
                            }
                            if (Authorization.getAuthorizedUser().getAccountType() == User.ACCOUNT_TYPES.FACEBOOK) {
                                multipartEntity.addPart("account_type", new StringBody("facebook", Charset.forName("UTF-8")));
                            } else if (Authorization.getAuthorizedUser().getAccountType() == User.ACCOUNT_TYPES.TWITTER) {
                                multipartEntity.addPart("account_type", new StringBody("twitter", Charset.forName("UTF-8")));
                            } else {
                                multipartEntity.addPart("account_type", new StringBody("ibuildapp", Charset.forName("UTF-8")));
                            }
                            multipartEntity.addPart("account_id", new StringBody(Authorization.getAuthorizedUser().getAccountId(), Charset.forName("UTF-8")));
                            multipartEntity.addPart("username", new StringBody(Authorization.getAuthorizedUser().getFullName(), Charset.forName("UTF-8")));
                            multipartEntity.addPart("avatar", new StringBody(Authorization.getAuthorizedUser().getAvatarUrl(), Charset.forName("UTF-8")));

                            multipartEntity.addPart("text", new StringBody(messageEditText.getText().toString(), Charset.forName("UTF-8")));

                            httpPost.setEntity(multipartEntity);

                            Statics.onPost();

                            String resp = httpClient.execute(httpPost, new BasicResponseHandler());

                            recievedMessage = JSONParser.parseCommentsString(resp).get(0);

                            Log.d("", "");

                            handler.sendEmptyMessage(CLOSE_ACTIVITY_OK);

                        } catch (Exception e) {
                            Log.d("", "");

                            handler.sendEmptyMessage(CLOSE_ACTIVITY_BAD);
                        }

                    }
                }).start();
            }
        }
    }

    public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
        return false;
    }

    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    /**
     * Updates symbol counter when message text was changed.
     */
    public void afterTextChanged(Editable arg0) {
        if (symbolCounter == null) {
            symbolCounter = (TextView) findViewById(R.id.romanblack_audio_sendmessage_symbols_counter);
        }
        symbolCounter.setText(arg0.length() + "/150");
    }

    public void onGlobalLayout() {
        int heightDiff = root.getRootView().getHeight() - root.getHeight();
        if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
            keyboardShown();
        } else {
            keyboardHidden();
        }
    }
}
