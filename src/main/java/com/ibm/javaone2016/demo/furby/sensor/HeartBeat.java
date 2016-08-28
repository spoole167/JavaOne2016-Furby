package com.ibm.javaone2016.demo.furby.sensor;

import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractPassiveSensor;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;
import com.ibm.javaone2016.demo.furby.PassiveSensor;

public class HeartBeat  extends AbstractPassiveSensor{

	

	@Override
	public void start() {
		
		
				JsonObject event = new JsonObject();
				event.addProperty("name", "foo");
				event.addProperty("cpu", 90);
				event.addProperty("mem", 70);

				while (isAlive()) {

					publishEvent("hb","status", event);
					
					pause(10);
					
				}
	}

	
	

}
