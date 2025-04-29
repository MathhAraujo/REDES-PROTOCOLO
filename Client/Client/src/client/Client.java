package client;

import java.io.*;
import java.net.Socket;
import java.sql.Time;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Client {

    private Socket socket;
    private DataInputStream inServer;
    private DataOutputStream out;
    private Scanner inUser = new Scanner(System.in);
    private static final int port = 3030;
    private static final String stop_string = "##";

    public Client() {
        try {
            socket = new Socket("127.0.0.1", port);
            out = new DataOutputStream(socket.getOutputStream());
            inServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            inUser = new Scanner(System.in);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to connect to server");
        }

        if(handshake()) {
            writeMessages();
        }else{
            close();
        }
    }

    private boolean handshake() {
        String request = "SYN";
        String expectedResponse = "ACK";
        String actualResponse;
        String successfulResponse;

        try {
            out.writeUTF(request);
            actualResponse = inServer.readUTF();
            System.out.println("actualResponse");

            if (!actualResponse.equals(expectedResponse)) {
                throw new IOException();
            }

            successfulResponse = inServer.readUTF();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed handshake");
            return false;
        }

        System.out.println("Successful connection");
        System.out.println(successfulResponse);
        return true;

    }

    private void writeMessages() {
        String line = "";
        String response = "";
        String fullMessage;
        int packageSize = -1;
        int timeoutCount = 0;
        int packageId = 0;

        while (!line.equals(stop_string)) {
            System.out.println("Max message size: 12 characters"); //(2)(12)(2)
            line = inUser.nextLine();
            packageSize = line.length();

            try {
                if (packageSize > 12) {
                    throw new IOException();
                }

                fullMessage = String.format("%02d", packageId) + line + String.format("%02d", packageSize);
                out.writeUTF(fullMessage);

                response = inServer.readUTF();

                if (response.equals("Package limit reached")) {
                    throw new Exception();
                }

                if (response.isBlank() || !response.equals("ACK " + packageId)) {
                    System.out.println("Packet Loss");
                    out.writeUTF(fullMessage);
                    if(!inServer.readUTF().equals("ACK " + packageId)){
                        throw new TimeoutException();
                    }
                }

                System.out.println(response);
                packageId++;

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Package exceeded maximum limit of 12 characters");
            }catch (TimeoutException e){
                e.printStackTrace();
                System.out.println("Lost connection to server");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Package limit reached");
            }
        }
        close();
    }

    private void close() {
        try {
            socket.close();
            inServer.close();
            out.close();
            inUser.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to close client");
        }
    }


    public static void main(String[] args) {
        new Client();
    }
}
