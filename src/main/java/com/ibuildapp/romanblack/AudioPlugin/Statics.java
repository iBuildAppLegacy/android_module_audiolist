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

import android.graphics.Color;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnAuthListener;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnCommentPushedListener;
import com.ibuildapp.romanblack.AudioPlugin.callback.OnPostListener;
import com.ibuildapp.romanblack.AudioPlugin.callback.ServiceCallback;
import com.ibuildapp.romanblack.AudioPlugin.callback.ServiceManageInterface;
import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.CommentItem;
import java.util.ArrayList;

/**
 * This class contains global module variables.
 */
public class Statics {
    /*Module customization*/
    
    static int closedOrder = -1;

    static int backgroundColor = Color.parseColor("#cccccc");
    /*Module customization*/
    /* Color Scheme */
    static int color1 = Color.parseColor("#4d4948");// background
    static int color2 = Color.parseColor("#fff58d");// category header
    static int color3 = Color.parseColor("#fff7a2");// text header 
    static int color4 = Color.parseColor("#ffffff");// text
    static int color5 = Color.parseColor("#bbbbbb");// date
    /* Color Scheme ends */

    enum STATES {

        NO_MESSAGES, HAS_MESSAGES, AUTHORIZATION_NO, AUTHORIZATION_YES,
        AUTHORIZATION_FACEBOOK, AUTHORIZATION_TWITTER, AUTHORIZATION_EMAIL
    };
    /**
     * Base module URL.
     * This URL depending on service domen.
     */
  //  public static final String BASE_URL = "https://" + com.appbuilder.sdk.android.Statics.BASE_DOMEN + "/mdscr/audio";
    public static final String BASE_URL = com.appbuilder.sdk.android.Statics.BASE_DOMEN + "/mdscr/audio";
 //   public static final String BASE_URLTEST =  com.appbuilder.sdk.android.Statics.BASE_DOMEN + "/mdscr/audio";
    public static String APP_ID = "0";
    public static String MODULE_ID = "0";
    public static String APP_NAME = "";
    public static String FACEBOOK_APP_TOKEN = "";
    static String sharingOn = "off";
    static String commentsOn = "off";
    static boolean isOnline = false;
    static ServiceManageInterface serviceManageInterface = null;
    static ArrayList<OnAuthListener> onAuthListeners = new ArrayList<OnAuthListener>();
    static ArrayList<OnCommentPushedListener> onCommentPushedListeners = new ArrayList<OnCommentPushedListener>();
    static ArrayList<OnPostListener> onPostListeners = new ArrayList<OnPostListener>();
    static final ArrayList<ServiceCallback> serviceCallbacks = new ArrayList<ServiceCallback>();

    /**
     * This happen when user authorized.
     * This method call all preset callbacks.
     */
    static void onAuth() {
        if (onAuthListeners != null) {
            if (!onAuthListeners.isEmpty()) {
                for (int i = 0; i < onAuthListeners.size(); i++) {
                    onAuthListeners.get(i).onAuth();
                }
            }
        }
    }

    /**
     * This new comment information was recieved.
     * This method call all preset callbacks.
     */
    static void onCommentPushed(String appId, String moduleId, CommentItem comment) {
        if (onCommentPushedListeners != null) {
            if (!onCommentPushedListeners.isEmpty()) {
                for (int i = 0; i < onCommentPushedListeners.size(); i++) {
                    if (APP_ID.equals(appId) && MODULE_ID.equals(moduleId)) {
                        onCommentPushedListeners.get(i).
                                onCommentPushed(comment);
                    }
                }
            }
        }
    }

    /**
     * This happen when comments list was updates.
     * This method call all preset callbacks.
     */
    static void onCommentsUpdate(BasicItem item, int count, int newCommentsCount, ArrayList<CommentItem> comments) {
        if (onCommentPushedListeners != null) {
            if (!onCommentPushedListeners.isEmpty()) {
                for (int i = 0; i < onCommentPushedListeners.size(); i++) {
                    onCommentPushedListeners.get(i).onCommentsUpdate(item, count, newCommentsCount, comments);
                }
            }
        }
    }

    /**
     * This happen when user posted new comment.
     * This method call all preset callbacks.
     */
    static void onPost() {
        if (onPostListeners != null) {
            if (!onPostListeners.isEmpty()) {
                for (int i = 0; i < onPostListeners.size(); i++) {
                    onPostListeners.get(i).onPost();
                }
            }
        }
    }

    static public void initializing() {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).initializing();
            }
        }
    }

    static public void musicStarted() {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).musicStarted();
            }
        }
    }

    static public void error() {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).error();
            }
        }
    }

    static public void positionChanged(int position) {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).positionChanged(position);
            }
        }
    }

    static public void musicPaused(AudioItem item) {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).musicPaused(item);
            }
        }
    }

    static public void musicUnpaused(AudioItem item) {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).musicUnpaused(item);
            }
        }
    }

    static public void onServiceStarted() {
        if (!serviceCallbacks.isEmpty()) {
            for (int i = 0; i < serviceCallbacks.size(); i++) {
                serviceCallbacks.get(i).onServiceStarted();
            }
        }
    }
}
