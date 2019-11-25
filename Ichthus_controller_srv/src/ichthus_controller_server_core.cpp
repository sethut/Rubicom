#include "ichthus_controller/ichthus_controller.h"
#include <stdlib.h>

namespace ichthus_controller {
  IchthusController::IchthusController() {}
  IchthusController::~IchthusController() {}

  void IchthusController::loadStateVars()  {
    bool r1, r2, r3, r4;

    ////////////////////////////////////////////////
    /// step 1 : load config variables (static)
    ////////////////////////////////////////////////    
    nh.getParam("num_motors", num_motors);
    nh.getParam("motor_id_pedal_accel", motor_id_pedal_accel);
    nh.getParam("motor_id_pedal_decel", motor_id_pedal_decel);
    nh.getParam("motor_id_steer_wheel", motor_id_steer_wheel);
    nh.getParam("motor_id_lidar_front", motor_id_lidar_front);
    nh.getParam("motor_id_gear_stick",  motor_id_gear_stick);
    nh.getParam("motor_id_lidar_left",  motor_id_lidar_left);
    nh.getParam("motor_id_lidar_right", motor_id_lidar_right);
    //icthus_i30.yaml 에서 받아오는 parameter

    for (int i = 0; configVarList[i] != ""; i++) {
      string name = configVarList[i];
      StateVar var;
      var.property = CONFIG_PROP;
      r1 = r2 = r3 = r4 = false;
    

      // default ranging parameters
      r1 = nh.getParam((name + "/lower").c_str(),  var.lower);
      r2 = nh.getParam((name + "/upper").c_str(),  var.upper);
      r3 = nh.getParam((name + "/origin").c_str(), var.origin);
      r4 = nh.getParam((name + "/step").c_str(),   var.step);
      if (r1 || r2 || r3 || r4) {
	stateVars.insert(pair<string,StateVar>(configVarList[i], var));
	continue;
      }

      // lidar pose parameters
      r1 = nh.getParam((name + "/left").c_str(),  var._left);
      r2 = nh.getParam((name + "/right").c_str(), var._right);
      r3 = nh.getParam((name + "/front").c_str(), var._front);
      r4 = nh.getParam((name + "/rear").c_str(),  var._rear);
      if (r1 || r2 || r3 || r4) {
	stateVars.insert(pair<string,StateVar>(configVarList[i], var));
	continue;
      }

      // motion-related parameters
      r1 = nh.getParam((name + "/jerk").c_str(),    var._jerk);
      r2 = nh.getParam((name + "/margin").c_str(),  var._margin);
      r3 = nh.getParam((name + "/ac_step").c_str(), var._ac_step);
      r4 = nh.getParam((name + "/de_step").c_str(), var._de_step);
      if (r1 || r2 || r3 || r4) {
	stateVars.insert(pair<string,StateVar>(configVarList[i], var));
	continue;
      }

      // gearstick pose parameters      
      r1 = nh.getParam((name + "/park").c_str(),    var._park);
      r2 = nh.getParam((name + "/reverse").c_str(), var._reverse);
      r3 = nh.getParam((name + "/neutral").c_str(), var._neutral);
      r4 = nh.getParam((name + "/drive").c_str(),   var._drive);
      if (r1 || r2 || r3 || r4) {
	stateVars.insert(pair<string,StateVar>(configVarList[i], var));
	continue;
      }
      
      // pid gain parameters      
      r1 = nh.getParam((name + "/Kp").c_str(), var._Kp);
      r2 = nh.getParam((name + "/Ki").c_str(), var._Ki);
      r3 = nh.getParam((name + "/Kd").c_str(), var._Kd);
      if (r1 || r2 || r3) {
	stateVars.insert(pair<string,StateVar>(configVarList[i], var));
	continue;
      }

      // vehicle geometry parameters      
      r1 = nh.getParam((name + "/min_radius").c_str(), var._min_radius);
      r2 = nh.getParam((name + "/wheel_base").c_str(), var._wheel_base);
      if (r1 || r2) {
	stateVars.insert(pair<string,StateVar>(configVarList[i], var));
	continue;
      }
    }

    ////////////////////////////////////////////////
    /// step 2 : load state variables (dynamic)
    ////////////////////////////////////////////////    
    StateVar var;
    var.property = STATE_PROP;
    for (int i = 0; stateVarList[i] != ""; i++)
      stateVars.insert(pair<string,StateVar>(stateVarList[i], var));
  }

