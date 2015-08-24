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
package com.ibuildapp.romanblack.AudioPlugin.utils;

import android.text.TextUtils;
import android.util.Log;
import com.appbuilder.sdk.android.authorization.Authorization;
import com.appbuilder.sdk.android.authorization.FacebookAuthorizationActivity;
import com.ibuildapp.romanblack.AudioPlugin.Statics;
import com.ibuildapp.romanblack.AudioPlugin.entities.CommentItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.SetItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class provides static methods for JSON parsing.
 */
public class JSONParser {

    /**
     * Parses JSON comments data.
     * @param data JSON data to parse.
     * @return comments array
     */
    public static ArrayList<CommentItem> parseCommentsString(String data) {
        try {
            String resp = data;

            if (resp == null) {
                return null;
            }

            if (resp.length() == 0) {
                return null;
            }

            JSONObject mainObject = new JSONObject(resp);

            JSONArray messagesJSON = mainObject.getJSONArray("data");

            ArrayList<CommentItem> parsedMessages = new ArrayList<CommentItem>();

            for (int i = 0; i < messagesJSON.length(); i++) {
                JSONObject messageJSON = messagesJSON.getJSONObject(i);

                CommentItem tmpMessage = new CommentItem();
                tmpMessage.setId(Long.valueOf(messageJSON.getString("id")).longValue());
                tmpMessage.setAuthor(messageJSON.getString("username"));
                tmpMessage.setDate(new Date(
                        Long.valueOf(messageJSON.getString("create")).longValue()));
                tmpMessage.setAvatarUrl(messageJSON.getString("avatar"));
                tmpMessage.setText(messageJSON.getString("text"));

                try {
                    tmpMessage.setTrackId(Long.valueOf(messageJSON.getString("parent_id")).longValue());
                } catch (NumberFormatException nFEx) {
                    Log.e("", "");
                }

                try {
                    tmpMessage.setReplyId(Integer.valueOf(messageJSON.getString("reply_id")).intValue());
                } catch (NumberFormatException nFEx) {
                    Log.e("", "");
                }

                try {
                    tmpMessage.setCommentsCount(Integer.valueOf(messageJSON.getString("total_comments")).intValue());
                } catch (Exception ex) {
                    Log.d("", "");
                }

                parsedMessages.add(tmpMessage);
            }

            return parsedMessages;
        } catch (JSONException jSSONEx) {
            return null;
        }
    }

    /**
     * Downloads and parses JSON comments data.
     * @param url URL resource that contains JSON data
     * @return comments array
     */
    public static ArrayList<CommentItem> parseCommentsUrl(String url) {
        try {
            Log.e("URLTAG", url);
            
            String resp = loadURLData(url);

            if (resp == null) {
                return null;
            }

            if (resp.length() == 0) {
                return null;
            }

            JSONObject mainObject = new JSONObject(resp);

            JSONArray messagesJSON = mainObject.getJSONArray("data");

            ArrayList<CommentItem> parsedMessages = new ArrayList<CommentItem>();

            for (int i = 0; i < messagesJSON.length(); i++) {
                JSONObject messageJSON = messagesJSON.getJSONObject(i);

                CommentItem tmpMessage = new CommentItem();
                tmpMessage.setId(Long.valueOf(messageJSON.getString("id")).longValue());
                tmpMessage.setAuthor(messageJSON.getString("username"));
                tmpMessage.setDate(new Date(
                        Long.valueOf(messageJSON.getString("create")).longValue()));
                String date = tmpMessage.getDate().toString();
                tmpMessage.setAvatarUrl(messageJSON.getString("avatar"));
                tmpMessage.setText(messageJSON.getString("text"));

                try {
                    tmpMessage.setTrackId(Long.valueOf(messageJSON.getString("parent_id")).longValue());
                } catch (NumberFormatException nFEx) {
                    Log.e("", "");
                }

                try {
                    tmpMessage.setReplyId(Integer.valueOf(messageJSON.getString("reply_id")).intValue());
                } catch (NumberFormatException nFEx) {
                    Log.e("", "");
                }

                tmpMessage.setCommentsCount(Integer.valueOf(messageJSON.getString("total_comments")).intValue());

                parsedMessages.add(tmpMessage);
            }

            return parsedMessages;
        } catch (JSONException jSSONEx) {
            return null;
        }
    }

    /**
     * Downloads and parses JSON data about videos comments counts.
     * @param queryUrl URL resource that contains JSON data
     * @return comments counts HashMap that contains <video id, comments count>
     */
    public static HashMap<String, String> getVideoCommentsCount(String queryUrl) {
        try {
            Log.d("URLTAG", queryUrl);
            
            String resp = loadURLData(queryUrl);

            HashMap<String, String> result = new HashMap<String, String>();

            JSONObject mainObject = new JSONObject(resp);

            JSONArray commentCountsJSON = mainObject.getJSONArray("data");

            ArrayList<CommentItem> parsedMessages = new ArrayList<CommentItem>();

            for (int i = 0; i < commentCountsJSON.length(); i++) {
                try {
                    JSONObject commentCountJSON = commentCountsJSON.getJSONObject(i);

                    String id = commentCountJSON.getString("id");
                    String totalComments = commentCountJSON.getString("total_comments");

                    result.put(id, totalComments);
                } catch (Exception ex) {
                    Log.d("", "");
                }
            }

            return result;
        } catch (Exception ex) {
            Log.d("", "");

            return null;
        }
    }

