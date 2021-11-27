/***
 * EchoClient
 * Example of a TCP client 
 * Date: 10/01/04
 * Authors:
 */
package http.client;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;


public class WebPing {

    /**
     *  main method
     *  accepts a connection, receives a message from client then sends an echo to the client
     **/
    static Socket clientSocket = null;
    static PrintStream out = null;
    static Scanner sc = new Scanner(System.in);
    static BufferedReader in = null;
    static String httpServerHost = "localhost";
    static int httpServerPort = 3000;

    public static void main(String[] args){

        try {
            // creation socket ==> connexion
            InetAddress addr;
            clientSocket = new Socket(httpServerHost,httpServerPort);
            in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            out= new PrintStream(clientSocket.getOutputStream());
            addr = clientSocket.getInetAddress();
            System.out.println("Connected to " + addr);

            Thread envoyer = new Thread(new Runnable() {
                String msg;
                @Override
                public void run() {
                    while(true){
                        msg = sc.nextLine();
                        out.println(msg);
                        out.flush();
                    }
                }
            });
            envoyer.start();

            Thread recevoir = new Thread(new Runnable() {
                String msg;
                @Override
                public void run() {
                    try {
                        msg = in.readLine();
                        while(!msg.equals("logout")){
                            System.out.println(msg);
                            msg = in.readLine();
                        }
                        System.out.println("Deconnecté");
                        out.close();
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            recevoir.start();

        } catch (IOException e) {
            System.out.println("Can't connect to " + httpServerHost + ":" + httpServerPort);
            System.out.println(e);
        }
    }
}


