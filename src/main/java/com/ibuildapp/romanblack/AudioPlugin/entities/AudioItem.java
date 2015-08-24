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

import java.util.Date;

/**
 * Entity class that represents audio item.
 */
public class AudioItem extends BasicItem implements Serializable {

    private boolean playing = false;
    private Date duration = null;

    /**
     * Constructs new AudioItem.
     */
    public AudioItem() {
        super();
    }

    /**
     * Returns the audio stream duration.
     * @return stream duration
     */
    public Date getDuration() {
        return duration;
    }

    /**
     * Sets the audio stream duration.
     * @param duration stream duration to set
     */
    public void setDuration(Date duration) {
        this.duration = duration;
    }

    /**
     * Displays if audio stream is playing now.
     * @return true if the stream is playing now, false otherwise
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Sets the audio stream playing state.
     * @param playing stream playing state to set
     */
    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
}
