package vpn;
import java.io.IOException;
import java.net.*;

public class NetworkDiscoverClient {
	public static void discover(String networkName, int targetPort, int timeoutMs) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);
		
		String message = "DISCOVER: " + networkName;
		byte[] data = message.getBytes();
		
		DatagramPacket packet = new DatagramPacket(
				data, data.length,
				InetAddress.getByName("255.255.255.255"),
				targetPort
		);
		
		socket.send(packet);
		
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeoutMs) {
			try {
				byte[] buf = new byte[512];
				DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);
				socket.receive(responsePacket);
				
				String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
				String ip = responsePacket.getAddress().getHostAddress();
				
				System.out.println("Respuesta recibida de " + ip + ": " + response);
			} catch(SocketTimeoutException e){
				//We wait until time goes by
			}
		}
		
		socket.close();
		System.out.println("Search finished");
	}
}
