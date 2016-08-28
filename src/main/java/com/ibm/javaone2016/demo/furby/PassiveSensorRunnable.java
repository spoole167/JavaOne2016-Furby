package com.ibm.javaone2016.demo.furby;

public class PassiveSensorRunnable implements Runnable {

	private PassiveSensor sensor;
	public PassiveSensorRunnable(PassiveSensor sensor) {
		this.sensor=sensor;
	}

	@Override
	public void run() {
		
		sensor.start();
		
		while(true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		sensor.close();
	}

}
