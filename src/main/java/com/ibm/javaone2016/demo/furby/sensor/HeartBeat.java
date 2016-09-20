package com.ibm.javaone2016.demo.furby.sensor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractPassiveSensor;

public class HeartBeat  extends AbstractPassiveSensor{

	private static JsonArray addresses=new JsonArray();
	
	static {
		try {
		captureIPAddresses();
		} catch(IOException e) {
			
		}
	}

	public HeartBeat() {
		super();
		
	}

	private static void captureIPAddresses() throws IOException {
		
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface ni =  e.nextElement();
		    Enumeration<InetAddress> addrs = ni.getInetAddresses();
		    while (addrs.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) addrs.nextElement();
		        String host_address=i.getHostAddress();
		        if(host_address.contains(":")) continue; // ignore ipv6 addrs
		        if(host_address.startsWith("127.")) continue; // ignore local addr
		        addresses.add(i.getHostAddress());
		    }
		}
		
	}

	@Override
	public void start() {
		
		
				JsonObject event = new JsonObject();
				event.add("addresses",addresses);
				
				while (isAlive()) {

					publishEvent("hb","status", event);
					
					pause(10);
					
				}
	}

	
	

}
