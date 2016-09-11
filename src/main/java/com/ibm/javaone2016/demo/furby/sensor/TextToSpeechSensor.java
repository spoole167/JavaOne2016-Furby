package com.ibm.javaone2016.demo.furby.sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.javaone2016.demo.furby.AbstractActiveSensor;
import com.ibm.javaone2016.demo.furby.sensor.FurbyMotionController.Action;
import com.ibm.javaone2016.demo.furby.sensor.FurbyMotionController.OpenMouthAction;
import com.ibm.javaone2016.demo.furby.sensor.FurbyMotionController.PauseAction;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/*
 * Output sensor for converting a command into speech 
 * and outputing via available audio 
 * 
 */
public class TextToSpeechSensor extends AbstractActiveSensor {
	private String userid;
	private String password;
	
	WebSocketFactory factory = new WebSocketFactory();
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
		Object[] data=getAudioTranslationWithMarks(text);
		File sound=(File) data[0];
		float[] marks=(float[]) data[1];
		
		CommandLine playCommandLine = CommandLine.parse("play -q " + sound.getAbsolutePath());
		DefaultExecutor musicExecutor = new DefaultExecutor();
		musicExecutor.setExitValue(0);
		ExecuteWatchdog musicWatchdog = new ExecuteWatchdog(60000);
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		musicExecutor.setWatchdog(musicWatchdog);
		
		
		Thread t=new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					// set up furby
					List<Action> actions=new LinkedList<>();
					
					furby.wake();
					
					for(float f:marks) {
						actions.add(furby.new OpenMouthAction((long) f*1000));
						actions.add(furby.new PauseAction(1000));
					}
					
					// kick off music.
					musicExecutor.execute(playCommandLine,resultHandler);
			
				} catch (ExecuteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		t.start();

	}

	private Object[] getAudioTranslationWithMarks(String text) throws IOException {
		
		final List<Float> marks=new ArrayList<>();
		final List<byte[]> audio=new ArrayList<>();
		
		JsonParser parser=new JsonParser();
		// get token..
		OkHttpClient client = new OkHttpClient();
		String credential = Credentials.basic(userid, password);
		 Request request = new Request.Builder()
				 .addHeader("Authorization", credential)
				 .url("https://stream.watsonplatform.net/authorization/api/v1/token?url=https://stream.watsonplatform.net/text-to-speech/api")
			      .build();
		 String token=client.newCall(request).execute().body().string();
		 // got token 
		 LOG.info("got token {}",token);
		 
		 // convert string into marked up sequence 
		 String[] parts=text.split(" ");
		final  StringBuilder sb=new StringBuilder();
		 sb.append("<speak>");
		 int c=1;
		 for(String p:parts) {
			 if(p.trim().equals("")==false) {
				 sb.append(p.trim());
				 sb.append("<mark name='a"+c+"'>");
				 c++;
			 }
		 }
		 sb.append("</speak>");
		 
		 // make call...
		
			WebSocket socket=factory.setConnectionTimeout(5000)
			 .createSocket("https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?voice=en-US_AllisonVoice&watson-token="+token)
			 .addListener(new WebSocketAdapter() {
				 public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception
				    {
					 LOG.info("connected");
				     JsonObject o=new JsonObject();
			    	 o.addProperty("text", sb.toString());
			    	 o.addProperty("accept","audio/wav");
			    	 LOG.info("sending {}",o.toString());
			    	 try {
			    	 websocket.sendText(o.toString());
			    	 } catch(Exception e) {
			    		 e.printStackTrace();
			    	 }
			     }

				 public void onTextMessage(WebSocket websocket, String message) {
			       //  System.out.println(message);
			         if(message!=null) {
			        	 JsonElement json=parser.parse(message);
			        	if(json.isJsonObject()) {
			        		JsonObject obj=json.getAsJsonObject();
			        		JsonArray array=obj.getAsJsonArray("marks");
			        		float mark=array.get(0).getAsJsonArray().get(1).getAsFloat();
			        		marks.add(mark);
			        	}
			        	 
			         }
			     }
				 
				 @Override
				    public void onError(WebSocket websocket, WebSocketException cause) throws Exception
				    {
					 cause.printStackTrace();
				    }
				 @Override
				    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception
				    {
					 
					 audio.add(binary);
				    }
				 
				 @Override
				    public void onDisconnected(WebSocket websocket,
				        WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
				        boolean closedByServer) throws Exception
				    {
					 LOG.info("disconnected");
				    }

			 })
			 ;//.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE); << does not work!
			
			 try {
				socket.connect();
				while(socket.getState()!=WebSocketState.CLOSED) {
					Thread.sleep(100);
					
				}
			} catch (WebSocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 LOG.info("fetch completed");
			 LOG.info("got {} marks",marks.size());
			 LOG.info("got {} audio",audio.size());
		
		 
		 // turn binary into files...
			 File result = new File("/tmp/say.wav");
			 FileOutputStream fos=new FileOutputStream(result);
			 for(byte[] data:audio) {
				 fos.write(data);
			 }
			 fos.flush();
			 fos.close();
			 
		// turn marks into file
			 float[] ms=new float[marks.size()];
			 float start=0;
			 int i=0;
			 for(Float f:marks) {
				 ms[i]=(f-start);
				 start=f;
				 i++;
			 }
			 
		 return new Object[]{result,ms};
	}
	
	
	private File getAudioTranslationOld(String text) throws IOException {

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
