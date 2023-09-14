package com.mobile.viewcam;

import android.icu.util.Output;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class NetworkManager {
    private static final String TAG = "NetworkManager";

    private static final int PORT = 8181;
    private String IP;

    private boolean connected = false;

    private ServerSocket server;
    private Socket client;

    private InputStream in;
    private OutputStream out;

    public NetworkManager()
    {
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

    public void ListenClient()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client = server.accept();
                    getClientIO();
                } catch (IOException e) {
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

    private void getClientIO() throws IOException {
        in = client.getInputStream();
        out = client.getOutputStream();
    }

    public CameraManager.ImageSocket getImageSocket()
    {
        return imageSender;
    }

    /*
    *
    *
    * Callbacks
    *
    *
    * */

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

}
