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

import android.os.Handler;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.impl.conn.DefaultResponseParser;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class StreamProxy implements Runnable {

    private static final String LOG_TAG = StreamProxy.class.getName();
    private int port = 0;
    private int contentLength = 0;
    private boolean isRunning = true;
    private boolean contentFound = false;
    private ServerSocket socket;
    private Thread thread;
    private String contentType = "";
    private Handler handler = null;

    public int getPort() {
        return port;
    }

    public void init(Handler handler) {
        try {
            this.handler = handler;
            socket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            socket.setSoTimeout(5000);
            port = socket.getLocalPort();
        } catch (UnknownHostException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
        } catch (IOException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
        }
    }

    public void start() {
        if (socket == null) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
            throw new IllegalStateException("Cannot start proxy; it has not been initialized.");
        }

        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRunning = false;

        if (thread == null) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
            throw new IllegalStateException("Cannot stop proxy; it has not been started.");
        }

        thread.interrupt();
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
        }

        try {
            socket.close();
        } catch (IOException iOEx) {
        }
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null) {
                    continue;
                }

                HttpRequest request = readRequest(client);
                if (request == null) {
                    continue;
                }
                processRequest(request, client);
            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
                handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
            }
        }
    }

    private HttpRequest readRequest(Socket client) {
        HttpRequest request = null;
        InputStream is;
        String firstLine;
        try {
            is = client.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
            firstLine = reader.readLine();
        } catch (IOException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
            return request;
        }

        if (firstLine == null) {
            return request;
        }

        StringTokenizer st = new StringTokenizer(firstLine);
        String method = st.nextToken();
        String uri = st.nextToken();
        String realUri = uri.substring(1);
        request = new BasicHttpRequest(method, realUri);

        return request;
    }

    private HttpResponse download(String url) {
        DefaultHttpClient seed = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        if (url.startsWith("https")) {
            registry.register(new Scheme("https", PlainSocketFactory.getSocketFactory(), 80));
        } else {
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        }
        SingleClientConnManager mgr = new MyClientConnManager(seed.getParams(), registry);
        DefaultHttpClient http = new DefaultHttpClient(mgr, seed.getParams());
        HttpGet method = new HttpGet(url);
        HttpResponse response = null;
        try {
            Log.d(LOG_TAG, "starting download");
            response = http.execute(method);
            Log.d(LOG_TAG, "downloaded");
        } catch (ClientProtocolException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
            Log.e(LOG_TAG, "Error downloading", e);
        } catch (IOException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
            Log.e(LOG_TAG, "Error downloading", e);
        } catch (IllegalStateException e) {
            handler.sendEmptyMessage(BackGroundMusicService.STREAM_PROXY_ERROR);
        }
        return response;
    }

    private void processRequest(HttpRequest request, Socket client) throws IllegalStateException, IOException {
        if (request == null) {
            return;
        }

        Log.d(LOG_TAG, "processing");
        String url = request.getRequestLine().getUri();
        HttpResponse realResponse = download(url);
        if (realResponse == null) {
            return;
        }

        Log.d(LOG_TAG, "downloading...");

        InputStream data = realResponse.getEntity().getContent();
        StatusLine line = realResponse.getStatusLine();
        HttpResponse response = new BasicHttpResponse(line);
        response.setHeaders(realResponse.getAllHeaders());

        Log.d(LOG_TAG, "reading headers");
        StringBuilder httpString = new StringBuilder();
        httpString.append(response.getStatusLine().toString());

        httpString.append("\r\n");
        Header[] hs = response.getAllHeaders();
        for (Header h : response.getAllHeaders()) {
            if (h.getName().equalsIgnoreCase("Content-Type")) {
                contentType = h.getValue();
            }
            if (h.getName().equalsIgnoreCase("Content-Length")) {
                contentLength = Integer.parseInt(h.getValue());
            }
            httpString.append(h.getName()).append(": ").append(h.getValue()).append("\r\n");
        }
        if (!contentFound) {
            handler.sendEmptyMessage(BackGroundMusicService.FOUND_STREAM_CONTENT_TYPE);
        }
        httpString.append("\r\n");
    }

    /**
     * @return the contentLength
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @param contentFound the contentFound to set
     */
    public void setContentFound(boolean contentFound) {
        this.contentFound = contentFound;
    }

    private class IcyLineParser extends BasicLineParser {

        private static final String ICY_PROTOCOL_NAME = "ICY";

        private IcyLineParser() {
            super();
        }

        @Override
        public boolean hasProtocolVersion(CharArrayBuffer buffer, ParserCursor cursor) {
            boolean superFound = super.hasProtocolVersion(buffer, cursor);
            if (superFound) {
                return true;
            }
            int index = cursor.getPos();

            final int protolength = ICY_PROTOCOL_NAME.length();

            if (buffer.length() < protolength) {
                return false; // not long enough for "HTTP/1.1"
            }
            if (index < 0) {
                // end of line, no tolerance for trailing whitespace
                // this works only for single-digit major and minor version
                index = buffer.length() - protolength;
            } else if (index == 0) {
                // beginning of line, tolerate leading whitespace
                while ((index < buffer.length())
                        && HTTP.isWhitespace(buffer.charAt(index))) {
                    index++;
                }
            } // else within line, don't tolerate whitespace

            return index + protolength <= buffer.length()
                    && buffer.substring(index, index + protolength).equals(ICY_PROTOCOL_NAME);
        }

        @Override
        public ProtocolVersion parseProtocolVersion(CharArrayBuffer buffer,
                ParserCursor cursor) throws ParseException {

            if (buffer == null) {
                throw new IllegalArgumentException("Char array buffer may not be null");
            }
            if (cursor == null) {
                throw new IllegalArgumentException("Parser cursor may not be null");
            }

            final int protolength = ICY_PROTOCOL_NAME.length();

            int indexFrom = cursor.getPos();
            int indexTo = cursor.getUpperBound();

            skipWhitespace(buffer, cursor);

            int i = cursor.getPos();

            // long enough for "HTTP/1.1"?
            if (i + protolength + 4 > indexTo) {
                throw new ParseException("Not a valid protocol version: "
                        + buffer.substring(indexFrom, indexTo));
            }

            // check the protocol name and slash
            if (!buffer.substring(i, i + protolength).equals(ICY_PROTOCOL_NAME)) {
                return super.parseProtocolVersion(buffer, cursor);
            }

            cursor.updatePos(i + protolength);

            return createProtocolVersion(1, 0);
        }

        @Override
        public StatusLine parseStatusLine(CharArrayBuffer buffer,
                ParserCursor cursor) throws ParseException {
            return super.parseStatusLine(buffer, cursor);
        }
    }

    class MyClientConnection extends DefaultClientConnection {

        @Override
        protected HttpMessageParser createResponseParser(
                final SessionInputBuffer buffer,
                final HttpResponseFactory responseFactory, final HttpParams params) {
            return new DefaultResponseParser(buffer, new IcyLineParser(),
                    responseFactory, params);
        }
    }

    class MyClientConnectionOperator extends DefaultClientConnectionOperator {

        public MyClientConnectionOperator(final SchemeRegistry sr) {
            super(sr);
        }

        @Override
        public OperatedClientConnection createConnection() {
            return new MyClientConnection();
        }
    }

    class MyClientConnManager extends SingleClientConnManager {

        private MyClientConnManager(HttpParams params, SchemeRegistry schreg) {
            super(params, schreg);
        }

        @Override
        protected ClientConnectionOperator createConnectionOperator(
                final SchemeRegistry sr) {
            return new MyClientConnectionOperator(sr);
        }
    }
}
