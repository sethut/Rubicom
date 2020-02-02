#include "ichthus_error_calculator/ichthus_error_calculator.h"

int main(int argc, char* argv[]){
    ros::init(argc, argv, "ichthus_error_calculator");
    ichthus_error_calculator::IchthusErrorCalculator error_calculator;

    ros::spin();
    return 0;
}