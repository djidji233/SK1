package specification;

public class User {
    private String username;
    private String password;
    private boolean privilegeAdmin;
    private boolean privilegeUpload;
    private boolean privilegeDownload;
    private boolean privilegeDelete;
    private boolean privilegeSearch = true;

    public User(String name, String pass, boolean admin, boolean upload, boolean download, boolean delete){
        this.username=name;
        this.password=pass;
        this.privilegeAdmin=admin;
        if(admin){
            this.privilegeUpload = true;
            this.privilegeDownload = true;
            this.privilegeDelete = true;
        } else {
            this.privilegeUpload = upload;
            this.privilegeDownload = download;
            this.privilegeDelete = delete;
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isPrivilegeAdmin() {
        return privilegeAdmin;
    }

    public boolean isPrivilegeUpload() {
        return privilegeUpload;
    }

    public boolean isPrivilegeDownload() {
        return privilegeDownload;
    }

    public boolean isPrivilegeDelete() {
        return privilegeDelete;
    }

    public boolean isPrivilegeSearch() {
        return privilegeSearch;
    }

    @Override
    public String toString() {
        return username+","+password+","+privilegeAdmin+","+privilegeUpload
                +","+privilegeDownload+","+privilegeDelete+","+privilegeSearch;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof User){
            User u = (User) obj;
            return this.username == u.username && this.password == u.password;
        }
        return false;
    }
}
