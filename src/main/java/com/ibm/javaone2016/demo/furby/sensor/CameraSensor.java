package com.ibm.javaone2016.demo.furby.sensor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;

public class CameraSensor extends AbstractActiveSensor {

	private static final String SENSOR_NAME = "snapshot";
	private Webcam webcam;
	private WebcamMotionDetector detector;
	
	@Override
	public void configure(JsonObject serviceConfig) {
		
		webcam = Webcam.getDefault();
		detector = new WebcamMotionDetector(webcam);
		detector.setInterval(500); 
		detector.addMotionListener(new WebcamMotionListener() {
			
			@Override
			public void motionDetected(WebcamMotionEvent wme) {
				double area=wme.getArea();
				publishEvent(SENSOR_NAME,"movement", area);
				
			}
		});
		
	}


	public void start() {
		
		detector.start();
		webcam.open();
		
		while (isAlive()) {

			
			

			pause(60);
			
		}
		
		detector.stop();
		webcam.close();

	}
	
	@Override
	public String[] getCommands() {
		return new String[]{"snap"};
	}
	
	@Override 
	public void handleCommand(String command,JsonObject object ) {
		
	
	ByteArrayOutputStream baos=new ByteArrayOutputStream();
	
	try {
		ImageIO.write(webcam.getImage(), "PNG", baos);
	
		publishEvent(SENSOR_NAME,"camera", baos.toByteArray());
		baos.close();
	
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}

	
}
