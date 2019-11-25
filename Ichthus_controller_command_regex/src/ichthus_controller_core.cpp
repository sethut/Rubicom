#include "ichthus_controller/ichthus_controller.h"

namespace ichthus_controller{
    IchthusController::IchthusController(){
        arg1="\0";
        arg2="\0";
        current_count=0;
        num_command=0;
        index=0;
    }
    IchthusController::~IchthusController(){}

void IchthusController::mark_history(string arg1, string arg2){
    int mark_index;
    string temp=input.substr(input.find("!")+1,input.length()-1);
    mark_index=atoi(temp.c_str());
    if(mark_index<=current_count)
	    input=history[mark_index-1];
    else
	    cout<<"history index error"<<endl;
}

void IchthusController::show_history(string arg1, string arg2){
    for(int i=0; i<current_count;i++)
	    cout<<to_string(i+1)+"."<<history[i]<<endl;
    cout<<endl;
}

void IchthusController::init_param(){
    if(!private_nh.getParam("MAX_COMMAND_COUNT",MAX_COMMAND_COUNT)){
        ROS_ERROR("rosparam max_command_count error");
        exit(0);
    }
    while(1){
        command="command"+to_string(num_command);
        if(!private_nh.getParam("/"+command,command_regex))
            break;
        num_command++;
        command_list.push_back(command_regex);  
    }
}

void IchthusController::extract_arg(){
    arg1="\0";
    arg2="\0";
    arg1=match[1].str();
    arg2=match[2].str();
}

void IchthusController::func2(string arg1,string arg2){
    cout<<"func2 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;    
}
void IchthusController::func3(string arg1,string arg2){
    cout<<"func3 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func4(string arg1,string arg2){
    cout<<"func4 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func5(string arg1,string arg2){
    cout<<"func5 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func6(string arg1,string arg2){
    cout<<"func6 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func7(string arg1,string arg2){
    cout<<"func7 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func8(string arg1,string arg2){
    cout<<"func8 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func9(string arg1,string arg2){
    cout<<"func9 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func10(string arg1,string arg2){
    cout<<"func10 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func11(string arg1,string arg2){
    cout<<"func11 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func12(string arg1,string arg2){
    cout<<"func12 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func13(string arg1,string arg2){
    cout<<"func13 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func14(string arg1,string arg2){
    cout<<"func14 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func15(string arg1,string arg2){
    cout<<"func15 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::func16(string arg1,string arg2){
    cout<<"func16 Call"<<endl;
    cout<<"arg1: "<<arg1<<" "<<"arg2: "<<arg2<<endl<<endl;
}
void IchthusController::init_function_list(FP* fp_){
    fp_[0]=&IchthusController::show_history;
    fp_[1]=&IchthusController::mark_history;
    fp_[2]=&IchthusController::func2;//homing
    fp_[3]=&IchthusController::func3;//ecatoff
    fp_[4]=&IchthusController::func4;//estop
    fp_[5]=&IchthusController::func5;//get
    fp_[6]=&IchthusController::func6;//set ecat
    fp_[7]=&IchthusController::func7;//set obd
    fp_[8]=&IchthusController::func8;//set can
    fp_[9]=&IchthusController::func9;//set hvi
    fp_[10]=&IchthusController::func10;//set param
    fp_[11]=&IchthusController::func11;//set motion
    fp_[12]=&IchthusController::func12;//set controller
    fp_[13]=&IchthusController::func13;//set
    fp_[14]=&IchthusController::func14;//help
    fp_[15]=&IchthusController::func15;//quit
    fp_[16]=&IchthusController::func16;//show
}
void IchthusController::matching_command(){
    regex *regex_list[num_command];
    for(int i=0; i<num_command;i++)
        regex_list[i] = new regex(command_list[i]);
    FP fp[num_command];
    init_function_list(fp);
    
    while(ok()){
        getline(cin,input);
        if((regex_match(input,match,*regex_list[MARK_HISTORY_INDEX]))==1)
            mark_history(arg1,arg2);
        for(index=0; index<num_command; index++){
            if((regex_match(input,match,*regex_list[index]))==1){
                history.push_back(input);
                current_count++;
                extract_arg();
                (this->*fp[index])(arg1,arg2);
	            break;
            }
        }
        if(index==num_command)
            cout<<"unknown command"<<endl<<endl;
        if(input=="c")
            exit(0);
    }
}

void IchthusController::doloop(){
    init_param();
    matching_command();
}

}
