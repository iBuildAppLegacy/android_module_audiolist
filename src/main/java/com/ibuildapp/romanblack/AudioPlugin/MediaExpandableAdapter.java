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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.appbuilder.sdk.android.Utils;
import com.appbuilder.sdk.android.Widget;
import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.SetItem;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.http.util.ByteArrayBuffer;

/**
 * This class represents audios list adapter.
 */
public class MediaExpandableAdapter extends BaseExpandableListAdapter {

    private int imageWidth = 50;
    private int imageHeight = 50;
    private String cachePath = "";
    private Widget widget = null;
    private Context ctx = null;
    private LayoutInflater inflater = null;
    private ArrayList<BasicItem> items = null;
    private HashMap<Integer, Bitmap> bitmaps = new HashMap<Integer, Bitmap>();
    private SharePressedListener sharePressedListener = null;
    private OnThumbClickListener onThumbClickListener = null;

    /**
     * Constructs new MediaAdapter instance with given parameteers.
     * @param ctx activity that using this adapter
     * @param items array of audio and audio list items
     * @param widget module configuration data 
     * @param listView ExpandableListView that using this adapter
     */
    public MediaExpandableAdapter(Context ctx, ArrayList<BasicItem> items, Widget widget, ExpandableListView listView) {
        this.ctx = ctx;
        this.items = items;
        this.widget = widget;

        inflater = LayoutInflater.from(this.ctx);

        ImageDownloadTask idt = new ImageDownloadTask();
        idt.execute(items);
    }

    public int getGroupCount() {
        if (items == null) {
            return 0;
        } else {
            return items.size();
        }
    }

    public int getChildrenCount(int arg0) {
        if (!(items.get(arg0) instanceof SetItem)) {
            return 0;
        } else {
            return ((SetItem) items.get(arg0)).getTracksCount();
        }
    }

    public Object getGroup(int arg0) {
        return items.get(arg0);
    }

    public Object getChild(int arg0, int arg1) {
        if (items.get(arg0) instanceof SetItem) {
            return ((SetItem) items.get(arg0)).getTrack(arg1);
        } else {
            return null;
        }
    }

    public long getGroupId(int arg0) {
        return arg0;
    }

    public long getChildId(int arg0, int arg1) {
        return 1000 * arg0 + arg1;
    }

    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getGroupTypeCount() {
        return 2;
    }

