package com.ibm.javaone2016.demo.furby.sensor;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;

/*
 * Output sensor for converting a command into speech 
 * and outputing via available audio 
 * 
 */
public class TextToSpeechSensor extends AbstractActiveSensor {
	private TextToSpeech service;
	private String userid;
	private String password;

	@Override
	public void start() {

		while (isAlive()) {

			publishEvent("talk", "status", "ok");

			pause(10);

		}

	}

	@Override
	public String[] getCommands() {
		return new String[] { "say" };
	}

	@Override
	public void handleCommand(String command, JsonObject object) {

		if (command.equals("say")) {

			try {
				say(object);
			} catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {

				e.printStackTrace();
			}
		}
	}

	private void say(JsonObject object) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
		JsonElement e = object.get("text");
		if (e == null)
			return;
		String text = e.getAsString();
		File sound = getAudioTranslation(text);
		CommandLine cmdLine = CommandLine.parse("aplay "+sound.getAbsolutePath());
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
		executor.setWatchdog(watchdog);
		int exitValue = executor.execute(cmdLine);
		
	}

	private File getAudioTranslation(String text) throws IOException {

		long then=System.currentTimeMillis();
		File result=new File("/tmp/say.wav");
		
		String command ="/usr/bin/curl -s  -X GET -u \""+userid+"\":\""+password+"\" --output "+ result.getAbsolutePath()+ " \"https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?accept=audio/wav&text="+URLEncoder.encode(text)+"&voice=en-US_AllisonVoice\"";
		
		CommandLine cmdLine = CommandLine.parse(command);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
		executor.setWatchdog(watchdog);
		int exitValue = executor.execute(cmdLine);
		long now=System.currentTimeMillis();
		
		
		LOG.info("got {} bytes in {} millseconds", result.length(), now-then);
		
		return result;

	}

	@Override
	public void configure(JsonObject serviceConfig) {
		service = new TextToSpeech();
		service.setUsernameAndPassword(serviceConfig.get("username").getAsString(),
				serviceConfig.get("password").getAsString());

		userid = serviceConfig.get("username").getAsString();
		password = serviceConfig.get("password").getAsString();
	}

}
