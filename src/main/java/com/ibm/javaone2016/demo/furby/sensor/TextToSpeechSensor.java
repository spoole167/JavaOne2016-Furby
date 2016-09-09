package com.ibm.javaone2016.demo.furby.sensor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;
import com.ibm.watson.developer_cloud.service.WatsonService;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.util.WaveUtils;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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
		byte[] sound = getAudioTranslation(text);
		ByteArrayInputStream bin = new ByteArrayInputStream(sound);
		AudioInputStream audioInputStream = null;
		InputStream wav = WaveUtils.reWriteWaveHeader(bin);
		audioInputStream = AudioSystem.getAudioInputStream(wav);
		AudioFormat audioFormat = audioInputStream.getFormat();
		SourceDataLine line = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(audioFormat);
		line.start();

		int nBytesRead = 0;
		byte[] abData = new byte[250000];
		while (nBytesRead != -1) {
			try {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (nBytesRead >= 0) {
				int nBytesWritten = line.write(abData, 0, nBytesRead);
			}
		}

		line.drain();
		line.close();

	}

	private byte[] getAudioTranslation(String text) throws IOException {

		OkHttpClient client = new OkHttpClient();
		client.setAuthenticator(new Authenticator() {

			@Override
			public Request authenticate(Proxy proxy, Response response) throws IOException {
				String credential = Credentials.basic(userid, password);
				return response.request().newBuilder().header("Authorization", credential).build();
			}

			@Override
			public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
				return null;
			}
		});
		Request request = new Request.Builder()

				.url("https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?accept=audio/wav&text=" + text
						+ "&voice=en-US_AllisonVoice")
				.build();
		Response response = client.newCall(request).execute();
		byte[] sound = response.body().bytes();
		LOG.info("got {} bytes", sound.length);
		return sound;

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
