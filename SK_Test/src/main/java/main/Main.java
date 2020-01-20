package main;

import implementation.LocalStorageImpl;
import implementation.RemoteStorageImpl;

import java.util.Scanner;

public class Main {
    private static String[] credentials;
    private static Scanner skener;
    private static LocalStorageImpl localInstance;
    private static RemoteStorageImpl remoteInstance;
    private static boolean localflag; // true=local false=remote

    public static void main(String[] args) {
        localInstance = LocalStorageImpl.getInstance();
        remoteInstance = RemoteStorageImpl.getInstance();
        skener = new Scanner(System.in);
        work();
    }

    private static void setCredentials() {
        System.out.println("local/remote:");
        String lor = skener.nextLine();
        if (lor.equals("remote")) {
            localflag = false;
        } else if(lor.equals("local")){
            localflag = true;
        }

        System.out.println("username,password,root path:");
        credentials = skener.nextLine().split(",");
        if (localflag)
            localInstance.initialiseStorage(credentials[0], credentials[1], credentials[2]);
        else
            remoteInstance.initialiseStorage(credentials[0], credentials[1], credentials[2]);

        if(localflag){
            while (localInstance.getCurrentUser() == null) {
                System.out.println("username,password,root path:");
                credentials = skener.nextLine().split(",");
                localInstance.initialiseStorage(credentials[0], credentials[1], credentials[2]);
            }
        } else {
            while (remoteInstance.getCurrentUser() == null) {
                System.out.println("username,password,root path:");
                credentials = skener.nextLine().split(",");
                remoteInstance.initialiseStorage(credentials[0], credentials[1], credentials[2]);
            }
        }
    }
    private static void work(){

        setCredentials();

        //loop
        if (localflag) {
            while (true) {
                System.out.println("( regex: >>> ) COMMAND:");
                String[] data = skener.nextLine().split(">>>");
                if(data[0].equals("exit")){
                    System.exit(0);
                    break;
                }
                if(data[0].equals("logout")){
                    localInstance.logOut();
                    work();
                }
                switch (data[0]) {
                    case "add_user":
                        String[] au = data[1].split(",");
                        localInstance.addUser(au[0], au[1], Boolean.parseBoolean(au[2]), Boolean.parseBoolean(au[3]), Boolean.parseBoolean(au[4]), Boolean.parseBoolean(au[5]));
                        break;
                    case "make_directory":
                        localInstance.makeDirectory(data[1]);
                        break;
                    case "make_file":
                        localInstance.makeFile(data[1]);
                        break;
                    case "upload":
                        String[] paths = data[1].split(",");
                        localInstance.upload(paths, data[2]);
                        break;
                    case "upload_to_be_zipped":
                        String[] paths2 = data[3].split(",");
                        localInstance.uploadToBeZipped(data[1],data[2],paths2);
                        break;
                    case "download":
                        localInstance.download(data[1],data[2]);
                        break;
                    case "delete_directory":
                        localInstance.deleteDirectory(data[1]);
                        break;
                    case "delete_file":
                        localInstance.deleteFile(data[1]);
                        break;
                    case "preview_storage":
                        localInstance.previewStorage(data[1],data[2]);
                        break;
                    case "is_admin":
                        System.out.println(localInstance.isAdmin(data[1]));
                        break;
                    case "is_restricted_extension":
                        System.out.println(localInstance.isRestrictedExtension(data[1]));
                        break;
                    case "add_restricted_extension":
                        localInstance.addRestrictedExtension(data[1]);
                        break;
                    case "remove_restricted_extension":
                        localInstance.removeRestrictedExtension(data[1]);
                        break;
                    case "set_meta":
                        localInstance.setMeta(data[1],data[2],data[3]);
                        break;
                    case "get_meta":
                        localInstance.getMeta(data[1],data[2]);
                        break;
                }
            }
        } else {
            while (true) {
                System.out.println("( regex: >>> ) COMMAND:");
                String[] data = skener.nextLine().split(">>>");
                if(data[0].equals("exit")){
                    System.exit(0);
                    break;
                }
                if(data[0].equals("logout")){
                    remoteInstance.logOut();
                    work();
                }
                switch (data[0]) {
                    case "add_user":
                        String[] au = data[1].split(",");
                        remoteInstance.addUser(au[0], au[1], Boolean.parseBoolean(au[2]), Boolean.parseBoolean(au[3]), Boolean.parseBoolean(au[4]), Boolean.parseBoolean(au[5]));
                        break;
                    case "make_directory":
                        remoteInstance.makeDirectory(data[1]);
                        break;
                    case "make_file":
                        remoteInstance.makeFile(data[1]);
                        break;
                    case "upload":
                        String[] paths = data[1].split(",");
                        remoteInstance.upload(paths, data[2]);
                        break;
                    case "upload_to_be_zipped":
                        String[] paths2 = data[3].split(",");
                        remoteInstance.uploadToBeZipped(data[1],data[2],paths2);
                        break;
                    case "download":
                        remoteInstance.download(data[1],data[2]);
                        break;
                    case "delete_directory":
                        remoteInstance.deleteDirectory(data[1]);
                        break;
                    case "delete_file":
                        remoteInstance.deleteFile(data[1]);
                        break;
                    case "preview_storage":
                        remoteInstance.previewStorage(data[1],data[2]);
                        break;
                    case "is_admin":
                        System.out.println(remoteInstance.isAdmin(data[1]));
                        break;
                    case "is_restricted_extension":
                        System.out.println(remoteInstance.isRestrictedExtension(data[1]));
                        break;
                    case "add_restricted_extension":
                        remoteInstance.addRestrictedExtension(data[1]);
                        break;
                    case "remove_restricted_extension":
                        remoteInstance.removeRestrictedExtension(data[1]);
                        break;
                    case "set_meta":
                        remoteInstance.setMeta(data[1],data[2],data[3]);
                        break;
                    case "get_meta":
                        remoteInstance.getMeta(data[1],data[2]);
                        break;
                }
            }
        }
    }
}
