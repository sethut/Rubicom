package com.rubicom.vcf.ichthus;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


class Param {
    public int _idx;
    public String _name;
    public Integer _varid;
    public Double _value;
    public Integer _from;
    public Integer _vartype; //1(sparam),2(dparam)

    public Param(int idx, String name, Integer varid,Double value, Integer from, Integer vartype) {
        _idx = idx;
        _name = name;
        _varid = varid;
        _value = value;
        _from = from;
        _vartype = vartype;
    }
}
class Cmd {
    public int _idx;
    public String _name;
    public Integer _varid;
    public Cmd(int idx, String name, Integer varid) {
        _idx = idx;
        _name = name;
        _varid = varid;
    }
}

public class VCFServer {
    int cntPrm=0;
    int cntCmd=2;//0==e-stop 1==pullover
    public Param[] params=new Param[100];
    public Cmd[] cmds=new Cmd[100];

    public void setParam(Object obj) {
        VCF_msg vmsg = (VCF_msg) obj;
        VCF_msg.Cmd cmsg = vmsg.cmd.get(0);
        VCF_msg.Prm pmsg = vmsg.prm.get(0);
        Param param=new Param(cntPrm, cmsg._name, cmsg._varid,cmsg._value, cmsg._from, pmsg._vartype);
        params[cntPrm]=param;
        Log.d("ChatServer", "params["+cntPrm+"]._number= "+params[cntPrm]._idx);
        cntPrm++;
    }

    public void setCommand(Object obj) {
        VCF_msg vmsg = (VCF_msg) obj;
        VCF_msg.Cmd cmsg = vmsg.cmd.get(0);
        Cmd cmd=new Cmd(cntCmd, cmsg._name, cmsg._varid);
        Log.d("ChatServer", "cmd._name : "+cmd._name);
        cmds[cntCmd]=cmd;
        Log.d("ChatServer", "commands["+cntCmd+"]._number= "+cmds[cntCmd]._idx);
        cntCmd++;
    }

    public void setDynmcParam(Object obj){
        int i;
        /*Operational_msg omsg = (Operational_msg) obj;
        for(i=0;i<cntPrm;i++)
        {
            if(Vars[i]._varID==omsg._varID)
            {
                Vars[i]._value=omsg._value;
            }
        }여기까지 주*/

    }


/*public class VCFServer {
    int cntPrm=0;
    int cntCmd=2;//0==e-stop 1==pullover

    public Param[] params=new Param[100];
    public Cmd[] cmds=new Cmd[100];

    public void setParam(Object obj) {
        VCF_msg vmsg = (VCF_msg) obj;
        //VCF_msg.Extended emsg = vmsg.ext.get(0);
        Param param=new Param(cntPrm, vmsg._name, vmsg._c_ID, vmsg._value, vmsg._owner, vmsg._seqNo);
        params[cntPrm]=param;
        Log.d("ChatServer", "params["+cntPrm+"]._number= "+params[cntPrm]._idx);
        cntPrm++;
    }

    public void setCommand(Object obj) {
        VCF_msg vmsg = (VCF_msg) obj;
        Cmd cmd=new Cmd(cntCmd, vmsg._name, vmsg._c_ID);
        Log.d("ChatServer", "cmd._name : "+cmd._name);
        cmds[cntCmd]=cmd;
        Log.d("ChatServer", "commands["+cntCmd+"]._number= "+cmds[cntCmd]._idx);
        cntCmd++;
    }

   public void setDynmcParam(Object obj){
        int i;
        /*Operational_msg omsg = (Operational_msg) obj;
        for(i=0;i<cntPrm;i++)
        {
            if(Vars[i]._varID==omsg._varID)
            {
                Vars[i]._value=omsg._value;
            }
        }//여기까지 주*/

    public Param[] getParam() {
        return params;
    }
    public Cmd[] getCmd() {
        return cmds;
    }

    //////////////////////////inetSocket/////////////////////////////////////

    private InetSocket inetSocket;
    public VCFServer(Handler h, Activity a) {
        inetSocket = new InetSocket(h, a);
        Cmd cmd1=new Cmd(0,"E-stop",0);
        cmds[0]=cmd1;
        Cmd cmd2=new Cmd(1,"Pull over",0);
        cmds[1]=cmd2;
    }
    public boolean isAvailable() {
        return inetSocket.isAvailable();
    }
    public void sleep(int time) {
        try { Thread.sleep(time); }
        catch (Exception e) { e.printStackTrace(); }
    }
    public boolean connect(String hname, int hport, String myName, String peerName) {
        Log.d("ChatServer", "connect()");
        if (!inetSocket.connect(hname, hport, myName, peerName))
            return false;
        return true;
    }
    public boolean send(Message msg) {
        Log.d("ChatServer", "send()");
        return inetSocket.send(msg);
    }
    public boolean disconnect() {
        /*if (inetSocket.send(imsg) == false)
            return false;*/
        return inetSocket.disconnect();
    }
}