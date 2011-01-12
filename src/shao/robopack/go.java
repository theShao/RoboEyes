package shao.robopack;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTConnector;
import android.app.Activity;
import android.os.Bundle;

public class go extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		NXTConnector conn = new NXTConnector();
		
		// Connect to any NXT over Bluetooth
		boolean connected = conn.connectTo("btspp://", NXTComm.LCP);
		
		if (!connected) {
			System.err.println("Failed to connect to any NXT");
			System.exit(1);
		}
		
		DataOutputStream dos = conn.getDataOut();
		DataInputStream dis = conn.getDataIn();
		
		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
		UltrasonicSensor sonic = new UltrasonicSensor(SensorPort.S4);
		System.out.println("Tachometer A: " + Motor.A.getTachoCount());
		System.out.println("Tachometer C: " + Motor.C.getTachoCount());
		//Motor.A.rotate(5000);
		//Motor.C.rotate(-5000);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Sound.playTone(1000, 1000);
		System.out.println("Tachometer A: " + Motor.A.getTachoCount());
		System.out.println("Tachometer C: " + Motor.C.getTachoCount());
		for(int i = 0; i < 10; i++) {
			System.out.println("Dist: " + sonic.getDistance());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
}