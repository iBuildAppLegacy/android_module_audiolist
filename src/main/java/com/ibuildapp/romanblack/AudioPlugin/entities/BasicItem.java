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
package com.ibuildapp.romanblack.AudioPlugin.entities;

import java.io.Serializable;

/**
 * Basic entity class for audio stream and audio list.
 */
public class BasicItem implements Serializable {

    private boolean liked = false;
    private long id = 0;
    private String title = "";
    private String permalinkUrl = "";
    private String url = "";
    private String description = "";
    private String coverPath = "";
    private String coverUrl = "";
    private int totalComments = 0;
    private int likesCount = 0;

    public BasicItem() {
    }

    /**
     * Sets the item title.
     * @param value the title to set
     */
    public void setTitle(String value) {
        title = value;
    }

    /**
     * Returns the item title.
     * @return the item title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the item description.
     * @param value the title to set
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Returns the item description.
     * @return the item description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the item URL
     * @param value the item URL to set
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     * Returns the item URL.
     * @return the item URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the cover image of item URL.
     * @param value the image URL to set
     */
    public void setCoverUrl(String value) {
        coverUrl = value;
    }

    /**
     * Returns the cover image of item URL.
     * @return the image URL
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * Sets the cover image of item cache path.
     * @param value the image path to set
     */
    public void setCoverPath(String value) {
        coverPath = value;
    }

    /**
     * Returns the cover image of item cache path.
     * @return the image path
     */
    public String getCoverPath() {
        return coverPath;
    }

    /**
     * Returns the item ID.
     * @return the item ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the item ID.
     * @param id the ID to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the comments count that was made on this item.
     * @return the comments count
     */
    public int getTotalComments() {
        return totalComments;
    }

    /**
     * Sets the comments count that was made on this item.
     * @param totalComments the comments count to set
     */
    public void setTotalComments(int totalComments) {
        this.totalComments = totalComments;
    }

    /**
     * Returns the Facebook likes count that was made on this item.
     * @return the likes count
     */
    public int getLikesCount() {
        return likesCount;
    }

    /**
     * Sets the Facebook likes count that was made on this item.
     * @param likesCount the likes count to set
     */
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    /**
     * Returns the permalink URL of item. 
     * This URL is using to share.
     * @return the permalinkUrl the permalink URL
     */
    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    /**
     * Sets the permalink URL of item.
     * This URL is using to share.
     * @param permalinkUrl the permalinkUrl to set
     */
    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
    }

    /**
     * Returns true if this item was liked on Facebook by user.
     * @return true if this item was liked, false otherwise
     */
    public boolean isLiked() {
        return liked;
    }

    /**
     * Sets true if this item was liked on Facebook by user.
     * @param liked the liked state to set
     */
    public void setLiked(boolean liked) {
        this.liked = liked;
    }
}
