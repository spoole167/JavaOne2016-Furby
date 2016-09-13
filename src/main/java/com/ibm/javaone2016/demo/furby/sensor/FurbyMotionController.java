package com.ibm.javaone2016.demo.furby.sensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.ibm.javaone2016.demo.furby.AbstractSensor;
import com.ibm.javaone2016.demo.furby.sensor.FurbyMotionController.TestAction;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class FurbyMotionController {

	final GpioController gpio;
	final GpioPinDigitalInput home;
	final GpioPinDigitalOutput forwardPin;
	final GpioPinDigitalOutput backwardPin;
	int counter=-1;
	boolean atHome=false;
	boolean seeking=false;
	
	BlockingQueue<Action> actions = new ArrayBlockingQueue<>(1024);
	
	public FurbyMotionController() {
		gpio = GpioFactory.getInstance();
		home = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
		home.setShutdownOptions(true);
		home.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			    System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
		       if(event.getState()==PinState.HIGH) {
		    	   atHome=true;
		    	   if(seeking) {
		    		   counter=0;
		    		   seeking=false;
		    		   setOff();
		    	   }
		       }
			}

		});

		forwardPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyLED", PinState.LOW);
		backwardPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "MyLED", PinState.LOW);
		// set shutdown state for this pin
		forwardPin.setShutdownOptions(true, PinState.LOW);
		backwardPin.setShutdownOptions(true, PinState.LOW);

		drive();
	}

	private void pause(long time) {
		try {
			counter+=time;
			Thread.sleep(time);
			//System.out.println(">"+counter);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void setForwards() {
		setOff();
		forwardPin.high();
		backwardPin.low();
	}
	private void setBackwards() {
		setOff();
		forwardPin.low();
		backwardPin.high();
	}
	private void setOff() {
		forwardPin.low();
		backwardPin.low();
	}
	
	
	private void setup() {
		if(counter<0) {
			// need to be configured. 
			// move until home reached 
			actions.add(new Action(){

				@Override
				public void execute() {
					
					while(!atHome) {
						setForwards();
						pause(100);
						
					}
					
				}}); 
		}
	}
	public void sleep() {
		setup();
		moveTo(100);
		
	}

	private void drive() {
		Thread r=new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					Action a;
					try {
						a = actions.take();
						AbstractSensor.LOG.info("action {}",a.getClass().getName());
						a.execute();
						AbstractSensor.LOG.info("action completed");
						setOff();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			}
		});
		r.start();
		
		
	}

	private void moveTo(final long i) {
		actions.add(new Action(){

			@Override
			public void execute() {
				long l=i;
				if(l<0) {
					l=l*-1;
					setBackwards();
				}
				else {
					setForwards();
				}
				pause(l);
				setOff();
			}});
		
	}

	

	public void wake() {
		setup();
		moveTo(200);
		drive();
	}
	
    public static void main(String[] args) {

        FurbyMotionController furby=new FurbyMotionController();
         BufferedReader br = null;

    

         // Refer to this http://www.mkyong.com/java/how-to-read-input-from-$
         // for JDK 1.6, please use java.io.Console class to read system inp$
         br = new BufferedReader(new InputStreamReader(System.in));

         
         while (true) {

             System.out.print("Enter something : ");
             try {
				String input = br.readLine();
				long then=System.currentTimeMillis();
				switch(input) {
				case "q" :
						System.exit(0);
						break;
				case "f" :
					furby.moveTo(100);
					break;
				case "b" :
					furby.moveTo(-100);
					break;
				case "h" :
					furby.goHome();
					break;
			 	case "c" :
					furby.calibrate();
					break;
			 	case "s" :
					furby.speak();
					break;
			 	case "t" :
			 		
			 		for(int i=0;i<5;i++){
					furby.speak();
			 		}
			 		
					break;
				}
				long now=System.currentTimeMillis();
		 		System.out.println("took "+(now-then));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
         }
     }
     
	private void speak() {
		goHome();
		
		setForwards();
		moveTo(200);
		pause(100);
		moveTo(200);
		pause(100);
		moveTo(-200);
		pause(100);
		moveTo(-200);
		pause(100);
		moveTo(-200);
	}

	private void calibrate() {
		
		goHome();
		for(int i=0;i<10;i++) {
			long start=System.currentTimeMillis();
			seek();
			long now=System.currentTimeMillis();
			long diff=now-start;
			System.out.println(""+diff);
		}
		
	}

	private void seek() {
		// drive forwards until at home
		setForwards();
		goHome();
		
	}

	public static interface Action {

		void execute();
		
	}

	public class TestAction implements Action {

		@Override
		public void execute() {
			setForwards();
			pause(2000);
			System.out.println("forwards completed");
			setBackwards();
			pause(2000);
			System.out.println("backwards completed");
			setOff();
		}
		
	}
	
	public class Chat implements Action {

		private char[] chat;
		public Chat(String chat) {
			this.chat=chat.toCharArray();
		}
		
		@Override
		public void execute() {
			
			for(char c:chat) {
				switch(c) {
					
				case 'h' :
					goHome();
					break;
				case 'f' :
					setForwards();
					pause(100);
					break;
				case 'b' :
					setBackwards();
					pause(100);
					break;
				case 'p' :
					pause(100);
					break;
					
				}
			}
			
			setOff();
		}
	
		
		
	}
	
	public class Talk implements Action {

		private int turns=0;
		public Talk(int seconds) {
			turns=seconds;
			
		}
		
		@Override
		public void execute() {
			while(turns>0) {
				turns--;
				speak();
			}
			
		}
		
	}
	public class PauseAction implements Action {
		private long time;
		public PauseAction(long milliseconds) {
			this.time=milliseconds;
		}
		@Override
		public void execute() {
			setOff();
			pause(time);
			
		}
	}
	
	public class GoHomeAction implements Action {

		@Override
		public void execute() {
			seeking=true;
			setForwards();
			while(!atHome) {
				pause(100);
			}
			System.out.println("at home");
		}
		
	
	}
	public class OpenMouthAction implements Action {

		private long time;
		
		public OpenMouthAction(long milliseconds) {
			this.time=milliseconds;
		}
		@Override
		public void execute() {
			System.out.println("mouth for "+time);
			setForwards();
			pause(time);
			
		}
		
	}
	public void run(List<Action> incoming) {
		actions.addAll(incoming);
		
	}

	public void goHome() {
		actions.add(new GoHomeAction());
		
	}

	public void run(Action testAction) {
		actions.add(testAction);
		
	}
}
