package com.ibm.javaone2016.demo.furby;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.device.Command;
import com.ibm.iotf.client.device.CommandCallback;
import com.ibm.iotf.client.device.DeviceClient;

public class Controller implements CommandCallback, Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ActiveSensor.class);

	private DeviceClient client;
	private int port;
	private File root;
	private List<Thread> services = new LinkedList<>();
	private Map<String, ActiveSensor> commands = new HashMap<>();

	private BlockingQueue<Command> queue = new LinkedBlockingQueue<Command>();

	public Controller(File root,int port,DeviceClient client) {

		this.client = client;
		this.root=root;
		this.port=port;
	}

	public void addService(Sensor service) {

		service.setController(this);

		if (service instanceof PassiveSensor) {
			services.add(new Thread(new PassiveSensorRunnable((PassiveSensor) service)));
		} else {
			ActiveSensor at = (ActiveSensor) service;
			services.add(new Thread(at));
		}

		LOG.info("added service " + service.getClass().getName());

		if (service instanceof ActiveSensor) {
			ActiveSensor as = (ActiveSensor) service;
			String[] commandNames = as.getCommands();
			if (commandNames != null) {
				for (String command : commandNames) {
					if (command != null) {
						command = command.trim().toLowerCase();
						if (!command.equals("")) {
							commands.put(command, as);
							LOG.info("registered command " + command + " for " + as.getClass().getName());
						}
					}
				}
			}
		}

	}

	public synchronized void publishEvent(String service, String name, Object event) {

		if (!client.isConnected()) {
			waitForConnect();
		}

		String className = event.getClass().getSimpleName();

		if (event instanceof Number) {
			className = className.toLowerCase();
		} else {
			switch (className) {
			case "byte[]":
				event = convertByteArray((byte[]) event);
				className = "bytestream";
				break;
			case "URL":
				className = "url";
				event=event.toString();
				break;
			case "Boolean":
				className = "boolean";
			case "String":
				className = "string";
				break;
			case "JsonObject":
				className = "json";
				break;
			default:
				event = "unknown";

			}
		}

		String payload = service + ":" + name + ":" + className;

		client.publishEvent(payload, event, 1);

	}

	private void waitForConnect() {

		while (!client.isConnected()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

	}

	private String convertByteArray(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	@Override
	public void processCommand(Command cmd) {
		try {
			queue.put(cmd);
		} catch (InterruptedException e) {
		}

	}

	public void start() {

		LOG.info("controller thread starting");
		Thread t = new Thread(this);
		t.setDaemon(false);
		t.start();

	}

	@Override
	public void run() {

		LOG.info("Client = " + client);
		client.setKeepAliveInterval(120);
		client.setCommandCallback(this);

		try {
			client.connect();
			LOG.info("Client = " + client);
		} catch (MqttException e1) {
			LOG.error(e1.getMessage());
			e1.printStackTrace();
			return;
		}

		waitForConnect();

		for (Thread t : services) {
			t.start();
		}

		while (true) {

			Command cmd = null;
			try {
				cmd = queue.take();
				handleCommand(cmd);
			} catch (InterruptedException e) {
			}

		}

	}

	private void handleCommand(Command cmd) {

		String payload = cmd.getPayload();
		JsonObject object = new JsonParser().parse(payload).getAsJsonObject();

		String command = object.get("cmd").getAsString();

		LOG.info("command " + command + " received");
		ActiveSensor target = commands.get(command);
		if (target != null) {
			target.handleCommand(command, object);
		} else {
			LOG.info("no handlers found for " + command);
		}

	}

	public File getRoot() {
		return root;
	}

	public URL getURL(File file) {
		;
		
		try {
			return new URL("http://"+InetAddress.getLocalHost().getHostAddress()+":"+port+"/"+file.getPath());
		} catch (MalformedURLException | UnknownHostException e) {
		
			e.printStackTrace();
		}
		return null;
	}

}
