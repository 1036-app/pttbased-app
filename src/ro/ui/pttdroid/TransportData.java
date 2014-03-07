package ro.ui.pttdroid;

import java.io.Serializable;


public class TransportData implements Serializable
{ 

	private static final long serialVersionUID = 1L;
//该类所定义的数据需要在网络上传送，所以必须实现序列化

	public String time;  //精确到毫秒
	public String data;
	public String ipaddress;
	TransportData()
	{
		time=null;
		data=null;
		ipaddress=null;
	}
	public void setIP(String ip)
	{
		ipaddress=ip;	
	}
}