    /**
     * Prepares JSON data URL, downloads and parses JSON data about videos comments counts.
     * @return comments counts HashMap that contains <video id, comments count>
     */
    public static HashMap<String, String> getVideoCommentsCount() {
        return getVideoCommentsCount(Statics.BASE_URL + "/getcommentscount/"
                + com.appbuilder.sdk.android.Statics.appId + "/" + Statics.MODULE_ID + "/"
                + com.appbuilder.sdk.android.Statics.appId + "/"
                + com.appbuilder.sdk.android.Statics.appToken + "/");
    }

    /**
     * Prepares JSON data URL, downloads and parses JSON data about videos Facebook likes counts.
     * @param items video items array
     * @return likes counts HashMap that contains <video URL, likes count>
     */
    public static ArrayList<BasicItem> getVideoLikesCount(ArrayList<BasicItem> items) {
        try {
            String token = FacebookAuthorizationActivity.getFbToken(com.appbuilder.sdk.android.Statics.FACEBOOK_APP_ID, com.appbuilder.sdk.android.Statics.FACEBOOK_APP_SECRET);
            if ( TextUtils.isEmpty(token) )
                return items;

            if ( items == null || items.isEmpty() )
                return items;

            // collect urls list
            List<String> urlList = new ArrayList<String>();
            for (int i = 0; i < items.size(); i++) {
                BasicItem item = items.get(i);
                if ( !TextUtils.isEmpty(item.getPermalinkUrl()) )
                    urlList.add(URLEncoder.encode(items.get(i).getPermalinkUrl()));

                if (item instanceof SetItem) {
                    SetItem setItem = (SetItem) item;

                    for (int j = 0; j < setItem.getTracksCount(); j++) {
                        if ( !TextUtils.isEmpty(URLEncoder.encode(setItem.getTrack(j).getPermalinkUrl())) )
                            urlList.add(URLEncoder.encode(setItem.getTrack(j).getPermalinkUrl()));
                    }
                }
            }

            Map<String,String> likesCounts = FacebookAuthorizationActivity.getLikesForUrls(urlList, token);

            for (int i = 0; i < items.size(); i++) {
                try {
                    String iCountS = likesCounts.get(items.get(i).getPermalinkUrl());
                    if ( iCountS == null )
                        continue;

                    items.get(i).setLikesCount( Integer.parseInt(iCountS) );
                    if ( items.get(i) instanceof SetItem ) {
                        SetItem setItem = (SetItem) items.get(i);

                        for (int j = 0; j < setItem.getTracksCount(); j++) {
                            iCountS = likesCounts.get(setItem.getTrack(j).getPermalinkUrl());
                            if ( iCountS == null )
                                continue;
                            setItem.getTrack(j).setLikesCount(Integer.parseInt(iCountS));
                        }
                    }
                } catch (Exception ex) {
                    return items;
                }
            }

        } catch (Exception ex) {
            return items;
        }

        return items;
    }

    /**
     * Downloads and parses single comment from given URL.
     * @param url comment URL
     * @return comment
     */
    public static CommentItem parseSingleCommentUrl(String url) {
        try {
            Log.e("URLTAG", url);
            
            String resp = loadURLData(url);

            if (resp == null) {
                return null;
            }

            if (resp.length() == 0) {
                return null;
            }

            JSONObject mainObject = new JSONObject(resp);

            JSONArray array = mainObject.getJSONArray("data");

            JSONObject commentObject = array.getJSONObject(0);

            CommentItem tmpMessage = new CommentItem();
            tmpMessage.setId(Long.valueOf(commentObject.getString("id")).longValue());
            tmpMessage.setAuthor(commentObject.getString("username"));
            tmpMessage.setDate(new Date(
                    Long.valueOf(commentObject.getString("create")).longValue()));
            tmpMessage.setAvatarUrl(commentObject.getString("avatar"));
            tmpMessage.setText(commentObject.getString("text"));

            try {
                tmpMessage.setTrackId(Long.valueOf(commentObject.getString("parent_id")).longValue());
            } catch (NumberFormatException nFEx) {
                Log.e("", "");
            }

            try {
                tmpMessage.setReplyId(Integer.valueOf(commentObject.getString("reply_id")).intValue());
            } catch (NumberFormatException nFEx) {
                Log.e("", "");
            }

            try {
                tmpMessage.setCommentsCount(Integer.valueOf(commentObject.getString("total_comments")).intValue());
            } catch (Exception ex) {
                Log.d("", "");
            }

            return tmpMessage;
        } catch (JSONException jSSONEx) {
            return null;
        }
    }

    /**
     * Download URL data to String.
     * @param msgsUrl URL to download
     * @return data string
     */
    private static String loadURLData(String msgsUrl) {
        try {
            URL url = new URL(msgsUrl);
            URLConnection conn = url.openConnection();
            InputStreamReader streamReader = new InputStreamReader(conn.getInputStream());

            BufferedReader br = new BufferedReader(streamReader);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            String resp = sb.toString();

            return resp;
        } catch (IOException iOEx) {
            return "";
        }
    }
}
