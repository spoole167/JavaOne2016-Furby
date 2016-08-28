package com.ibm.javaone2016.demo.furby;

import com.google.gson.JsonObject;

public interface ActiveSensor extends Sensor, Runnable {

	
	String[] getCommands();

	void handleCommand(String command,JsonObject object);

}
