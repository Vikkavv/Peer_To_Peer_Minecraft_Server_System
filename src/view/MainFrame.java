package view;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
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

import jgit.GitUtils;
import jgit.TokenStore;
import minecraftServerManagement.ForgeUtils;
import vpn.DiscoveryResponder;
import vpn.NetworkDiscoverClient;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JMenu;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingConstants;

public class MainFrame {
	
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
	private static JMenu recentServersMenu = null;
	private static JPanel contentPane = null;
	private static JMenuItem addHostingUserBtn = null;
	private static JMenuItem repoInvitationsBtn = null;
	private static JMenuItem gitSignOutBtn = null;
	private static JMenuItem gitHubProfileBtn = null;
	private static JMenuItem cloneRepoBtn = null;
	
	public static File serverOpenedDirectory = null;
	public static BufferedWriter serverWriter = null;
	public static Process serverProcess = null;
	public static boolean serverIsOn = false;

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		ThemeManager.setupSystemTheme();
		
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
		

        List<Image> icons = List.of(
            new ImageIcon(MainFrame.class.getResource("/icons/P2PMSSIcon-16.png")).getImage(),
            new ImageIcon(MainFrame.class.getResource("/icons/P2PMSSIcon-32.png")).getImage(),
            new ImageIcon(MainFrame.class.getResource("/icons/P2PMSSIcon-64.png")).getImage()
        );

        frame.setIconImages(icons);
		
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
		JMenu gitMenu = new JMenu("Git");
		menuBar.add(fileMenu);
		menuBar.add(gitMenu);
		
		addHostingUserBtn = new JMenuItem("Add hosting user");
		addHostingUserBtn.addActionListener(addhstngUsrBtn -> {
			GitWindows.addHostingUser();
		});
		
		JMenuItem gitSignInBtn = new JMenuItem("Sign into GitHub");
		gitSignInBtn.addActionListener(gitLis ->{
			GitWindows.signIntoGitHubWnd();
			gitSignOutBtn.setVisible(true);
			repoInvitationsBtn.setVisible(true);
			gitHubProfileBtn.setVisible(true);
			cloneRepoBtn.setVisible(true);
			if(serverOpenedDirectory != null) {
				if(!GitUtils.repoExistInPath(serverOpenedDirectory.toPath())) {
					addHostingUserBtn.setVisible(false);
				}
				else {
					addHostingUserBtn.setVisible(true);
				}
			}
		});
		
		gitSignOutBtn = new JMenuItem("Sign out");
		gitSignOutBtn.addActionListener(gitOut -> {
			TokenStore.clear();
			gitSignOutBtn.setVisible(false);
			repoInvitationsBtn.setVisible(false);
			gitHubProfileBtn.setVisible(false);
			addHostingUserBtn.setVisible(false);
			cloneRepoBtn.setVisible(false);
		});
		
		repoInvitationsBtn = new JMenuItem("Server Invitations");
		repoInvitationsBtn.addActionListener(rpInvt -> {
			GitWindows.invitationslistWnd();
		});
		
		gitHubProfileBtn = new JMenuItem("Profile");
		gitHubProfileBtn.addActionListener(prfBtn -> {
			GitWindows.gitHubProfileWnd();
		});
		
		cloneRepoBtn = new JMenuItem("clone a server repo");
		cloneRepoBtn.addActionListener(clnRpBtn -> {
			GitWindows.cloneRepoWnd(frame);
		});
		
		if(!TokenStore.sessionIsOpened()) {
			gitSignOutBtn.setVisible(false);
			repoInvitationsBtn.setVisible(false);
			gitHubProfileBtn.setVisible(false);
			addHostingUserBtn.setVisible(false);
			cloneRepoBtn.setVisible(false);
		}
		
		gitMenu.add(gitSignInBtn);
		gitMenu.add(gitHubProfileBtn);
		gitMenu.add(cloneRepoBtn);
		gitMenu.add(addHostingUserBtn);
		gitMenu.add(repoInvitationsBtn);
		gitMenu.add(gitSignOutBtn);
		
		contentPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		openServerOptions(contentPane);
		frame.getContentPane().add(contentPane);
		if(serverOpenedDirectory != null) {
			actualServerPort = ForgeUtils.getServerPort(serverOpenedDirectory.toPath());
			checkServerStatus();
		}
		contentPane.setVisible(true);
		
		JMenuItem btnNewMinecraftServer = new JMenuItem("New Minecraft Server");
		btnNewMinecraftServer.setHorizontalAlignment(SwingConstants.LEFT);
		btnNewMinecraftServer.addActionListener(mcSrv -> {
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
		});
		
		JMenuItem openServerFolderBtn = new JMenuItem("Open Server Folder"); //20
		openServerFolderBtn.setHorizontalAlignment(SwingConstants.LEFT);
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
		
		recentServersMenu = new JMenu("Recent files...");
		recentServerListGenerator();
		
		fileMenu.add(openServerFolderBtn);
		fileMenu.add(btnNewMinecraftServer);
		fileMenu.add(recentServersMenu);
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
					JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
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
		if(!GitUtils.repoExistInPath(serverOpenedDirectory.toPath())) {
			addHostingUserBtn.setVisible(false);
		}
		else {
			addHostingUserBtn.setVisible(true);
			if(TokenStore.sessionIsOpened() && GitUtils.isRemoteRepoHeadFordward(serverOpenedDirectory.toPath()))
				GitUtils.pull(serverOpenedDirectory.toPath());
		}
		
