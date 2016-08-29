package com.ibm.javaone2016.demo.furby.sensor;

import java.io.File;
import java.util.function.Consumer;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;

public class FaceSensor extends AbstractActiveSensor {

	VideoCapture camera;
	CascadeClassifier cc;
	boolean found=false;
	
	
	@Override
	public String[] getCommands() {
		
		return new String[]{"snap"};
	}

	@Override
	public void handleCommand(String command, JsonObject object) {
		
		if(command.equals("snap")) {
			
			Mat frame = new Mat();
			camera.read(frame);
			 MatOfRect faces= new MatOfRect();
			cc.detectMultiScale(frame, faces);
			if(!faces.empty()) {
				Rect[] facesArray=faces.toArray();
				
				for (int i = 0; i < facesArray.length; i++)
				    Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
			
			}
			
			File face=newFile("face.jpg");
			Imgcodecs.imwrite(face.getAbsolutePath(), frame);
			
			
			publishEvent("face", "face",newURL("face.jpg"));
						
		}

	}

	@Override
	public void configure(JsonObject serviceConfig) {
		JsonArray libs=serviceConfig.get("libs").getAsJsonArray();
		libs.forEach(new Consumer<JsonElement>() {

			@Override
			public void accept(JsonElement t) {
				LOG.info("loading "+t.getAsString());
				System.load(t.getAsString());
			}
		});
	
		 camera = new VideoCapture(0);
		 
		 cc=new CascadeClassifier("/usr/local/share/OpenCV/haarcascades/haarcascade_frontalface_alt.xml");	
	}

	@Override
	public void start() {
		
		 MatOfRect faces= new MatOfRect();
		
		while(isAlive()) {
		
			try {
				
				Mat frame = new Mat();
				camera.read(frame);
				cc.detectMultiScale(frame, faces);
				if(!faces.empty()) {
					 if(!found) {
						 publishEvent("face", "count",faces.toArray().length);
						 found=true;
					 }
					
					
				} else {
					 if(found) {
						 publishEvent("face", "count",0);
						 found=false;
					 }
				}
				Thread.sleep(500);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}

}
