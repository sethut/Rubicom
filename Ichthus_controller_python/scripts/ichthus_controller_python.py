#!/usr/bin/env python
import rospy
import wx
import sys
import re
from time import sleep
from threading import Thread
class MyFrame(wx.Frame):
	def __init__(self, *args, **kwds):
        # begin wxGlade: MyFrame.__init__
		wx.Frame.__init__(self, *args, **kwds)
        	kwds["style"]=kwds.get("style", 0) | wx.DEFAULT_FRAME_STYLE
        	self.SetSize((950, 763))
	        self.SetPosition((500,160))
        	self.panel_1 = wx.Panel(self, wx.ID_ANY)
        	self.panel_2 = wx.Panel(self.panel_1, wx.ID_ANY)
        	self.panel_3 = wx.Panel(self.panel_2, wx.ID_ANY)
        	self.panel_4 = wx.Panel(self.panel_2, wx.ID_ANY)
        	self.panel_5 = wx.Panel(self.panel_1, wx.ID_ANY)
        	self.gauge_1 = wx.Gauge(self.panel_5,wx.ID_ANY,range=100)
        	self.gauge_2 = wx.Gauge(self.panel_5,wx.ID_ANY,range=100,style=wx.GA_HORIZONTAL | wx.GA_SMOOTH)
        	self.list_ctrl_1 = wx.ListCtrl(self.panel_3, wx.ID_ANY, style=wx.LC_REPORT)
        	self.text_ctrl_1 = wx.TextCtrl(self.panel_3, wx.ID_ANY, style=wx.TE_PROCESS_ENTER)
        	self.text_ctrl_2 = wx.TextCtrl(self.panel_4,wx.ID_ANY, style=wx.TE_MULTILINE|wx.TE_READONLY)
	        self.list_ctrl_1.InsertColumn(0, 'Index')
        	self.list_ctrl_1.InsertColumn(1, 'Command')
        	self.list_ctrl_1.SetColumnWidth(0, 100)
        	self.list_ctrl_1.SetColumnWidth(1, 400)
        	if sys.version_info[0] >=3:
        	    self.Bind(wx.EVT_TEXT_ENTER,self.OnEnter,self.text_ctrl_1)
        	else:
        	    self.Bind(wx.EVT_TEXT_ENTER,self.OnEnter_under_3,self.text_ctrl_1)
        	self.Bind(wx.EVT_LIST_ITEM_SELECTED,self.OnItemSelected,self.list_ctrl_1)
        	self.font3 = wx.Font(11, wx.FONTFAMILY_SWISS, wx.FONTSTYLE_NORMAL,wx.FONTWEIGHT_BOLD)
        	self.dlg=wx.ProgressDialog("VCS Connect","connecting with vcsd",100,style=wx.PD_APP_MODAL|wx.PD_AUTO_HIDE)
        	self.target_velocity_text= wx.StaticText(self.panel_5,wx.ID_ANY)
        	self.current_velocity_text= wx.StaticText(self.panel_5,wx.ID_ANY)
        	self.t_vel=0
        	self.c_vel=0
        	self.speed=0
        	self.regex_list=[]
		self.history=[]
        	self.monthread=Thread(target=self.mon_func)
        	self.vcs_connect()
        	self.__set_properties()
        	self.__do_layout()

	def __set_properties(self):
        	self.SetTitle("Ichthus Controller")
	        self.lookahead_distance = 30
        	self.lidar_hz="10hz"
	        self.text_ctrl_2.SetFont(self.font3)
        	self.text_ctrl_2.AppendText(" lookahead distance : ")
	        self.text_ctrl_2.AppendText(str(self.lookahead_distance)+'\n')
        	self.text_ctrl_2.AppendText(" lidar hz : ")
	        self.text_ctrl_2.AppendText(self.lidar_hz)
        # end wxGlade
	def __do_layout(self):
        # begin wxGlade: MyFrame.__do_layout
	        sizer_5 = wx.BoxSizer(wx.VERTICAL)
	        sizer_6 = wx.BoxSizer(wx.VERTICAL)
	        sizer_7 = wx.BoxSizer(wx.HORIZONTAL)
	        sizer_8 = wx.BoxSizer(wx.VERTICAL)
	        sizer_9 = wx.BoxSizer(wx.VERTICAL)
	        sizer_10 = wx.BoxSizer(wx.VERTICAL)
	        sizer_11 = wx.BoxSizer(wx.VERTICAL)
	
	        label_1 = wx.StaticText(self.panel_5, wx.ID_ANY, "target_velocity")
	        label_2 = wx.StaticText(self.panel_5, wx.ID_ANY, "current_velocity")
        
	        sizer_11.Add(label_1, 0, wx.ALIGN_CENTER | wx.ALL, 1)
	        sizer_11.Add(self.target_velocity_text, 0, wx.ALIGN_CENTER)
	        sizer_11.Add(self.gauge_1, 0, wx.ALL | wx.EXPAND, 15)
	        sizer_11.Add(self.gauge_2, 0, wx.ALL | wx.EXPAND, 15)
	        sizer_11.Add(label_2, 0, wx.ALIGN_CENTER | wx.ALL, 1)
	        sizer_11.Add(self.current_velocity_text,0,wx.ALIGN_CENTER)

	        sizer_10.Add(sizer_11, 1, wx.EXPAND, 0)
	        self.panel_5.SetSizer(sizer_10)
	        sizer_6.Add(self.panel_5, 0, wx.ALL | wx.EXPAND, 60)
        	sizer_8.Add(self.list_ctrl_1, 1, wx.BOTTOM | wx.EXPAND, 10)
        	sizer_8.Add(self.text_ctrl_1, 0, wx.ALL | wx.EXPAND, 13)
        	self.text_ctrl_1.SetFocus()

        	self.panel_3.SetSizer(sizer_8)
        	sizer_7.Add(self.panel_3, 1, wx.EXPAND, 0)
        	sizer_9.Add(self.text_ctrl_2, 1, wx.ALL| wx.EXPAND, 0)
        	self.panel_4.SetSizer(sizer_9)
        	sizer_7.Add(self.panel_4, 1, wx.ALL|wx.EXPAND, 0)
        	self.panel_2.SetSizer(sizer_7)
        	sizer_6.Add(self.panel_2, 2, wx.ALIGN_CENTER | wx.ALL | wx.EXPAND, 0)
        	self.panel_1.SetSizer(sizer_6)
        	sizer_5.Add(self.panel_1, 1, wx.ALL | wx.EXPAND, 0)
        	self.SetSizer(sizer_5)
        	self.Layout()
	# end wxGlade
	def init_regex_list(self):
    		num_command=0
	    	if rospy.search_param("MAX_COMMAND_COUNT") is None:
			rospy.logerr("check yaml file")
			exit()
	    	while True:
			str1="command"+str(num_command)
			if rospy.has_param(str1)==False:
				break
			command_regex=rospy.get_param(str1)
			self.regex_list.append(command_regex)
			num_command+=1

		for i in range(0,len(self.regex_list)):
			print(self.regex_list[i])

	def command_parsing(self):
		arg1="\0"
		arg2="\0"
		if re.match(self.regex_list[0],self.command):
			self.command=self.command[1:]
			self.command=self.history[int(self.command)]
		for index in range(1,len(self.regex_list)):
			match=re.match(self.regex_list[index],self.command)
			if match:
				self.history.append(self.command)
				if len(match.groups())==1:
					arg1=match.group(1)
				elif len(match.groups())==2:
					arg1=match.group(1)
					arg2=match.group(2)
				print("regex : %s"%self.regex_list[index]) 
				print("arg1 : %s"%arg1)
				print("arg2 : %s\n"%arg2)
				return True
		print("unknown command")
		return False						
	def mon_func(self):
		while True:
	            wx.MilliSleep(100)
	            self.t_vel=self.t_vel%100
	            self.t_vel+=1
	            wx.CallAfter(self.update_mon_GUI)    
	def update_mon_GUI(self):
        	self.gauge_1.SetValue(self.t_vel)
	        self.gauge_2.SetValue(self.t_vel)
        	self.current_velocity_text.SetLabel(str(self.t_vel))
	        self.target_velocity_text.SetLabel(str(self.t_vel))

	def vcs_connect(self):
        	message = "connecting"
	        keepGoing = True
        	count = 0
	        while count < 100:
            		if(count%20==0):
	                	message+="."
	            	count = count + 1
        	    	wx.MilliSleep(20)
        	    	keepGoing = self.dlg.Update(count,message)
        	self.dlg.Destroy()

    	def OnEnter(self, event):
        	if not self.text_ctrl_1.GetValue():
            		return
	        num_items = self.list_ctrl_1.GetItemCount()
        	self.command=self.text_ctrl_1.GetValue()
	        self.list_ctrl_1.InsertItem(num_items,str(num_items),num_items)
        	self.list_ctrl_1.SetItem(num_items,1,self.command)
	        self.list_ctrl_1.SetScrollPos(wx.VERTICAL,self.list_ctrl_1.GetScrollRange(wx.VERTICAL))
        	self.text_ctrl_1.Clear()
		self.command_parsing()

	def OnEnter_under_3(self,event):
        	if not self.text_ctrl_1.GetValue():
            		return
        	num_items = self.list_ctrl_1.GetItemCount()
        	self.command=self.text_ctrl_1.GetValue()
		ok=self.command_parsing()
		if ok==True:
        		self.list_ctrl_1.InsertStringItem(num_items, str(num_items))
	        	self.list_ctrl_1.SetStringItem(num_items, 1, self.command)
        		self.list_ctrl_1.SetScrollPos(wx.VERTICAL,self.list_ctrl_1.GetScrollRange(wx.VERTICAL))
	        self.text_ctrl_1.Clear()


	def OnItemSelected(self,event):
        	self.text_ctrl_1.Clear()
	        select=self.list_ctrl_1.GetFocusedItem()
        	select_command=self.list_ctrl_1.GetItemText(select,1)
	        self.text_ctrl_1.AppendText(select_command)
        	self.text_ctrl_1.SetFocus()
    

            
        
            
# end of class MyFrame

class MyApp(wx.App):
    def OnInit(self):
    	rospy.init_node('ichthus_controller_python_core')
        self.frame = MyFrame(None, wx.ID_ANY, "")
        self.SetTopWindow(self.frame)
	self.frame.init_regex_list()
        self.frame.monthread.start()
        self.frame.Show()
        return True

# end of class MyApp

if __name__ == "__main__":
    app = MyApp(0)
    app.MainLoop()
			
			
    				
