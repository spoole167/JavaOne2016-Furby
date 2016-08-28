package com.ibm.javaone2016.demo.furby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSensor implements Sensor{

	protected static final Logger LOG = LoggerFactory.getLogger(Sensor.class);
	private boolean keepAlive = true;
	
	private Controller controller;
	

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




public void close() {
	keepAlive=false;
	
	
}
}
