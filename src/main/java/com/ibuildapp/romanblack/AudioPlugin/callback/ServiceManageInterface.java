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

import com.ibuildapp.romanblack.AudioPlugin.BackGroundMusicService;
import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;
import java.util.ArrayList;

/**
 * This callback helps activity to manage background music service.
 * Must be implemented by music service.
 * @author minberg
 */
public interface ServiceManageInterface {

    /**
     * Call service to play current track.
     */
    public void play();

    /**
     * Call service to stop current track.
     */
    public void stop();

    /**
     * Call service to pause current track.
     */
    public void pause();

    /**
     * Call service to play next track.
     */
    public void next();

    /**
     * Call service to play previous track.
     */
    public void prev();

    /**
     * Sets the audio tracks to service.
     * @param items audio tracks array
     */
    public void setItems(ArrayList<AudioItem> items);
    
    /**
     * Cleans the current items of service.
     */
    public void cleanItems();

    /**
     * Returns service state.
     * @return service state
     */
    public BackGroundMusicService.MediaPlayerStates getState();

    /**
     * Sets current audio position to service.
     * @param position 
     */
    public void setPosition(int position);

    /**
     * Returns service current audio position.
     * @return audio position
     */
    public int getPosition();

    /**
     * Returns track progress.
     * @return track progress
     */
    public int getProgress();

    /**
     * Returns current track content type.
     * @return content type
     */
    public BackGroundMusicService.ContentTypes getContentType();

    /**
     * Seeks service to given position.
     * @param pos position
     */
    public void seekTo(int pos);

    /**
     * Returns current track duration.
     * @return track duration
     */
    public int getDuration();

    /**
     * Stops service.
     */
    public void stopService();

    /**
     * Returns current track info.
     * @return track item
     */
    public AudioItem getCurrentTrack();

    /**
     * Call service to set status bar notification.
     */
    public void setNotification();

    /**
     * Calls service to remove status bar notification.
     */
    public void removeNotification();

    /**
     * Sets playing positions interval.
     * @param startPosition start position
     * @param endPosition end position
     */
    public void setPositionsInterval(int startPosition, int endPosition);
}
