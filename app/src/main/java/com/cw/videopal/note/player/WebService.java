package com.cw.videopal.note.player;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.cw.videopal.page.PageAdapter_recycler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.Nullable;
import io.github.dkbai.tinyhttpd.nanohttpd.webserver.SimpleWebServer;

public class WebService extends  IntentService {

    public String TAG = "WebService";
    public static String root_path;

    public WebService() {
        super("WebService");
    }

    //    override fun onStart(intent: Intent?, startId: Int) {
//        SimpleWebServer.stopServer()
//        super.onStart(intent, startId)
//    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        SimpleWebServer.stopServer();
        super.onStart(intent, startId);
    }

    //    override fun onHandleIntent(intent: Intent?) {
//        try {
//            /** Running a server on Internal storage.
//             *
//             * I know the method [Environment.getExternalStorageDirectory] is deprecated
//             * but it is needed to start the server in the required path.
//             */
//
//            SimpleWebServer.runServer(
//                arrayOf(
//                    "-h",
//                    LocalPlayerActivity.deviceIpAddress,
//                    "-p 8080",
//                    "-d",
//                    Environment.getExternalStorageDirectory().absolutePath
//                )
//            )
//            Log.d(TAG, "Service Started on ${LocalPlayerActivity.deviceIpAddress}:8080")
//        } catch (e: Exception) {
//            Log.e(TAG, "Error: ${e.message}", e)
//        }
//    }

//    @Override
//    void onDestroy() {
//        SimpleWebServer.stopServer();
//        Log.d(TAG, "Service destroyed");
//        super.onDestroy();
//    }


    @Override
    public void onDestroy() {
        SimpleWebServer.stopServer();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String[] array = new String[]{"-h",
                PageAdapter_recycler.deviceIpAddress,
//                Note_cast2.deviceIpAddress,
                "-p 8080",
                "-d",
                root_path
        };
        SimpleWebServer.runServer(
                array
        );
    }

    /**
     * Making sure public utility methods remain static
     */
    public static String findIPAddress(Context context) {
        WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            if (wifiManager.getConnectionInfo() != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return InetAddress.getByAddress(
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(wifiInfo.getIpAddress())
                                .array()
                ).getHostAddress();
            } else
                return null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}