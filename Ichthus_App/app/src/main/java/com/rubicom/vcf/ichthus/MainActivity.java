package com.rubicom.vcf.ichthus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.TestLooperManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class msgType{
    public static int TYPE_INIT = 0;
    public static int TYPE_PRM = 1;
    public static int TYPE_CMD = 2;
    public static int TYPE_OPER= 3;
}

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE="com.example.myfirstapp.MESSAGE";
    Messenger mService = null;
    private ProgressDialog ringProgressDialog;
    boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new mHandler());

    private Timer timerPoll = null;
    int retryTimer = 3;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("MainActivity", "onServiceConnected()");
            mService= new Messenger(service);
            try{
                Message msg = Message.obtain(null, VCFAgent.MSG_CLIENT_REGISTER);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }catch (RemoteException e){}
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("MainActivity", "onServiceDisconnected");
            mService = null;
        }
    };

    void doBindService(){
        Log.d("MainActivity","doBindService()");
        if(!mIsBound){
            bindService(new Intent(this,VCFAgent.class), mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }
    }

    void doUnbindService(){
        Log.d("MainActivity", "doUnbindService()");
        if(mIsBound){
            if(mService != null){
                try {
                    Message msg =Message.obtain(null, VCFAgent.MSG_CLIENT_UNREGISTER);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e){}
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    ///////////////////////service/////////////////////////////////


    //////////////////////AlertDialog/////////////////////////////
    int msgIdx; //index in service
    int msgNum; //index in activity
    int numView;
    ///////////////////////////////////////////////////////////////
    int cmdCnt=13;
    int prmCnt=1;
    int dynCnt=1;
    int cmdIdx,prmIdx,dynIdx;

    public int [] tempPkt=new int[3];   //number of var = tempPkt[0] ,value of var= tempPkt[1] , param/command = tempPkt[2]

    //public Button[] btns =new Button[cmdCnt];
    public TextView[] DynmcViews =new TextView[4];
    public TextView[] ParamViews =new TextView[1];

    private int[] paramNum = new int[prmCnt];
    private int[] commandNum = new int[cmdCnt];
    private int[] dynmcNum = new int[dynCnt];

    private String[] paramName = new String[prmCnt];
    private String[] commandName = new String[cmdCnt];
    private String[] dynmcName = new String[dynCnt];

    private TextView prm0_view, dynmc0_view, dynmc1_view, dynmc2_view, dynmc3_view, msg_view;
    private Button update_btn,cmd0_btn,prm0_btn,dynmc0_btn,dynmc1_btn,dynmc2_btn,dynmc3_btn;
    private Button estop_btn,pullover_btn;
    private AlertDialog cmd_alert, dynmc_alert;
    private EditText prm0_edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VCFAgent.updateActivity(this);
        prm0_edit= findViewById(R.id.prm0_edit);

        prm0_view= findViewById(R.id.prm0_view);
        dynmc0_view= findViewById(R.id.dynmc0_view);
        dynmc1_view= findViewById(R.id.dynmc1_view);
        dynmc2_view= findViewById(R.id.dynmc2_view);
        dynmc3_view= findViewById(R.id.dynmc3_view);
        msg_view= findViewById(R.id.msg_view);

        update_btn= findViewById(R.id.update_btn);
        cmd0_btn= findViewById(R.id.cmd0_btn);
        //cmd1_btn = findViewById(R.id.cmd1_btn);
        dynmc0_btn = findViewById(R.id.dynmc0_btn);
        dynmc1_btn = findViewById(R.id.dynmc1_btn);
        dynmc2_btn = findViewById(R.id.dynmc2_btn);
        dynmc3_btn = findViewById(R.id.dynmc3_btn);
        prm0_btn = findViewById(R.id.prm0_btn);
        estop_btn = findViewById(R.id.estop_btn);
        pullover_btn = findViewById(R.id.pullover_btn);

        DynmcViews[0] = dynmc0_view;
        DynmcViews[1] = dynmc1_view;
        DynmcViews[2] = dynmc2_view;
        DynmcViews[3] = dynmc3_view;

        ParamViews[0] = prm0_view;
        //btns[1] = cmd1_btn;
        View.OnClickListener listener= new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId())
                {
                    case R.id.update_btn : Update(); break;
                    case R.id.cmd0_btn :  cmd_alert.show(); break;
                    case R.id.dynmc0_btn: dynmc_alert.show(); numView=0; break;
                    case R.id.dynmc1_btn: dynmc_alert.show(); numView=1;break;
                    case R.id.dynmc2_btn: dynmc_alert.show(); numView=2;break;
                    case R.id.dynmc3_btn: dynmc_alert.show(); numView=3;break;
                    case R.id.prm0_btn : PrmSend(0); break;
                    case R.id.estop_btn: Estop(); break;
                    case R.id.pullover_btn: Pullover(); break;
                }
            }
        };
        cmd_alert= CmdSend();
        dynmc_alert= DynmcSend();

        update_btn.setOnClickListener(listener);
        cmd0_btn.setOnClickListener(listener);
        dynmc0_btn.setOnClickListener(listener);
        dynmc1_btn.setOnClickListener(listener);
        dynmc2_btn.setOnClickListener(listener);
        dynmc3_btn.setOnClickListener(listener);
        prm0_btn.setOnClickListener(listener);
        estop_btn.setOnClickListener(listener);
        pullover_btn.setOnClickListener(listener);

        doBindService();
    }

    class mHandler extends Handler {
        public void handleMessage(Message msg)  {
            switch (msg.what) {
                case VCFAgent.MSG_AGENT_RESPONSE:
                    if(msg.arg1==msgType.TYPE_PRM)// arg1=type=1==init prm
                    {
                        Param umsg= (Param) msg.obj;
                        Log.d("MainActivity", "recv_PARAM");
                        if(umsg._vartype == 1) PrintPrm(umsg);
                        else if(umsg._vartype == 2) PrintDynmcPrm(umsg);
                    }
                    else if(msg.arg1==msgType.TYPE_CMD) // arg1=type=2==init cmd
                    {
                        Cmd umsg= (Cmd) msg.obj;
                        Log.d("MainActivity", "recv_CMD");
                        PrintCmd(umsg);
                    }
                    else if(msg.arg1==msgType.TYPE_OPER)// arg1=type=3==oper ACK OR DYNAMIC
                    {
                        Log.d("MainActivity", "recv_DYNMC_PRM");
                        Param umsg= (Param) msg.obj;
                        DynmcViews[numView].setText(""+ umsg._name +"\n" + umsg._value);
                        //dynmc0_view.setText(""+umsg.  _value);
                    }
                    break;
                case VCFAgent.MSG_AGENT_COMPLETED:
                    Log.d("MainActivity", "recv_COMPLETED");
                    VCFAgent.isUpdated=true;
                    request(msgType.TYPE_INIT, null);
                    ringProgressDialog.dismiss();
                    Log.d("MainActivity", "prmIdx: "+prmIdx);
                    prmIdx = 0;
                    cmdIdx = 0;
                    dynIdx = 0;
                    Log.d("MainActivity", "prmIdx: "+prmIdx);
                    break;

                case VCFAgent.MSG_CONNECT_SUCCESS:
                    Log.d("MainActivity", "CONNECT_SUCCESS");
                    retryTimer = 3;
                    disableTimerPoll();
                    //wait until completed
                    break;

                case VCFAgent.MSG_CONNECT_FAIL:
                    Log.d("MainActivity", "CONNECT_FAIL");
                    VCFAgent.isUpdated = false;
                    retryTimer--;
                    disableTimerPoll();
                    if(retryTimer <= 0) {
                        finish();
                    }
                    else {
                        Log.d("MainActivity", "retry times = "+retryTimer);
                        Update();
                    }
                    break;

                case VCFAgent.MSG_AGENT_ERROR:
                    ErrorMessage();
                    break;

                case VCFAgent.MSG_AGENT_ABORTED:
                    AbortActivity();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    public void Estop(){
        tempPkt[0]=0;
        tempPkt[1]=0;
        tempPkt[2]=msgType.TYPE_CMD;
        if(VCFAgent.isUpdated) {
            sendMessageToService(VCFAgent.MSG_CLIENT_SEND, msgType.TYPE_OPER, tempPkt);
            msg_view.setTextColor(Color.parseColor("#0000FF"));
            msg_view.setText("Send E-stop");
        }
    }

    public void Pullover(){
        tempPkt[0]=1;
        tempPkt[1]=0;
        tempPkt[2]=msgType.TYPE_CMD;
        if(VCFAgent.isUpdated) {
            sendMessageToService(VCFAgent.MSG_CLIENT_SEND, msgType.TYPE_OPER, tempPkt);
            msg_view.setTextColor(Color.parseColor("#0000FF"));
            msg_view.setText("Send PullOver");
        }
    }

    //activity must be blocked before update complete
    public void AbortActivity()
    {
        ringProgressDialog.dismiss();
        Toast.makeText(this,"agent aborted",Toast.LENGTH_SHORT).show();
    }

    public void ErrorMessage()
    {
        Toast.makeText(this,"agent error",Toast.LENGTH_SHORT).show();
    }

    //request parameter and command to server
    public void Update(){
        VCFAgent.isUpdated =false;

        VCF_msg imsg = new VCF_msg.Builder()
                ._seqNo(1)
                ._version(0)
                ._result(1)
                ._msg("test msg")
                ._cycle(10).build();
        sendMessageToService(VCFAgent.MSG_CLIENT_SEND, msgType.TYPE_INIT, imsg);
        Toast.makeText(this, "Try to send message To Server", Toast.LENGTH_SHORT).show();
        enableTimerPoll();

        ringProgressDialog = ProgressDialog.show(this, "Please wait ...", "Up to 15 seconds", true);
        ringProgressDialog.setCancelable(true);
    }

    public void PrmSend(int msgNum){
        try {
            int value = Integer.parseInt(prm0_edit.getText().toString());

            if(prm0_edit.length() == 0) return;
            if(value >=100) { // use min,max in umsg
                Toast.makeText(this, "Write value<=100", Toast.LENGTH_SHORT).show();
                return;
            }
            tempPkt[0]=paramNum[msgNum];
            tempPkt[1]=value;
            tempPkt[2]=msgType.TYPE_PRM;
            if(VCFAgent.isUpdated) {
                sendMessageToService(VCFAgent.MSG_CLIENT_SEND,msgType.TYPE_OPER,tempPkt);
                msg_view.setTextColor(Color.parseColor("#0000FF"));
                msg_view.setText("Send "+paramName[msgNum]);
            }
            else Log.d("MainActivity", "PrmSend() fail, not updated");
        }
        catch (NumberFormatException e){
            Toast.makeText(this, "Write integer value", Toast.LENGTH_SHORT).show();
        }
    }


    public AlertDialog CmdSend(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("SET Command :")
                .setSingleChoiceItems(commandName, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        msgIdx=commandNum[which];
                        msgNum=which;
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tempPkt[0]=msgIdx;
                        tempPkt[1]=0; //never used
                        tempPkt[2]=msgType.TYPE_CMD;
                        if(VCFAgent.isUpdated) {
                            sendMessageToService(VCFAgent.MSG_CLIENT_SEND,msgType.TYPE_OPER,tempPkt);
                            msg_view.setTextColor(Color.parseColor("#0000FF"));
                            msg_view.setText("Send "+commandName[msgNum]);
                        }
                        else Log.d("MainActivity", "CmdSend() fail, not updated");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        msg_view.setTextColor(Color.parseColor("#FF0000"));
                        msg_view.setText("Send no msg");
                    }
                });
        return builder.create();
    }

    public AlertDialog DynmcSend(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("SET Parameter :")
                .setItems(dynmcName, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(VCFAgent.isUpdated) {
                            request(msgType.TYPE_OPER, dynmcNum[which]);
                            msg_view.setTextColor(Color.parseColor("#0000FF"));
                            msg_view.setText("Request " + dynmcName[which]);
                        }
                    }
                });
        return builder.create();
    }

    public void request(int type, Object obj){
        sendMessageToService(VCFAgent.MSG_CLIENT_REQUEST, type, obj); //if type==init, obj dosen't use
    }

    public void PrintDynmcPrm(Param umsg){
        dynmcNum[dynIdx] = umsg._idx;
        dynmcName[dynIdx] = umsg._name;
        dynIdx++;
    }
    public void PrintPrm(Param umsg){
        paramNum[prmIdx] = umsg._idx;
        paramName[prmIdx] = umsg._name;
        //save umsg._min&max and use in prm_send()
        ParamViews[0].setText(umsg._name);
        prmIdx++;
    }

    public void PrintCmd(Cmd umsg){
        commandNum[cmdIdx] = umsg._idx;
        commandName[cmdIdx] = umsg._name;
        //btns[cmdidx].setText(umsg._name);
        cmdIdx++;
    }

    private void sendMessageToService(int what, int type, Object obj) {
        if(mIsBound){
            if(mService != null){
                try {
                    Message msg= Message.obtain(null, what, type, 0, obj);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }catch (RemoteException e){
                }
            }
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        Log.d("MainActivity", "onDestroy()");
        try{
            doUnbindService();
            stopService(new Intent(MainActivity.this, VCFAgent.class));
        }catch (Throwable t){
            Log.e("MainActivity","Failed to unbind from the service",t);
        }
    }

    private class PollAgent extends TimerTask {
        int countdown = 5;
        public void run() {
            if (countdown == 0) {
                disableTimerPoll();
                if (VCFAgent.isUpdated) {
                    VCFAgent.isUpdated = false;
                }
                Log.d("MainActivity", "update fail, remain times : " + retryTimer);
            }
            else {
                if (VCFAgent.isUpdated) {
                    disableTimerPoll();
                }
                else
                    countdown--;
                Log.d("MainActivity", "countdown = "+countdown);
            }
        }
    }

    private void enableTimerPoll() {
        if (timerPoll == null) {
            PollAgent job = new PollAgent();
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