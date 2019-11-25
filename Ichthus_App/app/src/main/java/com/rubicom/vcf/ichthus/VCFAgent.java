package com.rubicom.vcf.ichthus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


class ServerInfo{
    public String servAddr;
    public  int servPort;
    public String myName;
    public String peerName;
    public ServerInfo(String addr, int port, String s1, String s2){
        servAddr = addr;
        servPort = port;
        myName = s1;
        peerName = s2;
    }
}

public class VCFAgent extends Service {
    static final int MSG_CLIENT_REGISTER = 1;
    static final int MSG_CLIENT_UNREGISTER = 2;
    static final int MSG_CLIENT_SEND = 3;
    static final int MSG_CLIENT_REQUEST=4;
    static final int MSG_SERVER_SEND = 5;
    static final int MSG_AGENT_RESPONSE = 6;
    static final int MSG_AGENT_COMPLETED = 7;
    static final int MSG_AGENT_ABORTED = 8;
    static final int MSG_AGENT_ERROR = 9;
    static final int MSG_CONNECT_SUCCESS = 10;
    static final int MSG_CONNECT_FAIL = 111;

    private final int TimeToConnect = 1000;
    private static boolean isRunning = false;
    private static boolean isConnected = false;
    public static boolean isUpdated = false;
    private Handler mHandler = new IncomingHandler();
    private ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // list of registered clients
    private static WeakReference<Activity> mainActivity;
    private Messenger mMessenger = new Messenger(mHandler); // handler for clients to send msg to this service
    private VCFServer vcfServer=null;
    private Timer timerPoll = null;
    private int retryTimer = 3;
    private int cmdidx=2;
    private boolean ackCheck;
    public static void updateActivity(Activity activity){mainActivity=new WeakReference<Activity>(activity);}//get Activity instance
    public static boolean isRunning() {
        return isRunning;
    }
    Message tempMsg;


    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CLIENT_REGISTER:
                    mClients.add(msg.replyTo);
                    Log.d("CommService", "CLIENT_REGISTER");
                    break;
                case MSG_CLIENT_UNREGISTER:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_CLIENT_SEND:
                    if((msg.arg1==msgType.TYPE_INIT)&&(vcfServer==null)) {
                        connectToServer();
                        vcfServer.sleep(TimeToConnect);
                        if(!vcfServer.isAvailable())
                        {
                            sendMessageToUI(MSG_CONNECT_FAIL, 0,0); //SEND fail MSG to activity;
                            Log.d("CommService", "connect fault");
                            break;
                        }
                        else {
                            sendMessageToUI(MSG_CONNECT_SUCCESS, 0,0);
                        }
                    }
                    else if((msg.arg1==msgType.TYPE_INIT)&&(vcfServer!=null)) {
                        vcfServer.cntCmd = cmdidx;//0==e-stop 1==pullover
                        vcfServer.cntPrm = 0;

                    }
                    else if(msg.arg1==msgType.TYPE_OPER) msg.obj=setMessage(msg);

