package ro.ui.pttdroid;

import java.io.Serializable;


public class TransportData implements Serializable
{ 

	private static final long serialVersionUID = 1L;
//�����������������Ҫ�������ϴ��ͣ����Ա���ʵ�����л�

	public String time;  //��ȷ������
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
