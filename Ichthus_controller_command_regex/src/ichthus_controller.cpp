#include <ros/ros.h>
#include <iostream>
#include <string>
#include <regex>
#include <vector>

using namespace std;
using namespace ros;

int main(int argc, char* argv[]){
  init(argc,argv,"ichthus_controller");
  NodeHandle private_nh("~");
  int MAX_COMMAND_COUNT;
  int num_command=0;
  if(!private_nh.getParam("/MAX_COMMAND_COUNT",MAX_COMMAND_COUNT))
    ROS_ERROR("rosparam max_command_count error");
  string callback_name;
  string command_regex;
  vector<pair<string,string>> command_callback;
  vector<string> history;
  string input;
  smatch sh;
    
  while(1){
    string command="command"+to_string(num_command);
    string callback="callback"+to_string(num_command);
    if(!private_nh.getParam("/"+command,command_regex)){
        break;
    }
    private_nh.getParam("/"+callback,callback_name);
    num_command++;
    command_callback.push_back({command_regex,callback_name});
  }   

  regex regex_history_mark("^![0-9]{1,3}$");
  regex regex_history("^history");
  regex *regex_list[num_command];
  for(int i=0; i<num_command;i++)
    regex_list[i] = new regex(command_callback[i].first);

  int current_count=0;
  char a[100];
  char *b=nullptr;
  int index;
  int history_index;
    
  while(ok()){
    getline(cin,input);
    if(regex_match(input,sh,regex_history_mark)==1){
      strcpy(a,input.c_str());
      b=strtok(a,"!");
      history_index=atoi(b);
      if(history_index<=current_count)
	      input=history[history_index-1];
      else
	      cout<<"history index error"<<endl<<endl;
    }
    else if(regex_match(input,sh,regex_history)==1){
      for(int i=0; i<current_count;i++)
	      cout<<to_string(i+1)+"."<<history[i]<<endl;
    }
    else{
      history.push_back(input);
      current_count+=1; 
    }
    for(index=0; index<num_command;index++){
      if((regex_match(input,sh,*regex_list[index]))==1){
	      cout<<"callback function : "<<command_callback[index].second<<endl<<endl;
	      break;
      }
    }
    if(index==num_command)
      cout<<"unknown command"<<endl;
  }
  return 0;
}
