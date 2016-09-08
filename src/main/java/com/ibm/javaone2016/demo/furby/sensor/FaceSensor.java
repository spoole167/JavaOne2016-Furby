package com.ibm.javaone2016.demo.furby.sensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Consumer;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;

public class FaceSensor extends AbstractActiveSensor {

	Store store;
	VideoCapture camera;
	CascadeClassifier cc;
	boolean found = false;

	@Override
	public String[] getCommands() {

		return new String[] { "snap" };
	}

	@Override
	public void handleCommand(String command, JsonObject object) {

		if (command.equals("snap")) {

			publishEvent("face", "face", newURL("face.jpg"));

		}

	}

	@Override
	public void configure(JsonObject serviceConfig) {
		JsonArray libs = serviceConfig.get("libs").getAsJsonArray();
		libs.forEach(new Consumer<JsonElement>() {

			@Override
			public void accept(JsonElement t) {
				LOG.info("loading " + t.getAsString());
				System.load(t.getAsString());
			}
		});

		int device = 0;

		JsonElement deviceElement = serviceConfig.get("device");
		if (deviceElement != null) {
			device = deviceElement.getAsInt();
		}

		camera = new VideoCapture(device);

		LOG.info("camera is " + camera);
		cc = new CascadeClassifier("/usr/local/share/OpenCV/haarcascades/haarcascade_frontalface_alt.xml");
	
		// create store to hold images
		
		store=new Store(serviceConfig.getAsJsonObject("database"));
		
	}

	@Override
	public void start() {

		MatOfRect faces = new MatOfRect();

		while (isAlive()) {

			try {

				Mat frame = new Mat();
				camera.read(frame);
				cc.detectMultiScale(frame, faces);

				if (!faces.empty()) {

					if (!found) {

						Rect largest=null;
						long size=0;
						for(Rect r:faces.toArray()) {
							long face_size=r.height*r.width;
							if(face_size>size)  largest=r;
						}
						
						Mat face = new Mat(frame,largest);
						File faceFile = newFile("face.jpg");
						Imgcodecs.imwrite(faceFile.getAbsolutePath(), face);
						try {
						store.saveFace(faceFile);
						} catch(IOException ioe) {
							ioe.printStackTrace();
						}
						publishEvent("face", "present", true);
						found = true;
					}

				} else {
					if (found) {
						publishEvent("face", "present", false);
						found = false;
					}
				}
				Thread.sleep(500);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public  class Store {
		
		CloudantClient client;
		Database db = null;
		String sensorclientID;
		
		public Store(JsonObject config) {
			
			
			sensorclientID=FaceSensor.this.getMetadata() .get("client").getAsString();
			
			String username=config.get("username").getAsString();
			String password=config.get("password").getAsString();
			
			client = ClientBuilder.account(username)
					.username(username)
					.password(password)
					.build();
			
			db = client.database(sensorclientID, true);
		    
		}
		
		public void saveFace(File face) throws IOException {
		
			String id=String.valueOf(System.currentTimeMillis());
			JsonObject facedoc=new JsonObject();
			facedoc.add("geo",getGeoData());
			facedoc.addProperty("_id",id);
			facedoc.addProperty("created", new Date().toString());
			
			Response r=db.save(facedoc);

			HashMap obj = db.find(HashMap.class, id);
			Response img=db.saveAttachment(new FileInputStream(face),"face.jpg","image/jpg",obj.get("_id").toString(),obj.get("_rev").toString());
			
		}

		private JsonObject getGeoData() {
			try {
			URL geo=new URL("http://freegeoip.net/json");
			InputStream in=geo.openStream();
			InputStreamReader isr=new InputStreamReader(in);
			JsonParser p=new JsonParser();
			JsonElement data=p.parse(isr);
			return data.getAsJsonObject();
			}catch(Exception e) {
				e.printStackTrace();
			}
			return new JsonObject();
		}
	}
}
