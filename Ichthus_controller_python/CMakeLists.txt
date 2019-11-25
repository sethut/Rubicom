cmake_minimum_required(VERSION 2.8.3)
project(ichthus_controller_python)

#find_package(autoware_build_flags REQUIRED)
find_package(catkin REQUIRED COMPONENTS
  rospy
  std_msgs
  )

#catkin_install_python(PROGRAMS scripts/
#	ichthus_controller_python.py
#	DESTINATION $(CATKIN_PACKAGE_BIN_DESTINATION))

catkin_package(CATKIN_DEPENDS
  rospy
  std_msgs
  )

###########################
### build configuration ###
###########################

include_directories(
  ${catkin_INCLUDE_DIRS}
  include
  )

install(DIRECTORY cfg/
  DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/cfg/command_list.yaml
  )

install(DIRECTORY launch/
  DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/ichthus_controller_python.launch
  )

