
package lejos.pc.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Implementation of NXTComm using the Bluecove libraries 
 * on Microsoft Windows. 
 * 
 * Should not be used directly - use NXTCommFactory to create
 * an appropriate NXTComm object for your system and the protocol
 * you are using.
 *
 */
public class NXTCommAndroid implements NXTComm {
	private static Vector<BluetoothDevice> devices;
    private static Vector<NXTInfo> nxtInfos;
	private BluetoothSocket nxtBTSocket;
	private OutputStream os;
	private InputStream is;
	private NXTInfo nxtInfo;
	
    // Member fields
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // this is the only OUI registered by LEGO, see http://standards.ieee.org/regauth/oui/index.shtml
    public static final String OUI_LEGO = "00:16:53";

	public NXTInfo[] search(String name, int protocol) throws NXTCommException {

		devices = new Vector<BluetoothDevice>();
		nxtInfos = new Vector<NXTInfo>();

		if ((protocol & NXTCommFactory.BLUETOOTH) == 0)
			return new NXTInfo[0];

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        boolean legoDevicesFound = false;
        
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // only add LEGO devices
                if (device.getAddress().startsWith(OUI_LEGO)) {
                    legoDevicesFound = true;
                    nxtInfo = new NXTInfo();
                    nxtInfo.name = device.getName();
                    nxtInfo.deviceAddress = device.getAddress();
    				nxtInfo.protocol = NXTCommFactory.BLUETOOTH;

    				if (name == null || name.equals(nxtInfo.name))
    					nxtInfos.addElement(nxtInfo);
    				else
    					continue;

    				System.out.println("Found: " + nxtInfo.name);
                }
            }
        }
        
		NXTInfo[] nxts = new NXTInfo[nxtInfos.size()];
		for (int i = 0; i < nxts.length; i++)
			nxts[i] = (NXTInfo) nxtInfos.elementAt(i);
		return nxts;
	}

	public boolean open(NXTInfo nxt, int mode) throws NXTCommException {

        if (mode == RAW) throw new NXTCommException("RAW mode not implemented");
		// Construct URL if not present
        
        try {

            BluetoothSocket nxtBTsocketTEMPORARY;
            BluetoothDevice nxtDevice = null;
            nxtDevice = mBtAdapter.getRemoteDevice(nxt.deviceAddress);

            if (nxtDevice == null) {
                Log.w("BT", "no_paired_nxt");
                return false;
            } 

            nxtBTsocketTEMPORARY = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
            nxtBTsocketTEMPORARY.connect();
            nxtBTSocket = nxtBTsocketTEMPORARY;

            is = new DataInputStream(nxtBTSocket.getInputStream());
            os = new DataOutputStream(nxtBTSocket.getOutputStream());
            nxt.connectionState = (mode == LCP ? NXTConnectionState.LCP_CONNECTED : NXTConnectionState.PACKET_STREAM_CONNECTED);
            return true;

        } catch (IOException e) {
            //Log.d("BTCommunicator", "error createNXTConnection()", e);
        	nxt.connectionState = NXTConnectionState.DISCONNECTED;
            return false;
        }
	}

    public boolean open(NXTInfo nxt) throws NXTCommException
    {
        return open(nxt, PACKET);
    }

	public void close() throws IOException {
		if (os != null)
			os.close();
		if (is != null)
			is.close();
		if (nxtBTSocket != null)
			nxtBTSocket.close();
	}

	/**
	 * Sends a request to the NXT brick.
	 * 
	 * @param message
	 *            Data to send.
	 */
	public synchronized byte[] sendRequest(byte[] message, int replyLen)
			throws IOException {

		// length of packet (Least and Most significant byte)
		// * NOTE: Bluetooth only. 
		int LSB = message.length;
		int MSB = message.length >>> 8;

		if (os == null)
			return new byte[0];

		// Send length of packet:
		os.write((byte) LSB);
		os.write((byte) MSB);

		os.write(message);
		os.flush();

		if (replyLen == 0)
			return new byte[0];

		byte[] reply = null;
		int length = -1;

		if (is == null)
			return new byte[0];

		do {
			length = is.read(); // First byte specifies length of packet.
		} while (length < 0);

		int lengthMSB = is.read(); // Most Significant Byte value
		length = (0xFF & length) | ((0xFF & lengthMSB) << 8);
		reply = new byte[length];
		int len = is.read(reply);
		if (len != replyLen) throw new IOException("Unexpected reply length");

		return (reply == null) ? new byte[0] : reply;
	}

	public byte[] read() throws IOException {

        int lsb = is.read();
		if (lsb < 0) return null;
		int msb = is.read();
        if (msb < 0) return null;
        int len = lsb | (msb << 8);
		byte[] bb = new byte[len];
		for (int i=0;i<len;i++) bb[i] = (byte) is.read();

		return bb;
	}
	
    public int available() throws IOException {
        return 0;
    }

	public void write(byte[] data) throws IOException {
        os.write((byte)(data.length & 0xff));
        os.write((byte)((data.length >> 8) & 0xff));
		os.write(data);
		os.flush();
	}

	public OutputStream getOutputStream() {
		return new NXTCommOutputStream(this);
	}

	public InputStream getInputStream() {
		return new NXTCommInputStream(this);
	}

	public String stripColons(String s) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c != ':') {
				sb.append(c);
			}
		}

		return sb.toString();
	}
}
