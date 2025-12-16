package minecraftServerManagement;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class ForgeUtils {
	
	public static final Path DIR_INSTALLERS = Paths.get("forge_installers");
		
	public static Path downloadForgeInstaller(String version){
		if(!(Files.exists(DIR_INSTALLERS)))
			try {
				Files.createDirectories(DIR_INSTALLERS);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		
		String url = String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar", version, version);
		
		Path destination = DIR_INSTALLERS.resolve(String.format("forge-%s-installer.jar", version));
		
		try(InputStream in = new URL(url).openStream()) {
			Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(null, "Forge url not found, contact with our technical support", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return destination;
	}
	
	public static void installForgeServer(Path forgeInstallerFile, Path forgeServerInstalationDirectory){
		ProcessBuilder pb = new ProcessBuilder(
				"java",
				"-jar",
				forgeInstallerFile.toAbsolutePath().toString(),
				"--installServer"
		);
		
		pb.directory(forgeServerInstalationDirectory.toFile());
		pb.inheritIO();
		
		Process p;
		try {
			p = pb.start();
			p.waitFor();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(null, "Process interrupted, try again", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		try {
			Files.delete(forgeInstallerFile);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Can not delete forge installer, path inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static boolean acceptEULA(Path forgeServerInstalationDirectory){
		Path eula = forgeServerInstalationDirectory.resolve("eula.txt");
		if(!(Files.exists(eula))) 
			try {
				Files.createFile(eula);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}

		try {
			Files.writeString(eula, 
					  "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).\r\n"
					+ "eula=true");
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static String downloadForgeMetadata() {
		URL url;
		try {
			url = new URL("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml");
			try(InputStream in = url.openStream()){
				return new String(in.readAllBytes());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		} catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, "Forge url not found, contact with our technical support", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}
	
	public static List<String> getForgeVersionsList(String forgeMetadata){
		List<String> forgeVersions = new ArrayList<>();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document document = null;
		
		try {
			builder = factory.newDocumentBuilder();
			document = builder.parse(new ByteArrayInputStream(forgeMetadata.getBytes()));
		}catch(ParserConfigurationException p) {
			JOptionPane.showMessageDialog(null, "XML metadata parser error", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (SAXException e) {
			JOptionPane.showMessageDialog(null, "XML metadata parser error", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		if(document != null) {
			NodeList versionNodes = document.getElementsByTagName("version");
			
			for(int i = 0; i < versionNodes.getLength(); i++) {
				forgeVersions.add(versionNodes.item(i).getTextContent());
			}
		}
		
		return forgeVersions;
	}
	
	public static List<String> getMinecraftVersionsList(String forgeMetadata){
		List<String> forgeVersions = getForgeVersionsList(forgeMetadata);
		List<String> minecraftVersions = new ArrayList<>();
		
		for(String forgeVersion : forgeVersions) {
			minecraftVersions.add(forgeVersion.split("-")[0]);
		}
		minecraftVersions = new ArrayList<>(minecraftVersions.stream().distinct().toList());
		minecraftVersions.add(0, "Select Minecraft version");
		
		return minecraftVersions;
	}
	
	public static List<String> getForgeVersionsForMinecraftVersion(String minecraftVersion, List<String> forgeVersions){
		List<String> forgeVersionsFilteredList = new ArrayList<>();
		Stream<String> forgeVersionsFilteredStream = forgeVersions.stream().filter(fgVer -> fgVer.split("-")[0].equals(minecraftVersion));
		forgeVersionsFilteredList.addAll(forgeVersionsFilteredStream.toList());
		forgeVersionsFilteredList.add(0, "Select a Forge version");
		
		return forgeVersionsFilteredList;
	}
	
	public static void openURL(String url) {
	    try {
	        Desktop desktop = Desktop.getDesktop();
	        if (desktop.isSupported(Desktop.Action.BROWSE)) {
	            desktop.browse(new URI(url));
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public static void openModsFolder(Path serverDirectory) {
		File modsDirectory = new File(serverDirectory.toString() + "\\mods");
		if(!(Files.exists(serverDirectory))) {
			try {
				Files.createDirectories(modsDirectory.toPath());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else {
			try {
				Desktop.getDesktop().open(modsDirectory);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public static Process executeMinecraftServer(Path serverDirectory) {
		String serverJarName = getServerJarName(serverDirectory);
		String ram = getServerRAMAlloc(serverDirectory);
		if(serverJarName != null) {
			try {
				ProcessBuilder pb;
				if(!(serverJarName.contains(" "))) {
					pb = new ProcessBuilder(
						    "java",
						    ram,
						    "-jar",
						    serverJarName,
						    "nogui"
					);
				}
				else {
					pb = new ProcessBuilder();
					List<String> command = pb.command();
					for(String argument : serverJarName.split(" ")) {
						command.add(argument);
					}
					command.add("--nogui");
				}

				pb.directory(serverDirectory.toFile());
	            pb.redirectErrorStream(true);
	            Process serverProcess = pb.start();

	            BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
	            String line;
	            while ((line = reader.readLine()) != null) {
	                if (line.contains("Done")) {
	                    return serverProcess; 
	                }
	            }

	        } catch (IOException e) {
	            return null;
	        }
		}
		return null;
	}
	
	public static Thread getServerOutputs(Process serverProcess, JTextArea consoleArea) {
		 Thread consoleThread = new Thread(() -> {
		     try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
		
		         String line;
		         while ((line = reader.readLine()) != null) {
		             String finalLine = line;
		             SwingUtilities.invokeLater(() -> {
		                 consoleArea.append(finalLine + "\n");
		                 consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
		             });
		         }
		
		     } catch (IOException e) {}
		 }, "ServerOutputReader");
		 consoleThread.start();
		return consoleThread;
	}
	
	public static BufferedWriter configureServerWriter(Process serverProcess, BufferedWriter serverWriter) {
		serverWriter = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
		return serverWriter;
	}
	
    public static void sendCommand(String command, Process serverProcess, BufferedWriter serverWriter) {
        if (serverProcess == null || serverProcess.isAlive()) {
            try {
                serverWriter.write(command);
                serverWriter.newLine();
                serverWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
	public static String getServerRAMAlloc(Path serverDirectory) {
        Path path = Path.of(serverDirectory + "/user_jvm_args.txt");
        List<String> lines;
		try {
			lines = Files.readAllLines(path);
		} catch (IOException e) {
			return "-Xmx1G";
		}
        
        if (!lines.isEmpty()) {
            String ultimaLinea = lines.get(lines.size() - 1);
            return ultimaLinea.replaceAll("# ", "");
        }
        return "-Xmx1G";
	}
	
	public static String getServerJarName(Path serverDirectory) {
		try (BufferedReader br = new BufferedReader(new FileReader(serverDirectory.toString() + "/run.bat"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("java")) {
                    return line.replaceAll("java -jar ", "").replaceAll(" --onlyCheckJava", "").trim();
                }
            }
        } catch (IOException e) {
            return null;
        }
		return null;
	}
	
	public static boolean checkIfExistsNetworkNameFileAndCreateIfNot() {
		if(!(Files.exists(Paths.get("data/networkName.properties")))) {
			try {
				Files.createFile(Paths.get("data/networkName.properties"));
				return false;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return true;
	}
	
	public static String getNetworkName() {
		if(checkIfExistsNetworkNameFileAndCreateIfNot()) {
			Properties props = new Properties();
			File file = new File("data/networkName.properties");
			try(FileInputStream in = new FileInputStream(file)){
				props.load(in);
				if(!(props.containsKey("networkName"))) {
					props.setProperty("networkName", "DefaultNetworkName");
				    FileOutputStream out = new FileOutputStream(file);
			        props.store(out, "Network name updated");
			        out.close();
				}
				return props.getProperty("networkName");
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible (networkName.properties)", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return "DefaultNetworkName";
	}
	
	public static void setNetworkName(String newNetworkName) {
		if(checkIfExistsNetworkNameFileAndCreateIfNot()) {
			Properties props = new Properties();
			File file = new File("data/networkName.properties");
			try(FileInputStream in = new FileInputStream(file)){
				props.load(in);
				props.setProperty("networkName", newNetworkName);
			    FileOutputStream out = new FileOutputStream(file);
		        props.store(out, "Network name updated");
		        out.close();
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public static int getServerPort(Path serverDirectory) {
		Path serverProperties = serverDirectory.resolve("server.properties");
		if(Files.exists(serverProperties)) {
			Properties props = new Properties();
			try(FileInputStream in = new FileInputStream(serverProperties.toFile())){
				props.load(in);
				return Integer.parseInt(props.getProperty("server-port"));
			} 
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return 0;
	}
	
	public static void setServerPort(Path serverDirectory, int newPort) {
		Path serverProperties = serverDirectory.resolve("server.properties");
		if(Files.exists(serverProperties)) {
			Properties props = new Properties();
			try(FileInputStream in = new FileInputStream(serverProperties.toFile())){
				props.load(in);
				props.setProperty("server-port", "" + newPort);
			    FileOutputStream out = new FileOutputStream(serverProperties.toFile());
		        props.store(out, "server properties updated");
		        out.close();
			} 
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