    @Override
    public int getGroupType(int groupPosition) {
        if (items.get(groupPosition) instanceof SetItem) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getChildTypeCount() {
        return 1;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return 0;
    }

    public View getGroupView(int arg0, boolean arg1, View arg2, ViewGroup arg3) {
        if (items.get(arg0) instanceof AudioItem) {
            if (arg2 == null) {
                arg2 = inflater.inflate(R.layout.romanblack_audio_list_item, null);
            }

            ImageView thumbImageView = (ImageView) arg2.findViewById(R.id.romanblack_audio_listview_item_thumb);
            thumbImageView.setOnClickListener(new ThumbClickListener(arg0, -1));

            ProgressBar progress = (ProgressBar) arg2.findViewById(R.id.romanblack_audio_listview_item_progress);

            if (((AudioItem) items.get(arg0)).isPlaying()) {
                thumbImageView.setImageResource(R.drawable.romanblack_audio_pause);

                if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_INIT) {
                    progress.setVisibility(View.VISIBLE);
                } else {
                    progress.setVisibility(View.GONE);
                }
            } else {
                progress.setVisibility(View.GONE);
                thumbImageView.setImageResource(R.drawable.romanblack_audio_play);
            }

            TextView titleTextView = (TextView) arg2.findViewById(R.id.romanblack_audio_listview_item_title);
            titleTextView.setTextColor(Statics.color2);
            titleTextView.setText(items.get(arg0).getTitle());

            LinearLayout commentsCounter = (LinearLayout) arg2.findViewById(R.id.romanblack_audio_listview_item_comments_count_layput);
            if (Statics.commentsOn.equalsIgnoreCase("off")) {
                commentsCounter.setVisibility(View.INVISIBLE);
            }

            TextView commentsCountTextView = (TextView) arg2.findViewById(R.id.romanblack_audio_listview_item_comments_count);
            if (items.get(arg0).getTotalComments() == 0) {
                commentsCountTextView.setText("0");
            } else if (items.get(arg0).getTotalComments() > 99) {
                commentsCountTextView.setText("99+");
            } else {
                commentsCountTextView.setText(items.get(arg0).getTotalComments() + "");
            }

            arg2.setOnClickListener(new PlayerStartListener(arg0, -1));
        } else if (items.get(arg0) instanceof SetItem) {
            if (arg2 == null) {
                arg2 = inflater.inflate(R.layout.romanblack_audio_list_item_set, null);
            }

            ImageView thumbImageView = (ImageView) arg2.findViewById(R.id.romanblack_audio_listview_item_thumb);
            thumbImageView.setOnClickListener(new ThumbClickListener(arg0, -1));

            if (items.get(arg0).getCoverUrl().length() > 0) {
                if (thumbImageView != null) {
                    if (items.get(arg0).getCoverPath().length() > 0) {
                        thumbImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                        Bitmap bitmap = null;
                        Integer key = new Integer(arg0);
                        if (bitmaps.containsValue(key)) {
                            bitmap = bitmaps.get(key);
                        } else {
                            try {
                                bitmap = decodeImageFile(items.get(arg0).getCoverPath());
                                bitmaps.put(key, bitmap);
                            } catch (Exception e) {
                                Log.d("", "");
                            }
                        }

                        if (bitmap != null) {
                            thumbImageView.setImageBitmap(bitmap);
                        }
                    }
                }
            } else {
                thumbImageView.setImageDrawable(ctx.getResources().getDrawable(R.drawable.romanblack_audio_placeholder));
            }

            ImageView arrowImageView = (ImageView) arg2.findViewById(R.id.romanblack_audio_listview_item_arrow);
            if (((SetItem) items.get(arg0)).isExpanded()) {
                arrowImageView.setImageResource(R.drawable.romanblack_audio_set_arrow_closed);
            } else {
                arrowImageView.setImageResource(R.drawable.romanblack_audio_set_arrow);
            }

            TextView titleTextView = (TextView) arg2.findViewById(R.id.romanblack_audio_listview_item_title);
            titleTextView.setTextColor(Statics.color2);
            titleTextView.setText(items.get(arg0).getTitle());

            TextView tracksCountTextView = (TextView) arg2.findViewById(R.id.romanblack_audio_listview_tracks_count);
            tracksCountTextView.setText(((SetItem) items.get(arg0)).getTracksCount() + "");

            LinearLayout commentsCounter = (LinearLayout) arg2.findViewById(R.id.romanblack_audio_listview_item_comments_count_layput);
            commentsCounter.setOnClickListener(new PlayerStartListener(arg0, -1));
            if (Statics.commentsOn.equalsIgnoreCase("off")) {
                commentsCounter.setVisibility(View.INVISIBLE);
            }

            TextView commentsCountTextView = (TextView) arg2.findViewById(R.id.romanblack_audio_listview_item_comments_count);
            if (items.get(arg0).getTotalComments() == 0) {
                commentsCountTextView.setText("0");
            } else if (items.get(arg0).getTotalComments() > 99) {
                commentsCountTextView.setText("99+");
            } else {
                commentsCountTextView.setText(items.get(arg0).getTotalComments() + "");
            }

            arg2.setOnClickListener(new ThumbClickListener(arg0, -1));
        }

        ViewTag tag = new ViewTag(arg0, -1);
        arg2.setTag(tag);

        return arg2;
    }

    public View getChildView(int arg0, int arg1, boolean arg2, View arg3, ViewGroup arg4) {
        if (arg3 == null) {
            arg3 = inflater.inflate(R.layout.romanblack_audio_list_item_in_set, null);
        }

        AudioItem item = ((SetItem) items.get(arg0)).getTrack(arg1);

        ImageView thumbImageView = (ImageView) arg3.findViewById(R.id.romanblack_audio_listview_item_thumb);
        thumbImageView.setOnClickListener(new ThumbClickListener(arg0, arg1));

        ProgressBar progress = (ProgressBar) arg3.findViewById(R.id.romanblack_audio_listview_item_progress);

        if (item.isPlaying()) {
            thumbImageView.setImageResource(R.drawable.romanblack_audio_pause);

            if (Statics.serviceManageInterface.getState() == BackGroundMusicService.MediaPlayerStates.PLAYER_INIT) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }
        } else {
            progress.setVisibility(View.GONE);
            thumbImageView.setImageResource(R.drawable.romanblack_audio_play);
        }

