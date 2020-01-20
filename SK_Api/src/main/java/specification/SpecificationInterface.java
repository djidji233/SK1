package specification;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public interface SpecificationInterface {
    // pri zvanju metoda u konzoli regex je: >>>
    /**
     * incijalizacija roota skladista, singleton
     * @param username - account credentials
     * @param password - account credentials
     * @param path - path roota
     *  local - kompletan path do novog roota skladista npr. C:\Users\Djordje Zivanovic\SK\root
     *  remote - samo naziv tog novog roota npr. /storage
     */
    void initialiseStorage(String username,String password,String path);

    /**
     * dodaje novog usera u fajl koji predstavlja bazu usera i u userArray u okviru implementacije
     * @param username - korisnicko ime
     * @param password - lozinka
     * @param admin - privilegije za admina
     * @param upload - privilegije za upload
     * @param download - privilegije za download
     * @param delete - privilegije za delete
     * console call: add_user
     */
    void addUser(String username,String password,boolean admin, boolean upload, boolean download, boolean delete);

    /**
     * pravi novi/e direktorijum/e na poslatoj putanji
     * @param DirName - putanja od roota u dubinu kako zelis npr. local: \TestDir\Dublje ili remote: /folder/folder2
     *  console call: make_directory
     */
    void makeDirectory(String DirName); // makedir s-1,10 == s1,s2,..,s10

    /**
     * pravi novi fajl na poslatoj putanji
     * @param FileName - putanja od roota u dubinu kako zelis npr. local: \dir\dirrr\fajl.java ili remote: /folder3/blabla.jpg
     * console call: make_file
     */
    void makeFile(String FileName); // makefile s

    /**
     * uzima fajlove sa prosledjenih putanja i uploaduje na path u skladistu
     * @param filePaths - kompletan path to fajla npr. C:\Users\Djordje Zivanovic\Desktop\Slike\slika.png
     * @param path - od roota gde zelis da uploadujes npr. local: \TestDir ili remote: /folder
     * console call: upload
     */
    void upload(String[] filePaths,String path); //jednog ili vise

    /**
     * uzima fajlove sa prosledjenih putanja, zipuje ih i uploaduje na putanju
     * @param path - putanja od roota gde zelimo da uploadujemo npr. local: \TestDir ili remote: /folder
     * @param nameOfZip - ime zipovanog fajla
     * @param filePaths - kompletne putanje do fajlova koje zelimo da zipujemo npr. C:\Users\Djordje Zivanovic\Desktop\Slike\slika.png
     * console call: upolad_to_be_zipped
     */
    void uploadToBeZipped(String path, String nameOfZip, String[] filePaths);

    /**
     * prakticno kopira sa source patha na destination path
     * source je nebitno da li je direktorijum ili fajl
     * @param sourcePath - mora potpun path npr. C:\Users\Djordje Zivanovic\Desktop\Slike\slika.png
     * @param destinationPath - mora potpun path ukljucujuci i root npr. C:\Users\Djordje Zivanovic\SK\root
     * console call: download
     */
    void download(String sourcePath, String destinationPath);

    /**
     * brise direktorijum
     * @param dirName - path o roota npr. local: \TestDir ili remote: /folder
     * console call: delete_directory
     */
    void deleteDirectory(String dirName);

    /**
     * brise fajl
     * @param fileName - path od roota npr. local: \TestDir\fajl.txt ili remote /folder/fajl.txt
     * console call: delete_file
     */
    void deleteFile(String fileName);

    /**
     * prikazuje sta se nalazi na odredjenoj putanji(dubini) u skladistu
     * @param path - path od roota npr. local: \TestDir ili remote /folder
     * @param filter - moze biti vrednosti: all,dir,file
     * console call: preview_storage
     */
    void previewStorage(String path, String filter);

    /**
     * proverava da li je nalog sa tim usernameom admin
     * @param username - korisnicko ime
     * @return
     * console call: is_admin
     */
    boolean isAdmin(String username);

    /**
     * proverava da li je zabranjena ekstenzija
     * @param extension - ekstenzija
     * @return
     * console call: is_restricted_extension
     */
    boolean isRestrictedExtension(String extension); //ADMIN Privilege

    /**
     * dodaje zabranjenu ekstenziju
     * @param extension
     * console call: add_restricted_extension
     */
    void addRestrictedExtension(String extension);

    /**
     * uklanja zabranjenu ekstenziju
     * @param extension
     * console call: remove_restricted_extension
     */
    void removeRestrictedExtension(String extension);

    /**
     * kreira ili dopunjava vec postojeci meta fajl
     * @param path - kompletan path do fajla sa sve ekstenzijom npr. local: C: \Users \Djordje Zivanovic \SK \root\ users.txt
     *               ili remote /storage/users.txt
     * @param key - kljuc koji ide u JSON meta
     * @param value - vrednost koja ide u JSON meta
     * console call: set_meta
     */
    void setMeta(String path, String key, String value);

    /**
     *  uzima value iz mete za prosledjen key
     * @param path - kompletan path do fajla sa sve ekstenzijom npr. local: C: \Users \Djordje Zivanovic \SK \root\ users.txt
     *               ili remote /storage/users.txt
     * @param key - kljuc po kom trazi
     * @return
     * console call: get_meta
     */
    boolean getMeta(String path,String key);

    /**
     * odjavljivanje
     * restartuje currentUsera
     * console call: logout
     */
    void logOut();
}
