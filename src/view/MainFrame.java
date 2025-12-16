package view;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import minecraftServerManagement.ForgeUtils;
import vpn.DiscoveryResponder;
import vpn.NetworkDiscoverClient;

import java.awt.BorderLayout;
import java.awt.Cursor;

import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JMenu;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class MainFrame {
	
	private static File serverOpenedDirectory = null;
	private static File newMinecraftServerDirectory = null;
	private static String forgeMetadata = null;
	private static String forgeVersion = null;
	private static JButton turnOnOffBtn = null;
	private static JTextPane ipServerHostingPane = null;
	private static JTextArea consoleArea = null;
	private static Thread consoleThread = null;
	private static JTextField comandInput = null;
	private static String networkName = null;
	private static int actualServerPort = 0;
	private static DiscoveryResponder responder = null;
	
	public static BufferedWriter serverWriter = null;
	public static Process serverProcess = null;
	public static boolean serverIsOn = false;

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame window = new MainFrame();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		
		checkIfExistsDataFolder();
		//Initialize networkName
		networkName = ForgeUtils.getNetworkName();
		
		frame = new JFrame();
		int frameWidht = 750;
		int frameHeight = 450;
		frame.setBounds((Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (frameWidht / 2), (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (frameHeight / 2), frameWidht, frameHeight);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		frame.setTitle("Peer To Peer Minecraft Server System");

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            	saveAndClose();
            }
        });
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.NORTH);

		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorder(null);
		menuBar.setBorderPainted(false);
		frame.setJMenuBar(menuBar);
		
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		JPanel contentPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		openServerOptions(contentPane);
		frame.add(contentPane);
		if(serverOpenedDirectory != null) {
			actualServerPort = ForgeUtils.getServerPort(serverOpenedDirectory.toPath());
			checkServerStatus();
		}
		contentPane.setVisible(true);
		
		JButton btnNewMinecraftServer = new JButton("New Minecraft Server");
		btnNewMinecraftServer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int result = fileChooser.showOpenDialog(frame);
				if(result == JFileChooser.APPROVE_OPTION) {
					newMinecraftServerDirectory = fileChooser.getSelectedFile();
					if(newMinecraftServerDirectory.isDirectory() && newMinecraftServerDirectory.list().length != 0) {
			            JOptionPane.showMessageDialog(
			                   panel,
			                    "Debe seleccionar un directorio vacío.",
			                    "Error",
			                    JOptionPane.ERROR_MESSAGE
			            );
					}
					else {
						// We create a new window to select the Minecraft version to create a Forge server.
						JDialog versionSelectFrame = new JDialog();
						versionSelectFrame.setResizable(false);
						int versionSelectWidht = 500;
						int versionSelectHeight = 300;
						versionSelectFrame.setSize(versionSelectWidht, versionSelectHeight);
						versionSelectFrame.setLocationRelativeTo(panel);
						versionSelectFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						versionSelectFrame.setVisible(true);
						//We create a JPanel to dispose all the elements of the UI interface.
						JPanel versionSelectPanel = new JPanel(new BorderLayout());
						versionSelectFrame.getContentPane().add(versionSelectPanel);
						
						JPanel contentPanel = new JPanel();
						JLabel label = new JLabel("Selecciona la version de minecraft y Forge");
						JPanel selectsPanel = new JPanel(new GridBagLayout());
						GridBagConstraints gbc = new GridBagConstraints();
						gbc.fill = GridBagConstraints.HORIZONTAL;
						gbc.insets = new Insets(80, 35, 80, 35);
						
						//We collect all the versions to show them in the selects.
						forgeMetadata = ForgeUtils.downloadForgeMetadata();
						
						JComboBox<String> minecraftVersionsSelect = new JComboBox<String>(ForgeUtils.getMinecraftVersionsList(forgeMetadata).toArray(new String[0]));
						JComboBox<String> forgeVersionsSelect = new JComboBox<String>();
						minecraftVersionsSelect.addActionListener(fgs -> {
								forgeVersionsSelect.setVisible(false);
								forgeVersion = null;
								forgeVersionsSelect.removeAllItems();
								if(!(minecraftVersionsSelect.getSelectedItem().toString().equals("Select Minecraft version"))) {
									for(String version : ForgeUtils.getForgeVersionsForMinecraftVersion(minecraftVersionsSelect.getSelectedItem().toString(), ForgeUtils.getForgeVersionsList(forgeMetadata))) {
										forgeVersionsSelect.addItem(version);
									}
									forgeVersionsSelect.setVisible(true);
								}
								else forgeVersionsSelect.setVisible(false);
						});
						
						gbc.gridx = 1;
						gbc.weightx = 0.5;
						selectsPanel.add(forgeVersionsSelect, gbc);
						gbc.gridx = 0;
						gbc.gridy = 0;
						gbc.weightx = 0.5;
						selectsPanel.add(minecraftVersionsSelect, gbc);
						contentPanel.add(label, FlowLayout.LEFT);
						contentPanel.add(selectsPanel, BorderLayout.SOUTH);
						versionSelectPanel.add(contentPanel, BorderLayout.CENTER);
						
						//We create a wrapper for the buttons.
						JPanel buttonsWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
						//Buttons...
						JButton versionAcceptButton = new JButton("Aceptar");
						versionAcceptButton.setEnabled(false);
						
						forgeVersionsSelect.addActionListener(fgs -> {
							if(forgeVersionsSelect.isVisible()) {
								forgeVersion = forgeVersionsSelect.getSelectedItem().toString();
								if(forgeVersion != null && !(forgeVersion.equals("Select a Forge version"))) {
									versionAcceptButton.setEnabled(true);
								}
								else versionAcceptButton.setEnabled(false);
							}
						});
						versionAcceptButton.addActionListener(btnL -> {
							versionSelectFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							Path forgeInstallerPath = ForgeUtils.downloadForgeInstaller(forgeVersion);
							ForgeUtils.installForgeServer(forgeInstallerPath, newMinecraftServerDirectory.toPath());
							versionSelectFrame.setCursor(Cursor.getDefaultCursor());
							Object[] buttonsOptions = {"Open EULA", "Cancel", "Accept"};
				            int opt = JOptionPane.showOptionDialog(
				                    null, 
				                    "By pressing accept you are agreeding the terms and conditions of MinecraftEula", 
				                    "Do you want to accept the EULA?",
				                    JOptionPane.INFORMATION_MESSAGE,
				                    JOptionPane.DEFAULT_OPTION,
				                    null,
				                    buttonsOptions,
				                    buttonsOptions[0]
				            );
				            if(opt == 2) {
				            	boolean eulaAccepted = ForgeUtils.acceptEULA(newMinecraftServerDirectory.toPath());
				            	if(eulaAccepted) {
				            		Object[] finalButton = {"Accept"};
						            int opt1 = JOptionPane.showOptionDialog(
					            		null,
					            		"Server installed correctly",
					            		"Successful!",
					                    JOptionPane.INFORMATION_MESSAGE,
					                    JOptionPane.DEFAULT_OPTION,
					                    null,
					                    finalButton,
					                    finalButton[0]
		                    		);
						            if(opt1 == 0) versionSelectFrame.dispose();
				            	}
				            }
				            if(opt == 1) {
				            	forgeVersion = null;
				            	newMinecraftServerDirectory = null;
				            }
				            if(opt == 0) {
				            	ForgeUtils.openURL("https://aka.ms/MinecraftEULA");
				            }
						});
						
						JButton versionCancelButton = new JButton("Cancelar");
						buttonsWrapper.add(versionCancelButton);
						buttonsWrapper.add(versionAcceptButton);
						
						versionCancelButton.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								versionSelectFrame.dispose();
							}
						});
						
						versionSelectPanel.add(buttonsWrapper, BorderLayout.SOUTH);
				
						versionSelectFrame.setVisible(true);
						forgeVersionsSelect.setVisible(false);
					}
				}
				
				if(result == JFileChooser.CANCEL_OPTION) newMinecraftServerDirectory = null;
			}
		});
		
		JButton openServerFolderBtn = new JButton("Open Server Folder");
		openServerFolderBtn.addActionListener(opSer -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(frame);
			if(result == JFileChooser.APPROVE_OPTION) {
				serverOpenedDirectory = fileChooser.getSelectedFile();
				actualServerPort = ForgeUtils.getServerPort(serverOpenedDirectory.toPath());
				if(serverOpenedDirectory.isDirectory()) {
					if(Files.exists(Paths.get(serverOpenedDirectory.toPath().toString()+"/run.bat"))) {
						openServerOptions(contentPane);
						Properties props = new Properties();
						if(!(Files.exists(Paths.get("data/recentServers.properties")))) {
							try {
								Files.createFile(Paths.get("data/recentServers.properties"));
							} catch (IOException e) {
								JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
							}
						}
						File file = new File("data/recentServers.properties");
						try(FileInputStream in = new FileInputStream(file)){
							props.load(in);
							String recentServers = "";
							if(props.containsKey("recentServers"))
								//If the path is not included yet, we add them at the beginning, otherwise, we search the string-path in the value and move it to the beginning as it is the most recent folder opened.
								if(!(props.get("recentServers").toString().contains(serverOpenedDirectory.toPath().toString().replaceAll("\\\\", "/")))) recentServers = serverOpenedDirectory.toPath().toString().replaceAll("\\\\", "/") + "|" +  props.getProperty("recentServers");
								else recentServers = serverOpenedDirectory.toPath().toString().replaceAll("\\\\", "/") + "|" + props.getProperty("recentServers").replaceAll(serverOpenedDirectory.toPath().toString().replaceAll("\\\\", "/"), "").replace("||", "|").replaceAll("[|\\n]", "");
							else recentServers = serverOpenedDirectory.toPath().toString().replaceAll("\\\\", "/");
							props.setProperty("recentServers", recentServers);
						    FileOutputStream out = new FileOutputStream(file);
					        props.store(out, "Updated recent servers");
					        out.close();
						}
						catch(IOException e) {
							JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
					else JOptionPane.showMessageDialog(null, "Select a minecraft server folder", "Error", JOptionPane.ERROR_MESSAGE);
				}
				else {
					serverOpenedDirectory = null;
					JOptionPane.showMessageDialog(null, "The selected destination must be a server directory", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		fileMenu.add(openServerFolderBtn);
		fileMenu.add(btnNewMinecraftServer);
	}
	
	private void checkServerStatus() {
		String networkDiscoveryResult = NetworkDiscoverClient.surroundDiscoverIOException(networkName, actualServerPort, 3000);
		if(networkDiscoveryResult != "NotFound") {
			ipServerHostingPane.setText("Server ip: " + networkDiscoveryResult);
			if(!serverIsOn) turnOnOffBtn.setEnabled(false);
		}
		else {
			ipServerHostingPane.setText("Server is off");
			turnOnOffBtn.setEnabled(true);
		}
		ipServerHostingPane.setVisible(true);
	}
	
	private void openServerOptions(JPanel fatherFrame) {
		fatherFrame.removeAll();
		if(serverOpenedDirectory == null) {
			Properties props = new Properties();
			if(!(Files.exists(Paths.get("data/recentServers.properties")))) {
				try {
					Files.createFile(Paths.get("data/recentServers.properties"));
					fatherFrame.add(new JLabel("Open a minecraft server folder"));
					return;
				} catch (IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "File not found or inaccessible (recentServers.properties)", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			else {
				File file = new File("data/recentServers.properties");
				try(FileInputStream in = new FileInputStream(file)) {
					props.load(in);
					if(props.containsKey("recentServers")) {
						serverOpenedDirectory = new File(props.getProperty("recentServers").split("\\|")[0]);
					}
					else { 
						fatherFrame.add(new JLabel("Open a minecraft server folder"));
						return;
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		JPanel content = new JPanel(new BorderLayout());
		JPanel consoleContent = new JPanel(new BorderLayout());
		JPanel topContent = new JPanel(new BorderLayout());
		JPanel leftContent = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel rightContent = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JLabel serverName = new JLabel("Server: " + serverOpenedDirectory.toString().substring(serverOpenedDirectory.toString().lastIndexOf("\\") + 1, serverOpenedDirectory.toString().length()));
		turnOnOffBtn = new JButton("On");
		turnOnOffBtn.setEnabled(false);
		
		
		turnOnOffBtn.addActionListener(trnOnOffBtn -> {
			if(!serverIsOn) {
				String serverNameString = serverName.getText();
				serverName.setText(serverNameString + " - Server is turning on...");
				topContent.revalidate();
				topContent.repaint();
				fatherFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				new Thread(() -> {
					try {
						serverProcess = ForgeUtils.executeMinecraftServer(serverOpenedDirectory.toPath());
						responder = new DiscoveryResponder(networkName).listenAsync(actualServerPort);
						//If is done...
						if(serverProcess != null) {
							fatherFrame.setCursor(Cursor.getDefaultCursor());
							serverName.setText(serverNameString);
							turnOnOffBtn.setText("Off");
							serverIsOn = true;
							consoleArea = new JTextArea(15, 60);
							JScrollPane scroll = new JScrollPane(consoleArea);
							consoleArea.setEditable(false);
							comandInput = new JTextField();
							comandInput.setColumns(20);
							consoleContent.add(scroll, BorderLayout.CENTER);
							consoleContent.add(comandInput, BorderLayout.SOUTH);
							comandInput.addActionListener(msg -> {
								ForgeUtils.sendCommand(comandInput.getText(), serverProcess, serverWriter);
								comandInput.setText("");
							});
							consoleThread = ForgeUtils.getServerOutputs(serverProcess, consoleArea);
							serverWriter = ForgeUtils.configureServerWriter(serverProcess, serverWriter);
							content.add(consoleContent, BorderLayout.SOUTH);
							consoleContent.revalidate();
							consoleContent.repaint();
							checkServerStatus();
						}
					}catch(Exception e) {} 
				}).start();
			}
			else {
				ForgeUtils.sendCommand("/stop", serverProcess, serverWriter);
				new Thread(() ->{
					
					try { serverProcess.waitFor();} catch (InterruptedException e) {}
					
					consoleThread.interrupt();
					serverProcess = null;
					
					SwingUtilities.invokeLater(() -> {
						consoleContent.removeAll();
						consoleArea = null;
						comandInput = null;
						serverWriter = null;
						serverIsOn = false;
						turnOnOffBtn.setText("On");
						responder.closeListeningSocket();
						checkServerStatus();
					});
					
				}).start();
			}
		});
		
		
		JButton openServerModsFolderBtn = new JButton("Open Mods Folder");
		openServerModsFolderBtn.addActionListener(mds -> {
			ForgeUtils.openModsFolder(serverOpenedDirectory.toPath());
		});
		
		JButton serverConfigBtn = new JButton("Configuration");
		serverConfigBtn.addActionListener(conf -> {
			serverConfigsFrame(fatherFrame);
		});
		
		JButton createServerRepoBtn = new JButton();
		ipServerHostingPane = new JTextPane();
		ipServerHostingPane.setEditable(false);
		ipServerHostingPane.setVisible(false);
		
		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(rfshBtnE -> {
			checkServerStatus();
		});
		
		fatherFrame.add(content, BorderLayout.CENTER);
		content.add(leftContent, BorderLayout.WEST);
		content.add(rightContent, BorderLayout.EAST);
		content.add(topContent, BorderLayout.NORTH);
		topContent.add(serverName);
		leftContent.add(turnOnOffBtn);
		rightContent.add(ipServerHostingPane);
		rightContent.add(refreshBtn);
		rightContent.add(openServerModsFolderBtn);
		rightContent.add(serverConfigBtn);
		checkServerStatus();
		fatherFrame.revalidate();
		fatherFrame.repaint();
	}
	
	private void saveAndClose() {
		if(serverIsOn) {
	        JOptionPane.showMessageDialog(
	                frame,                   
	                "Guardando mundo y cerrando servidor",
	                "Server activo",
	                JOptionPane.INFORMATION_MESSAGE
	        );
	        ForgeUtils.sendCommand("/stop", serverProcess, serverWriter);
	        try { 
	            serverProcess.waitFor();
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	    frame.dispose();
	    System.exit(0);
	}
	
	private void serverConfigsFrame(JPanel fatherFrame) {
		JDialog configDialog = new JDialog(frame, "Server Configurations");
		configDialog.setLayout(new BorderLayout());
		configDialog.setResizable(false);
		int configDialogWidht = 300;
		int configDialogHeight = 200;
		configDialog.setSize(configDialogWidht, configDialogHeight);
		configDialog.setLocationRelativeTo(fatherFrame);
		configDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JPanel contentPane = new JPanel(new GridLayout(4, 1));
		JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JScrollPane scroll = new JScrollPane(contentPane);
		JLabel networkIDLabel = new JLabel("Nombre de la red");
		JTextField networkIDInput = new JTextField();
		JLabel serverPortLabel = new JLabel("Server port");
		JTextField serverPortInput = new JTextField();
		JButton saveBtn = new JButton("Save");
		
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		scroll.setBorder(null);
		networkIDInput.setText(ForgeUtils.getNetworkName());
		serverPortInput.setText(ForgeUtils.getServerPort(serverOpenedDirectory.toPath())+"");
		
		contentPane.add(networkIDLabel);
		contentPane.add(networkIDInput);
		contentPane.add(serverPortLabel);
		contentPane.add(serverPortInput);
		buttonsPane.add(saveBtn);
		configDialog.add(scroll, BorderLayout.NORTH);
		configDialog.add(buttonsPane, BorderLayout.SOUTH);
		
		configDialog.setVisible(true);
		
		saveBtn.addActionListener(save -> {
			if(!(ForgeUtils.getNetworkName().equals(networkIDInput.getText()))){
				ForgeUtils.setNetworkName(networkIDInput.getText());
				networkName = networkIDInput.getText();
			}
			if(!((ForgeUtils.getServerPort(serverOpenedDirectory.toPath())+"").equals(serverPortInput.getText()))){
				ForgeUtils.setServerPort(serverOpenedDirectory.toPath(), Integer.parseInt(serverPortInput.getText()));
				actualServerPort = Integer.parseInt(serverPortInput.getText());
			}
			configDialog.dispose();
		});
	}
	
	private void checkIfExistsDataFolder() {
		if(!(Files.exists(Paths.get("data")))) {
			try {
				Files.createDirectory(Paths.get("data"));
			}
			catch(IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible (data)", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
