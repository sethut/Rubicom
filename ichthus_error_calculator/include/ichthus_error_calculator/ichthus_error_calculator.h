#include <iostream>
#include <ros/ros.h>
#include "geometry_msgs/PoseStamped.h"
#include "autoware_msgs/Lane.h"
#include <amathutils_lib/amathutils.hpp>
using namespace std;
using namespace ros;

namespace ichthus_error_calculator{
    
	enum STATE_TYPE {INITIAL_STATE, WAITING_STATE, FORWARD_STATE, STOPPING_STATE, EMERGENCY_STATE,
	TRAFFIC_LIGHT_STOP_STATE,TRAFFIC_LIGHT_WAIT_STATE, STOP_SIGN_STOP_STATE, STOP_SIGN_WAIT_STATE, FOLLOW_STATE, 
    LANE_CHANGE_STATE, OBSTACLE_AVOIDANCE_STATE, GOAL_STATE, FINISH_STATE, YIELDING_STATE, BRANCH_LEFT_STATE, 
    BRANCH_RIGHT_STATE}; // from roadnetwork.h

    class IchthusErrorCalculator{
        private:
            ros::NodeHandle nh;
            ros::Subscriber sub_current_pose;
            ros::Subscriber sub_final_waypoints;
            ros::Subscriber sub_current_behavior;
            ros::Subscriber sub_ndt_status;
        public:
            IchthusErrorCalculator();
            ~IchthusErrorCalculator();

            geometry_msgs::Pose current_pose;
            autoware_msgs::Lane final_waypoints;
            autoware_msgs::Lane initial_waypoints;
            geometry_msgs::Point current_point;
            geometry_msgs::Point vehicle_start_point;
            geometry_msgs::Point waypoint_start_point;

            void callbackCurrentPose(const geometry_msgs::PoseStampedConstPtr &msg);
            void callbackFinalWaypoints(const autoware_msgs::LaneConstPtr& msg);
            void callbackCurrentBehavior(const geometry_msgs::TwistStampedConstPtr& msg);

            double get_expected_convergence_distance(void);
            double get_distance(geometry_msgs::Point &a , geometry_msgs::Point &b);
            double get_distance(geometry_msgs::Pose &a, geometry_msgs::Pose& b);
            bool check_vehicle_convergence(geometry_msgs::Point &start_point, geometry_msgs::Point& current_point, double& expected_convergence_distance);

            bool check_ready_to_calculate(void);
            int find_nearest_waypoint(const geometry_msgs::Pose &current_pos, const autoware_msgs::Lane& traj);
            bool calculate_current_error(const geometry_msgs::Pose &current_pose, const autoware_msgs::Lane& traj, double& current_error);
            void calculate_error(void);
            
            int start_index;
            double start_waypoint_speed;

            int current_behavior;
            
            bool ndt_ok;
            bool current_pose_ok;
            bool waypoints_ok;
            bool ready_to_calculate_error;

            double expected_convergence_distance;
            double sum_square_error;
            double mean_square_error;
            double root_mean_square_error;
            int error_calculation_count;

            
    };
}