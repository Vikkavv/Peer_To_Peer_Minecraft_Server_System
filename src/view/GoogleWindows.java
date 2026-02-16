package view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import com.google.api.services.drive.Drive.About;

import cloud.google.GoogleDriveCloudProvider;
import jgit.GitUtils;
import jgit.TokenStore;

public class GoogleWindows {
	
	private static boolean hasErrors = false;
	
	public static void addHostingUser() {
		//Dialog creation and configurations.
		JDialog addHostingUserDialog  = new JDialog();
		addHostingUserDialog.setTitle("Add hosting user to this server");
		addHostingUserDialog.getContentPane().setLayout(new BorderLayout());
		addHostingUserDialog.setResizable(false);
		int widthSignInDialog = 310;
		int heightSignInDialog = 150;
		addHostingUserDialog.setSize(widthSignInDialog, heightSignInDialog);
		addHostingUserDialog.setLocationRelativeTo(null);
		addHostingUserDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addHostingUserDialog.setVisible(true);
		
		//General layout for the components.
		JPanel contentPane = new JPanel(new GridLayout(2,1));
		JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				
		//Labels and Inputs
		String googleDriveEmailLabelText = "Google account email";
		JLabel googleDriveEmailLabel = new JLabel(googleDriveEmailLabelText);
		JTextField googleDriveEmailInput = new JTextField();
		
		//Buttons
		JButton cancelBtn = new JButton("Cancel");
		JButton addUserBtn = new JButton("Add user");
		
		//Components configurations.
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		//Push the general containers and all its children.
		contentPane.add(googleDriveEmailLabel);
		contentPane.add(googleDriveEmailInput);
		
		buttonsPane.add(cancelBtn);
		buttonsPane.add(addUserBtn);
		
		addHostingUserDialog.add(contentPane, BorderLayout.NORTH);
		addHostingUserDialog.add(buttonsPane, BorderLayout.SOUTH);
		
		//Event listeners
		addUserBtn.addActionListener(addUsrBtn -> {
			hasErrors = false;
			String errorMessageTemplate = "<html>%s - <span style='color:#fa4545'>%s</span></html>";
			
			boolean emailIsEmpty = fieldIsEmpty(googleDriveEmailLabel, googleDriveEmailInput);
			Pattern pattern = Pattern.compile("[a-zA-Z0-9._]+@[a-zA-Z]+(([.][a-z]+)*)[.][a-z]{2,}");
			Matcher matcher = pattern.matcher(googleDriveEmailInput.getText());
			if(!matcher.find()  && !emailIsEmpty) {
				googleDriveEmailLabel.setText(String.format(errorMessageTemplate, googleDriveEmailLabelText, "Use a valid email format."));
				hasErrors = true;
			}
			
			if(hasErrors) return;
			
    		Object[] confirmButtons = {"Cancel","Accept"};
            int opt = JOptionPane.showOptionDialog(
        		null,
        		"Are you sure do you want to add the user '" + googleDriveEmailInput.getText() + "' to the hosting list?",
        		"Invitation confirmation",
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                confirmButtons,
                confirmButtons[0]
    		);
            
            if(opt == 1) {
            	boolean invitedSuccessfully = MainFrame.cloudProvider.inviteUser(googleDriveEmailInput.getText());
				if(invitedSuccessfully) {
			        JOptionPane.showMessageDialog(
			                null,                   
			                "User invited to hosting successfully!",
			                "Google Drive",
			                JOptionPane.INFORMATION_MESSAGE
			        );
				}
				else {
					JOptionPane.showMessageDialog(null, "Unable to invite user to hosting, try again.", "Google Drive error", JOptionPane.ERROR_MESSAGE);
				}
            }
			
            
            addHostingUserDialog.dispose();
		});
		
		cancelBtn.addActionListener(cnlbtn -> {
			addHostingUserDialog.dispose();
		});
	}
	
