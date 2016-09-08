package com.ibm.javaone2016.demo.furby;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.device.DeviceClient;
import com.ibm.javaone2016.demo.furby.sensor.HeartBeat;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;

public class Main {

	private static final String HEATBEAT_CLASS_NAME = HeartBeat.class.getCanonicalName();

	public static void main(String[] args) throws Exception {

		
		
		// find config file
		File configFile =new File(System.getProperty("user.home") + "/.furby.json");
		
		if (configFile.exists() == false) {
			configFile = new File("/boot/furby.json");
		}
		
		// create json version
		JsonObject config = new JsonParser().parse(new FileReader(configFile)).getAsJsonObject();

	
		
		// set missing values...
		JsonObject meta;
		
		JsonElement configElement=config.get("config");
		if(configElement!=null) {
			meta=configElement.getAsJsonObject();
		
		}
		else {
			meta=new JsonObject();
		
		}
		
		// add defaults.
	
		if(!meta.has("webport")) meta.addProperty("webport",8080);
		if(!meta.has("root")) meta.addProperty("root","/tmp/furby");
		
		
		// configure system
		
		File root=new File(meta.get("root").getAsString());
		root.mkdirs();
		
		configWebServer(meta);
		
		
		// load IOT device

		DeviceClient client = getIOTClient(config);
		
		// create controller

		Controller controller = new Controller(meta,client);

		
		// add sensors
		JsonElement sensorElement=config.get("sensors");
		JsonObject sensorList;
		
		if(sensorElement!=null) {
			sensorList = sensorElement.getAsJsonObject();
		}
		else {
			sensorList=new JsonObject();
		}
		// add a default if nothing registered

		if (sensorList == null || sensorList.entrySet().isEmpty()) {
			sensorList = new JsonObject();
			sensorList.add(HEATBEAT_CLASS_NAME, new JsonObject());

		}

		// register  sensors
		
		for (Map.Entry<String, JsonElement> m : sensorList.entrySet()) {

			String sensorClass=m.getKey().trim();
			Sensor service = loadService(sensorClass);
			if (service != null) {
				System.out.println("registering "+sensorClass);
				controller.addService(service);
				service.configure(m.getValue().getAsJsonObject());
			}

		}

		
		
		// start controller..
		controller.start();
		
	}

	private static void configWebServer(JsonObject meta) {
		
		File root=new File(meta.get("root").getAsString());
		staticFiles.externalLocation(root.getAbsolutePath());
		
		port(meta.get("webport").getAsInt());
		
		get("/", (req, res) -> "Hello World");
		
	}

	private static Sensor loadService(String key) {

		try {
			Class<Sensor> c = (Class<Sensor>) Class.forName(key);
			Sensor service = c.newInstance();
 
			return service;

		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;

	}

	private static DeviceClient getIOTClient(JsonObject config) throws Exception {

		JsonObject devconfig = config.getAsJsonObject("iot");

		Properties options = new Properties();

		for (Map.Entry<String, JsonElement> entry : devconfig.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().getAsString();
			options.put(key, value);
		}

		if (options.containsKey("id") == false) {
			String serial = System.getenv("SERIAL");
			if (serial == null || serial.trim().equals("")) {
				serial = "simulator";
			}
			options.put("id", serial.trim());
		}
		
		DeviceClient c = new DeviceClient(options);
		c.setKeepAliveInterval(120);
		
		
		return c;
	}

	public static SpeechToText getSpeechService(JsonObject config) {

		config = config.getAsJsonObject("speech2text");

		String url = config.get("url").getAsString();
		String p = config.get("password").getAsString();
		String u = config.get("username").getAsString();

		SpeechToText service = new SpeechToText();

		service.setUsernameAndPassword(u, p);
		service.setEndPoint(url);

		return service;

	}

	public static TextToSpeech getTextService(JsonObject config) {
		config = config.getAsJsonObject("text2speech");

		String url = config.get("url").getAsString();
		String p = config.get("password").getAsString();
		String u = config.get("username").getAsString();

		TextToSpeech service = new TextToSpeech();
		service.setUsernameAndPassword(u, p);
		service.setEndPoint(url);

		return service;

	}

}
