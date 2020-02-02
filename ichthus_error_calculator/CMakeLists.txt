cmake_minimum_required(VERSION 2.8.3)
project(ichthus_error_calculator)

## Compile as C++11, supported in ROS Kinetic and newer
add_compile_options(-std=c++11 -lpcanbasic)


find_package(autoware_build_flags REQUIRED)
find_package(catkin REQUIRED COMPONENTS
  autoware_config_msgs
  autoware_build_flags
  amathutils_lib
  roscpp
  std_msgs
  message_generation
  )

#add_service_files(FILES
#  con_msg.srv
#  )

#generate_messages(DEPENDENCIES
#  std_msgs
#)

catkin_package(CATKIN_DEPENDS
  autoware_config_msgs
  amathutils_lib
  roscpp
  std_msgs
  message_runtime
  )

# Resolve system dependency on yaml-cpp, which apparently does not
# provide a CMake find_package() module.
find_package(PkgConfig REQUIRED)
pkg_check_modules(YAML_CPP REQUIRED yaml-cpp)
find_path(YAML_CPP_INCLUDE_DIR NAMES yaml_cpp.h PATHS ${YAML_CPP_INCLUDE_DIRS})
find_library(YAML_CPP_LIBRARY NAMES YAML_CPP PATHS ${YAML_CPP_LIBRARY_DIRS})
link_directories(${YAML_CPP_LIBRARY_DIRS})
if(NOT ${YAML_CPP_VERSION} VERSION_LESS "0.5")
  add_definitions(-DHAVE_NEW_YAMLCPP)
endif(NOT ${YAML_CPP_VERSION} VERSION_LESS "0.5")

###########################
### build configuration ###
###########################
##SET(CMAKE_CXX_FLAGS "-O2 -g -Wall -no-pie${CMAKE_CXX_FALGS}")
SET(CMAKE_CXX_FLAGS "-O2 -g -Wall ${CMAKE_CXX_FALGS}")
include_directories(
  include
  ${catkin_INCLUDE_DIRS}
)

add_executable(ichthus_error_calculator
src/ichthus_error_calculator_core.cpp
src/ichthus_error_calculator_node.cpp
)

target_link_libraries(ichthus_error_calculator
  ${catkin_LIBRARIES}
)

install(TARGETS ichthus_error_calculator
  ARCHIVE DESTINATION ${CATKIN_PACKAGE_LIB_DESTINATION}
  LIBRARY DESTINATION ${CATKIN_PACKAGE_LIB_DESTINATION}
  RUNTIME DESTINATION ${CATKIN_PACKAGE_BIN_DESTINATION}
)
  