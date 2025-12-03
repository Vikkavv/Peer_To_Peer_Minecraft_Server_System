package vpn;

public class Main {
	public static void main(String[] args) throws Exception {
		new DiscoveryResponder("Amigos123").listen(25565);
	}
}
