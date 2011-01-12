package shao.robopack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTConnector;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

/*
 * NXT comms implemented in the form of a textview for logging and background thread.
 */
public class CommsView extends TextView {
	public String requestList = "";
	public boolean connected = false;
	NxtCommunicator nxtCommThread = null;
	NXTConnector conn;

	public CommsView(Context context, AttributeSet as) {
		super(context, as);
	}
	
	public void kill() {
		try {
			conn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Open comms with the NXT - creates a background thread to open and manage the connection and passes a handler so the thread
	 * can update the screen.
	 */
	public void startComms(String nxtName) {		
		conn = new NXTConnector();
		
		// Connect to any NXT over Bluetooth
		boolean connected = conn.connectTo("btspp://", NXTComm.LCP);
		
		if (!connected) {
			System.err.println("Failed to connect to any NXT");
			System.exit(1);
		}
		NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
		for(int i = 0; i < 10; i++) {
			
		}
	}

	public void writeLine(String str) {
		//string str = getText + "\n" + str
		setText(getText() + str);
	}

	//handler defined inline - lets the thread have access to the gui object
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {

			writeLine(msg.getData().getString("str"));
		}
	};
}

class NxtCommunicator extends Thread {
	DataInputStream nxtIn = null;
	DataOutputStream nxtOut = null;
	Vector<String> requestList = new Vector<String>();
	Handler mHandler;
	boolean running;

	public NxtCommunicator(DataInputStream dis, DataOutputStream dos, Handler h) {
		mHandler = h;
		nxtIn = dis;
		nxtOut = dos;
	}	

	@Override
	public void run() {
        while (true) {
            // read length of message and the message itself
            int length;

            try {
                length = nxtIn.readByte();
                length = (nxtIn.readByte() << 8) + length;
                byte[] returnMessage = new byte[length];
                nxtIn.read(returnMessage);

                if ((length >= 2) && (returnMessage[0] == 0x02));
                   // dispatchMessage(returnMessage);

            } catch (IOException e) {
                // don't inform the user when connection is already closed
                boolean connected = false;
				if (connected)
                    //sendState(STATE_RECEIVEERROR);

                return;
            }
        }
	}



    private void waitSomeTime(int millis) {
        try {
            Thread.sleep(millis);

        } catch (InterruptedException e) {
        }
    }

}// End of NxtCommunicator class

