package minecraftServerManagement;

import java.util.HashMap;
import java.util.Map;

import cloud.ZipUtils;
import jgit.TokenStore;
import view.GeneralConfigurationsWindows;
import view.MainFrame;

public class CustomCommands {
	private static Map<String, Runnable> commandsActions;
	private static Map<String, String> commandsInfo;
	private static String userNickname = null;
	private static String command = null;
	
	private static String directCommands = "\\player\\op";
	
	private static Runnable customHelpCommand = () -> {
		for(Map.Entry<String, String> entry : commandsInfo.entrySet()) {
			ForgeUtils.sendCommand("/tell " + userNickname + " " + entry.getKey() + ": " + entry.getValue(), MainFrame.getServerProcess(), MainFrame.getServerWriter());
		}
	};
	
	private static Runnable customStopCommand = () -> {
		if(MainFrame.cloudProviderInUse != null && ((MainFrame.cloudProviderInUse.equals("GitHub") && TokenStore.sessionIsOpened()) || (MainFrame.cloudProvider != null && MainFrame.cloudProvider.getProviderName().equals(MainFrame.cloudProviderInUse) && MainFrame.cloudProvider.isSessionOpened())))
			ForgeUtils.sendCommand("/title @a subtitle {\"text\":\"Saving backup in " + MainFrame.cloudProviderInUse + ".\",\"bold\":true,\"color\":\"#ccff11\"}", MainFrame.getServerProcess(), MainFrame.getServerWriter());
		ForgeUtils.sendCommand("/title @a title {\"text\":\"Server is shutting down.\",\"bold\":true,\"color\":\"#ff8000\"}", MainFrame.getServerProcess(), MainFrame.getServerWriter());
		try {Thread.sleep(2000);} catch(InterruptedException e) {}
		if(MainFrame.window != null) MainFrame.window.turnOffServer();
	};
	
	private static Runnable customOpCommand = () -> {
		String[] dividedCommandWords = command.trim().split(" ");
		if(dividedCommandWords.length > 2) {
			ForgeUtils.sendCommand("/msg " + userNickname + " Unknown command '" + command + "', use \\help to get more information.", MainFrame.getServerProcess(), MainFrame.getServerWriter());
			return;
		}
		String userToAddToOps = dividedCommandWords[1];
		String currentOpsListValue = "";
		if(ZipUtils.existsDirectory(GeneralConfigurationsWindows.USER_OPS_PATH))
			currentOpsListValue = ZipUtils.getDataFromPropertiesFile("usersOpsForCustomCommands", GeneralConfigurationsWindows.USER_OPS_PATH);
		if(currentOpsListValue.contains(userToAddToOps)) {
			ForgeUtils.sendCommand("/msg " + userNickname + " The user '" + userToAddToOps + "' is already an operator.", MainFrame.getServerProcess(), MainFrame.getServerWriter());
			return;
		}
		if(currentOpsListValue.trim().isEmpty())
			ZipUtils.createOrModiFyPropertiesFile("usersOpsForCustomCommands", userToAddToOps, GeneralConfigurationsWindows.USER_OPS_PATH);
		else
			ZipUtils.createOrModiFyPropertiesFile("usersOpsForCustomCommands", currentOpsListValue + ", " + userToAddToOps, GeneralConfigurationsWindows.USER_OPS_PATH);
	};
	private static Runnable addTabHearts = () -> {
		ForgeUtils.sendCommand("/scoreboard objectives add Hearts health", MainFrame.getServerProcess(), MainFrame.getServerWriter());
		ForgeUtils.sendCommand("/scoreboard objectives setdisplay list Hearts", MainFrame.getServerProcess(), MainFrame.getServerWriter());
	};
	
	
	private static Runnable removeTabHearts = () -> {
		ForgeUtils.sendCommand("/scoreboard objectives remove Hearts", MainFrame.getServerProcess(), MainFrame.getServerWriter());
	};
	
	private static Runnable customCarpetPlayerCommand = () -> {
		String userToSpawn = null;
		if(command.contains("spawn")) userToSpawn = command.split(" ")[1];
		ForgeUtils.sendCommand("/" + command.substring(1), MainFrame.getServerProcess(), MainFrame.getServerWriter());
		if(userToSpawn != null)
			try {Thread.sleep(200);} catch(InterruptedException e) {}
			ForgeUtils.sendCommand("/tp " + userToSpawn + " " + userNickname, MainFrame.getServerProcess(), MainFrame.getServerWriter());
	};
	
	
	static {
		commandsActions = new HashMap<>();
		commandsActions.put("\\help", customHelpCommand);
		commandsActions.put("\\stop", customStopCommand);
		commandsActions.put("\\op", customOpCommand);
		commandsActions.put("\\tabHearts add", addTabHearts);
		commandsActions.put("\\tabHearts remove", removeTabHearts);
		commandsActions.put("\\player", customCarpetPlayerCommand);
		
		commandsInfo = new HashMap<>();
		commandsInfo.put("\\player <name> (attack | dismount | drop | dropStack | hotbar | jump | kill | look | mount | move | shadow | sneak | spawn | sprint | stop | swapHands | unsneak | unsprint | use)", "The original player carpet mod command ported to backslash commands. (It requires to have carpet mod installed)");
		commandsInfo.put("\\tabHearts (add | remove)", "Makes a new scoreboard in the tab list to show everyone's hearts or removes it.");
		commandsInfo.put("\\op <name>", "Adds the specified player to the backslash commands operators list.");
		commandsInfo.put("\\stop", "Closes the server as the vanilla command but saves the server into the cloud provider in use in that moment.");
	}
	
	public static boolean processCustomCommand(String line) {
		int lastColonPos = line.lastIndexOf(':');
		String infoPostColon = line.substring(lastColonPos + 1);
		userNickname = infoPostColon.substring(2, infoPostColon.lastIndexOf(">"));
		command = infoPostColon.substring(infoPostColon.lastIndexOf("\\")).trim();
		String localCommand = command;
		if(directCommands.contains(command.trim().split(" ")[0])) command = command.trim().split(" ")[0];
		
		if(commandsActions.containsKey(command)) {
			
			if(!command.equals("\\help")) {
				String customCommandsOps = ZipUtils.getDataFromPropertiesFile("usersOpsForCustomCommands", GeneralConfigurationsWindows.USER_OPS_PATH);
				if(customCommandsOps != null && !customCommandsOps.isBlank() && !customCommandsOps.contains(userNickname)) {
					ForgeUtils.sendCommand("/msg " + userNickname + " You do not have permission to execute this command. Ask an operator to add you.", MainFrame.getServerProcess(), MainFrame.getServerWriter());
					return false;
				}
			}
			
			Runnable action = commandsActions.get(command);
			if(directCommands.contains(command)) command = localCommand;
			action.run();
			command = null;
			userNickname = null;
			return true;
			
		}
		else 
			ForgeUtils.sendCommand("/msg " + userNickname + " Unknown command '" + command + "', use \\help to get more information.", MainFrame.getServerProcess(), MainFrame.getServerWriter());
			
		command = null;
		userNickname = null;
		return false; 
	}
	
	
}
