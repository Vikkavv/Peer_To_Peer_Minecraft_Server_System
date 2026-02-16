package view;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cloud.CloudStorageProvider;
import cloud.ZipUtils;
import cloud.google.GoogleDriveCloudProvider;
import jgit.GitUtils;
import jgit.TokenStore;
import minecraftServerManagement.ForgeUtils;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
	private static JMenu recentServersMenu = null;
	private static JMenuItem addHostingUserBtn = null;
	private static JMenuItem repoInvitationsBtn = null;
	private static JMenuItem gitSignOutBtn = null;
	private static JMenuItem gitHubProfileBtn = null;
	private static JMenuItem cloneRepoBtn = null;
	private static JMenuItem GoogleAddHostingUserBtn = null;

	public static String networkName = null;
	public static final Path CLOUD_PROVIDER_IN_USE_PATH = Path.of("data/cloudProviderInUse.properties");
	public static CloudStorageProvider cloudProvider = null;
	public static String cloudProviderInUse = null;
	public static String cloudInUseReminderText[];
	public static JMenuItem cloudInUseReminderMenuText;
	public static MainFrame window = null;
	public static ServerPortManager portManager = new ServerPortManager();

	private JFrame frame;
	private JTabbedPane tabbedPane;
	private List<ServerTab> serverTabs = new ArrayList<>();
	private int colorIndex = 0;

	// Color palette for tabs - light/dark pairs
	private static final Color[][] TAB_COLORS = {
		{new Color(0x3B82F6), new Color(0x60A5FA)}, // Blue
		{new Color(0x10B981), new Color(0x34D399)}, // Green
		{new Color(0xEF4444), new Color(0xF87171)}, // Red
		{new Color(0xF59E0B), new Color(0xFBBF24)}, // Orange
		{new Color(0x8B5CF6), new Color(0xA78BFA)}, // Purple
		{new Color(0xEC4899), new Color(0xF472B6)}, // Pink
	};

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		ThemeManager.setupSystemTheme();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new MainFrame();
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

	private Color nextTabColor() {
		int idx = colorIndex % TAB_COLORS.length;
		colorIndex++;
		return TAB_COLORS[idx][ThemeManager.isDarkMode() ? 1 : 0];
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		checkIfExistsDataFolder();
		networkName = ForgeUtils.getNetworkName();
		if(ZipUtils.existsDirectory(Path.of("data/google_tokens/StoredCredential"))) {
			cloudProvider = new GoogleDriveCloudProvider();
			cloudProvider.authenticate();
		}
		cloudProviderInUse = ZipUtils.getDataFromPropertiesFile("cloudProviderInUse", CLOUD_PROVIDER_IN_USE_PATH);

		frame = new JFrame();
		int frameWidth = 900;
		int frameHeight = 550;
		frame.setBounds((Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (frameWidth / 2), (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (frameHeight / 2), frameWidth, frameHeight);
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

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorder(null);
		menuBar.setBorderPainted(false);
		frame.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		JMenu cloudMenu = new JMenu("Cloud");
		JMenu saveBackupsToCloudMenu = new JMenu("Save backups to cloud...");
		JMenu gitMenu = new JMenu("GitHub");
		gitMenu.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/github.png")));
		JMenu googleDriveMenu = new JMenu("Google Drive");
		googleDriveMenu.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/google-drive.png")));
		cloudInUseReminderText = new String[]{"<html><span style=' color: rgb(177, 177, 177);'>%s.</span></html>", "Currently saving backups in ", "No cloud provider configured yet", "%s choosen for saving backups, but you are not logged in"};
		cloudInUseReminderMenuText = new JMenuItem(cloudInUseReminderText[0].formatted(cloudProviderInUse != null ? ( cloudProviderInUse.equals("GitHub") && !TokenStore.sessionIsOpened() ? cloudInUseReminderText[3].formatted(cloudProviderInUse) : ( cloudProvider == null ? cloudInUseReminderText[3].formatted(cloudProviderInUse) : cloudInUseReminderText[1] + cloudProviderInUse)) : cloudInUseReminderText[2]));
		cloudInUseReminderMenuText.setEnabled(false);

		menuBar.add(fileMenu);
		menuBar.add(cloudMenu);
		cloudMenu.add(saveBackupsToCloudMenu);
		cloudMenu.add(gitMenu);
		cloudMenu.add(googleDriveMenu);
		cloudMenu.add(cloudInUseReminderMenuText);

		JRadioButtonMenuItem gitMenuItem = new JRadioButtonMenuItem("GitHub");
		gitMenuItem.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/github.png")));
		gitMenuItem.addActionListener(ghList -> {
			radioBtnListener(cloudMenu, saveBackupsToCloudMenu, gitMenuItem);
		});

		JRadioButtonMenuItem googleDriveMenuItem = new JRadioButtonMenuItem("Google Drive");
		googleDriveMenuItem.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/google-drive.png")));
		googleDriveMenuItem.addActionListener(gglList -> {
			radioBtnListener(cloudMenu, saveBackupsToCloudMenu, googleDriveMenuItem);
		});

		ButtonGroup group = new ButtonGroup();
		group.add(gitMenuItem);
		group.add(googleDriveMenuItem);

		Iterator<AbstractButton> it = group.getElements().asIterator();
		while(it.hasNext()) {
			JRadioButtonMenuItem radioBtn = (JRadioButtonMenuItem) it.next();
			if(radioBtn.getText().replaceAll(" ", "").equals(cloudProviderInUse)) radioBtn.setSelected(true);
		}

		saveBackupsToCloudMenu.add(gitMenuItem);
		saveBackupsToCloudMenu.add(googleDriveMenuItem);

		JMenuItem installInvitedServerBtn = new JMenuItem("Install invited server folder");
		installInvitedServerBtn.addActionListener(insInviServBtn -> {
			GoogleWindows.cloneServerFolderWnd(frame);
		});

		JMenuItem signOutDriveBtn = new JMenuItem("Sign out");
		JMenuItem loggedInGoogleDriveText = new JMenuItem("<html><span style='color: rgb(177, 177, 177);'>Logged in Google Drive</span></html>");
		loggedInGoogleDriveText.setEnabled(false);

		JMenuItem googleProfileBtn = new JMenuItem("Profile");
		googleProfileBtn.addActionListener(gglprf -> {
			GoogleWindows.googleProfileWnd();
		});

		GoogleAddHostingUserBtn = new JMenuItem("Add hosting user");
		GoogleAddHostingUserBtn.addActionListener(gglhtusrBtn -> {
			GoogleWindows.addHostingUser();
		});

		JMenuItem signIntoDriveBtn = new JMenuItem("Sign into Google Drive");
		signIntoDriveBtn.addActionListener(sgnggldr -> {
			cloudProvider = new GoogleDriveCloudProvider();
			new Thread(() -> {
				cloudProvider.authenticate();
			}).start();
			SwingUtilities.invokeLater(() -> {
				if(cloudProvider != null || cloudProvider.isSessionOpened()) {
					signIntoDriveBtn.setVisible(false);
					signOutDriveBtn.setVisible(true);
					loggedInGoogleDriveText.setVisible(true);
					googleProfileBtn.setVisible(true);
					installInvitedServerBtn.setVisible(true);
					if(cloudProvider.getProviderName().equals(cloudProviderInUse)) {
						cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[1] + cloudProviderInUse));
					}
				}
			});
		});

		signOutDriveBtn.addActionListener(sgntDrvBtn -> {
			String savedProviderName = cloudProvider.getProviderName();
			cloudProvider.closeSession();
			if(cloudProvider == null || !cloudProvider.isSessionOpened()) {
				signOutDriveBtn.setVisible(false);
				loggedInGoogleDriveText.setVisible(false);
				googleProfileBtn.setVisible(false);
				GoogleAddHostingUserBtn.setVisible(false);
				installInvitedServerBtn.setVisible(false);
				signIntoDriveBtn.setVisible(true);
				if(cloudProviderInUse.equals(savedProviderName)) {
					cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[3].formatted(cloudProviderInUse)));
				}
			}
		});

		if(cloudProvider == null || !cloudProvider.isSessionOpened() && cloudProvider instanceof GoogleDriveCloudProvider) {
			loggedInGoogleDriveText.setVisible(false);
			signIntoDriveBtn.setVisible(true);
			signOutDriveBtn.setVisible(false);
			googleProfileBtn.setVisible(false);
			GoogleAddHostingUserBtn.setVisible(false);
			installInvitedServerBtn.setVisible(false);
		}
		else {
			loggedInGoogleDriveText.setVisible(true);
			signIntoDriveBtn.setVisible(false);
			googleProfileBtn.setVisible(true);
			installInvitedServerBtn.setVisible(true);
		}

		googleDriveMenu.add(signIntoDriveBtn);
		googleDriveMenu.add(loggedInGoogleDriveText);
		googleDriveMenu.add(googleProfileBtn);
		googleDriveMenu.add(GoogleAddHostingUserBtn);
		googleDriveMenu.add(installInvitedServerBtn);
		googleDriveMenu.add(signOutDriveBtn);

		addHostingUserBtn = new JMenuItem("Add hosting user");
		addHostingUserBtn.addActionListener(addhstngUsrBtn -> {
			GitWindows.addHostingUser();
		});

		JMenuItem gitSignInBtn = new JMenuItem("Sign into GitHub");
		gitSignInBtn.addActionListener(gitLis ->{
			GitWindows.signIntoGitHubWnd(() -> {
				gitSignOutBtn.setVisible(true);
				repoInvitationsBtn.setVisible(true);
				gitHubProfileBtn.setVisible(true);
				cloneRepoBtn.setVisible(true);
				if(cloudProviderInUse.equals("GitHub")) {
					cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[1] + cloudProviderInUse));
				}
				ServerTab activeTab = getActiveServerTab();
				if(activeTab != null) {
					if(!GitUtils.repoExistInPath(activeTab.getServerDirectory().toPath())) {
						addHostingUserBtn.setVisible(false);
					}
					else {
						addHostingUserBtn.setVisible(true);
					}
				}
			});
		});

		gitSignOutBtn = new JMenuItem("Sign out");
		gitSignOutBtn.addActionListener(gitOut -> {
			TokenStore.clear();
			if(cloudProviderInUse.equals("GitHub")) {
				cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[3].formatted(cloudProviderInUse)));
			}
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

		// Initialize tabbed pane
		tabbedPane = new JTabbedPane();
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

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
		                   frame,
		                    "Debe seleccionar un directorio vacío.",
		                    "Error",
		                    JOptionPane.ERROR_MESSAGE
		            );
				}
				else {
					JDialog versionSelectFrame = new JDialog();
					versionSelectFrame.setResizable(false);
					int versionSelectWidht = 500;
					int versionSelectHeight = 300;
					versionSelectFrame.setSize(versionSelectWidht, versionSelectHeight);
					versionSelectFrame.setLocationRelativeTo(frame);
					versionSelectFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					versionSelectFrame.setVisible(true);
					JPanel versionSelectPanel = new JPanel(new BorderLayout());
					versionSelectFrame.getContentPane().add(versionSelectPanel);

					JPanel contentPanel = new JPanel();
					JLabel label = new JLabel("Selecciona la version de minecraft y Forge");
					JPanel selectsPanel = new JPanel(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.fill = GridBagConstraints.HORIZONTAL;
					gbc.insets = new Insets(80, 35, 80, 35);

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

					JPanel buttonsWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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

		JMenuItem openServerFolderBtn = new JMenuItem("Open Server Folder");
		openServerFolderBtn.setHorizontalAlignment(SwingConstants.LEFT);
		openServerFolderBtn.addActionListener(opSer -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(frame);
			if(result == JFileChooser.APPROVE_OPTION) {
				File selectedDir = fileChooser.getSelectedFile();
				if(selectedDir.isDirectory()) {
					if(Files.exists(Paths.get(selectedDir.toPath().toString()+"/run.bat")) || Files.exists(Paths.get(selectedDir.toPath().toString()+"/start.bat"))) {
						openServerInNewTab(selectedDir);
						saveToRecentServers(selectedDir);
					}
					else JOptionPane.showMessageDialog(null, "Select a minecraft server folder", "Error", JOptionPane.ERROR_MESSAGE);
				}
				else {
					JOptionPane.showMessageDialog(null, "The selected destination must be a server directory", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		recentServersMenu = new JMenu("Recent files...");
		recentServerListGenerator();

		JMenuItem generalConfigurationsMenuItem = new JMenuItem("General configurations");
		generalConfigurationsMenuItem.addActionListener(gncnf -> {
			GeneralConfigurationsWindows.generalConfigurations();
		});

		JMenuItem toggleThemeBtn = new JMenuItem(ThemeManager.isDarkMode() ? "Switch to Light Mode" : "Switch to Dark Mode");
		toggleThemeBtn.addActionListener(thm -> {
			ThemeManager.toggleTheme();
			toggleThemeBtn.setText(ThemeManager.isDarkMode() ? "Switch to Light Mode" : "Switch to Dark Mode");
		});

		fileMenu.add(openServerFolderBtn);
		fileMenu.add(btnNewMinecraftServer);
		fileMenu.add(recentServersMenu);
		fileMenu.add(generalConfigurationsMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(toggleThemeBtn);

		// Auto-open the most recent server if available
		File lastServer = getLastOpenedServer();
		if (lastServer != null) {
			openServerInNewTab(lastServer);
		}
	}

	public void openServerInNewTab(File serverDir) {
		// Check if a tab for this directory already exists
		for (int i = 0; i < serverTabs.size(); i++) {
			if (serverTabs.get(i).getServerDirectory().getAbsolutePath().equals(serverDir.getAbsolutePath())) {
				tabbedPane.setSelectedIndex(i);
				return;
			}
		}

		Color tabColor = nextTabColor();
		ServerTab tab = new ServerTab(serverDir, tabColor);
		JPanel panel = tab.buildServerPanel();
		serverTabs.add(tab);

		int index = tabbedPane.getTabCount();
		tabbedPane.addTab(tab.getServerName(), panel);
		tabbedPane.setTabComponentAt(index, createTabComponent(tab));
		tabbedPane.setSelectedIndex(index);

		recentServerListGenerator();
	}

	private JPanel createTabComponent(ServerTab tab) {
		JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		tabComponent.setOpaque(false);

		JLabel titleLabel = new JLabel(tab.getServerName());
		titleLabel.setForeground(tab.getTabColor());
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

		JButton closeBtn = new JButton("\u00D7");
		closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
		closeBtn.setBorderPainted(false);
		closeBtn.setContentAreaFilled(false);
		closeBtn.setFocusPainted(false);
		closeBtn.setPreferredSize(new Dimension(20, 20));
		closeBtn.addActionListener(e -> closeServerTab(tab));

		tabComponent.add(titleLabel);
		tabComponent.add(closeBtn);
		return tabComponent;
	}

	public void closeServerTab(ServerTab tab) {
		if (tab.isServerOn()) {
			int confirm = JOptionPane.showConfirmDialog(frame,
					"Server '%s' is still running. Stop it and close the tab?".formatted(tab.getServerName()),
					"Confirm close",
					JOptionPane.YES_NO_OPTION);
			if (confirm != JOptionPane.YES_OPTION) return;
			tab.turnOffServer();
		}

		int index = serverTabs.indexOf(tab);
		if (index >= 0) {
			serverTabs.remove(index);
			tabbedPane.removeTabAt(index);
		}
	}

	public ServerTab getActiveServerTab() {
		int index = tabbedPane.getSelectedIndex();
		if (index >= 0 && index < serverTabs.size()) {
			return serverTabs.get(index);
		}
		return null;
	}

	private void saveAndClose() {
		// Check if any servers are running
		boolean anyRunning = serverTabs.stream().anyMatch(ServerTab::isServerOn);
		if (anyRunning) {
	        JOptionPane.showMessageDialog(
	                frame,
	                "Guardando mundos y cerrando servidores activos",
	                "Servidores activos",
	                JOptionPane.INFORMATION_MESSAGE
	        );
	        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        for (ServerTab tab : serverTabs) {
	        	if (tab.isServerOn()) {
	        		tab.turnOffServerSync();
	        	}
	        }
	        frame.setCursor(Cursor.getDefaultCursor());
	    }

	    frame.dispose();
	    System.exit(0);
	}

	void serverConfigsFrame(JPanel fatherFrame, ServerTab tab) {
		JDialog configDialog = new JDialog(frame, "Server Configurations");
		configDialog.getContentPane().setLayout(new BorderLayout());
		configDialog.setResizable(false);
		int configDialogWidht = 300;
		int configDialogHeight = 240;
		configDialog.setSize(configDialogWidht, configDialogHeight);
		configDialog.setLocationRelativeTo(fatherFrame);
		configDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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

		File serverDir = tab.getServerDirectory();
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		scroll.setBorder(null);
		networkIDInput.setText(ForgeUtils.getNetworkName());
		serverPortInput.setText(ForgeUtils.getServerPort(serverDir.toPath())+"");
		serverRamAllocInput.setText(ForgeUtils.getServerRAMAlloc(serverDir.toPath()).replaceAll("[-Xmx|G|M]",""));

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
			if(!((ForgeUtils.getServerPort(serverDir.toPath())+"").equals(serverPortInput.getText()))){
				int newPort = Integer.parseInt(serverPortInput.getText());
				ForgeUtils.setServerPort(serverDir.toPath(), newPort);
				tab.setServerPort(newPort);
			}
			if(!((ForgeUtils.getServerRAMAlloc(serverDir.toPath())+"").replaceAll("[-Xmx|G|M]", "").equals(serverRamAllocInput.getText().replaceAll("[-Xmx|G|M]", "")))) {
				Pattern pattern = Pattern.compile("^[0-9]*$");
				Matcher matcher = pattern.matcher(serverRamAllocInput.getText().replaceAll("[-Xmx|G|M]", ""));
				if(matcher.find()) {
					try {
						ForgeUtils.setServerRAMAlloc(serverDir.toPath(), Integer.parseInt(serverRamAllocInput.getText().replaceAll("[-Xmx|G|M]", "")));
					}
					catch(Exception ramExpection) {
						if(ramExpection.getMessage().equalsIgnoreCase("Ram exceeded"))
							serverRamAllocLabel.setText("<html>RAM (GB or MB) <span style='color:#fa4545'>Memoria libre insuficiente</span></html>");
						else ramExpection.printStackTrace();
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

	void createRepoForTab(ServerTab tab, JPanel fatherPanel) {
		fatherPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		File serverDir = tab.getServerDirectory();
		boolean localRepoCreated = GitUtils.createRepoIfNotExistsInPath(serverDir.toPath());
		if(localRepoCreated) {
			String token;
			try {token = TokenStore.loadToken();}
			catch(Exception e) {JOptionPane.showMessageDialog(null, "The GitHub token is not correct, invalid or nonexistent, consider sign in again", "Error", JOptionPane.ERROR_MESSAGE); return;}

			String json = GitUtils.createRepoInGitHub(token, tab.getServerName());
			Map<String, String> responseMap = GitUtils.convertJsonStringToMap(json);
			boolean repoCreatedCorrectly = GitUtils.linkLocalRepoToExternal(responseMap.get("clone_url"), token, serverDir.toPath());

			GitUtils.setSkipWorktree(serverDir.toPath(), Path.of(serverDir.toString() + "/server.properties"), true);
			GitUtils.setSkipWorktree(serverDir.toPath(), Path.of(serverDir.toString() + "/user_jvm_args.txt"), true);

			if(repoCreatedCorrectly) {
				fatherPanel.setCursor(Cursor.getDefaultCursor());
		        JOptionPane.showMessageDialog(
		                frame,
		                "Repo created and linked successfully!",
		                "Git",
		                JOptionPane.INFORMATION_MESSAGE
		        );
		        return;
			}
			else {
				JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		fatherPanel.setCursor(Cursor.getDefaultCursor());
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

	private File getLastOpenedServer() {
		if(!(Files.exists(Paths.get("data/recentServers.properties")))) {
			try {
				Files.createFile(Paths.get("data/recentServers.properties"));
			} catch (IOException e) {}
			return null;
		}
		Properties props = new Properties();
		File file = new File("data/recentServers.properties");
		try(FileInputStream in = new FileInputStream(file)) {
			props.load(in);
			if(props.containsKey("recentServers")) {
				String first = props.getProperty("recentServers").split("\\|")[0];
				File dir = new File(first);
				if (dir.isDirectory()) return dir;
			}
		} catch (IOException e) {}
		return null;
	}

	private void saveToRecentServers(File serverDir) {
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
			String path = serverDir.toPath().toString().replaceAll("\\\\", "/");
			String recentServers = "";
			if(props.containsKey("recentServers"))
				if(!(props.get("recentServers").toString().contains(path)))
					recentServers = path + "|" + props.getProperty("recentServers");
				else
					recentServers = path + "|" + props.getProperty("recentServers").replaceAll(path, "").replace("||", "|").replaceAll("[|\\n]", "");
			else recentServers = path;
			props.setProperty("recentServers", recentServers);
		    FileOutputStream out = new FileOutputStream(file);
	        props.store(out, "Updated recent servers");
	        out.close();
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible", "Error", JOptionPane.ERROR_MESSAGE);
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
						openServerInNewTab(new File(serverDirectory));
					});
					recentServersMenu.add(item);
				}
			}
			else {
				recentServersMenu.add(new JMenuItem("No recent files opened..."));
				return;
			}
		} catch (IOException e) {
			recentServersMenu.add(new JMenuItem("No recent files opened..."));
		}
		recentServersMenu.revalidate();
		recentServersMenu.repaint();
	}

	private void radioBtnListener(JMenu cloudMenu, JMenu saveBackupsToCloudMenu, JRadioButtonMenuItem radioButton) {
	    SwingUtilities.invokeLater(() -> {
	    	cloudProviderInUse = radioButton.getText().replaceAll(" ", "");
	    	if(!cloudProviderInUse.equals("GitHub")) {
	    		if(cloudProvider == null || !cloudProvider.isSessionOpened())
	    			cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[3].formatted(cloudProviderInUse)));
	    		else
	    			cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[1] + cloudProviderInUse));
	    	}
	    	else {
	    		if(!TokenStore.sessionIsOpened())
	    			cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[3].formatted(cloudProviderInUse)));
	    		else
	    			cloudInUseReminderMenuText.setText(cloudInUseReminderText[0].formatted(cloudInUseReminderText[1] + cloudProviderInUse));
	    	}
			ZipUtils.createOrModiFyPropertiesFile("cloudProviderInUse", cloudProviderInUse, CLOUD_PROVIDER_IN_USE_PATH);
			cloudMenu.doClick();
			saveBackupsToCloudMenu.doClick();
	    });
	}

	public JFrame getFrame() {
		return frame;
	}

	// Static convenience methods for backward compatibility with code that references MainFrame statics
	public static Process getServerProcess() {
		if (window == null) return null;
		ServerTab tab = window.getActiveServerTab();
		return tab != null ? tab.getServerProcess() : null;
	}

	public static java.io.BufferedWriter getServerWriter() {
		if (window == null) return null;
		ServerTab tab = window.getActiveServerTab();
		return tab != null ? tab.getServerWriter() : null;
	}

	public static boolean isServerOn() {
		if (window == null) return false;
		ServerTab tab = window.getActiveServerTab();
		return tab != null && tab.isServerOn();
	}

	public static File getServerOpenedDirectory() {
		if (window == null) return null;
		ServerTab tab = window.getActiveServerTab();
		return tab != null ? tab.getServerDirectory() : null;
	}

	public static String getServerName() {
		if (window == null) return "";
		ServerTab tab = window.getActiveServerTab();
		return tab != null ? tab.getServerName() : "";
	}

	public static int getActualServerPort() {
		if (window == null) return 0;
		ServerTab tab = window.getActiveServerTab();
		return tab != null ? tab.getServerPort() : 0;
	}

	public void turnOffServer() {
		ServerTab tab = getActiveServerTab();
		if (tab != null) tab.turnOffServer();
	}
}
