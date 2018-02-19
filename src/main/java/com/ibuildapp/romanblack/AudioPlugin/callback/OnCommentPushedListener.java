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
package com.ibuildapp.romanblack.AudioPlugin.callback;

import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.CommentItem;
import java.util.ArrayList;

/**
 * This interface must be implemented to catch when new comment was sent or when comments list was refreshed.
 */
public interface OnCommentPushedListener {

    /**
     * This callback is invoked when new comment was pushed.
     * @param item new comment
     */
    public void onCommentPushed(CommentItem item);

    /**
     * This callback is invoked when the comment list was updated.
     * @param item audio item on which comments was made
     * @param commentItem new comment
     * @param count total comments count 
     * @param newCommentsCount new comments count
     * @param comments updated comments list
     */
    public void onCommentsUpdate(BasicItem item, int count, int newCommentsCount, ArrayList<CommentItem> comments);
}
