package http.server;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;


public class WebServer {
  private static ServerSocket s;
  private static final String DOSSIER_FICHIERS_HTML = "doc/";
  private static final String NOT_FOUND = "doc/not_found.html";
  private static final String ERROR = "doc/error.html";
  private static final String DELETED = "doc/deleted.html";
  private static final String BAD_REQUEST = "doc/bad_request.html";
  private static final String NOT_IMPLEMENTED = "doc/not_implemented.html";
  private static final String FORBIDDEN_ACCESS = "doc/forbidden_access.html";
  private static final String VERSION = "HTTP/1.1";
  private static final Header h = new Header();


  /**
   * Constructeur du WebServer
   * Cette méthode permet d'initialiser le serveur avec un numéro de port donné.
   * Une fois que le client se connecte au serveur, on lance le menu HTTP pour que le client puisse faire ses requêtes.
   */
  protected void start() {

    System.out.println("Webserver starting up on port 80");
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(3000);
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return;
    }
    System.out.println("Waiting for connection");
    for (;;) {
      try {
        // wait for a connection
        Socket remote = s.accept();
        // remote is now the connected socket
        System.out.println(remote);
        System.out.println("Connection, sending data.");
        // read the request - listen to the message
        BufferedInputStream in = new BufferedInputStream(remote.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(remote.getOutputStream());
        // on appelle le menu dédié à recevoir des requêtes et y répondre
        menu(out,in, remote);
      } catch(Exception e){
          e.printStackTrace();
      }
    }
  }
  /**
   * Cette méthode nous permet de gérer les requêtes HTTP d'un client en continu.
   * Tout d'abord, on lit le header reçu, en y retirant le fichier demandé et le type de requête (GET,POST...)
   * On traite ensuite la demande selon le type de requête en entrant dans les méthodes dédiées
   * Si le type de requête n'a pas été traité dans notre code, on rend un header du statut '501 Not Implemented'
   * Si le fichier donné ne se trouve pas dans le répertoire "/doc", on refuse l'accès (problème de sécurité)
   * Sinon les autres erreurs sont des erreurs système
   * @param  out  permet d'afficher des données au client
   * @param  in   prend en compte les valeurs entrées par le client
   * @param remote nous permet de réutiliser la socket
   * @throws IOException gestion d'erreur
   */
  public void menu (BufferedOutputStream out, BufferedInputStream in, Socket remote) throws IOException {
    byte[] buffer = new byte[256];
    try {
      // on lit le header
      String header = "";
      int c = '\0', bprec = '\0';
      boolean newline = false;
      while((c = in.read()) != -1 && !(newline && bprec == '\r' && c == '\n')) {
        if(bprec == '\r' && c == '\n') {
          newline = true;
        } else if(!(bprec == '\n' && c == '\r')) {
          newline = false;
        }
        bprec = c;
        header += (char) c;
      }
      // on l'affiche sur le serveur pour suivre ce qui se passe
      System.out.println("REQUEST :");
      System.out.println(header);

      // si c = -1 ca veut dire qu'il y a une erreur et le header ne se termine pas par \n
      if (c != -1 && !header.isEmpty()) {
        // on divise le header avec des espaces pour séparer le type de la requête et le nom de fichier
        String[] words = header.split(" ");
        String requestType = words[0];
        String ressource = words[1].substring(1);
        h.setVersion(VERSION);
        System.out.println(ressource);
        // si le fichier n'est pas donné, on renvoit une erreur de bad request
        if (!ressource.isEmpty() && !ressource.equals("doc/")) {
          // si le fichier est dans le dossier des fichiers HTML on fait les requêtes HTTP
          if (ressource.contains(DOSSIER_FICHIERS_HTML)) {
            switch (requestType) {
              // on rentre dans les méthodes dédiées aux différentes requêtes
              case "GET" -> httpGET(out, ressource);
              case "HEAD" -> httpHEAD(out, ressource);
              case "DELETE" -> httpDELETE(out, ressource);
              case "PUT" -> httpPUT(in, out, ressource);
              case "POST" -> httpPOST(in, out, ressource);
              default -> {
                // si le type de requête n'est pas implémentée on renvoit une erreur
                h.setStatus("501 Not Implemented");
                h.setContentType(NOT_IMPLEMENTED);
                File error = new File(NOT_IMPLEMENTED);
                h.setContentLength(error);
                BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
                // on écrit le fichier d'erreur dans le stream de l'utilisateur
                out.write(h.error().getBytes(StandardCharsets.UTF_8));
                int nbRead;
                while ((nbRead = fileIn.read(buffer)) != -1) {
                  out.write(buffer, 0, nbRead);
                }
                out.flush();
                fileIn.close();
              }
            }
            // on reitère sur le menu pour que l'utilisateur puisse continuer à faire des requêtes
            menu(out, in, remote);
          } else {
            // si le document n'est pas dans le dossier dédiée on refuse la requête
            h.setStatus("403 Forbidden");
            h.setContentType(FORBIDDEN_ACCESS);
            File error = new File(FORBIDDEN_ACCESS);
            h.setContentLength(error);
            BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
            out.write(h.error().getBytes(StandardCharsets.UTF_8));
            int nbRead;
            while ((nbRead = fileIn.read(buffer)) != -1) {
              out.write(buffer, 0, nbRead);
            }
            out.flush();
            fileIn.close();
          }
        } else {
          // si le document n'est pas entrée on renvoit bad request
          h.setStatus("400 Bad Request");
          h.setContentType(BAD_REQUEST);
          File error = new File (BAD_REQUEST);
          h.setContentLength(error);
          BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
          out.write(h.error().getBytes(StandardCharsets.UTF_8));
          int nbRead;
          while ((nbRead = fileIn.read(buffer)) != -1) {
            out.write(buffer, 0, nbRead);
          }
          out.flush();
          fileIn.close();
        }
      }
    } catch(Exception e){
      // sinon c'est une erreur système
      e.printStackTrace();
      h.setStatus("500 Internal Server Error");
      h.setContentType(ERROR);
      File error = new File(ERROR);
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
      h.setContentLength(error);
      out.write(h.error().getBytes(StandardCharsets.UTF_8));
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }
      out.flush();
      fileIn.close();
    }
  }
  /**
   * La méthode HTTP PUT est une méthode permettant d'écrire dans un fichier.
   * Si le fichier demandé existe deja, on écrase son contenu et on écrit dedans (Header de status 200 0K)
   * Si il existe pas, on crée le fichier demandé et on écrit dedans (Header de statut 201 Created)
   * On lit ainsi les paramètres dans la requête PUT et on les écrit dans le fichier demandé
   * Cette méthode peut être testée grâce au fichier 'form_testPOST.html' en utilisant POSTMAN
   * Les paramètres sont ensuite écrits dans le document 'doc_testPOST.html'
   * @param in permet de lire les entrées du client
   * @param out permet d'afficher des données au client
   * @param ressource correspond au nom de la ressource
   * @throws IOException gestion des erreurs
   */
  public void httpPUT (InputStream in, BufferedOutputStream out, String ressource) throws IOException {
    byte[] buffer = new byte[256];
    char c;
    try {
      File htmlFile = new File (ressource);
      if (!htmlFile.exists()) {
        h.setStatus("201 Created");
        htmlFile.createNewFile();
      }else {
        h.setStatus("200 OK");
      }
      if (htmlFile.exists()) {
        FileWriter outFile = new FileWriter(ressource, false);
        StringBuilder textBuilder = new StringBuilder();

        // read stream data into buffer
        in.read(buffer);

        // for each byte in the buffer
        for (byte b : buffer) {

          // convert byte to character
          c = (char) b;
          if (b != 0) {
            textBuilder.append(c);
          }
        }
        String[] lines = textBuilder.toString().split("&");
        String content = "";
        for (String s : lines) {
          if (s.contains("=")) {
            String param = s.substring(0, s.indexOf("="));
            String value = s.substring(s.indexOf("=") + 1);
            content += "<p>Param : " + param + " of value :" + value + "</p>\r\n";
          } else {
            content += "<p>" + s + "</p>\r\n";
          }
        }
        outFile.write(content);
        outFile.close();
        h.setContentType(ressource);
        h.setCreationDate(htmlFile);
        h.setLastModified(htmlFile);
        h.setContentLength(htmlFile);
        out.write(h.toString().getBytes(StandardCharsets.UTF_8));
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));

        int nbRead;
        while ((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
      } else {
        h.setStatus("404 Not Found");
        h.setContentType(NOT_FOUND);
        File not_found = new File(NOT_FOUND);
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(not_found));
        h.setContentLength(not_found);
        out.write(h.error().getBytes(StandardCharsets.UTF_8));
        int nbRead;
        while((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
      }
    }catch (Exception e){
      e.printStackTrace();
      h.setStatus("500 Internal Server Error");
      h.setContentType(ERROR);
      File error = new File(ERROR);
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
      h.setContentLength(error);
      out.write(h.toString().getBytes(StandardCharsets.UTF_8));
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }
      out.flush();
      fileIn.close();
    }
  }
  /**
   * La méthode HTTP POST est une méthode permettant d'écrire dans un fichier.
   * Si le fichier demandé existe deja, on écrit à la fin du document (Header de status 200 0K)
   * Si il existe pas, on crée le fichier demandé et on écrit dedans (Header de statut 201 Created)
   * On lit ainsi les paramètres dans la requête POST et on les écrit dans le fichier demandé
   * Cette méthode peut être testée en lancant le formulaire form_testPOST.html
   * Les paramètres sont ensuite écrits dans le document 'doc_testPOST.html'
   * @param in permet de lire les entrées du client
   * @param out permet d'afficher des données au client
   * @param ressource correspond au nom de la ressource
   * @throws IOException gestion d'erreurs
   */
  public void httpPOST (InputStream in, BufferedOutputStream out, String ressource) throws IOException {
    byte[] buffer = new byte[256];
    char c;
    try {
      File htmlFile = new File (ressource);
      if (!htmlFile.exists()) {
        h.setStatus("201 Created");
        htmlFile.createNewFile();
      }else {
        h.setStatus("200 OK");
      }
      if (htmlFile.exists()) {
        // on écrit les paramètres dans le fichier demandé
        FileWriter outFile = new FileWriter(ressource, true);

        StringBuilder textBuilder = new StringBuilder();

        // read stream data into buffer
        in.read(buffer);

        // for each byte in the buffer
        for (byte b : buffer) {

          // convert byte to character
          c = (char) b;
          if (b != 0) {
            textBuilder.append(c);
          }
        }
        String[] lines = textBuilder.toString().split("&");
        String content = "";
        for (String s : lines) {
          // si les valeurs sont encodées sous formes de valeurs separées avec = et &
          // on les divise et on les ajoute au content
          if (s.contains("=")) {
            String param = s.substring(0, s.indexOf("="));
            String value = s.substring(s.indexOf("=") + 1);
            content += "<p>Param: " + param + " of value: " + value + "</p>\r\n";
          } else {
            // sinon on ajoute directement au content sans retoucher
            content += "<p>" + s + "</p>\r\n";
          }
        }
        outFile.write(content);
        outFile.close();
        // on envoit le header + le fichier modifié
        h.setContentType(ressource);
        h.setCreationDate(htmlFile);
        h.setLastModified(htmlFile);
        h.setContentLength(htmlFile);
        out.write(h.toString().getBytes(StandardCharsets.UTF_8));
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));

        int nbRead;
        while ((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
      }else {
        h.setStatus("404 Not Found");
        h.setContentType(NOT_FOUND);
        File not_found = new File(NOT_FOUND);
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(not_found));
        h.setContentLength(not_found);
        out.write(h.error().getBytes(StandardCharsets.UTF_8));
        int nbRead;
        while((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
      }
    }catch (Exception e){
      // erreur système
      e.printStackTrace();
      h.setStatus("500 Internal Server Error");
      h.setContentType(ERROR);
      File error = new File(ERROR);
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
      h.setContentLength(error);
      out.write(h.error().getBytes(StandardCharsets.UTF_8));
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }
      out.flush();
      fileIn.close();
    }
  }

  /**
   * La méthode HTTP DELETE est une méthode permettant de supprimer dans un fichier.
   * Un code de statut 202 (Accepted) si l'action est en passe de réussir mais n'a pas encore été confirmée.
   * Un code de statut 200 (OK) si l'action a été confirmée et que le message de réponse inclut une représentation décrivant le statut.
   * Cette méthode peut être testée gràce en utilisant postman grâce à la requête
   * DELETE localhost:3000/doc/[nom de fichier]
   * @param out permet d'afficher des données au client
   * @param ressource correspond au nom de la ressource
   * @throws IOException gestion d'erreurs
   */
  public void httpDELETE(BufferedOutputStream out, String ressource) throws IOException {
    File htmlFile = new File(ressource);
    byte[] buffer = new byte[256];
    // si le fichier existe on le supprime et on renvoit le header de statut 200 OK
    if (htmlFile.exists()) {
      try {
        h.setStatus("202 Accepted");
        // si le delete a été réussi on renvoit 200 OK
        htmlFile.delete();
        h.setStatus("200 OK");
        h.setContentType(DELETED);
        File delete = new File(DELETED);
        h.setContentLength(delete);
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(delete));
        out.write(h.toString().getBytes(StandardCharsets.UTF_8));
        int nbRead;
        while ((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
        // erreur système
      } catch (Exception e) {
        e.printStackTrace();
        h.setStatus("500 Internal Server Error");
        h.setContentType(ERROR);
        File error = new File(ERROR);
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
        h.setContentLength(error);
        out.write(h.error().getBytes(StandardCharsets.UTF_8));
        int nbRead;
        while((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
      }
      // si le fichier existe pas
    }else {
      h.setStatus("404 Not Found");
      h.setContentType(NOT_FOUND);
      File not_found = new File(NOT_FOUND);
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(not_found));
      h.setContentLength(not_found);
      out.write(h.error().getBytes(StandardCharsets.UTF_8));
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }
      out.flush();
      fileIn.close();
    }
  }

  /**
   * La méthode HTTP HEAD est une méthode permettant d'afficher le header de réponse.
   * Si le fichier demandé existe, on affiche le header (Header de status 200 0K)
   * Si il existe pas, on renvoit le header de status '404 Not Found'
   * Dans le header, on peut retrouver la date de création, la dernière modification,
   * la taille etc.
   * Cette méthode peut être testée en utilisant postman grâce à la requête
   * HEADER localhost:3000/doc/[nom de fichier]
   * @param out permet d'afficher des données au client
   * @param ressource correspond au nom de la ressource
   * @throws IOException gestion d'erreur
   */
  public void httpHEAD(BufferedOutputStream out,String ressource) throws IOException {
    byte[] buffer = new byte[256];
    try {
      // si le fichier existe on renvoit les informations du header
      File htmlFile = new File(ressource);
      if (htmlFile.exists()) {
        h.setStatus("200 OK");
        h.setCreationDate(htmlFile);
        h.setLastModified(htmlFile);
        h.setContentLength(htmlFile);
        h.setContentType(ressource);
        // on affiche la page html demandée
        out.write(h.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
      } else {
        // sinon on renvoit 404 Not Found
        h.setStatus("404 Not Found");
        h.setContentType(NOT_FOUND);
        File not_found = new File(NOT_FOUND);
        h.setContentLength(not_found);
        out.write(h.error().getBytes(StandardCharsets.UTF_8));
        out.flush();
      }
    }catch (Exception e){
      // erreur système
      h.setStatus("500 Internal Server Error");
      h.setContentType(ERROR);
      File error = new File(ERROR);
      h.setContentLength(error);
      out.write(h.error().getBytes(StandardCharsets.UTF_8));
      out.flush();
    }
  }

  /**
   * La méthode HTTP GET est une méthode permettant d'afficher un fichier.
   * Si le fichier demandé existe, on affiche celui-ci (Header de status 200 0K)
   * Si il existe pas, on renvoit le header de status '404 Not Found'
   * Dans le header, on peut retrouver la date de creation, la dernière modification,
   * la taille etc.
   * Cette méthode peut être testée en utilisant postman grâce à la requête
   * GET localhost:3000/doc/[nom de fichier]
   * @param out permet d'afficher des données au client
   * @param ressource correspond au nom de la ressource
   * @throws IOException gestion d'erreur
   */
  public static void httpGET(BufferedOutputStream out, String ressource) throws IOException {
    byte[] buffer = new byte[256];
    try {
      File htmlFile = new File(ressource);
      // si le fichier existe on le renvoit avec le statut 200 OK
      if (htmlFile.exists() && htmlFile.isFile()) {
        h.setStatus("200 OK");
        h.setCreationDate(htmlFile);
        h.setLastModified(htmlFile);
        h.setContentLength(htmlFile);
        h.setContentType(ressource);
        out.write(h.toString().getBytes(StandardCharsets.UTF_8));
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));
        int nbRead;
        while((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        fileIn.close();
        out.flush();
        // si il existe pas on renvoit le header avec le status 404 Not Found
      }else {
        h.setStatus("404 Not Found");
        h.setContentType(NOT_FOUND);
        File not_found = new File(NOT_FOUND);
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(not_found));
        h.setContentLength(not_found);
        out.write(h.error().getBytes(StandardCharsets.UTF_8));
        int nbRead;
        while((nbRead = fileIn.read(buffer)) != -1) {
          out.write(buffer, 0, nbRead);
        }
        out.flush();
        fileIn.close();
      }
      // erreur système
    }catch (Exception e){
      h.setStatus("500 Internal Server Error");
      h.setContentType(ERROR);
      File error = new File(ERROR);
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(error));
      h.setContentLength(error);
      out.write(h.error().getBytes(StandardCharsets.UTF_8));
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }
      out.flush();
      fileIn.close();
    }
  }

  /** La méthode main pour lancer le serveur
   * @param args
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start();
  }
}
