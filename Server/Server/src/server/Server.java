package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Server {

    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private static final int port = 3030;
    private static final String stropString = "##";

    private int maxWindow;
    private int confirmationType;

    public Server() {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
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
            System.out.println("Failed to accept connection");
        }

        if(handshake()){
            setup();
            readPackages();
        }
    }

    private boolean handshake() {
        String expectedRequest = "SYN";
        String response = "ACK";

        try {
            if(!expectedRequest.equals(in.readUTF())) {
                throw new IOException();
            }

            out.writeUTF(response);
            System.out.println("Successful connection");

        } catch (IOException e) {
            System.out.println("Failed handshake");
            return false;
        }

        return true;

    }

    private void setup(){
        String line;
        try {
            line = in.readUTF();

            this.maxWindow = Integer.parseInt(line.split("\\|")[0]);
            this.confirmationType = Integer.parseInt(line.split("\\|")[1]);

            out.writeUTF("ACK");
        }catch(IOException e){
            System.out.println("Failed to read setup data");
            try {
                out.writeUTF("NaN");
            } catch (IOException ex) {
                System.out.println("Failed to write response");
            }
        }
    }

    private void close() {
        try {
            in.close();
            server.close();
        } catch (IOException e) {
            System.out.println("Failed to close the server");
        }
    }

    private void readPackages() {
        String line, packageData;
        String[] data;
        Random rand = new Random();
        String expectedChecksum;
        String comparableChecksum;
        int packageIdBefore = -1;
        int packageId;
        int windowCount = 1;
        int packageCount = 1;
        int expectedPackages;
        int checksum = 0;

        System.out.println();

        while (true) {
            try {
                line = in.readUTF();
                if(line.equals(stropString)) {
                    System.out.println("User ended Connection");
                    break;
                }

                System.out.println(line);
                data = line.split("\\|");
                packageId = Integer.parseInt(data[0]);
                packageData = data[1];
                expectedChecksum = data[2];
                expectedPackages = Integer.parseInt(data[3]);

                for(int i = 0; i < packageData.length(); i++) {
                    checksum += packageData.charAt(i);
                }

                checksum = checksum * packageData.length() * expectedPackages + packageId;
                comparableChecksum = String.valueOf(checksum);

                if (comparableChecksum.length() < 4) {
                    comparableChecksum = String.format("%04d", checksum);
                }else{
                    comparableChecksum = comparableChecksum.substring(comparableChecksum.length() - 4);
                }

                if(!comparableChecksum.equals(expectedChecksum) || packageId != packageIdBefore + 1) {
                    throw new Exception();
                }

                packageIdBefore = packageId;

                if(windowCount == maxWindow || expectedPackages == packageCount) {
                    out.writeUTF("ACK: " + packageCount);
                    System.out.println("Sending ACK: " + packageCount);
                    System.out.println("Elapsed time: " + rand.nextInt(1000) + "ms");
                    System.out.println();
                    windowCount = 0;

                    if(packageCount >= expectedPackages) {
                        packageCount = 0;

                    }
                }

                packageCount++;
                windowCount++;

            } catch (IOException e) {
                System.out.println("Failed to read package");
            } catch (Exception e) {
                System.out.println("Packet Loss");
            }

        }
        close();
    }

    public static void main(String[] args) {
        new Server();
    }

}
