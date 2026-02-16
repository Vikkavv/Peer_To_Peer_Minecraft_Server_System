package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import cloud.ZipUtils;
import minecraftServerManagement.ForgeUtils;

public class GeneralConfigurationsWindows {

	public static final Path USER_OPS_PATH = Path.of("data/userOps.properties");
	private static boolean hasErrors = false;

	public static void generalConfigurations() {
		//Dialog creation and configurations.
		JDialog generalConfigurationsDialog  = new JDialog();
		generalConfigurationsDialog.setTitle("General configurations");
		generalConfigurationsDialog.getContentPane().setLayout(new BorderLayout());
		generalConfigurationsDialog.setResizable(false);
		int widthGeneralConfigurationsDialog = 560;
		int heightGeneralConfigurationsDialog = 200;
		generalConfigurationsDialog.setSize(widthGeneralConfigurationsDialog, heightGeneralConfigurationsDialog);
		generalConfigurationsDialog.setLocationRelativeTo(null);
		generalConfigurationsDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		generalConfigurationsDialog.setVisible(true);



		//General layout for the components.
		JPanel contentPane;
		JScrollPane scrollPane;

		contentPane = new JPanel(new GridLayout(4, 1));
		scrollPane = new JScrollPane(contentPane);
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 20));
		scrollPane.setPreferredSize(new Dimension(355, 100));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		String usersOpsLabelText = "Users operators (format: name, name, name...)";
		JLabel usersOpsLabel = new JLabel(usersOpsLabelText);
		JTextField userOpsInput = new JTextField();

		String usersOpsForCustomCommandsLabelText = "Custom commands operators (format: name, name, name...)";
		JLabel usersOpsForCustomCommandsLabel = new JLabel(usersOpsForCustomCommandsLabelText);
		JTextField usersOpsForCustomCommandsInput = new JTextField();

		JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveBtn = new JButton("Save");
		JButton closeBtn = new JButton("Close");

		//Default values

		if(ZipUtils.existsDirectory(USER_OPS_PATH)) {
			userOpsInput.setText(ZipUtils.getDataFromPropertiesFile("userOps", USER_OPS_PATH));
			usersOpsForCustomCommandsInput.setText(ZipUtils.getDataFromPropertiesFile("usersOpsForCustomCommands", USER_OPS_PATH));
		}

		//Configurations
		scrollPane.setBorder(null);

		contentPane.add(usersOpsLabel);
		contentPane.add(userOpsInput);
		contentPane.add(usersOpsForCustomCommandsLabel);
		contentPane.add(usersOpsForCustomCommandsInput);

		buttonsPane.add(closeBtn);
		buttonsPane.add(saveBtn);

		generalConfigurationsDialog.add(scrollPane, BorderLayout.NORTH);
		generalConfigurationsDialog.add(buttonsPane, BorderLayout.SOUTH);

		//EventListeners
		saveBtn.addActionListener(svBtn -> {
			String errorTemplate = "<html>%s <span style='color: #fa4545;'>%s</span></html>";
			String errorMessage = "Invalid format";

			usersOpsLabel.setText(usersOpsLabelText);
			usersOpsForCustomCommandsLabel.setText(usersOpsForCustomCommandsLabelText);

			if(ZipUtils.existsDirectory(USER_OPS_PATH) && userOpsInput.getText().equals(ZipUtils.getDataFromPropertiesFile("userOps", USER_OPS_PATH)) && usersOpsForCustomCommandsInput.getText().equals(ZipUtils.getDataFromPropertiesFile("usersOpsForCustomCommands", USER_OPS_PATH))) {
				generalConfigurationsDialog.dispose();
				return;
			}

			if(!checkFormatValidity(userOpsInput.getText()))
				usersOpsLabel.setText(errorTemplate.formatted(usersOpsLabelText, errorMessage));

			if(!checkFormatValidity(usersOpsForCustomCommandsInput.getText()))
				usersOpsForCustomCommandsLabel.setText(errorTemplate.formatted(usersOpsForCustomCommandsLabelText, errorMessage));

			if(!hasErrors) {
				String currentValue = ZipUtils.getDataFromPropertiesFile("userOps", USER_OPS_PATH);


				ZipUtils.createOrModiFyPropertiesFile("userOps", userOpsInput.getText(), USER_OPS_PATH);
				ZipUtils.createOrModiFyPropertiesFile("usersOpsForCustomCommands", usersOpsForCustomCommandsInput.getText(), USER_OPS_PATH);

				ServerTab activeTab = MainFrame.window != null ? MainFrame.window.getActiveServerTab() : null;
				if(activeTab != null && activeTab.isServerOn()) {
					if(ZipUtils.getDataFromPropertiesFile("userOps", USER_OPS_PATH) instanceof String ops && !ops.isBlank()) {
						for(String currentsNickname : currentValue.split(", ")) {
							if(!userOpsInput.getText().contains(currentsNickname))
								ForgeUtils.sendCommand("/deop " + currentsNickname, activeTab.getServerProcess(), activeTab.getServerWriter());
						}
						for(String nickname : userOpsInput.getText().split(", ")) {
							if(!currentValue.contains(nickname))
							ForgeUtils.sendCommand("/op " + nickname, activeTab.getServerProcess(), activeTab.getServerWriter());
						}
					}
				}
				else if(activeTab != null)
					ZipUtils.deleteDirectory(Path.of(activeTab.getServerDirectory().toString() + "/ops.json"));

				generalConfigurationsDialog.dispose();

			}

		});

		closeBtn.addActionListener(clsBtn -> {
			generalConfigurationsDialog.dispose();
		});
	}

	private static boolean checkFormatValidity(String text) {
		if(text.isBlank()) return true;
		Pattern regExp = Pattern.compile("^.*[^, ]$");
		Matcher matcher = regExp.matcher(text);

		if(matcher.matches()) {
			hasErrors = false;
			return true;
		}

		hasErrors = true;
		return false;
	}
}
