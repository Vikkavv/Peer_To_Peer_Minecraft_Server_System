package vpn;

import java.io.IOException;
import java.util.Scanner;

public class MainTwo {
	public static void main(String[] args) throws IOException {
		NetworkDiscoverClient.discover("Amigos123", 25565, 3000);
		while(true) {
			Scanner scanner = new Scanner(System.in);
			System.out.println("press any key to close...");
			String text = scanner.nextLine();
			if(!text.isEmpty() || text.equals(" ") || text.equals("\n")) break;
		}
	}
}
