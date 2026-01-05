package jgit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

import view.MainFrame;

public class TokenStore {
	private static final Path TOKEN_FILE = Path.of("data/credentials.dat");
	private static final Path USER_DATA_FILE = Path.of("data/userData.properties");
	
	public static void saveUserData(String nickname, String email, String token) {
		MainFrame.checkIfExistsDataFolder();
		if(!Files.exists(USER_DATA_FILE)) {
			try {
				Files.createFile(USER_DATA_FILE);
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible (userData.properties).", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(USER_DATA_FILE.toFile()); FileOutputStream out = new FileOutputStream(USER_DATA_FILE.toFile())){
			props.load(in);
			props.setProperty("nickname", nickname);
			props.setProperty("email", email);
	        props.store(out, "User data updated");
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible (userData.properties).", "Error", JOptionPane.ERROR_MESSAGE);
		}
		try {
			saveToken(token);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Error saving token, try again.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static Map<String, String> getSavedUserData() throws Exception{
		MainFrame.checkIfExistsDataFolder();
		Exception invalidSignInEx = new Exception("Session closed or invalid, sign in again.");
		if(!Files.exists(USER_DATA_FILE) || !Files.exists(TOKEN_FILE)) throw invalidSignInEx;
		Map<String, String> userData = new HashMap<>();
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(USER_DATA_FILE.toFile())){
			props.load(in);
			if(!props.containsKey("nickname") || !props.containsKey("email")) throw invalidSignInEx;
			userData.put("nickname", props.getProperty("nickname"));
			userData.put("email", props.getProperty("email"));
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible (userData.properties).", "Error", JOptionPane.ERROR_MESSAGE);
		}
		try {
			userData.put("token", loadToken());
		}
		catch(IOException ioe) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible (credentials.dat).", "Error", JOptionPane.ERROR_MESSAGE);
		}
		catch(SecurityException sece) {
			throw invalidSignInEx;
		}
		
		if(userData.isEmpty()) return null;
		return userData;
	}
	
	public static void saveToken(String token) throws Exception {
		MainFrame.checkIfExistsDataFolder();
		if(!Files.exists(TOKEN_FILE)) Files.createFile(TOKEN_FILE);
		
		SecretKey key = deriveKey();
		
		byte[] encrypted = encrypt(token.getBytes(StandardCharsets.UTF_8), key);
		byte[] hmac = hmac(encrypted, key);
		
		try(DataOutputStream out = new DataOutputStream(Files.newOutputStream(TOKEN_FILE))) {
			out.writeInt(encrypted.length);
			out.write(encrypted);
			out.writeInt(hmac.length);
			out.write(hmac);
		}
	}
	
	public static String loadToken() throws Exception {
		if(!Files.exists(TOKEN_FILE)) return null;
		
		SecretKey key = deriveKey();
		
		try(DataInputStream in = new DataInputStream(Files.newInputStream(TOKEN_FILE))){
			byte[] encrypted = new byte[in.readInt()];
			in.readFully(encrypted);
			
			byte[] storeHmac = new byte[in.readInt()];
			in.readFully(storeHmac);
			
			byte[] computedHmac = hmac(encrypted, key);
			if(!MessageDigest.isEqual(storeHmac, computedHmac)) {
				throw new SecurityException("Credenciales manipuladas");
			}
			
			byte[] decrypted = decrypt(encrypted, key);
			return new String(decrypted, StandardCharsets.UTF_8);
		}
	}
	
	//If we need to sign out.
	public static void clear() {
		try {
			Files.deleteIfExists(TOKEN_FILE);
			Files.deleteIfExists(USER_DATA_FILE);
	        JOptionPane.showMessageDialog(
	                null,                   
	                "Session closed successfully",
	                "Git",
	                JOptionPane.INFORMATION_MESSAGE
	        );
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "There is no session open.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static boolean sessionIsOpened() {
		if(Files.exists(TOKEN_FILE)) 
			return true;
		return false;
	}
	
	private static SecretKey deriveKey() throws Exception {
		String seed = 
				System.getProperty("user.name") +
				System.getProperty("os.name") +
				System.getProperty("user.home");
		
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		byte[] hash = sha.digest(seed.getBytes(StandardCharsets.UTF_8));
		
		return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
	}
	
	private static byte[] encrypt(byte[] data, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		
		return cipher.doFinal(data);
	}
	
	private static byte[] decrypt(byte[] data, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, key);
		
		return cipher.doFinal(data);
	}
	
	private static byte[] hmac(byte[] data, SecretKey key) throws Exception{
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(key);
		
		return mac.doFinal(data);
	}
}
