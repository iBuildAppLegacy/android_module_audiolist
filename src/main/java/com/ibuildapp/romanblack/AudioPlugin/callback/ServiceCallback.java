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

import com.ibuildapp.romanblack.AudioPlugin.entities.AudioItem;

/**
 * This callback must be implemented by activity.
 * It recieves background music service state changes.
 * @author minberg
 */
public interface ServiceCallback {

    /**
     * Invoked when service was started.
     */
    public void onServiceStarted();

    /**
     * Invoked when service start initializing.
     */
    public void initializing();

    /**
     * Invoked when music started playing.
     */
    public void musicStarted();

    /**
     * Invoked when some error occured.
     */
    public void error();

    /**
     * Invoked when current audio position was changed.
     * @param position new current audio position
     */
    public void positionChanged(int position);

    /**
     * Invoked when music was paused.
     * @param item audio track that was paused
     */
    public void musicPaused(AudioItem item);

    /**
     * Invoked when music was unpaused.
     * @param item audio track that was unpaused
     */
    public void musicUnpaused(AudioItem item);
}
