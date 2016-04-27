
package com.statistical.android.sdk;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * 连接处理器
 * 
 */
public class ConnectionProcessor implements Runnable {
    private static final int CONNECT_TIMEOUT_IN_MILLISECONDS = 30000;
    private static final int READ_TIMEOUT_IN_MILLISECONDS = 30000;

    private final StatisticalStore store_;
    private final DeviceId deviceId_;
    private final String serverURL_;
    private final SSLContext sslContext_;

    ConnectionProcessor(final String serverURL, final StatisticalStore store, final DeviceId deviceId, final SSLContext sslContext) {
        serverURL_ = serverURL;
        store_ = store;
        deviceId_ = deviceId;
        sslContext_ = sslContext;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    URLConnection urlConnectionForEventData(final String eventData) throws IOException {
        String urlStr = serverURL_ + "/a?";
        if(!eventData.contains("&crash="))
            urlStr += eventData;
        final URL url = new URL(urlStr);
        final HttpURLConnection conn;
        if (Statistical.publicKeyPinCertificates == null) {
            conn = (HttpURLConnection)url.openConnection();
        } else {
            HttpsURLConnection c = (HttpsURLConnection)url.openConnection();
            c.setSSLSocketFactory(sslContext_.getSocketFactory());
            conn = c;
        }
        conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLISECONDS);
        conn.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
        conn.setUseCaches(false);
        conn.setDoInput(true);
        String picturePath = UserData.getPicturePathFromQuery(url);
        if (Statistical.sharedInstance().isLoggingEnabled()) {
            Log.d(Statistical.TAG, "Got picturePath: " + picturePath);
        }
        if(!picturePath.equals("")){
        	
        	File binaryFile = new File(picturePath);
        	conn.setDoOutput(true);
        	String boundary = Long.toHexString(System.currentTimeMillis());
        	String CRLF = "\r\n";
        	String charset = "UTF-8";
        	conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        	OutputStream output = conn.getOutputStream();
        	PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            FileInputStream fileInputStream = new FileInputStream(binaryFile);
            byte[] buffer = new byte[1024];
            int len;
            try {
                while ((len = fileInputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            }catch(IOException ex){
                ex.printStackTrace();
            }
            output.flush(); 
            writer.append(CRLF).flush(); 
            fileInputStream.close();
            writer.append("--" + boundary + "--").append(CRLF).flush();
        }
        else if(eventData.contains("&crash=")){
            if (Statistical.sharedInstance().isLoggingEnabled()) {
                Log.d(Statistical.TAG, "Using post because of crash");
            }
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(eventData);
            writer.flush();
            writer.close();
            os.close();
        }
        else{
        	conn.setDoOutput(false);
        }
        return conn;
    }

    @Override
    public void run() {
        while (true) {
            final String[] storedEvents = store_.connections();
            if (storedEvents == null || storedEvents.length == 0) {
                break;
            }

            if (deviceId_.getId() == null) {
                if (Statistical.sharedInstance().isLoggingEnabled()) {
                    Log.i(Statistical.TAG, "No Device ID available yet, skipping request " + storedEvents[0]);
                }
                break;
            }
            final String eventData = storedEvents[0] + "&device_id=" + deviceId_.getId() + "&session_id=" + store_.getBeginSession() + deviceId_.getId();

            URLConnection conn = null;
            BufferedInputStream responseStream = null;
            try {
                conn = urlConnectionForEventData(eventData);
                conn.connect();

                responseStream = new BufferedInputStream(conn.getInputStream());
                final ByteArrayOutputStream responseData = new ByteArrayOutputStream(256); 
                int c;
                while ((c = responseStream.read()) != -1) {
                    responseData.write(c);
                }

                boolean success = true;
                if (conn instanceof HttpURLConnection) {
                    final HttpURLConnection httpConn = (HttpURLConnection) conn;
                    final int responseCode = httpConn.getResponseCode();
                    success = responseCode >= 200 && responseCode < 300;
                    if (!success && Statistical.sharedInstance().isLoggingEnabled()) {
                        Log.w(Statistical.TAG, "HTTP error response code was " + responseCode + " from submitting event data: " + eventData);
                    }
                }

                if (success && !TextUtils.isEmpty(responseData.toString("UTF-8"))) {
                    final JSONObject responseDict = new JSONObject(responseData.toString("UTF-8"));
                    success = responseDict.optString("result").equalsIgnoreCase("success");
                    if (!success && Statistical.sharedInstance().isLoggingEnabled()) {
                        Log.w(Statistical.TAG, "Response from Countly server did not report success, it was: " + responseData.toString("UTF-8"));
                    }
                }

                if (success) {
                    if (Statistical.sharedInstance().isLoggingEnabled()) {
                        Log.d(Statistical.TAG, "ok ->" + eventData);
                    }

                    store_.removeConnection(storedEvents[0]);
                }
                else {
                    break;
                }
            }
            catch (Exception e) {
                if (Statistical.sharedInstance().isLoggingEnabled()) {
                    Log.w(Statistical.TAG, "Got exception while trying to submit event data: " + eventData, e);
                }
                break;
            }
            finally {
                if (responseStream != null) {
                    try { responseStream.close(); } catch (IOException ignored) {}
                }
                if (conn != null && conn instanceof HttpURLConnection) {
                    ((HttpURLConnection)conn).disconnect();
                }
            }
        }
    }

    String getServerURL() { return serverURL_; }
    StatisticalStore getCountlyStore() { return store_; }
    DeviceId getDeviceId() { return deviceId_; }
}
