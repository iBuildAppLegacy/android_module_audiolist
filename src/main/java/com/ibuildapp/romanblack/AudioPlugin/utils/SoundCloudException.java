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

/**
 * This class represents Exception that SoundCloudLinkResolver throws.
 */
public class SoundCloudException extends Exception {

    private String msg = "";

    /**
     * Constructs new SoundCloudException
     * @param desc 
     */
    public SoundCloudException(String desc) {
    }

    @Override
    public String getMessage() {
        return msg;
    }
}
