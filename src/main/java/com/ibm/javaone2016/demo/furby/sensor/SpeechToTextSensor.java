package com.ibm.javaone2016.demo.furby.sensor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;
import com.ibm.javaone2016.demo.furby.ActiveSensor;
import com.ibm.watson.developer_cloud.speech_to_text.v1.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;

public class SpeechToTextSensor extends AbstractActiveSensor {

	private static final Logger LOG = LoggerFactory.getLogger(ActiveSensor.class);
	
	private SpeechToText service;

	public SpeechToTextSensor() {

	}  

	@Override
	public void start() {

		while (isAlive()) {

			pause(10);

		}

	}

	@Override
	public void configure(JsonObject serviceConfig) {
		service = new SpeechToText();
		service.setUsernameAndPassword(serviceConfig.get("username").getAsString(),
				serviceConfig.get("password").getAsString());
		
	}

	@Override
	public String[] getCommands() {
		return new String[] { "record" };
	}

	@Override
	public void handleCommand(String command, JsonObject object) {

		if (command.equals("record")) {
			try {
				LOG.info("recording...");
				JsonObject results=recordSound();
				publishEvent("record", "text", results);
				LOG.info("finished");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private JsonObject recordSound() throws LineUnavailableException, IOException {

		File wavFile=new File("/tmp/a1.wav");
		AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, true);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
		
		microphone.open();
		microphone.start();

		AudioInputStream ais = new AudioInputStream(microphone);
		Thread t=new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					AudioSystem.write(ais, fileType, wavFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		
		t.start();
		try {
			t.join(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		microphone.stop();
		microphone.close();
		ais.close();
		
		
		RecognizeOptions options = new RecognizeOptions().contentType("audio/wav")
				  .timestamps(true).wordAlternativesThreshold(0.9).continuous(true);
		SpeechResults sr=service.recognize(wavFile, options);
		
		String json=sr.toString();
		
		return new  JsonParser().parse(json).getAsJsonObject();
		

	}

	private void play(byte[] audio) throws LineUnavailableException, IOException {

		InputStream input = new ByteArrayInputStream(audio);
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, true);

		AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();
		int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
		byte buffer[] = new byte[bufferSize];

		int count;
		while ((count = ais.read(buffer, 0, buffer.length)) != -1) {
			if (count > 0) {
				line.write(buffer, 0, count);
			}
		}
		line.drain();
		line.close();

	}

}
