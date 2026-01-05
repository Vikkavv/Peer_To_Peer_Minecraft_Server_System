package jgit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import minecraftServerManagement.ForgeUtils;
import view.MainFrame;

public class GitUtils {
	
	public static volatile boolean serverAutoSaveIsActive = false;
	public static int autoSaveSecondsInterval = 5/*minutes*/ * 60; // 5 minutes by default.
	public static Thread autoSaveProcess = null; //By default.
	
	public static final Path JOINED_REPOS = Path.of("data/joined_repos.properties");
	
	public static boolean createRepoInPath(Path repoDirectory) {
		try {
			Git.init().setDirectory(repoDirectory.toFile()).call();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (GitAPIException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean createRepoIfNotExistsInPath(Path repoDirectory) {
		try {
			Git.open(repoDirectory.toFile());
			return false;
		} catch (IOException e) {
			return createRepoInPath(repoDirectory);
		}	
	}
	
	public static boolean repoExistInPath(Path repoDirectory) {
		try {
			Git.open(repoDirectory.toFile());
			return true;
		}
		catch(IOException e) {
			return false;
		}
	}
	
	public static String createRepoInGitHub(String token, String repoName) {
		String json = """
				{
					"name": "%s",
					"private": true
				}
				""".formatted(repoName);
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/user/repos"))
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.header("Authorization", "Bearer " + token)
				.header("User-Agent", "Peer_To_Peer_Minecraft_Server_System/1.0")
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.build();
		
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		
		if(response.statusCode() == 422) {
			JOptionPane.showMessageDialog(null, "Repository already exists.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		return response.body();
	}
	
	public static Map<String, String> convertJsonStringToMap(String json){
		Map<String, String> responseMap = new HashMap<>();
		json = json.replaceAll("[{|}]", "");
		for(String keyValueLine : json.split(",\"")) /*Split by: ," */ {
			String[] cells = keyValueLine.split("\":")/*Split by: ": */;
			responseMap.put(cells[0].replace('"', ' ').trim(), cells[1].replace('"', ' ').trim());
		}
		return responseMap;
	}
	
	public static List<Map<String, Object>> convertJsonStringToMapJson(String json){
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, new TypeReference<>() {});
		} catch (Exception e) {
			return null;
		}
	}
	
	public static boolean linkLocalRepoToExternal(String cloneUrl, String token, Path repoDirectory) {
		Map<String, String> userdata;
		try {userdata = TokenStore.getSavedUserData();} catch(Exception e) {return false;}
		UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(userdata.get("nickname"), token);
		
		try {
			Git git = Git.open(repoDirectory.toFile());
			
			git.remoteAdd()
				.setName("origin")
				.setUri(new URIish(cloneUrl))
				.call();
			
			git.add()
			    .addFilepattern(".")
			    .call();

			git.commit()
			    .setMessage("Initial commit")
			    .call();
			
			git.push()
				.setCredentialsProvider(credentials)
				.call();
			
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
	}
	
	public static void setSkipWorktree(Path repoDirectory, Path filePath, boolean shouldSkip) {
        try (Git git = Git.open(repoDirectory.toFile())) {
            Repository repo = git.getRepository();
            DirCache cache = repo.lockDirCache();

            try {
                DirCacheEditor editor = cache.editor();
                editor.add(new DirCacheEditor.PathEdit(filePath.toString()) {
                    @Override
                    public void apply(DirCacheEntry entry) {
                        entry.setAssumeValid(shouldSkip);
                    }
                });
                editor.commit();
            } finally {
                cache.unlock();
            }
        } catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
		}
    }
	public static boolean inviteHostingUser(String username) {
		try {
			Map<String, String> userData = TokenStore.getSavedUserData();
			String repo = MainFrame.getServerName();
			return inviteHostingUser(userData.get("token"), userData.get("nickname"), repo, username);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	public static boolean inviteHostingUser(String token, String owner, String repo, String username) {
		String json = """
				{
					"permission": "push"
				}
				""";
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/collaborators/" + username))
				.PUT(HttpRequest.BodyPublishers.ofString(json))
				.header("Authorization", "Bearer " + token)
				.header("User-Agent", "Peer_To_Peer_Minecraft_Server_System/1.0")
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.build();
		
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return response.statusCode() == 201 || response.statusCode() == 204;
	};
	
	public static String getAllPendingInvitations() {
		String token;
		try {
			token = TokenStore.loadToken();
		} catch (Exception e) {return null;}
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/user/repository_invitations"))
				.GET()
				.header("Authorization", "Bearer " + token)
				.header("User-Agent", "Peer_To_Peer_Minecraft_Server_System/1.0")
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.build();
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		return response.body();
	}
	
	public static boolean acceptInvitationById(int id) {
		String token;
		try {
			token = TokenStore.loadToken();
		} catch (Exception e) {return false;}
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/user/repository_invitations/" + id))
				.method("PATCH", HttpRequest.BodyPublishers.noBody())
				.header("Authorization", "Bearer " + token)
				.header("User-Agent", "Peer_To_Peer_Minecraft_Server_System/1.0")
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.build();
		
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch(Exception e) {
			return false;
		}
		if(Integer.toString(response.statusCode()).startsWith("2")) //Accepts all 200 result codes.
			return true;			
		
		return false;
	}
	
	public static void saveRepoJoined(String repo) {
		if(!(Files.exists(JOINED_REPOS)))
			try {
				Files.createFile(JOINED_REPOS);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File not found or inaccessible " + JOINED_REPOS, "Error", JOptionPane.ERROR_MESSAGE);
			}
		
		Map<String, String> userData;
		try {userData = TokenStore.getSavedUserData();}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Session invalid, consider sign in again.", "Error", JOptionPane.ERROR_MESSAGE); 
			return;
		}
		
		String nickname = userData.get("nickname");
		
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(JOINED_REPOS.toFile()); FileOutputStream out = new FileOutputStream(JOINED_REPOS.toFile());){
			props.load(in);
			if(props.containsKey("joined_repos_by_" + nickname)) {
				String previousValue = props.getProperty("joined_repos_by_" + nickname);
				props.setProperty("joined_repos_by_" + nickname, previousValue + "," + repo);
			}
			else props.setProperty("joined_repos_by_" + nickname, repo);
			props.store(out, "Updated joined repos by users.");
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + JOINED_REPOS, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static List<String> getRepoJoined() {
		if(!(Files.exists(JOINED_REPOS))) return null;
		
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(JOINED_REPOS.toFile())){
			Map<String, String> userData = TokenStore.getSavedUserData();
			
			props.load(in);
			if(props.containsKey("joined_repos_by_" + userData.get("nickname"))) {
				String[] reposArray = props.getProperty("joined_repos_by_" + userData.get("nickname")).split(",");
				return Arrays.asList(reposArray);
			}
		}
		catch(IOException ioe) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + JOINED_REPOS, "Error", JOptionPane.ERROR_MESSAGE);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Session invalid, consider sign in again.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		return null;
	}
	
	public static boolean cloneRepoInPath(Path clonePath, String repoFullName) {
		Map<String, String> userdata;
		try {userdata = TokenStore.getSavedUserData();} 
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Session invalid, consider sign in again.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(userdata.get("nickname"), userdata.get("token"));
		
		String cloneUrl = "https://github.com/%s.git".formatted(repoFullName);
		try {
			Git.cloneRepository()
				.setURI(cloneUrl)
				.setDirectory(clonePath.toFile())
				.setCredentialsProvider(credentials)
				.call();
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + clonePath + " or git error occurred.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
	
	public static boolean autoCommitAndPush() {
		return autoCommitAndPush(false);
	}
	
	public static boolean autoCommitAndPush(boolean isServerStopping) {
		Map<String, String> userdata;
		try {userdata = TokenStore.getSavedUserData();} 
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Session invalid, consider sign in again.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(userdata.get("nickname"), userdata.get("token"));
		
		try {
			
			Git git = Git.open(MainFrame.serverOpenedDirectory);
			
			git.add()
		    	.addFilepattern(".")
		    	.call();

			git.commit()
				.setAuthor(userdata.get("nickname"), userdata.get("email"))
			    .setMessage("Backup saved by %s at date %s".formatted(userdata.get("email"), LocalDate.now() + (isServerStopping ? " while server stopping" : "")))
			    .call();
			
			git.push()
				.setCredentialsProvider(credentials)
				.call();
			
			return true;
				
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + MainFrame.serverOpenedDirectory, "Error", JOptionPane.ERROR_MESSAGE);
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	public static void setAutoSaveInterval(int seconds) {
		autoSaveSecondsInterval = seconds;
		saveAutoSaveInterval();
		if(serverAutoSaveIsActive) {
			serverAutoSaveIsActive = false;
			activeAutoSave();
		}
	}
	
	public static void saveAutoSaveInterval() {
		saveAutoSaveInterval(autoSaveSecondsInterval);
	}
	
	public static void saveAutoSaveInterval(int seconds) {
		if(seconds < 2 * 60) throw new RuntimeException("Autosave interval can not be lower than 2 minutes");
		
		Path networkNamePath = Path.of("data/networkName.properties");
		if(!(Files.exists(networkNamePath))) return;
		
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(networkNamePath.toFile()); FileOutputStream out = new FileOutputStream(networkNamePath.toFile())){
			props.load(in);
			
			int savedSeconds = -1;
			if(props.containsKey("autoSaveInterval")) savedSeconds = Integer.parseInt(props.getProperty("autoSaveInterval"));
			if(savedSeconds == seconds) return;
			
			props.setProperty("autoSaveInterval", Integer.toString(seconds));
			props.store(out, "Updated seconds interval");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + networkNamePath, "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		if(autoSaveSecondsInterval != seconds) autoSaveSecondsInterval = seconds;
	}
	
	public static int getSavedAutoSaveInteval() {
		Path networkNamePath = Path.of("data/networkName.properties");
		if(!(Files.exists(networkNamePath))) return autoSaveSecondsInterval;
		
		Properties props = new Properties();
		try(FileInputStream in = new FileInputStream(networkNamePath.toFile())){
			props.load(in);
			if(props.containsKey("autoSaveInterval"))
				autoSaveSecondsInterval = Integer.parseInt(props.getProperty("autoSaveInterval"));
			return autoSaveSecondsInterval;
		}
		catch(IOException ioe) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible " + networkNamePath, "Error", JOptionPane.ERROR_MESSAGE);
			ioe.printStackTrace();
		}
		return autoSaveSecondsInterval;
	}
	
	public static void activeAutoSave() {
		//All will be or null/false or notNull/true at the same time, but like this is more compressible the conditional.
		if(MainFrame.serverIsOn && MainFrame.serverProcess != null && MainFrame.serverWriter != null) {
			autoSaveProcess = new Thread(() -> {
				serverAutoSaveIsActive = true;
				while(serverAutoSaveIsActive) {
					try {
						ForgeUtils.sendCommand("/save-off", MainFrame.serverProcess, MainFrame.serverWriter);
						ForgeUtils.sendCommand("/save-all flush", MainFrame.serverProcess, MainFrame.serverWriter);
						ForgeUtils.sendCommand("/say Saving world, creating backup...", MainFrame.serverProcess, MainFrame.serverWriter);
						//Esto al ser de git lo podrías ignorar y pensar en meter aquí la subida de el google drive...
						if(!autoCommitAndPush()) {
							serverAutoSaveIsActive = false;
						}
						ForgeUtils.sendCommand("/save-on", MainFrame.serverProcess, MainFrame.serverWriter);
						Thread.sleep(autoSaveSecondsInterval * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				autoSaveProcess.interrupt();
			});
			
			autoSaveProcess.start();
		}
	}
	
	public static Boolean isRemoteRepoHeadFordward(Path repoPath) {
		Map<String, String> userdata;
		try {userdata = TokenStore.getSavedUserData();} catch(Exception e) {return false;}
		UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(userdata.get("nickname"), userdata.get("token"));
		
		try (Git git = Git.open(repoPath.toFile())) {

		    git.fetch().setCredentialsProvider(credentials).call();

		    Repository repo = git.getRepository();
		  
		    String branch = repo.getBranch();

		    ObjectId local  = repo.resolve("HEAD");
		    ObjectId remote = repo.resolve("refs/remotes/origin/" + branch);

		    try (RevWalk walk = new RevWalk(repo)) {

		        RevCommit localCommit  = walk.parseCommit(local);
		        RevCommit remoteCommit = walk.parseCommit(remote);

		        if (walk.isMergedInto(localCommit, remoteCommit)) {
		            return true;
		        } else if (walk.isMergedInto(remoteCommit, localCommit)) {
		            return false;
		        }
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean pull(Path repoPath) {
		Map<String, String> userdata;
		try {userdata = TokenStore.getSavedUserData();} catch(Exception e) {return false;}
		UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(userdata.get("nickname"), userdata.get("token"));
		
		try {
			Git git = Git.open(repoPath.toFile());
			
			PullResult result = git.pull()
				.setCredentialsProvider(credentials)
				.call();
			return result.isSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}

