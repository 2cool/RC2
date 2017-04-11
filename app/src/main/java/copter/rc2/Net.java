package copter.rc2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;


import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Net {
	//private static final int MAX_UDP_DATAGRAM_LEN = 1500;

    public static boolean net_runing=true;
	private static  int UDP_SERVER_PORT = 9876;
	
public Net(int port,int timeOut){
	UDP_SERVER_PORT=port; 
}
	public void stop(){

		//udp_server_run=false;
		//client_runing=false;
	}
	
	static InetAddress copterAddress,myIP=null;
	int port=0;	
	
	static int threads=0;

	static Context context;
	private void connect2esp(){
		String networkSSID = "ESP_9BB84B";
		//String networkPass = "pass";

		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + networkSSID + "\"";
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

		if (wifiManager.isWifiEnabled()==false)
			wifiManager.setWifiEnabled(true);
		while (wifiManager.isWifiEnabled()==false) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e2) {
			}
		}

		WifiInfo wi=wifiManager.getConnectionInfo();
		String sss=wi.getSSID().replaceAll("\"","");
		if (!sss.equals(networkSSID)) {
			wifiManager.addNetwork(conf);
			List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
			for (WifiConfiguration i : list) {
				if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
					wifiManager.disconnect();
					wifiManager.enableNetwork(i.networkId, true);
					wifiManager.reconnect();

					break;
				}
			}
		}

	}
	public void start(){
		if (threads>0 || net_runing==false)
			return;
		threads++;
		Thread thread = new Thread() {
            @Override
            public void run() {
				Disk.close();
				Disk.createOrOpen();
				while (net_runing) {

					//connect2esp();

					runTCPClient(Disk.getIP());
					try {
						Thread.sleep(100);
					} catch (InterruptedException e2) {
					}
				}
            }
        };
        thread.start();	
	}
	

	byte buffer[]=new byte[1024]; 

	final int IP=0;
	final int PORT=1;
	public boolean runTCPClient(String ip_port){
		if (ip_port==null)
			return false;
		String s[]=ip_port.split(":");
		Socket socket=null;
		OutputStream out=null;
		InputStream in=null;	
		boolean ret=true;
		try{
		in=null;
		out=null;
		 try {

			// copterAddress=InetAddress.getByName("192.168.4.1");
			// copterAddress=InetAddress.getByName("192.168.1.155");
			 //copterAddress=InetAddress.getByName("192.168.137.38");
			 copterAddress=InetAddress.getByName(s[IP]);
			 UDP_SERVER_PORT=Integer.parseInt(s[PORT]);
			 socket = new Socket(copterAddress, UDP_SERVER_PORT);
			 socket.setSoTimeout(6000);
			 out=socket.getOutputStream();
			 in =socket.getInputStream();


			int timeOutErrors=0;
			Log.i("UDP","TCP_CLIENT STARTING...");


            String str="";
			 Commander.new_connection();
			while(net_runing){

				int len =Commander.get(buffer);
				//Log.i("UDP",Integer.toString((cnt)));
				out.write(buffer, 0, len);
				out.flush();
				
				//Log.i("UDP","READING... "+Integer.toString(cnt));

				try{
					len=in.read(buffer);
				//	Log.i("UDP","<-"+buffer);
					Commander.link=true;
					Telemetry.bufferReader_(buffer,len);


				}catch(java.net.SocketTimeoutException e){
                   // socket.setSoTimeout(5000);
					Log.i("UDP","SocketTimeoutException");
					//if (++timeOutErrors>=1){
						//Log.i("UDP","SocketTimeoutException & reboot");
						if (socket.isClosed() == false && socket.isConnected()) {
							socket.shutdownInput();
							socket.shutdownOutput();
							socket.close();
							ret=false;
							Commander.link=false;
						}
						socket=null;
						break;                 
					//}
		        }
				
			}
                
            } catch (Exception e) {
            	Log.i("UDP", "EXCEPTION "+e.getMessage());
            	if (e.getMessage().indexOf("ECONNREFUSED")!=-1)
            		ret= true;
            	else
            		ret= false;
            	
           } 
		 
		 finally {
			 Commander.link=false;
			 if (MainActivity.drawView!=null)
					MainActivity.drawView.postInvalidate();
			 Log.i("UDP","TCP_CLIENT KILLED!"); 

			if (socket != null) {
				Log.i("UDP", "close");
				try {
					if (socket.isClosed() == false && socket.isConnected()){
						socket.shutdownInput();
						socket.shutdownOutput();
						socket.close();
					}
					socket=null;
				} catch (IOException e) {
					ret=false;
					Log.i("UDP", e.getMessage());
				}
			}
		}
		}catch (Exception e){
			Log.i("UDP", "TCP CLIENT EXCEPTION "+e.getMessage());
		}
		return ret;
	}
}