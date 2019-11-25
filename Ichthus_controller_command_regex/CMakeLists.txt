cmake_minimum_required(VERSION 2.8.3)
project(ichthus_controller)

## Compile as C++11, supported in ROS Kinetic and newer
add_compile_options(-std=c++11)

#find_package(autoware_build_flags REQUIRED)
find_package(catkin REQUIRED COMPONENTS
  roscpp
  std_msgs
  )

catkin_package(CATKIN_DEPENDS
  roscpp
  std_msgs
  )

###########################
### build configuration ###
###########################

include_directories(
  ${catkin_INCLUDE_DIRS}
  include
  )

add_executable(ichthus_controller
  src/ichthus_controller_core.cpp
  src/ichthus_controller_node.cpp
  )

target_link_libraries(ichthus_controller
  ${catkin_LIBRARIES}
  )

install(TARGETS ichthus_controller
  ARCHIVE DESTINATION ${CATKIN_PACKAGE_LIB_DESTINATION}
  LIBRARY DESTINATION ${CATKIN_PACKAGE_LIB_DESTINATION}
  RUNTIME DESTINATION ${CATKIN_PACKAGE_BIN_DESTINATION}
  )

install(DIRECTORY cfg/
  DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/cfg
  )

install(DIRECTORY launch/
  DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/launch
  )

