package implementation;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v1.DbxEntry;
import com.dropbox.core.v1.DbxWriteMode;
import com.dropbox.core.v2.files.*;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import specification.SpecificationInterface;
import specification.User;

import java.io.*;
import java.nio.file.Path;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RemoteStorageImpl extends AbstractDropboxProvider implements SpecificationInterface {

    private static RemoteStorageImpl instance = null;
    private String root;
    private File users;
    private User currentUser;
    private ArrayList<User> usersArray;
    private ArrayList<String> restrictedExtensions;
    private int zipCount = 0;

    private RemoteStorageImpl() {

    }

    public static RemoteStorageImpl getInstance() {
        if (instance == null)
            instance = new RemoteStorageImpl();
        return instance;
    }

    public void setCurrentUser(String username) {
        for (User u : usersArray) {
            if (u.getUsername().equals(username)) {
                currentUser = u;
                System.out.println("users array -> "+usersArray);
                //System.out.println("setCurrentUser() [LocalStorageImpl] current user became: " + username);
            }
        }
        if (!currentUser.getUsername().equals(username)) {
            System.out.println("setCurrentUser() [LocalStorageImpl] user you want to set doesn't exist");
        }
    }

    private boolean findIfFileExists(String path) {
        try {
            Metadata result = getClient().files().getMetadata(path); // kompletan path
            if (result.getPathDisplay().equals(path)) {
                return true;
            }
        } catch (Exception e) {
            //System.out.println("exception u findIfFileExists() [RemoteStorageImpl] -> verovatno path ne postoji");
            //e.printStackTrace();
        }
        return false;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void initialiseStorage(String username, String password, String path) {
        usersArray = new ArrayList<>();
        restrictedExtensions = new ArrayList<>();
        try {
            root = path;
            if (!findIfFileExists(root)) {
                getClient().files().createFolderV2(root);

                users = new File("users.txt");
                try {
                    users.createNewFile();
                    User u = new User(username, password, true, true, true, true);
                    if (!usersArray.contains(u))
                        usersArray.add(u);
                    setCurrentUser(username);

                    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(users)));
                    writer.println(u);
                    writer.close();
                } catch (Exception e) {
                    System.out.println("exception u initialiseStorage() [LocalStorageImpl]");
                    e.printStackTrace();
                }
                InputStream inputStream = new FileInputStream(users);
                getClient().files().uploadBuilder(root + "/" + users.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                inputStream.close();
                users.delete();
            } else {
                try {
                    //download
                    File users = new File("users.txt");
                    OutputStream outs = new FileOutputStream(users);
                    getClient().files().download(root + "/users.txt").download(outs);
                    //read
                    BufferedReader br = new BufferedReader(new FileReader(users));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        User u = new User(parts[0], parts[1], Boolean.parseBoolean(parts[2]), Boolean.parseBoolean(parts[3])
                                , Boolean.parseBoolean(parts[4]), Boolean.parseBoolean(parts[5]));
                        if (!usersArray.contains(u))
                            usersArray.add(u);
                    }
                    for (User u : usersArray) {
                        if (u.getUsername().equals(username)) {
                            setCurrentUser(username);
                        }
                    }
                    if (currentUser == null) {
                        System.out.println("This user doesn't exist. Please log in with existing user.");
                    }
                    br.close();
                    users.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            System.out.println("exception u initialiseStorage() [RemoteStorageImpl]");
            e.printStackTrace();
        }
    }

    public void addUser(String username, String password, boolean admin, boolean upload, boolean download, boolean delete) {
        if (currentUser.isPrivilegeAdmin()) {
            try {
                File users = new File("users.txt");
                OutputStream outs = new FileOutputStream(users);
                getClient().files().download(root + "/users.txt").download(outs);

                User u = new User(username, password, admin, upload, download, delete);
                if (!usersArray.contains(u))
                    usersArray.add(u);
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(users, true)));

                writer.println(u);
                writer.close();

                InputStream inputStream = new FileInputStream(users);
                getClient().files().uploadBuilder(root + "/" + users.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                outs.close();
                inputStream.close();
                users.delete();

            } catch (Exception e) {
                System.out.println("exception u addUser() [RemoteStorageImpl]");
                e.printStackTrace();
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void makeDirectory(String DirName) {
        // path od roota
        if (currentUser.isPrivilegeUpload()) {
            if (!findIfFileExists(root + DirName)) {
                try {
                    getClient().files().createFolderV2(root + DirName);
                } catch (Exception e) {
                    System.out.println("exception u makeDirectory() [RemoteStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("makeDirectory() [RemoteStorageImpl] - that directory already exists");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void makeFile(String fileName) {
        // path od roota
        if (currentUser.isPrivilegeUpload()) {
            if (!findIfFileExists(root + fileName)) {
                String[] parts = fileName.split("\\.");
                int ind = parts[0].lastIndexOf("/");
                String dirs = parts[0].substring(0, ind);
                makeDirectory(dirs);

                if (!restrictedExtensions.contains(parts[1])) {
                    File f = new File(parts[0].substring(ind + 1));
                    try {
                        f.createNewFile();
                        InputStream inputStream = new FileInputStream(f);
                        getClient().files().uploadBuilder(root + dirs + "/" + f.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                        inputStream.close();
                        f.delete();
                    } catch (Exception e) {
                        System.out.println("exception u makeFile() [RemoteStorageImpl]");
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Pokusavas da napravis fajl sa zabranjenom ekstenzijom");
                }
            } else {
                System.out.println("makeFile() [RemoteStorageImpl] - that file already exists");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void upload(String[] paths, String path) {
        // path od roota
        if (currentUser.isPrivilegeUpload()) {
            if (findIfFileExists(root + path)) {
                //making file array
                ArrayList<File> fileArray = new ArrayList<>();
                for (String s : paths) {
                    File tmp = new File(s);
                    fileArray.add(tmp);
                }
                //work
                for (File f : fileArray) {
                    if (!restrictedExtensions.contains(FilenameUtils.getExtension(f.getName()))) {
                        try {
                            InputStream inputStream = new FileInputStream(f);
                            getClient().files().uploadBuilder(root + path + "/" + f.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                            inputStream.close();
                        } catch (Exception e) {
                            System.out.println("exception u upload() [RemoteStorageImpl]");
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("upload() -> restricted extension!");
                    }
                }
            } else {
                System.out.println("upload() [RemoteStorageImpl] path ne postoji");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void uploadToBeZipped(String path, String nameOfZip, String[] filePaths) {
        //path od roota
        if (currentUser.isPrivilegeUpload()) {
            boolean restricted = false; //flag da li zipuje zabranjenu ekstenziju
            if (findIfFileExists(root + path)) {
                try {
                    //making file array
                    ArrayList<File> srcFiles = new ArrayList<>();
                    for (String s : filePaths) {
                        File tmp = new File(s);
                        if (restrictedExtensions.contains(FilenameUtils.getExtension(tmp.getName())))
                            restricted = true;
                        srcFiles.add(tmp);
                    }
                    //work
                    if (!restricted) {
                        FileOutputStream fos;
                        if (!nameOfZip.equals("")) {
                            fos = new FileOutputStream(nameOfZip + ".zip");
                        } else {
                            fos = new FileOutputStream("DefaultZipName" + zipCount + ".zip");
                            zipCount++;
                        }

                        ZipOutputStream zipOut = new ZipOutputStream(fos);
                        for (File fileToZip : srcFiles) {
                            FileInputStream fis = new FileInputStream(fileToZip);
                            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                            zipOut.putNextEntry(zipEntry);

                            byte[] bytes = new byte[1024];
                            int length;
                            while ((length = fis.read(bytes)) >= 0) {
                                zipOut.write(bytes, 0, length);
                            }
                            fis.close();
                        }
                        zipOut.close();
                        fos.close();

                        File zip;
                        if (!nameOfZip.equals("")) {
                            zip = new File(nameOfZip + ".zip");
                        } else {
                            zip = new File("DefaultZipName" + (zipCount - 1) + ".zip");
                        }

                        InputStream inputStream = new FileInputStream(zip);
                        getClient().files().uploadBuilder(root + path + "/" + zip.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                        inputStream.close();
                        zip.delete();
                    } else {
                        System.out.println("uploadToBeZipped() [RemoteStorageImpl] - pokusavas da zipujes zabranjenu ekstenziju");
                    }

                } catch (Exception e) {
                    System.out.println("exception u uploadToBeZipped() [LocalStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("uploadToBeZipped() [RemoteStorageImpl] path ne postoji");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void download(String sourcePath, String destinationPath) {
        // moraju potpuni pathovi
        if (currentUser.isPrivilegeDownload()) {
            OutputStream downloadFile;
            if (findIfFileExists(sourcePath)) {
                try {
                    int index = sourcePath.lastIndexOf("/");
                    String fname = sourcePath.substring(index + 1);
                    downloadFile = new FileOutputStream(destinationPath + File.separator + fname);
                    FileMetadata metadata = getClient().files().downloadBuilder(sourcePath).download(downloadFile);
                    downloadFile.close();
                } catch (Exception e) {
                    System.out.println("exception u download() [RemoteStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("download() [RemoteStorageImpl] - file doesn't exist");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void deleteDirectory(String dirName) {
        // od roota pa na dalje
        if (currentUser.isPrivilegeDelete()) {
            if (findIfFileExists(root + dirName)) {
                try {
                    DeleteResult metadata = getClient().files().deleteV2(root + dirName);
                } catch (Exception e) {
                    System.out.println("exception u deleteDirectory() [RemoteStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("deleteDirecotry() [RemoteStorageImpl] - directory doesn't exist");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void deleteFile(String fileName) {
        // od roota pa na dalje
        if (currentUser.isPrivilegeDelete()) {
            if (findIfFileExists(root + fileName)) {
                try {
                    DeleteResult metadata = getClient().files().deleteV2(root + fileName);
                } catch (Exception e) {
                    System.out.println("exception u deleteFile() [RemoteStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("deleteFile() [RemoteStorageImpl] - file doesn't exist");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void previewStorage(String path, String filter) {
        // path od roota
        try {
            ListFolderResult listing = getClient().files().listFolderBuilder(root + path).start();
            switch (filter) {
                case "all":
                    for (Metadata child : listing.getEntries()) {
                        //print (child.getPathDisplay(),child.getPathDisplay());
                        System.out.println(child.getPathDisplay());
                    }
                    break;
                case "dir":
                    for (Metadata child : listing.getEntries()) {
                        if (child instanceof FolderMetadata)
                            System.out.println(child.getPathDisplay());
                    }
                    break;
                case "file":
                    for (Metadata child : listing.getEntries()) {
                        if (child instanceof FileMetadata)
                            System.out.println(child.getPathDisplay());
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("exception u previewStorage() [RemoteStorageImpl]");
            e.printStackTrace();
        }
    }

    public boolean isAdmin(String username) {
        boolean flag = false;
        for (User u : usersArray) {
            if (u.getUsername().equals(username)) {
                if (u.isPrivilegeAdmin())
                    flag = true;
            }
        }
        return flag;
    }

    public boolean isRestrictedExtension(String extension) {
        return restrictedExtensions.contains(extension);
    }

    public void addRestrictedExtension(String extension) {
        if (currentUser.isPrivilegeAdmin()) {
            restrictedExtensions.add(extension);
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void removeRestrictedExtension(String extension) {
        if (currentUser.isPrivilegeAdmin()) {
            restrictedExtensions.remove(extension);
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void setMeta(String path, String key, String value) {
        // mora potpun path
        if (currentUser.isPrivilegeAdmin()) {
            if (findIfFileExists(path)) {
                int pos = path.lastIndexOf(".");
                int ind = path.lastIndexOf("/");
                //da li vec postoji meta
                String tmp1 = path.substring(0, pos);
                tmp1 = tmp1.concat(".meta");
                System.out.println(tmp1);

                String fname = path.substring(ind + 1, pos);

                if (!findIfFileExists(tmp1)) {
                    //meta za taj fajl ne postoji
                    try {

                        File meta = new File(fname + ".meta");
                        JSONObject jsonmeta = new JSONObject();


                        jsonmeta.put(key, value);
                        FileWriter file = new FileWriter(meta);
                        file.write(jsonmeta.toString());
                        file.close();
                        //upload
                        InputStream inputStream = new FileInputStream(meta);
                        String npath = path.substring(0, ind);
                        getClient().files().uploadBuilder(npath + "/" + meta.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                        inputStream.close();
                        meta.delete();
                    } catch (Exception e) {
                        System.out.println("exception u setMeta() [LocalStorageImpl]");
                        e.printStackTrace();
                    }
                } else { //meta vec postoji
                    OutputStream downloadFile;
                    try {
                        //download
                        downloadFile = new FileOutputStream(fname + ".meta");
                        FileMetadata metadata = getClient().files().downloadBuilder(tmp1).download(downloadFile);
                        downloadFile.close();
                        //edit
                        File meta = new File(fname + ".meta");
                        JSONObject jsonmeta;
                        FileReader reader1 = new FileReader(meta);
                        jsonmeta = new JSONObject(new JSONTokener(reader1));

                        jsonmeta.put(key, value);
                        FileWriter file = new FileWriter(meta);
                        file.write(jsonmeta.toString());
                        file.close();
                        reader1.close();
                        //upload
                        InputStream inputStream = new FileInputStream(meta);
                        String npath = path.substring(0, ind);
                        getClient().files().uploadBuilder(npath + "/" + meta.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream);
                        inputStream.close();
                        meta.delete();
                    } catch (Exception e) {
                        System.out.println("exception u download() [RemoteStorageImpl]");
                        e.printStackTrace();
                    }

                }

            } else {
                System.out.println("setMeta() [RemoteStorageImpl] - file doesn't exist");
            }
        } else {
            System.out.println("setMeta() [RemoteStorageImpl] - only admin can do this");
        }
    }

    public boolean getMeta(String path, String key) {
        // mora potpun path
        if (currentUser.isPrivilegeAdmin()) {
            if (findIfFileExists(path)) {
                int pos = path.lastIndexOf(".");
                int ind = path.lastIndexOf("/");
                //da li vec postoji meta
                String tmp1 = path.substring(0, pos);
                tmp1 = tmp1.concat(".meta");
                System.out.println(tmp1);
                String fname = path.substring(ind + 1, pos);
                OutputStream downloadFile;
                try {
                    //download
                    downloadFile = new FileOutputStream(fname + ".meta");
                    FileMetadata metadata = getClient().files().downloadBuilder(tmp1).download(downloadFile);
                    downloadFile.close();
                    //read
                    File meta = new File(fname + ".meta");
                    FileReader reader1 = new FileReader(meta);
                    JSONObject jsonmeta = new JSONObject(new JSONTokener(reader1));
                    String value = jsonmeta.getString(key);
                    System.out.println("getMeta() [LocalStorageImpl] Value is: " + value);
                    reader1.close();
                    meta.delete();
                    return true;

                } catch (Exception e) {
                    System.out.println("exception u getMeta() [LocalStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("getMeta() [RemoteStorageImpl] - that meta doesn't exist");
            }
        } else {
            System.out.println("getMeta() [RemoteStorageImpl] - only admin can do this");
        }
        return false;
    }

    public void logOut() {
        currentUser = null;
        System.out.println("log outovao si se");
    }

}
