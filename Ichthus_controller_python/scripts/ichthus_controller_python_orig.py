#!/usr/bin/env python
import rospy
import threading
import time
import sys
import ichthus_2

class ichthus_controller_python_core():

	def __init__(self):
		rospy.init_node('ichthus_controller_python_core')
		wxPython=ichthus_2.MyApp()
		wxPython.MainLoop()
		self.MAX_COMMAND_COUNT=0
		self.num_command=0
		self.regex_list=[]
		self.command_regex=""
		if rospy.search_param("MAX_COMMAND_COUNT") is None:
			rospy.logerr("check yaml file")
			exit()
		self.regex_init()
	def regex_init(self):
		while True:
			str1="command"+str(self.num_command)
			if rospy.has_param(str1) == False:
    				break
			self.command_regex=rospy.get_param(str1)
			self.regex_list.append(self.command_regex)
			self.num_command+=1
		for i in range(0,len(self.regex_list)):
			print(self.regex_list[i])

	def command_parsing(self):
		while True:
			str2=""
			str2=wxPython.getIchthus_command()
			for index in range(0,len(self.regex_list)):
				match=re.match(self.regex_list[index],str2)
				if match :
					arg1="\0"
					arg2="\0"
					if len(match.groups())==1:
						arg1=match.group(1)
					elif len(match.groups())==2:
						arg1=match.group(1)
						arg2=match.group(2)
					print("regex : %s"%self.regex_list[index]) 
					print("arg1 : %s"%arg1)
					print("arg2 : %s\n"%arg2)
					break	
			
    				
if __name__=="__main__":
	ichthus_controller=ichthus_controller_python_core()
	ichthus_controller.command_parsing()
