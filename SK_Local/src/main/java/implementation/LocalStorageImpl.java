package implementation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import specification.SpecificationInterface;
import specification.User;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class LocalStorageImpl implements SpecificationInterface {

    private static LocalStorageImpl instance = null;
    private File users;
    private File root;
    private User currentUser = null;
    private ArrayList<User> usersArray;
    private ArrayList<String> restrictedExtensions;
    private int zipCount = 0;

    private LocalStorageImpl() {

    }

    public static LocalStorageImpl getInstance() {
        if (instance == null)
            instance = new LocalStorageImpl();
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void initialiseStorage(String username, String password, String path) {
        usersArray = new ArrayList<>();
        restrictedExtensions = new ArrayList<>();
        root = new File(path);
        root.mkdir();
        users = new File(root + File.separator + "users.txt");
        if (users.exists()) {
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
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
        }
    }

    public void addUser(String username, String password, boolean admin, boolean upload, boolean download, boolean delete) {
        if (currentUser.isPrivilegeAdmin()) {
            try {
                User u = new User(username, password, admin, upload, download, delete);
                if (!usersArray.contains(u))
                    usersArray.add(u);
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(users, true)));

                writer.println(u);
                writer.close();
            } catch (Exception e) {
                System.out.println("exception u addUser() [LocalStorageImpl]");
                e.printStackTrace();
            }
        } else {
            System.out.println("samo admin moze da dodaje usere");
        }
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

    public void makeDirectory(String DirName) {
        // od roota u dubinu kako zelis
        if (currentUser.isPrivilegeUpload()) {
            File oldroot = new File(root.getAbsolutePath());
            String[] dirnames = DirName.split(File.separator + File.separator);
            for (int i = 0; i < dirnames.length; i++) {
                File dir = new File(root.getAbsolutePath() + File.separator + dirnames[i]);
                dir.mkdir();
                root = dir;
            }
            root = oldroot;
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void makeFile(String FileName) {
        // od roota u dubinu
        if (currentUser.isPrivilegeUpload()) {
            String[] parts = FileName.split("\\.");
            int ind = parts[0].lastIndexOf(File.separator);
            String dirs = parts[0].substring(0, ind);
            if (dirs.length() > 0)
                makeDirectory(dirs);

            if (!restrictedExtensions.contains(parts[1])) {
                File f = new File(root.getAbsolutePath() + File.separator + FileName);
                try {
                    f.createNewFile();
                } catch (Exception e) {
                    System.out.println("exception u makeFile() [LocalStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Pokusavas da napravis fajl sa zabranjenom ekstenzijom");
            }
        } else {
            System.out.println("You don't have the privilege");
        }

    }

    public void upload(String[] paths, String path) {
        // paths kompletan path do fajla, path od roota gde zelis da uploadujes
        if (currentUser.isPrivilegeUpload()) {
            File pom = new File(root.getAbsolutePath() + path);
            if (pom.exists()) {
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
                            download(f.getAbsolutePath(), root.getAbsolutePath() + path);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("exception u upload() [LocalStorageImpl]");
                        }
                    } else {
                        System.out.println("upload() -> restricted extension!");
                    }
                }
            } else {
                System.out.println("upload() [LocalStorageImpl] path ne postoji");
            }
        } else {
            System.out.println("You don't have the privilege.");
        }
    }

    public void uploadToBeZipped(String path, String nameOfZip, String[] filePaths) {
        // path od roota
        if (currentUser.isPrivilegeUpload()) {
            File pom = new File(root.getAbsolutePath() + path);
            boolean restricted = false; //flag da li zipuje zabranjenu ekstenziju
            if (pom.exists()) {
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
                            fos = new FileOutputStream(root + path + File.separator + nameOfZip + ".zip");
                        } else {
                            fos = new FileOutputStream(root + path + File.separator + "DefaultZipName" + zipCount + ".zip");
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
                    } else {
                        System.out.println("uploadToBeZipped() [LocalStorageImpl] - pokusavas da zipujes zabranjenu ekstenziju");
                    }

                } catch (Exception e) {
                    System.out.println("exception u uploadToBeZipped() [LocalStorageImpl]");
                    e.printStackTrace();
                }
            } else {
                System.out.println("uploadToBeZipped() [LocalStorageImpl] path ne postoji");
            }
        } else {
            System.out.println("You don't have the privilege");
        }

    }

    public void download(String source, String destination) {
        // moraju potpuni pathovi - ukljucujuci root
        if (currentUser.isPrivilegeDownload()) {
            File src = new File(source);
            File destDir = new File(destination);
            if (src.exists() && destDir.exists()) {

                if (src.isDirectory()) {
                    try {
                        FileUtils.copyDirectoryToDirectory(src, destDir);
                    } catch (IOException e) {
                        System.out.println("exception u dowload() [LocalStorageImpl] copyDirectoryToDirectory()");
                        e.printStackTrace();
                    }
                } else if (src.isFile()) {
                    try {
                        FileUtils.copyFileToDirectory(src, destDir);
                    } catch (IOException e) {
                        System.out.println("exception u dowload() [LocalStorageImpl] copyFileToDirectory()");
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("download() -> file doesn't exist [LocalStorageImpl]");
            }
        } else {
            System.out.println("You don't have the privilege");
        }

    }

    public void deleteDirectory(String dirName) {
        // path od roota
        if (currentUser.isPrivilegeDelete()) {
            File index = new File(root + dirName);
            if (index.exists() && index.isDirectory()) {
                if (index.list().length > 0) {
                    String[] entries = index.list();
                    for (String s : entries) {
                        File currentFile = new File(index.getPath(), s);
                        currentFile.delete();
                    }
                }
                index.delete();
                System.out.println("deleteDirectory() [LocalStorageImpl]: Uspesno obrisan direktorijum");
            } else {
                System.out.println("Greska u deleteDirectory() [LocalStorageImpl] - direktorijum ne postoji");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void deleteFile(String fileName) {
        // path od roota
        if (currentUser.isPrivilegeDelete()) {
            File file = new File(root + fileName);
            if (file.exists() && fileName != "users.txt" && file.isFile()) {
                file.delete();
                System.out.println("deleteFile() [LocalStorageImpl]: Uspesno obrisan fajl");
            } else {
                System.out.println("Greska u deleteFile() [LocalStorageImpl] -> Fajl ili ne postoji ili pokusavas da obrises users.txt");
            }
        } else {
            System.out.println("You don't have the privilege");
        }
    }

    public void previewStorage(String path, String filter) {
        // path od roota
        File[] directories = null;
        switch (filter) {
            case "all":
                directories = new File(root.getAbsolutePath() + path).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return true;
                    }
                });
                break;
            case "dir":
                directories = new File(root.getAbsolutePath() + path).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                break;
            case "file":
                directories = new File(root.getAbsolutePath() + path).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isFile();
                    }
                });
                break;
        }
        //testing
        assert directories != null;
        if (directories.length > 0) {
            for (File directory : directories) {
                System.out.println(directory.getName());
            }
        } else {
            System.out.println("previewStorage() [LocalStorageImpl] no items match your search");
        }
    }

    public boolean isAdmin(String usrnm) {
        boolean flag = false;
        for (User u : usersArray) {
            if (u.getUsername().equals(usrnm)) {
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

    public void logOut() {
        currentUser = null;
        System.out.println("log outovao si se");
    }

    public void setMeta(String path, String key, String value) {
        // mora potpun path
        if (currentUser.isPrivilegeAdmin()) {
            try {
                int pos = path.lastIndexOf(".");
                if (pos > 0)
                    path = path.substring(0, pos);
                File meta = new File(path + ".meta");
                JSONObject jsonmeta;
                if (meta.exists()) {
                    FileReader reader1 = new FileReader(meta);
                    jsonmeta = new JSONObject(new JSONTokener(reader1));
                } else {
                    jsonmeta = new JSONObject();
                }
                jsonmeta.put(key, value);
                FileWriter file = new FileWriter(meta);
                file.write(jsonmeta.toString());
                file.close();
            } catch (Exception e) {
                System.out.println("exception u setMeta() [LocalStorageImpl]");
                e.printStackTrace();
            }
        } else {
            System.out.println("setMeta() [LocalStorageImpl] - only admin can do this");
        }
    }

    public boolean getMeta(String path, String key) {
        // mora potpun path
        if (currentUser.isPrivilegeAdmin()) {
            try {
                int pos = path.lastIndexOf(".");
                if (pos > 0)
                    path = path.substring(0, pos);
                File meta = new File(path + ".meta");
                if (meta.exists()) {
                    FileReader reader1 = new FileReader(meta);
                    JSONObject jsonmeta = new JSONObject(new JSONTokener(reader1));
                    String value = jsonmeta.getString(key);
                    System.out.println("getMeta() [LocalStorageImpl] Value is: " + value);
                    return true;
                } else {
                    System.out.println("getMeta() [LocalStorageImpl] It doesn't exist.");
                }
            } catch (Exception e) {
                System.out.println("exception u getMeta() [LocalStorageImpl]");
                e.printStackTrace();
            }

        } else {
            System.out.println("setMeta() [LocalStorageImpl] - only admin can do this");
        }
        return false;
    }
}
