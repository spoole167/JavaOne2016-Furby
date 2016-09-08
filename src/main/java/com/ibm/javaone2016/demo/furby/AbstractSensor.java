package com.ibm.javaone2016.demo.furby;

import java.io.File;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public abstract class AbstractSensor implements Sensor{

	protected static final Logger LOG = LoggerFactory.getLogger(Sensor.class);
	private boolean keepAlive = true;
	
	
	private Controller controller;
	
	protected File newFile(String name) {
		File root=controller.getRoot();
		File mine=new File(root,this.getClass().getName());
		mine.mkdirs();
		return new File(mine,name);
	}
	protected URL newURL(String name) {
		
		File mine=new File(this.getClass().getName());
		return controller.getURL(new File(mine,name));
		
	}
	protected boolean isAlive() {
		return keepAlive;
	}

	protected void pause(int i) {
		try {
			Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
			LOG.info("pause interupted");
		}

	}



	
protected void publishEvent(String service,String name, Object event) {
		
		LOG.info(name);
		controller.publishEvent(service,name,event);
		
	}


public void setController(Controller controller) {
	if(controller==null) throw new IllegalArgumentException("missing controller");
	this.controller=controller;
	
}


public JsonObject getMetadata() {
	if(controller==null) throw new IllegalArgumentException("missing controller");
	return controller.getMetadata();
}

public void close() {
	keepAlive=false;
	
	
}
}
