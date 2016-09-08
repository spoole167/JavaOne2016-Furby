package com.ibm.javaone2016.demo.furby;

import com.google.gson.JsonObject;

public interface Sensor {

	void configure(JsonObject serviceConfig);
	
	void setController(Controller controller);

	void start();
	 
	void close(); 
	
	JsonObject getMetadata();
}
