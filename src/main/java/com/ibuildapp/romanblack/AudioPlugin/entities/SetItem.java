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
import java.util.ArrayList;

/**
 * Entity class that represents set of audio streams.
 */
public class SetItem extends BasicItem implements Serializable {

    /**
     * Constructs new SetItem instance.
     */
    public SetItem() {
        super();
    }
    
    private boolean expanded = false;
    private ArrayList<AudioItem> audios = new ArrayList<AudioItem>();

    /**
     * Returns the count of audio items.
     * @return the count of audio items
     */
    public int getTracksCount() {
        if (audios == null) {
            return 0;
        } else {
            return audios.size();
        }
    }

    /**
     * Returns track at given position.
     * @param position the track position
     * @return the audio item
     */
    public AudioItem getTrack(int position) {
        if (audios == null) {
            return null;
        } else if ((audios.size() - 1) < position) {
            return null;
        } else {
            return audios.get(position);
        }
    }

    /**
     * Returns all audio tracks that this set contains.
     * @return the audio items array
     */
    public ArrayList<AudioItem> getTracks() {
        return audios;
    }

    /**
     * Sets audio tracks to this set.
     * @param audios audio tracks to set
     */
    public void setTracks(ArrayList<AudioItem> audios) {
        this.audios = audios;
    }

    /**
     * Displays if this set is expanded on main module page now.
     * @return true if the stream is expanded now, false otherwise
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Sets the set expanded state.
     * @param playing set expanded state to set
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
