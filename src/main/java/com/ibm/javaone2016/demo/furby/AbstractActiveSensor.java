package com.ibm.javaone2016.demo.furby;

public abstract class AbstractActiveSensor extends AbstractSensor implements ActiveSensor {

	@Override
	public void run() {
		start();
		close();
		
	}

}
