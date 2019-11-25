package com.rubicom.vcf.ichthus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class InetSocket {
    private String servName;
    private int servPort;
    private String reqAck, resAck;
    private Socket socket;
    //SocketChannel socket;

    private Handler hSendThread, hMainThread;
    private OutputStream oStream;
    private BufferedSink oSink;
    private ProgressDialog ringProgressDialog;
    private Activity baseActivity; // needed for showing a ringProgressDialog
    private final int maxTimeToJoin = 3000; // 3 seconds
    private final int maxTimeToClose = 1000; // 1 second
    private final int maxPollingInterval = 1000; // 1 second
    private final int maxTimeToWait = 3000;
    private final int sleepDuration = 100; // 100 ms
    private final int maxSleepCount = maxTimeToWait / sleepDuration;
    private int serverState = 0;
    private final int flagConnecting = 1; // indicate that ConnectThread is running
    private final int flagConnected = 2; // indicate that socket is connected
    private final int flagSendRunning = 4; // indicate that SendThread is running
    private final int flagRecvRunning = 8; // indicate that RecvThread is running
    private final int ServerAvailable = (flagConnected | flagSendRunning | flagRecvRunning);
    private final int ServerUnavailable = 0;
    public int nMsgsSent = 0;
    public int nMsgsRecv = 0;


    public InetSocket(Handler h, Activity a) {
        hMainThread = h;
        baseActivity = a;
    }
    public boolean isAvailable() {
        return ((serverState & ServerAvailable) == ServerAvailable);
    }
    public boolean connect(String hname, int hport, String req, String ack) {
        String dialog;
        Log.d("InetSocket", "connect() called in serverState = " + serverState);
        if ((serverState & flagConnecting) == flagConnecting || (serverState & flagConnected) == flagConnected)
            return false;
        if (waitForServerState(ServerUnavailable, "MainThread") == false) {
            Log.d("InetSocket", "waitForServerState(Unavailable) timed out!");
            return false;
        }
        // At this point, serverState == ServerUnavailable
        setServerStateFlag(flagConnecting);
        servName = hname;
        servPort = hport;
        reqAck = req;
        resAck = ack;

        Log.d("InetSocket", "reqAck = " + reqAck);
        if (reqAck == null)
            dialog = "Connecting to " + hname + ":" + hport;
        else
            dialog = "Waiting to be acknowledged from " + resAck;

        ringProgressDialog = ProgressDialog.show(baseActivity, "Please wait ...", dialog, true);

        ringProgressDialog.setCancelable(true);
        startThread(runnableConnect);

        // ConnectThread is terminated as soon as it establishes a connection to the server.
        nMsgsSent = 0;
        nMsgsRecv = 0;
        return true;
    }

    public boolean disconnect() {
        Log.d("InetSocket", "disconnect() called in serverState = " + serverState);
        if ((serverState & (flagConnecting | flagConnected)) == 0)
            return false;
        if (waitForServerState(flagConnected, "MainThread") == false) {
            Log.d("InetSocket", "waitForServerState(Connected) timed out!");
            return false;
        }
        sleep(maxTimeToClose); // we have to wait for any last string to be delivered to the server.
        //  At this point, serverState == ServerConnected or ServerAvailable.
        if ((serverState & flagConnected) == flagConnected) {
            try { socket.close(); } // as a side effect, RecvThread & SendThread are terminated.
            catch (Exception e) { e.printStackTrace(); }
        }
        return true;
    }

    public boolean send(Message tmsg) {
        if ((serverState & (flagConnecting | flagConnected)) == 0)
            return false;
        if (waitForServerState(ServerAvailable, "MainThread") == false) {
            Log.d("InetSocket", "waitForServerState(Available) timed out!");
            return false;
        }
        Log.d("InetSocket", "Serverstate= "+serverState);
        Message msg = Message.obtain();
        msg.obj = tmsg.obj;
        msg.arg1 = tmsg.arg1;
        msg.setTarget(hSendThread);
        msg.sendToTarget();
        return true;
    }

    private Runnable runnableConnect = new Runnable() {
        @Override
        public void run() {
            try {
                InetSocketAddress socketAddress = new InetSocketAddress(servName, servPort);
                socket = new Socket();
                socket.connect(socketAddress, maxTimeToJoin); // If this fails, then it will raise an exception

                setServerStateFlag(flagConnected);

                Log.d("InetSocket", "runnableThread()");
                startThread(runnableSend);
                startThread(runnableRecv);

            } catch (Exception e) {
                Log.d("InetSocket", "ConnectThread : connect() fails!");
                e.printStackTrace();
            }
            resetServerStateFlag(flagConnecting);
            if (reqAck == null);
            ringProgressDialog.dismiss();
        }
    };

    private Runnable runnableSend = new Runnable() {
        @Override
        public void run() {
            setServerStateFlag(flagSendRunning);
            Looper.prepare(); // The message loop starts
            Log.d("InetSocket", "sendThread running");
            hSendThread = new Handler() { // defined here to be available after the loop starts
                public void handleMessage(Message msg) {
                    try {
                        oStream = socket.getOutputStream();
                        oSink = Okio.buffer(Okio.sink(oStream));
                        int type = msg.arg1;
                        oSink.writeByte(type);
                        VCF_msg vmsg = (VCF_msg) msg.obj;
                        //VCF_msg.Cmd cmsg = vmsg.cmd.get(0);
                        //VCF_msg.Prm pmsg = vmsg.prm.get(0);
                        int size = vmsg.ADAPTER.encodedSize(vmsg);
                        oSink.writeByte(size);
                        Log.d("InetSocket", "SEND_INIT");
                        vmsg.ADAPTER.encode(oSink,vmsg);

                        Log.d("SendThread", "--------------------");
                        Log.d("SendThread", "seqNo = "+vmsg._seqNo);
                        Log.d("SendThread", "version = "+vmsg._version);
                        Log.d("SendThread", "result = "+vmsg._result);
                        Log.d("SendThread", "msg = "+vmsg._msg);
                        //Log.d("SendThread", "from = "+vmsg.cmd.);
                        //Log.d("SendThread", "name = "+vmsg.prm);
                        //Log.d("SendThread", "varid = "+cmsg._varid);
                        //Log.d("SendThread", "value = "+cmsg._value);
                        //Log.d("SendThread", "vartype = "+pmsg._vartype);
                        //Log.d("SendThread", "min = "+pmsg._min);
                        //Log.d("SendThread", "max = "+pmsg._max);*/
                        Log.d("SendThread", "cycle = "+vmsg._cycle);
                        Log.d("SendThread", "--------------------");

                        byte[] array;
                        array = oSink.buffer().readByteArray();
                        oStream.write(array, 0, array.length);
                        oStream.flush();

                        nMsgsSent++;
                        for (int i = 0; i < array.length; i++) {
                            Log.d("SendThread", "b["+i+"]="+(int) array[i]);
                        }
                        Log.d("SendThread", "[" + nMsgsSent + "]th message sent (" + array.length + " bytes) : " + String.valueOf(array));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            };
            Looper.loop(); // The message loop ends
            resetServerStateFlag(flagSendRunning);
            Log.d("InetSocket", "SendThread terminated");

            if ((serverState & flagConnected) == flagConnected) {
                try { socket.close(); }
                catch (Exception e) { e.printStackTrace(); }
                resetServerStateFlag(flagConnected);
                Log.d("InetSocket", "Socket closed");
            }
        }
    };

    private Runnable runnableRecv = new Runnable() {
        @Override
        public void run() {
            setServerStateFlag(flagRecvRunning);
            Log.d("InetSocket", "recvThread running");
            try {
                BufferedSource source = Okio.buffer(Okio.source(socket.getInputStream()));
                Message msg;
                VCF_msg vmsg;
                while (true)
                {
                    while (!source.exhausted())
                    {
                        byte[] hdr = source.readByteArray(2); //hdr[0] = type, hdr[1] = size
                        int type=hdr[0];
                        int size=hdr[1]; //CHECK
                        byte[] bytes = source.readByteArray(size);
                        Log.d("RecvThread", "type: "+(int) hdr[0]);
                        /*for (int i = 0; i < bytes.length; i++) {
                            Log.d("RecvThread", "b["+i+"]="+(int) bytes[i]);
                        }*/
                        switch(type)
                        {
                            case 0: //base msg
                                Log.d("InetSocket", "recv_INIT");
                                vmsg = new VCF_msg.Builder().build();
                                vmsg = vmsg.ADAPTER.decode(bytes);

                                Log.d("SendThread", "--------------------");
                                Log.d("SendThread", "seqNo = "+vmsg._seqNo);
                                Log.d("SendThread", "version = "+vmsg._version);
                                Log.d("SendThread", "result = "+vmsg._result);
                                Log.d("SendThread", "msg = "+vmsg._msg);
                                /*Log.d("SendThread", "from = "+cmsg._from);
                                Log.d("SendThread", "name = "+cmsg._name);
                                Log.d("SendThread", "varid = "+cmsg._varid);
                                Log.d("SendThread", "value = "+cmsg._value);
                                Log.d("SendThread", "vartype = "+pmsg._vartype);
                                Log.d("SendThread", "min = "+pmsg._min);
                                Log.d("SendThread", "max = "+pmsg._max);*/
                                Log.d("SendThread", "cycle = "+vmsg._cycle);
                                Log.d("SendThread", "--------------------");

                                msg= Message.obtain(null, VCFAgent.MSG_SERVER_SEND, msgType.TYPE_INIT, 0, vmsg);

                                msg.setTarget(hMainThread);
                                msg.sendToTarget();
                                break;
                            case 1: //extended msg
                                Log.d("InetSocket", "recv_INIT PRM");
                                vmsg = new VCF_msg.Builder().build();
                                vmsg = vmsg.ADAPTER.decode(bytes);
                                //VCF_msg.Cmd cmsg = vmsg.cmd.get(0);
                                //VCF_msg.Prm pmsg = vmsg.prm.get(0);
                                Log.d("SendThread", "--------------------");
                                Log.d("SendThread", "seqNo = "+vmsg._seqNo);
                                Log.d("SendThread", "version = "+vmsg._version);
                                Log.d("SendThread", "result = "+vmsg._result);
                                Log.d("SendThread", "msg = "+vmsg._msg);
                                /*Log.d("SendThread", "from = "+cmsg._from);
                                Log.d("SendThread", "name = "+cmsg._name);
                                Log.d("SendThread", "varid = "+cmsg._varid);
                                Log.d("SendThread", "value = "+cmsg._value);
                                Log.d("SendThread", "vartype = "+pmsg._vartype);
                                Log.d("SendThread", "min = "+pmsg._min);
                                Log.d("SendThread", "max = "+pmsg._max);*/
                                Log.d("SendThread", "cycle = "+vmsg._cycle);
                                Log.d("SendThread", "--------------------");

                                msg= Message.obtain(null, VCFAgent.MSG_SERVER_SEND, msgType.TYPE_PRM, 0, vmsg);

                                msg.setTarget(hMainThread);
                                msg.sendToTarget();
                                break;
                            case 2: //init command
                                Log.d("InetSocket", "recv_INIT_CMD");
                                vmsg = new VCF_msg.Builder().build();
                                vmsg = vmsg.ADAPTER.decode(bytes);


                                Log.d("RecvThread", "--------------------");
                                Log.d("RecvThread", "seqNo = "+vmsg._seqNo);
                                Log.d("RecvThread", "result = "+vmsg._result);
                                /*Log.d("RecvThread", "response = "+vmsg._response);
                                Log.d("RecvThread", "owner = "+vmsg._owner);
                                Log.d("RecvThread", "name = "+vmsg._name);
                                Log.d("RecvThread", "c_ID = "+vmsg._c_ID);
                                Log.d("RecvThread", "p_ID = "+vmsg._p_ID);
                                Log.d("RecvThread", "value = "+vmsg._value);
                                Log.d("RecvThread", "--------------------");*/

                                msg= Message.obtain(null, VCFAgent.MSG_SERVER_SEND, msgType.TYPE_CMD, 0, vmsg);

                                msg.setTarget(hMainThread);
                                msg.sendToTarget();
                                break;
                            case 3: //oper (param=to:1  cmd=to:2)
                                Log.d("InetSocket", "recv_OPER");
                                vmsg = new VCF_msg.Builder().build();
                                vmsg = vmsg.ADAPTER.decode(bytes);

                                Log.d("RecvThread", "--------------------");
                                Log.d("RecvThread", "seqNo = "+vmsg._seqNo);
                                Log.d("RecvThread", "result = "+vmsg._result);
                                /*Log.d("RecvThread", "response = "+vmsg._response);
                                Log.d("RecvThread", "owner = "+vmsg._owner);
                                Log.d("RecvThread", "name = "+vmsg._name);
                                Log.d("RecvThread", "c_ID = "+vmsg._c_ID);
                                Log.d("RecvThread", "p_ID = "+vmsg._p_ID);
                                Log.d("RecvThread", "value = "+vmsg._value);
                                Log.d("RecvThread", "--------------------");*/

                                msg= Message.obtain(null, VCFAgent.MSG_SERVER_SEND, msgType.TYPE_OPER, 0, vmsg);

                                msg.setTarget(hMainThread);
                                msg.sendToTarget();
                                break;
                        }

                        nMsgsRecv++;
                        Log.d("RecvThread", "[" + nMsgsRecv + "]th message received");
                    }
                }
            } catch (Exception e) { // abnormal close
                Log.d("InetSocket", "Socket closed abnormally");
            }
            resetServerStateFlag(flagConnected);
            hSendThread.getLooper().quit(); // to terminate SendThread
            resetServerStateFlag(flagRecvRunning);
            Log.d("InetSocket", "RecvThread terminated");
        }
    };
    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }
    public void sleep(int time) {
        try { Thread.sleep(time); }
        catch (Exception e) { e.printStackTrace(); }
    }
    synchronized private void setServerStateFlag(int flag) { serverState = (serverState | flag); }
    synchronized private void resetServerStateFlag(int flag) { serverState = (serverState & ~flag); }
    private boolean waitForServerState(int flag, String who) {
        int count = 0;
        while (((serverState & flag) != flag) && count < maxSleepCount) {
            Log.d("InetSocket", who + " : waitForServerState(" + flag + "&" + serverState + ") waiting...");
            sleep(sleepDuration);
            count++;
        }
        if (((serverState & flag) == flag)) return true;
        else return false;
    }
}