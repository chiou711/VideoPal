package com.cw.videopal.refplayer.server;

import android.app.IntentService;
import android.content.Intent;

import com.cw.videopal.page.PageAdapter_recycler;

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
                "-p 8080",
                "-d",
                root_path
        };
        SimpleWebServer.runServer(
//                arrayOf(
//                        "-h",
//                        LocalPlayerActivity.deviceIpAddress,
//                        "-p 8080",
//                        "-d",
//                        Environment.getExternalStorageDirectory().getAbsolutePath()
//                )
                array
        );
//        Log.d(TAG, "Service Started on ${LocalPlayerActivity.deviceIpAddress}:8080");

    }
}