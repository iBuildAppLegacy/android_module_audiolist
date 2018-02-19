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

import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.BasicItem;
import com.ibuildapp.romanblack.AudioPlugin.entities.SetItem;
import java.util.ArrayList;

/**
 * This class helps to resilve position between activity and bacground music service.
 */
public class PositionResolver {

    public final int ERROR_CANT_FIND_AUDIO_POSITION = -1;
    public final int ERROR_NOT_AN_AUDIO = -2;
    public final int ERROR_NOT_A_SET = -3;
    public final int ERROR_INDEX_OUT_OF_BOUNDS = -4;
    public final int ERROR_UNKNOWN = -5;
    private ArrayList<BasicItem> basicItems = null;
    private ArrayList<AudioItem> audioItems = null;

    /**
     * Constructs new PositionResolver instance with given audio and audio set items.
     * @param basicItems audio and audio set items list
     */
    public PositionResolver(ArrayList<BasicItem> basicItems) {
        this.basicItems = basicItems;

        resolveAudioItems();
    }

    /**
     * Creates new audio items list from audio and audio set items list.
     */
    private void resolveAudioItems() {
        ArrayList<AudioItem> tmpItems = new ArrayList<AudioItem>();

        for (int i = 0; i < this.basicItems.size(); i++) {
            if (this.basicItems.get(i) instanceof AudioItem) {
                tmpItems.add((AudioItem) this.basicItems.get(i));
            } else if (this.basicItems.get(i) instanceof SetItem) {
                tmpItems.addAll(((SetItem) this.basicItems.get(i)).getTracks());
            }
        }

        this.audioItems = tmpItems;
    }

    /**
     * Returns audio list position from module structure.
     * @param position audio item or audio set item position
     * @param childPosition audio item position in set
     * @return audio item position in audio list
     */
    public int getAudioPosition(int position, int childPosition) {
        if (childPosition < 0) {
            if (basicItems.get(position) instanceof SetItem) {
                return ERROR_NOT_AN_AUDIO;
            }

            for (int i = 0; i < audioItems.size(); i++) {
                if (audioItems.get(i).getId() == basicItems.get(position).getId()) {
                    return i;
                }
            }

            return ERROR_CANT_FIND_AUDIO_POSITION;
        } else {
            if (basicItems.get(position) instanceof AudioItem) {
                return ERROR_NOT_A_SET;
            }

            AudioItem audioItem = ((SetItem) basicItems.get(position)).getTrack(childPosition);

            for (int i = 0; i < audioItems.size(); i++) {
                if (audioItem.getId() == audioItems.get(i).getId()) {
                    return i;
                }
            }

            return ERROR_CANT_FIND_AUDIO_POSITION;
        }
    }

    /**
     * Returns set item or audio item position from audio items list.
     * @param position audio item position of audio item list
     * @return audio or audio set item position 
     */
    public int getGroupPosition(int position) {
        if (audioItems.size() < (position + 1)) {
            return ERROR_INDEX_OUT_OF_BOUNDS;
        }

        int count = -1;

        for (int i = 0; i < basicItems.size(); i++) {
            if (basicItems.get(i) instanceof AudioItem) {
                count++;
            } else if (basicItems.get(i) instanceof SetItem) {
                count = count + ((SetItem) basicItems.get(i)).getTracksCount();
            }

            if (position <= count) {
                return i;
            }
        }

        return ERROR_UNKNOWN;
    }

    /**
     * Returns audio item postion of audio set from audio item list position.
     * @param position item position of audio item list
     * @return audio item position of audio set
     */
    public int getChildPosition(int position) {
        if (audioItems.size() < (position + 1)) {
            return ERROR_INDEX_OUT_OF_BOUNDS;
        }

        int count = -1;
        int previousCount = -1;
        int groupPosition = -1;

        for (int i = 0; i < basicItems.size(); i++) {
            previousCount = count;

            if (basicItems.get(i) instanceof AudioItem) {
                count++;
            } else if (basicItems.get(i) instanceof SetItem) {
                count = count + ((SetItem) basicItems.get(i)).getTracksCount();
            }

            if (position <= count) {
                groupPosition = i;
                break;
            }
        }

        int resultPosition = count - previousCount - 1;

        try {
            if (!(basicItems.get(groupPosition) instanceof SetItem)) {
                return ERROR_NOT_A_SET;
            }

            if (((SetItem) basicItems.get(groupPosition)).getTracksCount() < (resultPosition + 1)) {
                return ERROR_INDEX_OUT_OF_BOUNDS;
            }

            return resultPosition;
        } catch (IndexOutOfBoundsException iOOBEx) {
            return ERROR_INDEX_OUT_OF_BOUNDS;
        }
    }

    /**
     * Returns audio and audio set items that was set in constructor or later.
     * @return the basicItems audio and audio set items
     */
    public ArrayList<BasicItem> getBasicItems() {
        return basicItems;
    }

    /**
     * Sets audio and audio set items. 
     * @param basicItems audio and audio set items to set
     */
    public void setBasicItems(ArrayList<BasicItem> basicItems) {
        this.basicItems = basicItems;

        resolveAudioItems();
    }

    /**
     * Returns resolved audio items list.
     * @return the audioItems
     */
    public ArrayList<AudioItem> getAudioItems() {
        return audioItems;
    }
}
