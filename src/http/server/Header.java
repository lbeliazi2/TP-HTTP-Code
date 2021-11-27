package http.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Header {
    private String version;
    private String status;
    private FileTime lastModified;
    private FileTime creationDate;
    private long contentLength;
    private String contentType;

    public Header() {

    }

    /** Constructeur du header
     * @param v version
     * @param s status
     * @param l date de la dernière modification
     * @param c date de création
     * @param cl taille du fichier
     * @param ct type de fichier
     */
    public Header (String v, String s, FileTime l, FileTime c , long cl, String ct){
        version = v;
        status = s;
        lastModified = l;
        creationDate = c;
        contentLength = cl;
        contentType = ct;
    }


    public FileTime getCreationDate() {
        return creationDate;
    }

    public FileTime getLastModified() {
        return lastModified;
    }

    public String getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /** Permet d'initialiser  la date de la dernière modification
     * grâce aux attributs du fichier
     * @param htmlFile fichier
     * @throws IOException recupère les erreurs
     */
    public void setLastModified(File htmlFile) throws IOException {
        Path filePath = htmlFile.toPath();
        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
        lastModified = attr.lastModifiedTime();

    }

    public void setStatus(String status) {
        this.status = status;
    }
    /** Permet d'initialiser la taille du fichier
     * grâce aux attributs du fichier
     * @param htmlFile fichier
     * @throws IOException recupère les erreurs
     */
    public void setContentLength(File htmlFile) throws IOException {
        Path filePath = htmlFile.toPath();
        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
        contentLength = attr.size();
    }
    /** Permet d'initialiser le type du fichier selon son nom
     * grâce aux attributs du fichier
     * @param file fichier
     */
    public void setContentType(String file) {
        String type = file.substring((file.lastIndexOf(".") + 1));
        switch (type){
            case "html":
                contentType = "text/html";
                break;
            case "mp4":
                contentType = "video/mp4";
                break;
            case "png":
                contentType = "image/png";
                break;
            case "mp3":
                contentType = "audio/mp3";
                break;
            case "jpeg":
                contentType = "image/jpg";
                break;

        }
    }
    /** Permet d'initialiser la date de création
     * grâce aux attributs du fichier
     * @param htmlFile fichier
     * @throws IOException recupère les erreurs
     */
    public void setCreationDate(File htmlFile) throws IOException {
        Path filePath = htmlFile.toPath();
        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
        creationDate = attr.creationTime();
    }
    /** Affiche le header en mode erreur
     * Soit sans les paramètres en plus comme les dates
     * @return String
     */
    protected String error() {
        String header = version + " " + status + "\r\n";
        header += "Content-Type: " + contentType + "\r\n";
        header += "Content-Length: " + contentLength + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println("ANSWER HEADER :");
        System.out.println(header);
        return header;
    }
    @Override
    /** Affiche le header
     */
    public String toString() {
        String header = version + " " + status + "\r\n";
        header += "Content-Type: " + contentType + "\r\n";
        header += "Content-Length: " + contentLength + "\r\n";
        header += "Creation-Date: " + creationDate + "\r\n";
        header += "Last-Modified: " + lastModified + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println("ANSWER HEADER :");
        System.out.println(header);
        return header;
    }

}