        TextView titleTextView = (TextView) arg3.findViewById(R.id.romanblack_audio_listview_item_title);
        titleTextView.setTextColor(Statics.color2);
        titleTextView.setText(item.getTitle());

        LinearLayout commentsCounter = (LinearLayout) arg3.findViewById(R.id.romanblack_audio_listview_item_comments_count_layput);
        if (Statics.commentsOn.equalsIgnoreCase("off")) {
            commentsCounter.setVisibility(View.INVISIBLE);
        }

        TextView commentsCountTextView = (TextView) arg3.findViewById(
                R.id.romanblack_audio_listview_item_comments_count);
        if (item.getTotalComments() == 0) {
            commentsCountTextView.setText("0");
        } else if (item.getTotalComments() > 99) {
            commentsCountTextView.setText("99+");
        } else {
            commentsCountTextView.setText(item.getTotalComments() + "");
        }

        arg3.setOnClickListener(new PlayerStartListener(arg0, arg1));

        ViewTag tag = new ViewTag(arg0, arg1);
        arg3.setTag(tag);

        return arg3;
    }

    public boolean isChildSelectable(int arg0, int arg1) {
        return false;
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
     * Refreshes rss list if image was downloaded.
     */
    private void viewUpdated() {
        this.notifyDataSetChanged();
    }

    /**
     * Refreshes rss list if image was downloaded.
     */
    private void downloadComplete() {
        this.notifyDataSetChanged();
    }

    private void downloadRegistration(int position, String value) {
        this.items.get(position).setCoverPath(value);
    }

    private void downloadRegistration(int position, int secondaryPosition, String value) {
        if (items.get(position) instanceof SetItem) {
            ((SetItem) items.get(position)).getTrack(secondaryPosition).setCoverPath(value);
        }
    }

    /**
     * Sets the external storage cache path.
     * @param cachePath the cache path to set
     */
    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    /**
     * Sets "Share" button on click listener.
     * @param sharePressedListener the listener to set
     */
    public void setSharePressedListener(SharePressedListener sharePressedListener) {
        this.sharePressedListener = sharePressedListener;
    }

    /**
     * Callback for Facebook likes.
     * Must be implemented by Activity that using this Adapter.
     */
    public void setOnThumbClickListener(OnThumbClickListener onThumbClickListener) {
        this.onThumbClickListener = onThumbClickListener;
    }

    /**
     * This class creates a background thread to download avatar images of comments.
     */
    private class ImageDownloadTask extends AsyncTask<ArrayList<BasicItem>, String, Void> {

        @Override
        protected Void doInBackground(ArrayList<BasicItem>... items) {
            try {//ErrorLogging

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;

                for (int i = 0; i < items[0].size(); i++) {
                    if (isCancelled()) {
                        return null;
                    }

                    items[0].get(i).setCoverPath(cachePath + "/images/" + Utils.md5(items[0].get(i).getCoverUrl()));

                    if (items[0].get(i).getCoverPath().length() > 0) {
                        File file = new File(items[0].get(i).getCoverPath());
                        if (file.exists()) {
                            publishProgress();
                            continue;
                        }
                    }

                    boolean needLoad = true;

                    if (items[0].get(i).getCoverUrl().length() == 0) {
                        needLoad = false;
                    }

                    if (needLoad) {
                        SystemClock.sleep(10);
                        try {
                            URL imageUrl = new URL(URLDecoder.decode(items[0].get(i).getCoverUrl()));
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
                            String filename = cachePath + "/images/" + Utils.md5(items[0].get(i).getCoverUrl());
                            FileOutputStream fos = new FileOutputStream(new File(filename));
                            fos.write(baf.toByteArray());
                            fos.close();

                            downloadRegistration(i, filename);
                        } catch (Exception e) {
                            Log.e("", "");
                        }

                        publishProgress();
                    }

                    if (items[0].get(i) instanceof SetItem) {
                        SetItem set = (SetItem) items[0].get(i);

                        for (int j = 0; j < set.getTracksCount(); j++) {
                            AudioItem item = set.getTrack(j);

                            item.setCoverPath(cachePath + "/images/" + Utils.md5(item.getCoverUrl()));

                            if (item.getCoverPath().length() > 0) {
                                File file = new File(item.getCoverPath());
                                if (file.exists()) {
                                    publishProgress();
                                    continue;
                                }
                            }

                            if (item.getCoverUrl().length() == 0) {
                                continue;
                            }

                            SystemClock.sleep(10);
                            try {
                                URL imageUrl = new URL(URLDecoder.decode(item.getCoverUrl()));
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
                                String filename = cachePath + "/images/" + Utils.md5(item.getCoverUrl());
                                FileOutputStream fos = new FileOutputStream(new File(filename));
                                fos.write(baf.toByteArray());
                                fos.close();

                                downloadRegistration(i, filename);
                            } catch (Exception e) {
                                Log.e("", "");
                            }
                        }
                    }
                }

                return null;

            } catch (Exception e) {//ErrorLogging
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... param) {
            viewUpdated();
        }

        @Override
        protected void onPostExecute(Void unused) {
            downloadComplete();
        }
    }

    /**
     * OnClickListener to set to share button.
     */
    public class BtnShareListener implements View.OnClickListener {

        private int position = 0;
        private int childPosition = 0;

        public BtnShareListener(int position, int childPosition) {
            this.position = position;
            this.childPosition = childPosition;
        }

        public void onClick(View arg0) {
            if (Utils.networkAvailable((Activity) ctx)) {
                if (sharePressedListener != null) {
                    sharePressedListener.onSharePressed(position, childPosition);
                }
            } else {
                Toast.makeText(ctx, ctx.getResources().getString(R.string.romanblack_audio_alert_share_need_internet),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * OnClickListener to set to adapter result item.
     */
    public class PlayerStartListener implements View.OnClickListener {

        private int position = 0;
        private int childPosition = 0;

        public PlayerStartListener(int position, int childPosition) {
            this.position = position;
            this.childPosition = childPosition;
        }

        public void onClick(View arg0) {
            if (Statics.commentsOn.equals("on")) {
                for(int i = 0; i < items.size(); i++) {
                    if(i == position)
                        continue;
                    if (items.get(i) instanceof AudioItem) {
                        AudioItem audioItem = (AudioItem) items.get(i);
                        if (audioItem.isPlaying()) {
                            Statics.serviceManageInterface.pause();
                        }
                    }
                }
                Intent it = new Intent(ctx, AudioPreviewActivity.class);
                it.putExtra("items", items);
                it.putExtra("position", position);
                it.putExtra("childPosition", childPosition);
                it.putExtra("cachePath", cachePath);
                it.putExtra("Widget", widget);
                ctx.startActivity(it);
            }
        }
    }

    /**
     * OnClickListener to set to audio thumbnail.
     */
    public class ThumbClickListener implements View.OnClickListener {

        private int position = 0;
        private int childPosition = -1;

        public ThumbClickListener(int position, int childPosition) {
            this.position = position;
            this.childPosition = childPosition;
        }

        public void onClick(View arg0) {
            if (onThumbClickListener != null) {
                onThumbClickListener.onThumbClick(position, childPosition);
            }
        }
    }

    /* CallBack for Facebook likes*/

    /* CallBack for Sharing
     * Must be implemented by Activity using this Adapter
     */
    public interface SharePressedListener {

        public void onSharePressed(int position, int childPosition);
    }
    /* CallBack for Thumb clicks*/

    /* CallBack for Thumb clicks
     * Must be implemented by Activity using this Adapter
     */
    public interface OnThumbClickListener {

        public void onThumbClick(int position, int childPosition);
    }
    /* CallBack for Facebook likes*/

    public static class ViewTag implements Serializable {

        public ViewTag(int position, int childPosition) {
            this.position = position;
            this.childPosition = childPosition;
        }
        private int position = -1;
        private int childPosition = -1;

        public int getPosition() {
            return this.position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getChildPosition() {
            return this.childPosition;
        }

        public void setChildPosition(int childPosition) {
            this.childPosition = childPosition;
        }

        public boolean isChild() {
            return childPosition >= 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ViewTag) {
                ViewTag tag = (ViewTag) o;

                if (this.position == tag.getPosition() && this.childPosition == tag.getChildPosition()) {
                    return true;
                }
            }

            return false;
        }
    }
}
