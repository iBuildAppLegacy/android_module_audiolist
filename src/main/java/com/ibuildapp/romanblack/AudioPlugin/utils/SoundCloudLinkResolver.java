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

import android.util.Log;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Stream;
import com.soundcloud.api.Token;
import java.io.IOException;

/**
 * This class helps to resolve audio stream URL from SoundCloud audio URL.
 * @author minberg
 */
public class SoundCloudLinkResolver {

    private static final String OAUTH2_CLIENT_ID = "a8a2acc83c83ed22496520cad824d2ba";
    private static final String OAUTH2_CLIENT_SECRET = "2ed5aa730942f010c5afd49f90242b74";
    private static final String USERNAME = "support@ibuildapp.com";
    private static final String PASSWORD = "SCIbuiDPP43@";

    /**
     * Resolves audio stream URL from SoundCloud audio URL.
     * @param soundCloudURL SoundCloud audio URL
     * @return audio stream URL
     * @throws SoundCloudException 
     */
    public static String resolveLink(String soundCloudURL) throws SoundCloudException {
        ApiWrapper wrapper = new ApiWrapper(OAUTH2_CLIENT_ID,
                OAUTH2_CLIENT_SECRET, null, null);

        Token token = null;

        try {
            token = wrapper.login(USERNAME, PASSWORD, Token.SCOPE_NON_EXPIRING);
        } catch (IOException iOEx) {
            throw new SoundCloudException(iOEx.getMessage());
        }

        Stream stream = null;

        try {
            stream = wrapper.resolveStreamUrl(soundCloudURL, true);
        } catch (IOException iOEx) {
            throw new SoundCloudException(iOEx.getMessage());
        }

        Log.e("", "");

        if (stream == null) {
            throw new SoundCloudException("Cannot resolve stream URL");
        } else {
            return stream.streamUrl;
        }
    }
}
