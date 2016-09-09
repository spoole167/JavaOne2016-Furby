package com.ibm.javaone2016.demo.furby.sensor;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;

/*
 * Output sensor for converting a command into speech 
 * and outputing via available audio 
 * 
 */
public class TextToSpeechSensor extends AbstractActiveSensor {
	private String userid;
	private String password;
	private FurbyMotionController furby=new FurbyMotionController();
	
	@Override
	public void start() {

		while (isAlive()) {

			publishEvent("talk", "status", "ok");

			pause(10);

		}

	}

	@Override
	public String[] getCommands() {
		return new String[] { "say", "sleep" };
	}

	@Override
	public void handleCommand(String command, JsonObject object) {

		try {
			switch (command) {

			case "say":
				wake(object);
				say(object);
				break;
			case "sleep":
				sleep(object);
				break;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void wake(JsonObject object) {
		furby.wake();
	}

	private void sleep(JsonObject object) {
		furby.sleep();
		
	}

	private void say(JsonObject object) throws IOException, IOException {
		JsonElement e = object.get("text");
		if (e == null)
			return;
		String text = e.getAsString();
		File sound = getAudioTranslation(text);
		CommandLine cmdLine = CommandLine.parse("aplay " + sound.getAbsolutePath());
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
		executor.setWatchdog(watchdog);
		int exitValue = executor.execute(cmdLine);

	}

	private File getAudioTranslation(String text) throws IOException {

		long then = System.currentTimeMillis();
		File result = new File("/tmp/say.wav");

		String command = "/usr/bin/curl -s  -X GET -u \"" + userid + "\":\"" + password + "\" --output "
				+ result.getAbsolutePath()
				+ " \"https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?accept=audio/wav&text="
				+ URLEncoder.encode(text) + "&voice=en-US_AllisonVoice\"";

		CommandLine cmdLine = CommandLine.parse(command);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
		executor.setWatchdog(watchdog);
		int exitValue = executor.execute(cmdLine);
		long now = System.currentTimeMillis();

		LOG.info("got {} bytes in {} millseconds", result.length(), now - then);

		return result;

	}

	@Override
	public void configure(JsonObject serviceConfig) {

		userid = serviceConfig.get("username").getAsString();
		password = serviceConfig.get("password").getAsString();
	}

}
