package com.ibm.javaone2016.demo.furby.sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.text_to_speech.v1.util.WaveUtils;

/*
 * Output sensor for converting a command into speech 
 * and outputing via available audio 
 * 
 */
public class TextToSpeechSensor extends AbstractActiveSensor {
	private TextToSpeech service;

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
		LOG.info("saying "+text);
		
		InputStream stream = service.synthesize(text, Voice.EN_ALLISON, "audio/wav");

		InputStream in = WaveUtils.reWriteWaveHeader(stream);
		File outFile=new File("/tmp/say.wav");
		OutputStream out = new FileOutputStream(outFile);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) > 0) {
			out.write(buffer, 0, length);
		}
		out.close();
		in.close();
		stream.close();
		
		LOG.info("translation completed");
		
		AudioInputStream audioIn = AudioSystem.getAudioInputStream(outFile);
		Clip clip = AudioSystem.getClip();
		clip.open(audioIn);
		clip.start();
	
		   
	}

	@Override
	public void configure(JsonObject serviceConfig) {
		service = new TextToSpeech();
		service.setUsernameAndPassword(serviceConfig.get("username").getAsString(),
				serviceConfig.get("password").getAsString());

	}

}