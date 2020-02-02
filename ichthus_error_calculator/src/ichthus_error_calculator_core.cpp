#include <iostream>
#include "ichthus_error_calculator/ichthus_error_calculator.h"
namespace ichthus_error_calculator{

    IchthusErrorCalculator::IchthusErrorCalculator(){
        sub_current_pose = nh.subscribe("/current_pose", 10,&IchthusErrorCalculator::callbackCurrentPose, this);
        sub_final_waypoints = nh.subscribe("/final_waypoints",10,&IchthusErrorCalculator::callbackFinalWaypoints,this);
        sub_current_behavior = nh.subscribe("/current_behavior",10,&IchthusErrorCalculator::callbackCurrentBehavior,this);
        //sub_ndt_status = nh.subscribe("/ndt_monitor/ndt_status", 1,&IchthusErrorCalculator::callbackNDTstatus,this);

        start_index = 0;
        start_waypoint_speed = 0;
        expected_convergence_distance = -1;
        mean_square_error = 0;
        sum_square_error = 0;
        root_mean_square_error = 0;
        error_calculation_count = 0;
        ndt_ok=true;
        current_pose_ok = false;
        waypoints_ok = false;
        ready_to_calculate_error = false;
    }

    IchthusErrorCalculator::~IchthusErrorCalculator(){}

    // void IchthusErrorCalculator::callbackNDTstauts(const std_msgs::StringConstPtr& msg){
    //     if(msg->data == "NDT_OK")
    //         ndt_ok = true;
    //     else
    //         ndt_ok = false;
    // }
    

    void IchthusErrorCalculator::callbackCurrentPose(const geometry_msgs::PoseStampedConstPtr& msg){
        current_pose = msg->pose;
        if(!current_pose_ok){
            //if(ndt_ok)
            vehicle_start_point = current_pose.position;
            current_pose_ok = true;
        }

        if(ready_to_calculate_error)
            calculate_error();
        else
            ready_to_calculate_error = check_ready_to_calculate();
    }


    void IchthusErrorCalculator::callbackFinalWaypoints(const autoware_msgs::LaneConstPtr& msg){
        final_waypoints = *msg;
        if(!waypoints_ok){
            initial_waypoints = *msg;
            start_index = find_nearest_waypoint(current_pose,initial_waypoints);

            if(start_index == -1)
                waypoints_ok = false;
            else{
                waypoint_start_point = initial_waypoints.waypoints.at(start_index).pose.pose.position;
                start_waypoint_speed = initial_waypoints.waypoints.at(start_index).twist.twist.linear.x;
                waypoints_ok = true;
            }
        }
    }

    void IchthusErrorCalculator::callbackCurrentBehavior(const geometry_msgs::TwistStampedConstPtr& msg){
        current_behavior = msg->twist.angular.y;

        if(current_behavior == STATE_TYPE::FINISH_STATE){
            mean_square_error = sum_square_error / error_calculation_count;
            root_mean_square_error = sqrt(mean_square_error);
            ROS_INFO("root_mean_square_error : %lf",root_mean_square_error);
            exit(0);
        }
    }

    double IchthusErrorCalculator::get_distance(geometry_msgs::Point &a, geometry_msgs::Point& b){
        return amathutils::find_distance(a,b);
    }

    double IchthusErrorCalculator::get_distance(geometry_msgs::Pose &a, geometry_msgs::Pose& b){
        return get_distance(a.position,b.position);
    }

    double IchthusErrorCalculator::get_expected_convergence_distance(){
        if(expected_convergence_distance != -1 )
            return expected_convergence_distance;

        if(!waypoints_ok && !current_pose_ok){
            ROS_INFO("necessary topics are not subscribed yet");
            return -1;
        }

        if(final_waypoints.waypoints.size() == 0){
            ROS_WARN("final_waypoints size is 0");
            return -1;
        }

        double convergence_distance = start_waypoint_speed * 5 ; // temporal value
        double initial_error = get_distance(vehicle_start_point , waypoint_start_point);

        convergence_distance = convergence_distance + initial_error;

        return convergence_distance;
    }

     bool IchthusErrorCalculator::check_vehicle_convergence(geometry_msgs::Point &start_point, geometry_msgs::Point& current_point,double& expected_convergence_distance){
        if(expected_convergence_distance == -1)
            return false;

        if(get_distance(start_point,current_point) >= expected_convergence_distance)
            return true;

        return false;
    }

    bool IchthusErrorCalculator::check_ready_to_calculate(){

        expected_convergence_distance = get_expected_convergence_distance();

        geometry_msgs::Point current_point = current_pose.position;
        if(check_vehicle_convergence(vehicle_start_point, current_point, expected_convergence_distance))
            return true;
    
        return false;
    }

    int IchthusErrorCalculator::find_nearest_waypoint(const geometry_msgs::Pose &current_pos, const autoware_msgs::Lane& traj){
        if(!waypoints_ok && !current_pose_ok){
            ROS_INFO("necessary topics are not subscribed yet");
            return -1;
        }

        if (traj.waypoints.size() == 0){
          ROS_WARN("trajectory size is zero");
          return -1;
        }

        double my_x = current_pose.position.x;
        double my_y = current_pose.position.y;

        int nearest_index = -1;
        double min_dist_squared = 625; //one final waypoints's max distance is 625
        
        for (unsigned int i = 0; i < traj.waypoints.size(); ++i)
        {
          double dx = my_x - traj.waypoints.at(i).pose.pose.position.x;
          double dy = my_y - traj.waypoints.at(i).pose.pose.position.y;
          double dist_squared = dx * dx + dy * dy; 

          if (dist_squared < min_dist_squared)
          {
            min_dist_squared = dist_squared;
            nearest_index= i;
          }
        }

        return nearest_index;
    }

    bool IchthusErrorCalculator::calculate_current_error(const geometry_msgs::Pose &current_pose, const autoware_msgs::Lane& traj, double& current_square_error){
        
        int nearest_index = find_nearest_waypoint(current_pose,final_waypoints);

        if(nearest_index == -1){
            ROS_WARN("no closeset waypoint found");
            return false;
        }

        double dx = current_pose.position.x - traj.waypoints.at(nearest_index).pose.pose.position.x;
        double dy = current_pose.position.y - traj.waypoints.at(nearest_index).pose.pose.position.y;
        current_square_error = dx * dx + dy * dy;
        
        return true;
    }       

    void IchthusErrorCalculator::calculate_error(){
        double current_square_error = 0;
        if(calculate_current_error(current_pose,final_waypoints,current_square_error)){
            ROS_INFO("<calculate_error> current_square_error : %lf",current_square_error);
            sum_square_error += current_square_error;
            error_calculation_count++;
        }
    }
}
