package vpn;
import java.io.IOException;
import java.net.*;

public class NetworkDiscoverClient {
	public static String discover(String networkName, int targetPort, int timeoutMs) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);
		socket.setSoTimeout(timeoutMs);
		
		String message = "DISCOVER: " + networkName;
		byte[] data = message.getBytes();
		
		DatagramPacket packet = new DatagramPacket(
				data, data.length,
				InetAddress.getByName("255.255.255.255"),
				targetPort
		);
		
		socket.send(packet);
		
		try {
			byte[] buf = new byte[512];
			DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);
			socket.receive(responsePacket);
			
			String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
			String ip = responsePacket.getAddress().getHostAddress();
			
			System.out.println("Respuesta recibida de " + ip + ": " + response);
			socket.close();
			return ip;
		} catch(SocketTimeoutException e){
			//We wait until time goes by
		}
		
		socket.close();
		return "NotFound";
	}
	
	public static String surroundDiscoverIOException(String networkName, int targetPort, int timeoutMs) {
		try {
			return discover(networkName, targetPort, timeoutMs);
		}catch(IOException e) {
			return "NotFound";
		}
	}
}