	public static void googleProfileWnd() {
		//Dialog creation and configurations.
		JDialog googleDriveProfileDialog  = new JDialog();
		googleDriveProfileDialog.setTitle("Google profile");
		googleDriveProfileDialog.getContentPane().setLayout(new BorderLayout());
		googleDriveProfileDialog.setResizable(false);
		int widthGoogleDriveProfileDialog = 360;
		int heightGoogleDriveProfileDialog = 150;
		googleDriveProfileDialog.setSize(widthGoogleDriveProfileDialog, heightGoogleDriveProfileDialog);
		googleDriveProfileDialog.setLocationRelativeTo(null);
		googleDriveProfileDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		googleDriveProfileDialog.setVisible(true);
		
		//General layout for the components.
		JPanel contentPane = new JPanel(new GridLayout(2,1));
		JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				
		//Labels and Inputs
		JLabel googleEmailLabel = new JLabel("Logged as:");
		JTextPane googleEmailInput = new JTextPane();
		
		//Default data
		Map<String, Object> userData = ((GoogleDriveCloudProvider) MainFrame.cloudProvider).getUserInfo();
		
		ImageIcon icon;
		JLabel imageLabel;
		try {
			icon = new ImageIcon(URL.of(URI.create((String) userData.get("profilePhoto")), null));
			Image image = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
			imageLabel = new JLabel(new ImageIcon(image));
		} catch (MalformedURLException | NullPointerException e) {
			imageLabel = null;
		}
		
		googleEmailInput.setText((String) userData.get("email"));
		
		
		//Buttons
		JButton closeBtn = new JButton("Close");
		
		//Components configurations.
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		googleEmailInput.setEditable(false);
		
		//Push the general containers and all its children.
		contentPane.add(googleEmailLabel);
		if(imageLabel != null) contentPane.add(imageLabel);
		contentPane.add(googleEmailInput);
		
		buttonsPane.add(closeBtn);
		
		googleDriveProfileDialog.add(contentPane, BorderLayout.NORTH);
		googleDriveProfileDialog.add(buttonsPane, BorderLayout.SOUTH);
		
		//Event Listeners
		closeBtn.addActionListener(clsBtn -> {
			googleDriveProfileDialog.dispose();
		});
	}
	
	public static void cloneServerFolderWnd(JFrame frame) {
		//First the user selects one of the repositories that he has joined.
		//Dialog creation and configurations.
		JDialog googleDriveServerFoldersCloneListDialog  = new JDialog();
		googleDriveServerFoldersCloneListDialog.setTitle("Server invited folders");
		googleDriveServerFoldersCloneListDialog.getContentPane().setLayout(new BorderLayout());
		googleDriveServerFoldersCloneListDialog.setResizable(false);
		int widthGoogleDriveServerFoldersCloneListDialog = 560;
		int heightGoogleDriveServerFoldersCloneListDialog = 230;
		googleDriveServerFoldersCloneListDialog.setSize(widthGoogleDriveServerFoldersCloneListDialog, heightGoogleDriveServerFoldersCloneListDialog);
		googleDriveServerFoldersCloneListDialog.setLocationRelativeTo(null);
		googleDriveServerFoldersCloneListDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		googleDriveServerFoldersCloneListDialog.setVisible(true);
		
		//We get the folders list
		List<String> serverFolderlist = MainFrame.cloudProvider.getInvitedFolderList();
		
		//General layout for the components.
		JPanel contentPane;
		if(serverFolderlist != null && serverFolderlist.size() > 0) {
			contentPane = new JPanel(new GridLayout(serverFolderlist.size(), 1)); //We use the 'serverFolderlist' size so we always get a grid that has the same number of rows than the length.
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		}
		else {
			contentPane = new JPanel(new BorderLayout());
			contentPane.setBorder(BorderFactory.createEmptyBorder(70, 200, 70, 200));
		} 
		
		JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		//Buttons
		createClonelistComponents(contentPane, frame, googleDriveServerFoldersCloneListDialog, serverFolderlist);
		JButton closeBtn = new JButton("Close");
		
		//Push the general containers and all its children.
		
		buttonsPane.add(closeBtn);
		
		googleDriveServerFoldersCloneListDialog.add(contentPane, BorderLayout.NORTH);
		googleDriveServerFoldersCloneListDialog.add(buttonsPane, BorderLayout.SOUTH);
		
		//Event Listeners
		closeBtn.addActionListener(clsBtn -> {
			googleDriveServerFoldersCloneListDialog.dispose();
		});
	}
	
