package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import cloud.ZipUtils;
import jgit.GitUtils;
import jgit.TokenStore;
import minecraftServerManagement.ForgeUtils;
import vpn.DiscoveryResponder;
import vpn.NetworkDiscoverClient;

public class ServerTab {

    private final File serverDirectory;
    private final Color tabColor;
    private Process serverProcess;
    private BufferedWriter serverWriter;
    private Thread consoleThread;
    private JTextArea consoleArea;
    private JTextField commandInput;
    private JButton turnOnOffBtn;
    private JTextPane ipServerHostingPane;
    private JPanel consoleContent;
    private boolean serverIsOn;
    private int serverPort;
    private DiscoveryResponder responder;
    private JPanel panel;

    public ServerTab(File serverDir, Color tabColor) {
        this.serverDirectory = serverDir;
        this.tabColor = tabColor;
        this.serverIsOn = false;
        this.serverPort = ForgeUtils.getServerPort(serverDir.toPath());
    }

    public JPanel buildServerPanel() {
        panel = new JPanel(new BorderLayout());

        // Cloud sync on open
        if (MainFrame.cloudProvider != null && MainFrame.cloudProviderInUse != null) {
            if (MainFrame.cloudProviderInUse.equals("GitHub")) {
                if (GitUtils.repoExistInPath(serverDirectory.toPath())) {
                    if (TokenStore.sessionIsOpened() && GitUtils.isRemoteRepoHeadFordward(serverDirectory.toPath())) {
                        new Thread(() -> GitUtils.pull(serverDirectory.toPath())).start();
                    }
                }
            } else if (MainFrame.cloudProvider.getProviderName().equals(MainFrame.cloudProviderInUse) && MainFrame.cloudProvider.isSessionOpened()) {
                new Thread(() -> MainFrame.cloudProvider.downloadServerBackup(serverDirectory.toPath())).start();
            }
        }

        JPanel content = new JPanel(new BorderLayout());
        consoleContent = new JPanel(new BorderLayout());
        JPanel topContent = new JPanel(new BorderLayout());
        JPanel leftContent = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel rightContent = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel serverName = new JLabel("Server: " + getServerName());
        turnOnOffBtn = new JButton("On");
        turnOnOffBtn.setEnabled(false);

        turnOnOffBtn.addActionListener(trnOnOffBtn -> {
            if (!serverIsOn) {
                String networkDiscoveryResult = NetworkDiscoverClient.surroundDiscoverIOException(MainFrame.networkName, serverPort, 3000);
                if (networkDiscoveryResult != "NotFound") {
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
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                new Thread(() -> {
                    try {
                        // Auto-assign port (avoids conflicts with other running servers)
                        int preferredPort = serverPort;
                        int assignedPort = MainFrame.portManager.assignPort(preferredPort);
                        if (assignedPort != preferredPort) {
                            ForgeUtils.setServerPort(serverDirectory.toPath(), assignedPort);
                            serverPort = assignedPort;
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null,
                                    "Port %d was in use. Server will start on port %d instead.".formatted(preferredPort, assignedPort),
                                    "Auto-port assignment",
                                    JOptionPane.INFORMATION_MESSAGE);
                            });
                        }

                        serverProcess = ForgeUtils.executeMinecraftServer(serverDirectory.toPath());
                        if (serverProcess != null) {
                            SwingUtilities.invokeLater(() -> {
                                panel.setCursor(Cursor.getDefaultCursor());
                                serverName.setText(serverNameString);
                                turnOnOffBtn.setText("Off");
                                serverIsOn = true;
                                consoleArea = new JTextArea(15, 60);
                                JScrollPane scroll = new JScrollPane(consoleArea);
                                consoleArea.setEditable(false);
                                commandInput = new JTextField();
                                commandInput.setColumns(20);
                                consoleContent.add(scroll, BorderLayout.CENTER);
                                consoleContent.add(commandInput, BorderLayout.SOUTH);
                                commandInput.addActionListener(msg -> {
                                    ForgeUtils.sendCommand(commandInput.getText(), serverProcess, serverWriter);
                                    commandInput.setText("");
                                });
                                serverWriter = ForgeUtils.configureServerWriter(serverProcess, serverWriter);
                                consoleThread = ForgeUtils.getServerOutputs(serverProcess, consoleArea, () -> onServerReady());
                                content.add(consoleContent, BorderLayout.SOUTH);
                                consoleContent.revalidate();
                                consoleContent.repaint();
                            });
                        }
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            panel.setCursor(Cursor.getDefaultCursor());
                            serverName.setText(serverNameString);
                        });
                    }
                }).start();
            } else {
                turnOffServer();
            }
        });

        JButton openServerModsFolderBtn = new JButton("Open Mods Folder");
        openServerModsFolderBtn.addActionListener(mds -> {
            if (!ZipUtils.existsDirectory(serverDirectory.toPath().resolve("mods")))
                ZipUtils.createDirectory(serverDirectory.toPath().resolve("mods"));
            ForgeUtils.openModsFolder(serverDirectory.toPath());
        });

        JButton serverConfigBtn = new JButton("Configurations");
        serverConfigBtn.addActionListener(conf -> {
            MainFrame.window.serverConfigsFrame(panel, this);
        });

        ipServerHostingPane = new JTextPane();
        ipServerHostingPane.setEditable(false);
        ipServerHostingPane.setVisible(false);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(rfshBtnE -> {
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            checkServerStatus(MainFrame.networkName);
            panel.setCursor(Cursor.getDefaultCursor());
        });

        panel.add(content, BorderLayout.CENTER);
        content.add(leftContent, BorderLayout.WEST);
        content.add(rightContent, BorderLayout.EAST);
        content.add(topContent, BorderLayout.NORTH);
        topContent.add(serverName);
        leftContent.add(turnOnOffBtn);
        rightContent.add(ipServerHostingPane);
        rightContent.add(refreshBtn);
        rightContent.add(openServerModsFolderBtn);

        if (!GitUtils.repoExistInPath(serverDirectory.toPath()) && TokenStore.sessionIsOpened()
                && MainFrame.cloudProviderInUse != null && MainFrame.cloudProviderInUse.equals("GitHub")) {
            JButton createServerRepoBtn = new JButton("Create repository");
            createServerRepoBtn.addActionListener(repo -> {
                MainFrame.window.createRepoForTab(this, panel);
            });
            rightContent.add(createServerRepoBtn);
        }
        if (MainFrame.cloudProviderInUse != null && MainFrame.cloudProvider != null
                && MainFrame.cloudProvider.getProviderName().equals(MainFrame.cloudProviderInUse)
                && MainFrame.cloudProvider.isSessionOpened()) {
            JButton createBackupsFolderBtn = new JButton("Create backups folder in %s".formatted(MainFrame.cloudProviderInUse));
            createBackupsFolderBtn.addActionListener(e -> {
                new Thread(() -> {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    MainFrame.cloudProvider.createSavingFolder();
                    panel.setCursor(Cursor.getDefaultCursor());
                }).start();
            });
            if (MainFrame.cloudProvider.hasRemoteServerFolder()) {
                createBackupsFolderBtn.setVisible(false);
            }
            rightContent.add(createBackupsFolderBtn);
        }

        rightContent.add(serverConfigBtn);
        checkServerStatus(MainFrame.networkName);

        return panel;
    }

    private void onServerReady() {
        responder = new DiscoveryResponder(MainFrame.networkName).listenAsync(serverPort);
        SwingUtilities.invokeLater(() -> checkServerStatus(MainFrame.networkName));

        if (ZipUtils.existsDirectory(GeneralConfigurationsWindows.USER_OPS_PATH)) {
            String ops = ZipUtils.getDataFromPropertiesFile("userOps", GeneralConfigurationsWindows.USER_OPS_PATH);
            if (ops != null && !ops.isBlank()) {
                for (String nickname : ops.split(", ")) {
                    ForgeUtils.sendCommand("/op " + nickname, serverProcess, serverWriter);
                }
            }
        }
    }

    public void checkServerStatus(String networkName) {
        String networkDiscoveryResult = NetworkDiscoverClient.surroundDiscoverIOException(networkName, serverPort, 3000);
        if (networkDiscoveryResult != "NotFound") {
            ipServerHostingPane.setText("Server ip: " + networkDiscoveryResult);
            if (!serverIsOn) turnOnOffBtn.setEnabled(false);
        } else {
            ipServerHostingPane.setText("Server is off");
            turnOnOffBtn.setEnabled(true);
        }
        ipServerHostingPane.setVisible(true);
    }

    public void turnOffServer() {
        if (!serverIsOn) return;
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ForgeUtils.sendCommand("/stop", serverProcess, serverWriter);
        new Thread(() -> {
            try {
                serverProcess.waitFor();
            } catch (InterruptedException e) {}

            if (TokenStore.sessionIsOpened() && GitUtils.repoExistInPath(serverDirectory.toPath())
                    && MainFrame.cloudProviderInUse != null && MainFrame.cloudProviderInUse.equals("GitHub")) {
                GitUtils.autoCommitAndPush(true);
            }
            if (MainFrame.cloudProvider != null && MainFrame.cloudProvider.getProviderName().equals(MainFrame.cloudProviderInUse)
                    && MainFrame.cloudProvider.isSessionOpened()) {
                if (MainFrame.cloudProvider.hasRemoteServerFolder()) {
                    ZipUtils.createZip(serverDirectory.toPath(), ZipUtils.BACKUPS_ZIPS_FOLDER);
                    MainFrame.cloudProvider.uploadServerBackup(ZipUtils.BACKUPS_ZIPS_FOLDER);
                }
            }

            if (consoleThread != null) consoleThread.interrupt();
            serverProcess = null;
            MainFrame.portManager.releasePort(serverPort);

            SwingUtilities.invokeLater(() -> {
                consoleContent.removeAll();
                consoleContent.revalidate();
                consoleContent.repaint();
                consoleArea = null;
                commandInput = null;
                serverWriter = null;
                serverIsOn = false;
                turnOnOffBtn.setText("On");
                if (responder != null) {
                    responder.closeListeningSocket();
                    responder = null;
                }
                checkServerStatus(MainFrame.networkName);
                panel.setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    public void turnOffServerSync() {
        if (!serverIsOn) return;
        ForgeUtils.sendCommand("/stop", serverProcess, serverWriter);
        try {
            serverProcess.waitFor();
        } catch (InterruptedException e) {}

        if (TokenStore.sessionIsOpened() && GitUtils.repoExistInPath(serverDirectory.toPath())
                && MainFrame.cloudProviderInUse != null && MainFrame.cloudProviderInUse.equals("GitHub")) {
            GitUtils.autoCommitAndPush(true);
        }
        if (MainFrame.cloudProvider != null && MainFrame.cloudProvider.getProviderName().equals(MainFrame.cloudProviderInUse)
                && MainFrame.cloudProvider.isSessionOpened()) {
            if (MainFrame.cloudProvider.hasRemoteServerFolder()) {
                ZipUtils.createZip(serverDirectory.toPath(), ZipUtils.BACKUPS_ZIPS_FOLDER);
                MainFrame.cloudProvider.uploadServerBackup(ZipUtils.BACKUPS_ZIPS_FOLDER);
            }
        }

        if (consoleThread != null) consoleThread.interrupt();
        serverProcess = null;
        MainFrame.portManager.releasePort(serverPort);
        if (responder != null) {
            responder.closeListeningSocket();
            responder = null;
        }
        serverIsOn = false;
    }

    public String getServerName() {
        String path = serverDirectory.toString();
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return path.substring(lastSep + 1);
    }

    public boolean isServerOn() { return serverIsOn; }
    public File getServerDirectory() { return serverDirectory; }
    public int getServerPort() { return serverPort; }
    public Color getTabColor() { return tabColor; }
    public JPanel getPanel() { return panel; }
    public Process getServerProcess() { return serverProcess; }
    public BufferedWriter getServerWriter() { return serverWriter; }
    public void setServerPort(int port) { this.serverPort = port; }
}
