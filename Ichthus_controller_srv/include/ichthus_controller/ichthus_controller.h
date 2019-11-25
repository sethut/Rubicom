/*
 * Copyright 2019 Soongsil University. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ICHTHUS_CONTROLLER_H
#define ICHTHUS_CONTROLLER_H

#include <ros/ros.h>
#include "ichthus_controller/con_msg.h"


#define MAX_STATE_VARS 100

using namespace std;
using namespace ros;

namespace ichthus_controller
{

  enum varProp { NO_PROP, CONFIG_PROP, STATE_PROP }; // variable's property
  
  class StateVar {
  public:
    double lower; // sometimes called _left, _de_step, _park, _Kp, _min_radius
    double upper; // sometimes called _right, _ac_step, _reverse, _Ki, _wheel_base
    double origin; // sometimes called _front, _jerk, _neutral, _Kd
    double step;   // sometimes called _rear, _margin, _drive
    double value;
    // lidar pose parameter names
#define _left  lower
#define _right upper
#define _front origin
#define _rear  step
    // motion-related parameter names
#define _de_step lower
#define _ac_step upper
#define _jerk    origin
#define _margin  step
    // gearstick pose parameter names 
#define _park    lower
#define _reverse upper
#define _neutral origin
#define _drive   step
    // pid gain parameter names
#define _Kp lower
#define _Ki upper
#define _Kd origin
    // vehicle geometry parameter names
#define _min_radius lower
#define _wheel_base upper

    int timestamp;
    varProp property; // state variable's property
    StateVar(): lower(0), upper(0), origin(0), step(0), value(0), timestamp(0), property(NO_PROP) {}
    void reset() {
      lower = 0;
      upper = 0;
      origin = 0;
      step = 0;
      value = 0;
      timestamp = 0;
      property = NO_PROP;
    }
    string to_string() {
      ostringstream ss;
      ss << *this;
      return ss.str();
    }
    friend std::ostream& operator<<(std::ostream &out, StateVar& var) {
      out << "[lower=" << var.lower
	  << ", upper=" << var.upper
	  << ", origin=" << var.origin
	  << ", step=" << var.step
	  << ", value=" << var.value
	  << ", timestamp=" << var.timestamp
	  << ", property=" << var.property << "]";
      return out;
    }
  };
  
class IchthusController
{
 private:
  string configVarList[MAX_STATE_VARS] = { // shouldnot include '.' in the names
    "pedal_accel_pos",
    "pedal_decel_pos",
    "steer_wheel_pos",
    "gear_stick_pos",
    "lidar_front_deg",
    "lidar_left_deg",
    "lidar_right_deg",
    "motion_pull_over",
    "motion_pedal_homing",
    "motion_lidar_homing",
    "motion_lidar_mapping",
    "motion_lidar_driving",
    "motion_lidar_parking",
    "cc_gains_accel",
    "cc_gains_decel",
    "cc_switch_margins",
    "cc_target_velocity",
    "cc_integral_base",
    "sc_vehicle_geometry",
    ""
  };

  string stateVarList[MAX_STATE_VARS] = { // should include '.' in the names
    "ecat.state",
    "ecat.num_motors",
    "pedal_accel.tpos",
    "pedal_decel.tpos",
    "steer_wheel.tpos",
    "steer_wheel.apos",
    "gear_stick.tpos",
    "lidar_front.tpos",
    "lidar_left.tpos",
    "lidar_right.tpos",
    "lidar_pose.mode",
    "motion.state",
    "cc.target_velocity",
    "sc.target_avelocity",
    ""
  };
  
  int num_motors;
  int motor_id_pedal_accel;
  int motor_id_pedal_decel;
  int motor_id_lidar_front;
  int motor_id_lidar_left;
  int motor_id_lidar_right;
  int motor_id_steer_wheel;
  int motor_id_gear_stick;

  ros::NodeHandle nh;
  
 public:
  map<string, StateVar> stateVars;

  IchthusController();
  ~IchthusController();
  void loadStateVars();
  void printStateVars();
  bool getStateVar(string name, StateVar& var);
  bool setStateVar(string name, double value, int time);
  string getStateVarList();
  string getStateVarListAll();
  bool serviceController(con_msg::Request &req, con_msg::Response &res);
  void doLoop();
};

}

#endif // ICHTHUS_CONTROLLER_H
