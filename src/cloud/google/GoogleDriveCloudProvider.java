package cloud.google;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import cloud.CloudStorageProvider;
import cloud.ZipUtils;
import minecraftServerManagement.ForgeUtils;
import view.MainFrame;

public class GoogleDriveCloudProvider implements CloudStorageProvider{

	private static final String APPLICATION_NAME = "PeerToPeerMinecraftServerSystem";
	private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final List<String> SCOPES = List.of(DriveScopes.DRIVE_FILE);
	private static final String CREDENTIALS_FOLDER = "data/google_tokens";
	private static final Map<String, String> metadata = new HashMap<>();
	private static final Map<String, String> metadataChildren = new HashMap<>();
	
	public static boolean isSearchingBackUpForClonning = false;
	
	static {
		metadata.put("app", APPLICATION_NAME);
		metadata.put("type", "root");
		
		metadataChildren.putAll(metadata);
		metadataChildren.put("type", "children");
	}

	private Drive drive = null;
	private Credential credential = null;
	
	@Override
	public void authenticate() {
		
		try(InputStream in = GoogleDriveCloudProvider.class.getResourceAsStream("/credentials/GoolgeDriveCredentials.json")) {
			
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
			
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),
					JSON_FACTORY,
					clientSecrets,
					SCOPES
			).setDataStoreFactory(new FileDataStoreFactory(new File(CREDENTIALS_FOLDER)))
			 .setAccessType("offline")
			 .setApprovalPrompt("force")
			 .build();
			
			LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).setLandingPages("https://p2pmss.vercel.app/OAuth/success", "https://p2pmss.vercel.app/OAuth/failed").build();
			
			Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
			this.credential = credential;
			
			this.drive = new Drive.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),
					JSON_FACTORY,
					credential
			).setApplicationName(APPLICATION_NAME)
			 .build();
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Credentials file not found or inaccessible or another thing went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (GeneralSecurityException e) {
			JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Google Drive error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void uploadServerBackup(Path serverZip) {
		if(drive == null) {
			JOptionPane.showMessageDialog(null, "Sign into Google Drive to be able to upload server data.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try {
			String rootFolderId = getServerFolderIdWithMetadata("P2PMSS-Backups", metadata, iAmOwner());
			String serverFolderId = getOrCreateServerFolderId(MainFrame.getServerName(), rootFolderId, metadataChildren, iAmOwner(), false);
			if(serverFolderId == null) getOrCreateServerFolderId(MainFrame.getServerName(), null, metadataChildren, iAmOwner(), false);
			
			LocalDateTime todayDate = LocalDateTime.now();
			
			com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
			fileMetadata.setName(String.format("%s_%s", MainFrame.getServerName(), todayDate));
			fileMetadata.setParents(List.of(serverFolderId));
			
			FileContent mediaContent = new FileContent("application/zip", serverZip.toFile());
			
			drive.files().create(fileMetadata, mediaContent)
			 .setFields("id, name")
			 .execute();
			ZipUtils.createOrModiFyPropertiesFile(MainFrame.getServerName() + "-" + getProviderName(), todayDate.toString(), lastServerBackUpDate);
		}
		catch(GoogleJsonResponseException e) {
			JOptionPane.showMessageDialog(null, "Unable to upload file: " + e.getDetails(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible or another thing went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	public boolean iAmOwner() {
		String rootFolderId = getServerFolderIdWithMetadata("P2PMSS-Backups", metadata, true);
		if(rootFolderId == null) return false; 
		return getServerFolderId(MainFrame.getServerName(), rootFolderId) != null;
	}

	@Override
	public boolean downloadServerBackup(Path serverDestinataryFolder) {
		if(!hasBackUp()) return false;
		
		if(!ZipUtils.existsDirectory(serverDestinataryFolder)) ZipUtils.createDirectory(serverDestinataryFolder);
		
		String parentFolderId = getServerFolderIdWithMetadata("P2PMSS-Backups", metadata, iAmOwner());
		String serverFolderId = getOrCreateServerFolderId(MainFrame.getServerName(), parentFolderId, metadataChildren, iAmOwner(), false);
		if(serverFolderId == null) serverFolderId = getOrCreateServerFolderId(MainFrame.getServerName(), null, metadataChildren, iAmOwner(), false);
		if(!Files.exists(ZipUtils.DOWNLOADS_BACKUPS_ZIPS_FOLDER)) ZipUtils.createDirectory(ZipUtils.DOWNLOADS_BACKUPS_ZIPS_FOLDER);
		try {
			
			FileList result = drive.files().list()
					.setQ("'"+ serverFolderId +"' in parents and trashed = false")
					.setOrderBy("createdTime desc")
					.setPageSize(1)
					.setFields("files(id, name, createdTime)")
					.execute();
			
			if(result.getFiles().isEmpty()) return false; //As a security fallBack.
			
			
			com.google.api.services.drive.model.File serverRemoteZip;
			
			try(OutputStream out = Files.newOutputStream(ZipUtils.DOWNLOADS_BACKUPS_ZIPS_FOLDER.resolve(result.getFiles().get(0).getName() + ".zip"))){
				serverRemoteZip = result.getFiles().get(0);
				drive.files().get(serverRemoteZip.getId()).executeMediaAndDownloadTo(out);
			}
			
			String ramAlloc = ForgeUtils.getServerRAMAlloc(serverDestinataryFolder);
			
			ZipUtils.deleteDirectory(serverDestinataryFolder);
			ZipUtils.createDirectory(serverDestinataryFolder);
			
			ZipUtils.unzip(Path.of(ZipUtils.DOWNLOADS_BACKUPS_ZIPS_FOLDER.toString() + "/" + serverRemoteZip.getName() + ".zip"), serverDestinataryFolder);
			
			try {ForgeUtils.setServerRAMAlloc(serverDestinataryFolder, Integer.parseInt(ramAlloc.replaceAll("[-Xmx|G]", "")));} catch (Exception ignored) {ignored.printStackTrace();}
			
			ZipUtils.createOrModiFyPropertiesFile(MainFrame.getServerName() + "-" + getProviderName(), serverRemoteZip.getCreatedTime().toString().replace("Z", ""), lastServerBackUpDate);
			
			return true;
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Something went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean hasBackUp() {
		if(drive == null) return false;
		 
		String appFolderId = getServerFolderIdWithMetadata("P2PMSS-Backups", metadata);
		
		String serverFolderId = getOrCreateServerFolderId(MainFrame.getServerName(), appFolderId, metadataChildren, iAmOwner(), false);
		if(serverFolderId == null) {
			serverFolderId = getOrCreateServerFolderId(MainFrame.getServerName(), null, metadataChildren, iAmOwner(), false);
			if(serverFolderId == null) return false;
		}
		
		try {
			FileList backupsFileList = drive.files().list()
			.setQ("'" + serverFolderId + "' in parents and trashed = false")
			.setSupportsAllDrives(true)
			.setIncludeItemsFromAllDrives(true)
			.setFields("files(id, name, createdTime)")
			.execute();
			
			List<com.google.api.services.drive.model.File> backups = backupsFileList.getFiles();
			if(backups.isEmpty()) return false;
			else if(isSearchingBackUpForClonning) return true;
			
			String savedLocalDateString = ZipUtils.getDataFromPropertiesFile(MainFrame.getServerName() + "-" + getProviderName(), lastServerBackUpDate);
			if(savedLocalDateString == null)  savedLocalDateString = LocalDateTime.of(LocalDate.of(2005, 3, 8), LocalTime.of(12, 35, 32, 456)).toString(); //Default date if not specified.
			
			LocalDateTime savedLocalDate = LocalDateTime.parse(savedLocalDateString);
			if(backups.stream().filter(file -> LocalDateTime.parse(file.getCreatedTime().toString().replace("Z", "")).isAfter(savedLocalDate)).toList().size() < 1)
				return false;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public String getProviderName() {
		return "GoogleDrive";
	}
	
	private String getServerFolderId(String folderName, String parentFolder) {
		return getOrCreateServerFolderId(folderName, parentFolder, null, true, false);
	}
	
	private String getServerFolderIdWithMetadata(String folderName, Map<String, String> metadata) {
		return getOrCreateServerFolderId(folderName, null, metadata, true, false);
	}
	
	private String getServerFolderIdWithMetadata(String folderName, Map<String, String> metadata, boolean owneds) {
		return getOrCreateServerFolderId(folderName, null, metadata, owneds, false);
	}
	
	private String getOrCreateServerFolderId(String folderName) {
		return getOrCreateServerFolderId(folderName, null, null, true, true);
	}
	
	private String getOrCreateServerFolderIdWithMetadata(String folderName, Map<String, String> metadata) {
		return getOrCreateServerFolderId(folderName, null, metadata, true, true);
	}
	
	
	
	private String getOrCreateServerFolderId(String folderName, String parentFolder, Map<String, String> metadata, boolean owneds, boolean createIfNotExists) {
		String queryString = "name='%s' and mimeType='application/vnd.google-apps.folder' and trashed=false";
		
		if(parentFolder != null) queryString = "name='%s' and mimeType='application/vnd.google-apps.folder' and '%s' in parents and trashed=false";

	    String query = String.format(
	            queryString,
	            folderName.replace("'", "\\'"),
	            "%s"
	    );
	    
	    if(owneds) query += " and 'me' in owners";
	    
	    if(parentFolder != null) 
		    query = String.format(
		    		query,
		    		parentFolder
		    );
	    
	    if(metadata != null) 
	    	query += " and appProperties has { key='app' and value='"+ APPLICATION_NAME +"' } and appProperties has { key='type' and value='" + metadata.get("type") +"' }";
	    
	    try {
		    FileList result = drive.files().list()
		            .setQ(query)
		            .setSpaces("drive")
		            .setFields("files(id, name)")
		            .execute();
		    
		    if (!result.getFiles().isEmpty()) {
		    	return result.getFiles().get(0).getId();
		    }
	    }
	    catch(IOException ioe) {
	    	if(ioe instanceof TokenResponseException toke) {
	    		String errorMessage = toke.getContent();
	    		if(errorMessage.contains("Token") && errorMessage.contains("expired")) {
	    			closeLocalSession();
	    			authenticate();
	    		}
	    	}
	    }
	    
	    if(!createIfNotExists) return null;

	    try {

		    com.google.api.services.drive.model.File folderMetadata =
		            new com.google.api.services.drive.model.File();

		    folderMetadata.setName(folderName);
		    folderMetadata.setMimeType("application/vnd.google-apps.folder");
		    if(parentFolder != null) folderMetadata.setParents(List.of(parentFolder));
		    if(metadata != null) folderMetadata.setAppProperties(metadata);

		    com.google.api.services.drive.model.File folder = drive.files()
		            .create(folderMetadata)
		            .setFields("id")
		            .execute();

		    return folder.getId();
	    }
	    catch(IOException ioe) {
			JOptionPane.showMessageDialog(null, "File " + folderName + " not found or inaccessible or another thing went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			ioe.printStackTrace();
	    }
	    return null;
	}

	@Override
	public void closeSession() {
	    try {
	        String revokeUrl = "https://oauth2.googleapis.com/revoke?token="
	                + this.credential.getAccessToken();
	        
	        Map<String, String> params = new HashMap<>();
	        params.put("token", credential.getRefreshToken());

	        UrlEncodedContent content = new UrlEncodedContent(params);

	        HttpRequestFactory requestFactory =
	                GoogleNetHttpTransport.newTrustedTransport()
	                        .createRequestFactory();

	        HttpRequest request =
	                requestFactory.buildPostRequest(
	                        new GenericUrl(revokeUrl),
	                        content);

	        request.execute();
	        
	        ZipUtils.deleteDirectory(Path.of(CREDENTIALS_FOLDER));
	        
	        this.credential = null;
	        this.drive = null;

	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	public void closeLocalSession() {
        ZipUtils.deleteDirectory(Path.of(CREDENTIALS_FOLDER));
        
        this.credential = null;
        this.drive = null;
	}

	@Override
	public boolean isSessionOpened() {
		return credential != null & drive != null;
	}

	@Override
	public boolean inviteUser(String email) {
		if(drive == null) return false;
		
		String parentFolderId = getServerFolderIdWithMetadata("P2PMSS-Backups", metadata);
		String folderId;
		
		folderId = getServerFolderId(MainFrame.getServerName(), parentFolderId);
		
		if(folderId == null) {
			JOptionPane.showMessageDialog(null, "Only the owner of this server can invite new hosts.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		Permission permission = new Permission()
		        .setType("user")
		        .setRole("writer")
		        .setEmailAddress(email);

		try {
			drive.permissions().create(folderId, permission)
			        .setSendNotificationEmail(true)
			        .setFields("id")
			        .execute();
			
			return true;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "File not found or inaccessible or another thing went wrong, try again.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean hasRemoteServerFolder() {
		if(drive == null) return false;
		String parentFolderId = getServerFolderIdWithMetadata("P2PMSS-Backups", metadata);
		String serverFolder = getOrCreateServerFolderId(MainFrame.getServerName(), parentFolderId, metadataChildren, iAmOwner(), false);
		if(serverFolder == null) getOrCreateServerFolderId(MainFrame.getServerName(), null, metadataChildren, iAmOwner(), false);
		return serverFolder != null;
	}

	@Override
	public void createSavingFolder() {
		if(drive == null) return;
		
		String parentId = getOrCreateServerFolderIdWithMetadata("P2PMSS-Backups", metadata);
		String backupFolderId = getOrCreateServerFolderId(MainFrame.getServerName(), parentId, metadataChildren, true, true);
		
		ZipUtils.createZip(MainFrame.getServerOpenedDirectory().toPath(), ZipUtils.BACKUPS_ZIPS_FOLDER);
		uploadServerBackup(ZipUtils.BACKUPS_ZIPS_FOLDER);
		
		if(backupFolderId != null) {
	        JOptionPane.showMessageDialog(
	                null,                   
	                "Backups folder created successfully!",
	                "Google Drive",
	                JOptionPane.INFORMATION_MESSAGE
	        );
	        // Button visibility is now handled within each ServerTab
		}
	}
	
	@Override
	public Map<String, Object> getUserInfo() {
		if (drive == null) return null;

	    try {
	        About about = drive.about()
	            .get()
	            .setFields("user(displayName,emailAddress,permissionId)")
	            .execute();

	        Map<String, Object> user = new HashMap<>();
	        user.put("profilePhoto", about.getUser().getPhotoLink());
	        user.put("displayName", about.getUser().getDisplayName());
	        user.put("email", about.getUser().getEmailAddress());
	        user.put("permissionId", about.getUser().getPermissionId());

	        return user;

	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	@Override
	public List<String> getInvitedFolderList() {
		if(drive == null) return null;
		String query =
		        "mimeType='application/vnd.google-apps.folder' " +
		        "and sharedWithMe " +
		        "and appProperties has { key='app' and value='" + APPLICATION_NAME + "' }  and trashed=false";

	    List<String> result = new ArrayList<>();
	    String pageToken = null;

	    do {
	        FileList files;
			try {
				files = drive.files().list()
				    .setQ(query)
				    .setFields(
				        "nextPageToken, files(id, name, owners, parents, appProperties)"
				    )
				    .setPageToken(pageToken)
				    .execute();
				
		        files.getFiles().forEach(file -> result.add(file.getId()));
		        pageToken = files.getNextPageToken();
			} catch (IOException e) {
				e.printStackTrace();
			}

	    } while (pageToken != null);

	    return result;
	}
	
	public List<String> getRelevantFolderInfo(String folderId) {

	    PermissionList permissionList;
		try {
			permissionList = drive.permissions()
			    .list(folderId)
			    .setFields(
			        "permissions(id, emailAddress, displayName, role, type)"
			    )
			    .execute();
			
			com.google.api.services.drive.model.File folder = drive.files()
				    .get(folderId)
				    .setFields("name")
				    .execute();
			
		    for (Permission p : permissionList.getPermissions()) {
		        if ("user".equals(p.getType())
		            && p.getEmailAddress() != null
		            && !p.getEmailAddress().equals("me")) {

		            return List.of(p.getEmailAddress(), folder.getName());
		        }
		    }
		    
		} catch (IOException e) {
			e.printStackTrace();
		}

	    return null;
	}
}