		JPanel content = new JPanel(new BorderLayout());
		JPanel consoleContent = new JPanel(new BorderLayout());
		JPanel topContent = new JPanel(new BorderLayout());
		JPanel leftContent = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel rightContent = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JLabel serverName = new JLabel("Server: " + getServerName());
		turnOnOffBtn = new JButton("On");
		turnOnOffBtn.setEnabled(false);
		
		
		turnOnOffBtn.addActionListener(trnOnOffBtn -> {
			if(!serverIsOn) {
				String networkDiscoveryResult = NetworkDiscoverClient.surroundDiscoverIOException(networkName, actualServerPort, 3000);
				if(networkDiscoveryResult != "NotFound") {
			        JOptionPane.showMessageDialog(
			                null,                   
			                "Server already opened by other host, if you really want to turn it on, change the networkName in configuration menu.",
			                "Info",
			                JOptionPane.INFORMATION_MESSAGE
			        );
			        turnOnOffBtn.setEnabled(false);
					return;
				}
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
//							if(TokenStore.sessionIsOpened() && GitUtils.repoExistInPath(serverOpenedDirectory.toPath())) GitUtils.activeAutoSave();
							content.add(consoleContent, BorderLayout.SOUTH);
							consoleContent.revalidate();
							consoleContent.repaint();
							checkServerStatus();
						}
					}catch(Exception e) {} 
				}).start();
			}
			else {
				fatherFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				ForgeUtils.sendCommand("/stop", serverProcess, serverWriter);
				new Thread(() ->{
					try { serverProcess.waitFor();} catch (InterruptedException e) {}
					if(TokenStore.sessionIsOpened() && GitUtils.repoExistInPath(serverOpenedDirectory.toPath())) GitUtils.autoCommitAndPush(true);
					
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
					fatherFrame.setCursor(Cursor.getDefaultCursor());
					
				}).start();
			}
		});
		
		
		JButton openServerModsFolderBtn = new JButton("Open Mods Folder");
		openServerModsFolderBtn.addActionListener(mds -> {
			ForgeUtils.openModsFolder(serverOpenedDirectory.toPath());
		});
		
		JButton serverConfigBtn = new JButton("Configurations");
		serverConfigBtn.addActionListener(conf -> {
			serverConfigsFrame(fatherFrame);
		});
		
		JButton createServerRepoBtn = new JButton("Create repository");
		createServerRepoBtn.addActionListener(repo -> {
			fatherFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			//First we create a local repository in the server path
			boolean localRepoCreated = GitUtils.createRepoIfNotExistsInPath(serverOpenedDirectory.toPath());
			if(localRepoCreated) { //If created successfully..
				
				//We retrieve the GitHub token if session is signed
				String token;
				try {token = TokenStore.loadToken();} 
				catch(Exception e) {JOptionPane.showMessageDialog(null, "The GitHub token is not correct, invalid or nonexistent, consider sign in again", "Error", JOptionPane.ERROR_MESSAGE); return;}
				
				//We create the repository in GitHub using the official API
				String json = GitUtils.createRepoInGitHub(token, getServerName());
				//Convert the String json that returns to a Map to access easily to the key-value fields.
				Map<String, String> responseMap = GitUtils.convertJsonStringToMap(json);
				//We link the local Git repository to the external GitHub repository.
				boolean repoCreatedCorrectly = GitUtils.linkLocalRepoToExternal(responseMap.get("clone_url"), token, serverOpenedDirectory.toPath());
				
				//We skip this two files in the workTree to conserve the local configuration of each user that host the server in this repository.
				GitUtils.setSkipWorktree(serverOpenedDirectory.toPath(), Path.of(serverOpenedDirectory.toString() + "/server.properties"), true);
				GitUtils.setSkipWorktree(serverOpenedDirectory.toPath(), Path.of(serverOpenedDirectory.toString() + "/user_jvm_args.txt"), true);
				
				if(repoCreatedCorrectly) {
					fatherFrame.setCursor(Cursor.getDefaultCursor());
			        JOptionPane.showMessageDialog(
			                frame,                   
			                "Repo created and linked successfully!",
			                "Git",
			                JOptionPane.INFORMATION_MESSAGE
			        );
			        openServerOptions(fatherFrame);
			        return;
				}
				else {
					JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		ipServerHostingPane = new JTextPane();
		ipServerHostingPane.setEditable(false);
		ipServerHostingPane.setVisible(false);
		
		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(rfshBtnE -> {
			fatherFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			checkServerStatus();
			fatherFrame.setCursor(Cursor.getDefaultCursor());
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
		if(!GitUtils.repoExistInPath(serverOpenedDirectory.toPath()))
			rightContent.add(createServerRepoBtn);
		rightContent.add(serverConfigBtn);
		checkServerStatus();
		fatherFrame.revalidate();
		fatherFrame.repaint();
		if(recentServersMenu != null) recentServerListGenerator();
	}
	
	private void saveAndClose() {
		if(serverIsOn) {
	        JOptionPane.showMessageDialog(
	                frame,                   
	                "Guardando mundo y cerrando servidor",
	                "Server activo",
	                JOptionPane.INFORMATION_MESSAGE
	        );
	        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        ForgeUtils.sendCommand("/stop", serverProcess, serverWriter);
	        try { 
	            serverProcess.waitFor();
	            if(TokenStore.sessionIsOpened() && GitUtils.repoExistInPath(serverOpenedDirectory.toPath())) GitUtils.autoCommitAndPush(true);
	            SwingUtilities.invokeLater(() -> {
		            frame.setCursor(Cursor.getDefaultCursor());

	            });
	        } catch (InterruptedException e) {
	            e.printStackTrace();
            }
	    }
		
	    frame.dispose();
	    System.exit(0);
	}
	
	private void serverConfigsFrame(JPanel fatherFrame) {
		JDialog configDialog = new JDialog(frame, "Server Configurations");
		configDialog.getContentPane().setLayout(new BorderLayout());
		configDialog.setResizable(false);
		int configDialogWidht = 300;
		int configDialogHeight = 240;
		configDialog.setSize(configDialogWidht, configDialogHeight);
		configDialog.setLocationRelativeTo(fatherFrame);
		configDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		//Array for the autoSaveSelect
		String[] autoSaveIntervalsTexts = { "5 mins", "10 mins", "30 mins", "1 h", "2 h" };
		int[] autoSaveIntervalsInts = { 5 * 60, 10 * 60, 30 * 60, 1 * 60 * 60, 2 * 60 * 60 };
		
		JPanel contentPane = new JPanel(new GridLayout(8, 1));
		JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JScrollPane scroll = new JScrollPane(contentPane);
		scroll.setPreferredSize(new Dimension(300, 165));
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JLabel networkIDLabel = new JLabel("Nombre de la red");
		JTextField networkIDInput = new JTextField();
		JLabel serverPortLabel = new JLabel("Server port");
		JTextField serverPortInput = new JTextField();
		JLabel serverRamAllocLabel = new JLabel("RAM (GB or MB)");
		JTextField serverRamAllocInput = new JTextField();
		
		JLabel autoSaveIntervalLabel = new JLabel("Intervalo del autoguardado");
		JComboBox<String> autoSaveIntervalSelect = new JComboBox<String>(autoSaveIntervalsTexts);
		autoSaveIntervalLabel.setVisible(false);
		autoSaveIntervalSelect.setVisible(false);
		
		autoSaveIntervalSelect.setSelectedIndex(Arrays.binarySearch(autoSaveIntervalsInts, GitUtils.getSavedAutoSaveInteval()));
		JButton saveBtn = new JButton("Save");
		
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		scroll.setBorder(null);
		networkIDInput.setText(ForgeUtils.getNetworkName());
		serverPortInput.setText(ForgeUtils.getServerPort(serverOpenedDirectory.toPath())+"");
		serverRamAllocInput.setText(ForgeUtils.getServerRAMAlloc(serverOpenedDirectory.toPath()).replaceAll("[-Xmx|G|M]",""));
		
		contentPane.add(networkIDLabel);
		contentPane.add(networkIDInput);
		contentPane.add(serverPortLabel);
		contentPane.add(serverPortInput);
		contentPane.add(serverRamAllocLabel);
		contentPane.add(serverRamAllocInput);
		contentPane.add(autoSaveIntervalLabel);
		contentPane.add(autoSaveIntervalSelect);
		buttonsPane.add(saveBtn);
		configDialog.getContentPane().add(scroll, BorderLayout.NORTH);
		configDialog.getContentPane().add(buttonsPane, BorderLayout.SOUTH);
		
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
			if(!((ForgeUtils.getServerRAMAlloc(serverOpenedDirectory.toPath())+"").replaceAll("[-Xmx|G|M]", "").equals(serverRamAllocInput.getText().replaceAll("[-Xmx|G|M]", "")))) {
				Pattern pattern = Pattern.compile("^[0-9]*$");
				Matcher matcher = pattern.matcher(serverRamAllocInput.getText().replaceAll("[-Xmx|G|M]", ""));
				if(matcher.find()) {
					try {
						ForgeUtils.setServerRAMAlloc(serverOpenedDirectory.toPath(), Integer.parseInt(serverRamAllocInput.getText().replaceAll("[-Xmx|G|M]", "")));
					}
					catch(Exception ramExpection) {
						serverRamAllocLabel.setText("<html>RAM (GB or MB) <span style='color:#fa4545'>Memoria libre insuficiente</span></html>");
						return;
					}
				}
			}
			int selectedAutosaveInteval = autoSaveIntervalsInts[autoSaveIntervalSelect.getSelectedIndex()];
			if(GitUtils.getSavedAutoSaveInteval() != selectedAutosaveInteval) {
				GitUtils.setAutoSaveInterval(selectedAutosaveInteval);
			}
			configDialog.dispose();
		});
	}
	
	public static void checkIfExistsDataFolder() {
		if(!(Files.exists(Paths.get("data")))) {
			try {
				Files.createDirectory(Paths.get("data"));
			}
			catch(IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void recentServerListGenerator() {
		recentServersMenu.removeAll();
		File file = new File("data/recentServers.properties");
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(file)) {
			props.load(in);
			if(props.containsKey("recentServers")) {
				for(String serverDirectory : props.getProperty("recentServers").split("\\|")) {
					JMenuItem item = new JMenuItem(serverDirectory);
					item.addActionListener(itm -> {
						serverOpenedDirectory = new File(serverDirectory);
						SwingUtilities.invokeLater(() -> {
							openServerOptions(contentPane);
						});
					});
					recentServersMenu.add(item);
				}
			}
			else { 
				recentServersMenu.add(new JMenuItem("No recent files opened..."));
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
		}
		recentServersMenu.revalidate();
		recentServersMenu.repaint();
	}
	
	public static String getServerName() {
	   return serverOpenedDirectory.toString().substring(serverOpenedDirectory.toString().lastIndexOf("\\") + 1, serverOpenedDirectory.toString().length());
	}

}