  void IchthusController::printStateVars()  {
    map<string, StateVar>::iterator it;
    int n = 0;

    for (n = 0, it = stateVars.begin(); it != stateVars.end(); ++it, n++) {
      cout << "stateVar[" << n << "]: "
	   << it->first.c_str()
	   << it->second << endl;
    }
  }

  bool IchthusController::getStateVar(string name, StateVar& var) {
    map<string, StateVar>::iterator it;

    it = stateVars.find(name.c_str());
    if (it == stateVars.end())
      return false;

    cout << "get: " << it->first.c_str() << it->second << endl;
    var = it->second;
    return true;
  }

  bool IchthusController::setStateVar(string name, double value, int time) {
    map<string, StateVar>::iterator it;

    it = stateVars.find(name.c_str());
    if (it == stateVars.end())
      return false;
    
    it->second.value = value;
    it->second.timestamp = time;
    cout << "set: " << it->first.c_str() << it->second << endl;
    return true;
  }

  string IchthusController::getStateVarList() {
    ostringstream ss;
    map<string, StateVar>::iterator it;

    it = stateVars.begin();
    ss << "[ " << it->first.c_str();
    ++it;
    for (; it != stateVars.end(); ++it)
      ss << ", " << it->first.c_str();
    ss << " ]";

    return ss.str();
  }

  string IchthusController::getStateVarListAll()  {
    ostringstream ss;
    map<string, StateVar>::iterator it;

    it = stateVars.begin();
    ss << "[ " << endl;
    ss << it->first.c_str() << it->second << endl;
    ++it;
    for (; it != stateVars.end(); ++it)
      ss << it->first.c_str() << it->second << endl;
    ss << " ]";

    return ss.str();
  }
  
  bool IchthusController::serviceController(con_msg::Request &req, con_msg::Response &res) {
    StateVar var;
    string cmd = req.cmd;
    string arg1 = req.arg1;
    string arg2 = req.arg2;
    
    if (cmd == "get") {
      if (arg1 == "varlist") {
	      if (arg2 == "")
	        res.res = getStateVarList(); // cmdstring: "get varlist"
	      else if (arg2 == "all")
	        res.res = getStateVarListAll(); // cmdstring: "get varlist all"
      }
      else if (getStateVar(arg1, var)) {
	if (arg2 == "lower")          res.res = "[lower=" + to_string(var.lower) + "]";
	else if (arg2 == "upper")     res.res = "[upper=" + to_string(var.upper) + "]";
	else if (arg2 == "origin")    res.res = "[origin=" + to_string(var.origin) + "]";
	else if (arg2 == "step")      res.res = "[step=" + to_string(var.step) + "]";
	else if (arg2 == "value")     res.res = "[value=" + to_string(var.value) + "]";
	else if (arg2 == "timestamp") res.res = "[timestamp=" + to_string(var.timestamp) + "]";
	else if (arg2 == "property")  res.res = "[property=" + to_string(var.property) + "]";
	else if (arg2 == "")          res.res = var.to_string();
      }
      else
	res.res = "[no such variable: " + arg1 + "]";
    }
    else if (cmd == "set") {
      if (setStateVar(arg1, atof(arg2.c_str()), 0)) { // what about time?
	getStateVar(arg1, var); // get the latest value of var
	res.res = var.to_string();
      }
      else
	res.res = "[no such variable: " + arg1 + "]";
    }
    return true;
  }

  void IchthusController::doLoop() {
    StateVar var;
    loadStateVars();
    printStateVars();
    getStateVar("steer_wheel_pos", var);
    ros::ServiceServer service = nh.advertiseService("ichthus_controller", &IchthusController::serviceController, this);
    ros::spin();
  }
}

