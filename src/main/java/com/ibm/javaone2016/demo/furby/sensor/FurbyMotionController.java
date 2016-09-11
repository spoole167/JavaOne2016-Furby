package com.ibm.javaone2016.demo.furby.sensor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
	final GpioPinDigitalOutput pin;
	final GpioPinDigitalOutput dir;
	int counter=-1;
	boolean atHome=false;
	BlockingQueue<Action> actions = new ArrayBlockingQueue<>(1024);
	
	public FurbyMotionController() {
		gpio = GpioFactory.getInstance();
		home = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
		home.setShutdownOptions(true);
		home.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			    System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
		       if(event.getState()==PinState.HIGH) atHome=true;
			}

		});

		pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyLED", PinState.LOW);
		dir = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "MyLED", PinState.LOW);
		// set shutdown state for this pin
		pin.setShutdownOptions(true, PinState.LOW);
		dir.setShutdownOptions(true, PinState.LOW);

		drive();
	}

	private void setup() {
		if(counter<0) {
			// need to be configured. 
			// move until home reached 
			actions.add(new Action(){

				@Override
				public void execute() {
					
					while(!atHome) {
						pin.pulse(100, true);
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
						a.execute();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			}
		});
		r.start();
		
		
	}

	private void moveTo(final int i) {
		actions.add(new Action(){

			@Override
			public void execute() {
				pin.pulse(i, true);
				
			}});
		
	}

	

	public void wake() {
		setup();
		moveTo(200);
		drive();
	}
	
	public static interface Action {

		void execute();
		
	}

	public class PauseAction implements Action {
		private long time;
		public PauseAction(long milliseconds) {
			this.time=milliseconds;
		}
		@Override
		public void execute() {
			pin.low();
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public class OpenMouthAction implements Action {

		private long time;
		
		public OpenMouthAction(long milliseconds) {
			this.time=milliseconds;
		}
		@Override
		public void execute() {
			pin.pulse(time, true);
			
		}
		
	}
}