	private static void createClonelistComponents(JPanel contentPane, JFrame frame, JDialog googleDriveServerFoldersCloneListDialog, List<String> serverFolderlist) {
		contentPane.removeAll();
		String labelTextTemplate = "<html><b>Creator: </b>%s - <b>Server Folder: </b>%s</html>";
		if(serverFolderlist == null || serverFolderlist.size() < 1) {
			contentPane.add(new JLabel("<html><span style='color: gray; text-align: center;'>No server folders to install</span></html>"));
			return;
		}
		
		for(String serverFolderId : serverFolderlist) {
			List<String> names = ((GoogleDriveCloudProvider) MainFrame.cloudProvider).getRelevantFolderInfo(serverFolderId);
			
			JPanel cloneContainer = new JPanel(new FlowLayout());
			cloneContainer.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
			JLabel textLabel = new JLabel(String.format(labelTextTemplate, names.get(0), names.get(1)));
			JButton cloneBtn = new JButton("clone");
			
			//Push all the children.
			cloneContainer.add(textLabel);
			cloneContainer.add(cloneBtn);
			contentPane.add(cloneContainer);
		
			cloneBtn.addActionListener(clnBtn -> {	
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int result = chooser.showOpenDialog(frame);
				if(result == JFileChooser.APPROVE_OPTION) {
					File cloneDirectory = chooser.getSelectedFile();
					if(cloneDirectory.isDirectory() && cloneDirectory.list().length != 0) {
			            JOptionPane.showMessageDialog(
			            		cloneContainer,
			                    "Debe seleccionar un directorio vacío.",
			                    "Error",
			                    JOptionPane.ERROR_MESSAGE
			            );
					}
					else {
						frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						new Thread(() -> {
							File cloneDirectoryServer = Path.of(cloneDirectory.toString(), names.get(1)).toFile();
							GoogleDriveCloudProvider.isSearchingBackUpForClonning = true;
							boolean clonedSuccessfully = MainFrame.cloudProvider.downloadServerBackup(cloneDirectoryServer.toPath());
							GoogleDriveCloudProvider.isSearchingBackUpForClonning = false;
							frame.setCursor(Cursor.getDefaultCursor());
							if(clonedSuccessfully) {
						        JOptionPane.showMessageDialog(
						                null,
						                "Server cloned successfully!",
						                "Google Drive",
						                JOptionPane.INFORMATION_MESSAGE
						        );
						        googleDriveServerFoldersCloneListDialog.dispose();
						        MainFrame.window.openServerInNewTab(cloneDirectoryServer);
							}
							else {
								 JOptionPane.showMessageDialog(
						                null,
						                "Server not installed. This server does not have any backups saved. Please ask the owner to create at least one backup.",
						                "Google Drive",
						                JOptionPane.INFORMATION_MESSAGE
						        );
							}
						}).start();
					}
				}
			});
		}
	}
	
	private static boolean fieldIsEmpty(JLabel errorLabel, JTextField input) {
		if(input.getText().trim().isEmpty()) {
			errorLabel.setText(String.format("<html>%s - <span style='color:#fa4545'>%s</span></html>", errorLabel.getText(), "Field can not be empty."));
			hasErrors = true;
			return true;
		}
		return false;
	}
}
