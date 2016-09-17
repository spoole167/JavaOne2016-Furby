package com.ibm.javaone2016.demo.furby.sensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
	public TextToSpeechSensor() {
		super();
		try {
			loadFurtunes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void loadFurtunes() throws IOException {
		
		InputStreamReader isr=new InputStreamReader(getClass().getResourceAsStream("/furtunes.txt"));
		BufferedReader br=new BufferedReader(isr);
		while(true) {
			String line=br.readLine();
			if(line==null) break;
			furtunes.add(line);
			
			StringBuilder chat=new StringBuilder();
			boolean f=true;
			for(String p:line.split(" ")) {
				for(int i=0;i<p.length();i++) {
					if(f) {
						chat.append("f");
					} else {
						chat.append("b");
					}
				}
				f=!f;
				chat.append("ppp");
			}
			furchat.add(chat.toString());
			
		}
		
	}

	private String userid;
	private String password;
	private Random r=new Random();
	
	WebSocketFactory factory = new WebSocketFactory();
	private FurbyMotionController furby=new FurbyMotionController();
	private List<String> furtunes=new LinkedList<>();
	private List<String> furchat=new LinkedList<>();
	@Override
	public void start() {

		while (isAlive()) {

			publishEvent("talk", "status", "ok");

			pause(10);

		}

	}

	@Override
	public String[] getCommands() {
		return new String[] { "say", "sleep","home","test","furtune" };
	}

	@Override
	public void handleCommand(String command, JsonObject object) {

		try {
			switch (command) {

			case "home" :
				furby.goHome();
				break;
				
			case "say":
				wake(object);
				say(object);
				break;
			
			case "furtune":
				wake(object);
				furtune(object);
				break;
			
				
			case "sleep":
				sleep(object);
				break;
				

			case "test":
				furby.run(furby.new TestAction());
				break;
			}
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void furtune(JsonObject object) throws IOException {
		
		int fortune=r.nextInt(furtunes.size());
		String saying=furtunes.get(fortune);
		JsonObject o=new JsonObject();
		o.addProperty("text",saying);
		o.addProperty("asis", true);
		o.addProperty("chat",furchat.get(fortune));
		say(o);
		
	}

	private void wake(JsonObject object) {
	furby.wake();
	}

	private void sleep(JsonObject object) {
		furby.sleep();
		
	}

	private void say(JsonObject object) throws IOException, IOException {
		
		Object[] data=getAudioTranslationWithMarks(object);
		File sound=(File) data[0];
		float[] marks=(float[]) data[1];
		final float audio_length=(float) data[2];
		
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
					
					List<Action> actions=new LinkedList<>();
					
					if(object.has("chat")) {
						String chat=object.get("chat").getAsString();
						actions.add(furby.new Chat(chat));
					}
					else {
						
					
					// set up furby
					
					
					furby.wake();
					
					actions.add(furby.new Talk((int)audio_length));
					//for(float f:marks) {
					//	actions.add(furby.new OpenMouthAction((long) (f*1000.0)));
					//	actions.add(furby.new PauseAction(1000));
					//}
					}
					
					furby.run(actions);
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

	private Object[] getAudioTranslationWithMarks(JsonObject object) throws IOException {
		
		JsonElement te = object.get("text");
		if (te == null)
			return null;
		
		String text = te.getAsString();
		
		String voice="en-US_LisaVoice";
		
		if(object.has("voice")) {
				voice=object.get("voice").getAsString();
				
		}
		
		
		
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
		
		 final  StringBuilder sb=new StringBuilder();
		 if(object.has("asis")) {
				sb.append(text);
			}
		 else {
		 // convert string into marked up sequence 
		 String[] parts=text.split(" ");
		
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
		 }
		 
		 //en-US_MichaelVoice
		 // make call...
		String url="https://stream.watsonplatform.net/text-to-speech/api/v1/synthesize?voice="+voice+"&watson-token="+token;
		LOG.info("url:{}",url);
			WebSocket socket=factory.setConnectionTimeout(5000)
			 .createSocket(url)
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
			 float audio_length=0;
			 float[] ms=new float[marks.size()];
			 if(marks.size()>0) {
			audio_length=marks.get(marks.size()-1);
			 float start=0;
			 int i=0;
			 for(Float f:marks) {
				 ms[i]=(f-start);
				 start=f;
				 i++;
			 }
			 }
		 return new Object[]{result,ms,audio_length};
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
