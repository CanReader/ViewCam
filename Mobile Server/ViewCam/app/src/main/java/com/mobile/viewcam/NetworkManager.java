package com.mobile.viewcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.util.Output;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class NetworkManager {

    //region variables

    private static final String TAG = "NetworkManager";

    private static final int PORT = 8181;
    private String IP;

    private boolean connected = false;

    static WifiManager wiMan;
    private ServerSocket server;
    private Socket client;

    private InputStream in;
    private OutputStream out;

    private Context context;

    //endregion

    //region Constructors

    public NetworkManager(Context context)
    {
        this.context = context;
        wiMan  = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        try {
            server = new ServerSocket(PORT);
            IP = server.getInetAddress().getHostAddress();

            ListenClient();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG,"The server has been successfully started! now listening for a client to " +
                "connect...");
    }

    //endregion

    //region Methods

    public void ListenClient()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client = server.accept();
                    getClientIO();
                }
                catch (SocketTimeoutException ignored){}
                catch (IOException e) {
                    connected = false;
                    e.printStackTrace();
                    return;
                }
                    connected = true;
                Log.i(TAG,"The client has been connected! The ip: " + client.getInetAddress().toString());
            }
        }).start();
    }

    public void closeServer() throws IOException {
        if(connected && client != null)
        {
            client.close();
            connected = false;
        }

        server.close();

        Log.i(TAG,"The server has been closed!");
    }

    //endregion

    //region Setters

    //endregion

    //region Getters

    public static boolean isWifiEnabled()
    {
        return (wiMan == null && wiMan.isWifiEnabled());
    }

    @SuppressLint("DefaultLocale")
    public String getLocalIP()
    {
        if(isWifiEnabled())
            return "";

        WifiInfo wiInfo = wiMan.getConnectionInfo();

        int ip = wiInfo.getIpAddress();

        return String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
    }

    private void getClientIO() throws IOException {
        in = client.getInputStream();
        out = client.getOutputStream();
    }

    public CameraManager.ImageSocket getImageSocket()
    {
        return imageSender;
    }


    //endregion

    //region Callback
    private final CameraManager.ImageSocket imageSender = new CameraManager.ImageSocket()
    {
        @Override
        public void Run(byte[] data) {
            if(server == null || client == null || out == null)
                return;

            try
            {
                out.write(data);
                out.flush();
            }
            catch (SocketException e)
            {
                Log.e("ERROR","The client has been disconnected! trying to reconnect");
                ListenClient();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    //endregion
}
