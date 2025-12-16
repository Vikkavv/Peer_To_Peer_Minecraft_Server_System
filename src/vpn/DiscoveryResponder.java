package vpn;

import java.net.*;

public class DiscoveryResponder {

	private final String myNetworkName;
	private DatagramSocket socket;
	
	public DiscoveryResponder(String name) {
		this.myNetworkName = name;
	}
	
	public void listen(int port) throws Exception {
		socket = new DatagramSocket(port);
		
		byte[] buf = new byte[512];
		
		while (true) {
			System.out.println("Escuchando...");
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			
			String msg = new String(packet.getData(), 0, packet.getLength());
			
			if(msg.equals(("DISCOVER: " + myNetworkName))) {
				if(isPortActive(port)) {
				byte[] response = "HERE".getBytes();
				DatagramPacket resp = new DatagramPacket(
						response, response.length,
						packet.getAddress(), packet.getPort()
				);
				
				socket.send(resp);
				System.out.println("Respondí a " + packet.getAddress());
				/*At least one server opened*/
				}
				else {
					System.out.println("No respondí, puerto cerrado");
					break;
				}
			}
		}
	}
	
	public void closeListeningSocket() {
		socket.close();
		socket = null;
	}
	
	public DiscoveryResponder listenAsync(int port) {
		new Thread(() -> {
		    try {
		        listen(port);
		    } 
		    catch(SocketException e) {}
		    catch (Exception e) {
		        e.printStackTrace();
		    }
		}).start();
		return this;
	}
	
	private boolean isPortActive(int port) {
	    try (Socket s = new Socket("localhost", port)) {
	        return true;
	        
	    } catch (Exception e) {
	        return false;
	    }
	}
}