                    if (isConnected == false) {
                        return; // The timer handler may falsely try to send a key.
                    }
                    tempMsg=obtainMessage(msg.what,msg.arg1,msg.arg2,msg.obj);
                    Log.d("vcfagent", String.valueOf(msg.obj));
                    Log.d("vcfagent", String.valueOf(tempMsg));
                    Log.d("vcfagent", String.valueOf(msg));
                    sendMessageToServer(msg);
                    break;
                case MSG_SERVER_SEND: // server mode
                    ackCheck=true;
                    if(msg.arg1==msgType.TYPE_INIT){
                        VCF_msg vmsg = (VCF_msg) msg.obj;
                        Log.d("CommService", "recv_INIT");
                        if(vmsg._result==-1) {
                            Log.d("CommService","ERROR MESSAGE");
                            sendMessageToUI(MSG_AGENT_ERROR, 0,0);
                            break;
                        }
                        if(vmsg._result==2) sendMessageToUI(MSG_AGENT_COMPLETED, 0,0);
                    }
                    else if(msg.arg1==msgType.TYPE_PRM)
                    {
                        VCF_msg vmsg = (VCF_msg) msg.obj;
                        Log.d("CommService", "recv_PARAM");
                        if(vmsg._result==-1) {
                            Log.d("CommService","ERROR MESSAGE");
                            sendMessageToUI(MSG_AGENT_ERROR, 0,0);
                            break;
                        }
                        else if(vmsg._result==2) sendMessageToUI(MSG_AGENT_COMPLETED, 0,0);
                        vcfServer.setParam(msg.obj);
                    }
                    else if(msg.arg1==msgType.TYPE_CMD)
                    {
                        VCF_msg vmsg = (VCF_msg) msg.obj;
                        Log.d("CommService", "recv_COMMAND");
                        if(vmsg._result==-1) {
                            Log.d("CommService","ERROR MESSAGE");
                            sendMessageToUI(MSG_AGENT_ERROR, 0,0);
                            break;
                        }
                        else if(vmsg._result==2) sendMessageToUI(MSG_AGENT_COMPLETED, 0,0);
                        vcfServer.setCommand(msg.obj);
                    }
                    else if(msg.arg1==msgType.TYPE_OPER)
                    {
                        VCF_msg vmsg= (VCF_msg) msg.obj;
                        if(vmsg._result==-1) {
                            Log.d("CommService","ERROR MESSAGE");
                            sendMessageToUI(MSG_AGENT_ERROR, 0,0);
                            break;
                        }
                        if(vmsg._result==1) vcfServer.setDynmcParam(msg.obj);
                        Log.d("CommService", "recv_OPER");
                    }
                    break;
                case MSG_CLIENT_REQUEST:
                    Log.d("CommService", "CLIENT_REQUEST");
                    int i;
                    if(msg.arg1==msgType.TYPE_INIT) {
                        Param[] params = vcfServer.getParam();
                        for (i = 0; i <= vcfServer.cntPrm; i++)
                        {
                            if(params[i]!=null) sendMessageToUI(MSG_AGENT_RESPONSE, msgType.TYPE_PRM, params[i]);
                        }

                        Cmd[] cmds = vcfServer.getCmd();
                        for (i = cmdidx; i <= vcfServer.cntCmd; i++)
                        {
                            if(cmds[i]!=null) sendMessageToUI(MSG_AGENT_RESPONSE, msgType.TYPE_CMD, cmds[i]);
                        }
                    }
                    else if(msg.arg1==msgType.TYPE_OPER) {
                        Param[] prm = vcfServer.getParam();
                        int idx=(int)msg.obj;
                        //for (i = 0; i <= vcfServer.cntPrm; i++)
                        if((prm[idx]!=null)/*&&(prm[i]._idx==idx)*/)//vartype 1 is dynmc_prm
                            sendMessageToUI(MSG_AGENT_RESPONSE, msgType.TYPE_OPER, prm[idx]);
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private void sendMessageToUI(int what, int type, Object obj) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try { // Send data as an char(key)
                mClients.get(i).send(Message.obtain(null, what, type, 0, obj));
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list;
                // we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    private boolean connectToServer() {
        Log.d("CommService", "CLIENT_CONNECT");
        vcfServer = new VCFServer(mHandler, mainActivity.get());
        ServerInfo info = new ServerInfo("192.168.0.13", 9000, null, null);
        if (!vcfServer.connect(info.servAddr, info.servPort, info.myName, info.peerName)) {
            //retry
            Log.d("CommService", "connection fail");
            return false;
        }
        isConnected = true;
        return true;
    }

    private void sendMessageToServer(Message msg){
        ackCheck = false;
        retryTimer--;
        if (vcfServer.send(msg) == false) {
            Log.d("CommService", "send fail");
        }
        enableTimerPoll();
    }

    private Object setMessage(Message msg){
        int [] tempPkt=(int[])msg.obj;
        int num = tempPkt[0];
        int value=tempPkt[1];

        Param pArray[];
        Cmd cArray[];

        if(tempPkt[2]== msgType.TYPE_PRM)
        {
            pArray= vcfServer.getParam();
            VCF_msg final_msg = new VCF_msg.Builder()
                    ._seqNo(1)
                    ._version(0)
                    ._result(1)
                    ._msg("TYPE_PRM")
                    ._cycle(1).build();
            return final_msg;
        }
        else
        {
            cArray = vcfServer.getCmd();
            VCF_msg final_msg = new VCF_msg.Builder()
                    ._seqNo(1)
                    ._version(0)
                    ._result(1)
                    ._msg("TYPE_CMD")
                    ._cycle(1).build();
            return final_msg;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("CommService", "onBind()");
        return mMessenger.getBinder();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("CommService", "onCreate()");
        isRunning = true;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("CommService", "onStartCommand(): startId = " + startId + ": " + intent);
        return START_STICKY; // run until explicitly stopped.
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("CommService", "onDestroy()");
        isRunning = false;
    }

    private class PollServer extends TimerTask {
        int countdown = 5;
        public void run() {
            if (countdown == 0) {
                disableTimerPoll();
                if (retryTimer <= 0) {
                    retryTimer = 3;
                    sendMessageToUI(MSG_AGENT_ABORTED,0,0);
                    vcfServer.disconnect(); //after this, activity has to blocked except update button.
                    vcfServer=null;
                }
                else {
                    Log.d("CommService", "disconnected, remain times : " + retryTimer);
                    sendMessageToServer(tempMsg);
                }
            }
            else {
                if (ackCheck) {
                    retryTimer = 3;
                    disableTimerPoll();
                }
                else
                    countdown--;
                Log.d("CommService", "countdown = "+countdown);
            }
        }
    }

    private void enableTimerPoll() {
        if (timerPoll == null) {
            PollServer job = new PollServer();
            timerPoll = new Timer(true);
            timerPoll.scheduleAtFixedRate(job, 1000, 1000); // start the timer of period 1s, 1s later from now
        }
    }
    private void disableTimerPoll() {
        if (timerPoll != null) {
            timerPoll.cancel();
            timerPoll = null;
        }
    }
}