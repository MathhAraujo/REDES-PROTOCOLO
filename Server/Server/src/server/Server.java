package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {

    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private static final int port = 3030;
    private static final String stropString = "##";

    public Server() {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to create server");
        }
        initConnection();
    }

    private void initConnection() {
        try {
            Socket clientSocket = server.accept();
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            out = new DataOutputStream(clientSocket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to accept connection");
        }

        if(handshake()){
            readPackages();
        }
        close();
    }

    private boolean handshake() {
        String expectedRequest = "SYN";
        String response = "ACK";
        String successfulResponse = "Server accepts only string format with the max size of 16 characters, with 10 packages maximum";

        try {
            if(!expectedRequest.equals(in.readUTF())) {
                throw new IOException();
            }

            out.writeUTF(response);
            System.out.println("Successful connection");
            out.writeUTF(successfulResponse);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed handshake");
            return false;
        }

        return true;

    }

    private void close() {
        try {
            in.close();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to close the server");
        }
    }

    private void readPackages() {
        String line = "";
        int recievedSize = -1;
        int packageIdBefore = -1;
        int packageId;
        int packageExpectedSize;
        int packageSize;
        while (!line.equals(stropString)) {
            try {
                line = in.readUTF();
                System.out.println(line);
                recievedSize = line.length();
                packageId = Integer.parseInt(line.substring(0, 2));
                packageSize = line.substring(2, recievedSize - 2).length();
                packageExpectedSize = Integer.parseInt(line.substring(recievedSize-2, recievedSize));

                if (packageId > 9){
                    throw new Exception();
                }

                if((packageId - 1 == packageIdBefore || packageIdBefore == -1) && (packageSize == packageExpectedSize)) {
                    packageIdBefore = packageId;
                    out.writeUTF("ACK " + packageId);
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to read package");
            }   catch (Exception e) {
                e.printStackTrace();
                System.out.println("Package limit reached");
                break;
            }

        }
    }

    public static void main(String[] args) {
        new Server();
    }

}
