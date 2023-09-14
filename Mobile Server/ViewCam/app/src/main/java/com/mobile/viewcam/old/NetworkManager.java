package com.mobile.viewcam.old;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.internal.utils.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

public class NetworkManager
{
    public static final int PORT = 8181;
    public static String LocalIP;

    private ServerSocket sock;
    private Socket client;

    private InputStream in;
    private OutputStream out;

    private ImageAnalyzer analyzer;

    private ImageView imger;

    public @ExperimentalGetImage NetworkManager(ImageView imgview)
    {
        imger = imgview;
        analyzer = new ImageAnalyzer();
        try {
            sock = new ServerSocket(PORT);
            LocalIP = sock.getLocalSocketAddress().toString();

            Log.i("INFO","The server socket is being started...");

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    AcceptClient();
                }
            }).start();

            Log.i("INFO","The server socket has been started! " + getIPAddress() + ":" + PORT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress() != null) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public ImageAnalyzer getAnalyzer()
    {
        return analyzer;
    }

    private void getIOStream(Socket socket) throws IOException {
        if(socket == null)
            return;

        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    private void AcceptClient()
    {
        try
        {
            client = sock.accept();
            getIOStream(client);
            Log.i("INFO","a connection has been established!");
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void HandleCommands()
    {
        //TODO: Write command handling functionality
    }

    @ExperimentalGetImage private class ImageAnalyzer implements ImageAnalysis.Analyzer
    {
        private Handler uiHandler;
        private byte[] imageData;

        public ImageAnalyzer()
        {
            uiHandler = new Handler(Looper.getMainLooper());
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void analyze(@NonNull ImageProxy image /*YUV_420_888*/)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {

            if(client != null && out != null)
            {
                try {
                    imageData = ImageUtil.yuvImageToJpegByteArray(image,new Rect(0,0,image.getWidth(),image.getHeight()),100);

                    if(imageData.length > 0)
                    {
                        out.write(imageData);
                        out.flush();
                        Log.i("INFO",imageData.length + " byte data has been sent to the client!");
                    }

                }catch (SocketException e)
                {
                    Log.e("ERROR","The client has been disconnected! trying to reconnect");
                    AcceptClient();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            image.close();

                }
            }).start();

        }
    }
}
