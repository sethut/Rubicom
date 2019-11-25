#ifndef ICHTHUS_CONTROLLER_H
#define ICHTHUS_CONTROLLER_H
#include <ros/ros.h>
#include <iostream>
#include <string>
#include <regex>
#include <vector>

using namespace std;
using namespace ros;

#define HISTORY_INDEX 0
#define MARK_HISTORY_INDEX 1

namespace ichthus_controller
{
    class IchthusController
    {
        typedef void (IchthusController::*FP)(string,string);
        private:
            NodeHandle private_nh;
            vector<string> command_list;
            vector<string> history;
            string command;
            string command_regex;
            string input;
            string arg1;
            string arg2;
            smatch match;
            int MAX_COMMAND_COUNT;
            int num_command;
            int current_count;
            int index;
        public:
            IchthusController();
            ~IchthusController();
            void init_param();
            void matching_command();
            void doloop();
            void extract_arg();
            void init_function_list(FP* fp_);
            void mark_history(string arg1,string arg2);
            void show_history(string arg1,string arg2);
            void func2(string arg1,string arg2);
            void func3(string arg1,string arg2);
            void func4(string arg1,string arg2);
            void func5(string arg1,string arg2);
            void func6(string arg1,string arg2);
            void func7(string arg1,string arg2);
            void func8(string arg1,string arg2);
            void func9(string arg1,string arg2);
            void func10(string arg1,string arg2);
            void func11(string arg1,string arg2);
            void func12(string arg1,string arg2);
            void func13(string arg1,string arg2);
            void func14(string arg1,string arg2);
            void func15(string arg1,string arg2);
            void func16(string arg1,string arg2);
    };
}

#endif //ICHTHUS_CONTROLLER_H

