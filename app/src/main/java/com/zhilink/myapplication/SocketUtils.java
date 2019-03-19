package com.zhilink.myapplication;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.test.espresso.core.internal.deps.guava.util.concurrent.ThreadFactoryBuilder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * socket
 *
 * @author xiemeng
 * @date 2019-3-7 09:09
 */
public class SocketUtils {

    private final String code = "GBK";

    private static final String TAG = "SocketUtils";

    private SocketUtils() {
    }

    public static SocketUtils getInstance() {
        return MainHolder.ZHI_LINK_DIALOG;
    }

    private static class MainHolder {
        private static final SocketUtils ZHI_LINK_DIALOG = new SocketUtils();
    }

    /**
     * 服务器地址
     */
    private String host = "192.168.12.216";
    /**
     * 连接端口号
     */
    private int port = 8888;

    private int timeout = 1000 * 20;

    private java.net.Socket socket = null;

    private BufferedReader in = null;

    private PrintWriter out = null;

    /**
     * 获取ip地址
     */
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddress = networkInterface.getInetAddresses(); enumIpAddress.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddress.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public interface SocketListener {
        /**
         * 成功
         */
        void onSuccess();

        /**
         * 失败
         */
        void onFailed();
    }

    /**
     * 连接服务器
     */
    public void connection(final SocketListener listener) {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("connection-%d").build();
        ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(host, port);
                    socket.setSoTimeout(timeout);
                    //接收消息的流对象
                    in = new BufferedReader(new InputStreamReader(socket
                            .getInputStream(), code));
                    //发送消息的流对象
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream(), code)), true);
                    Looper.prepare();
                    listener.onSuccess();
                    Looper.loop();
                } catch (IOException ex) {
                    Looper.prepare();
                    listener.onFailed();
                    Looper.loop();

                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * 发送数据
     */
    public void sendData(final String data) {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("sendData-%d").build();
        ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (socket != null && socket.isConnected()) {
                    if (out != null) {
                        out.println(data);
                    }
                }
            }
        });
    }

    public interface ReceiveMsgListener {
        /**
         * 成功
         *
         * @param msg json数据
         */
        void onSuccess(String msg);

        /**
         * 错误
         *
         * @param errorCode 失败
         */
        void onFailed(int errorCode);
    }

    private final int CORRECT = 0;

    private final int ERROR_CODE_EXCEPTION = 1;

    private final int ERROR_CODE_CONNECTED = 2;

    private ReceiveMsgListener msgListener;


    /**
     * 读取服务器发来的信息，并通过Handler发给UI线程
     */
    public void receiveMsg(final ReceiveMsgListener listener) {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("receiveMsg-%d").build();
        ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                msgListener = listener;
                //死循环守护，监控服务器发来的消息
                while (true) {
                    //如果服务器没有关闭
                    //连接正常
                    //如果输入流没有断开
                    if (!socket.isClosed() && socket.isConnected() && !socket.isInputShutdown()) {
                        try {
                            char[] bt = new char[1024];
                            final StringBuilder reqStr = new StringBuilder();
                            do {
                                if ((in.read(bt)) != -1) {
                                    reqStr.append(bt);
                                }
                            } while (in.ready());
                            Message message = new Message();
                            message.what = CORRECT;
                            message.obj = reqStr.toString().trim();
                            mHandler.sendMessage(message);

                            Log.i(TAG, "读取完毕" + reqStr.toString().trim());
                        } catch (IOException e) {
                            e.printStackTrace();
                            mHandler.sendEmptyMessage(ERROR_CODE_EXCEPTION);

                        }
                    } else {
                        mHandler.sendEmptyMessage(ERROR_CODE_CONNECTED);
                    }
                }
            }
        });
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                default:
                    break;
                case ERROR_CODE_CONNECTED:
                    msgListener.onFailed(ERROR_CODE_CONNECTED);
                    break;
                case ERROR_CODE_EXCEPTION:
                    msgListener.onFailed(ERROR_CODE_EXCEPTION);
                    break;
                case CORRECT:
                    String data = (String) msg.obj;
                    msgListener.onSuccess(data);
                    break;
            }
            return false;
        }
    });

    public void close() {
        try {
            if (null != in) {
                in.close();
            }
            if (null != out) {
                out.flush();
                out.close();
            }
            if (null != socket) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
