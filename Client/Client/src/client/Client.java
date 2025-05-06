package client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

public class Client {

    private Socket socket;
    private DataInputStream inServer;
    private DataOutputStream out;
    private Scanner inUser = new Scanner(System.in);
    private static final int port = 3030;
    private static final String stop_string = "##";

    private int maxMsgSize;
    private int maxWindow;
    private int confirmationType;

    public Client() {
        try {
            socket = new Socket("127.0.0.1", port);
            out = new DataOutputStream(socket.getOutputStream());
            inServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            inUser = new Scanner(System.in);

        } catch (Exception e) {
            System.out.println("Failed to connect to server");
        }

        if(handshake() && setup()) {
            writeMessages();
        }else{
            close();
        }
    }

    private boolean handshake() {
        String request = "SYN";
        String expectedResponse = "ACK";
        String actualResponse;

        try {
            out.writeUTF(request);
            actualResponse = inServer.readUTF();

            if (!actualResponse.equals(expectedResponse)) {
                throw new IOException();
            }

        } catch (IOException e) {
            System.out.println("Failed handshake");
            return false;
        }

        System.out.println("Successful connection");
        return true;

    }

    private boolean setup(){
        String response;

        System.out.print("Max msg size: ");
        this.maxMsgSize = inUser.nextInt();

        System.out.println("Choose one of the options: \n1- Go Back N\n2- Selective Repeat");
        this.confirmationType = inUser.nextInt();

        try{
            if(this.confirmationType == 1){
                System.out.print("Max window size: ");
                this.maxWindow = inUser.nextInt();
            }else {
                this.maxWindow = 1;
            }
            out.writeUTF(this.maxWindow + "|" + this.confirmationType);

            response = inServer.readUTF();

            return response.equals("ACK");

        }catch (IOException e){
            return false;
        }

    }

    private void writeMessages() {
        String line, ackWindow;
        String currentPackage;
        String checkSumToSend;
        List<String> packageList;
        int ackValueReceived;
        int packagesToSend;
        int packagesSent = 1;
        int packageSize;
        int windowCount = 1;
        int packageId = 0;
        int checksum = 0;

        while (true) {
            System.out.print("Write your message(Write '"+ stop_string + "' to exit): ");
            line = inUser.nextLine();

            if (line.equals(stop_string)) {
                System.out.println("Ended connection");
                break;
            }else if(line.length() > maxMsgSize) {
                System.out.println("Message exceeds maximum length of " + maxMsgSize);
                continue;
            }

            packageList = getParts(line);
            packagesToSend = packageList.size();

            for(int i = 0; i < packageList.size(); i++){
                try {
                    currentPackage = packageList.get(i);
                    packageSize = currentPackage.length();

                    for(int j = 0; j < packageSize; j++){
                        checksum += currentPackage.charAt(j);
                    }
                    checksum = checksum * packageSize * packagesToSend + packageId;
                    checkSumToSend = String.valueOf(checksum);

                    if(checkSumToSend.length() < 4){
                        checkSumToSend = String.format("%04d", checksum);

                    }else{
                        checkSumToSend = String.valueOf(checksum);
                        checkSumToSend = checkSumToSend.substring(checkSumToSend.length() - 4);
                    }

                    out.writeUTF(packageId + "|" + packageList.get(i) + "|" + checkSumToSend + "|" + packagesToSend);
                    packageId++;

                    if(windowCount == maxWindow || i == packageList.size() - 1){
                        ackWindow = inServer.readUTF();
                        ackValueReceived = Integer.parseInt(ackWindow.substring(5));

                        if(!ackWindow.contains("ACK") || packagesSent != ackValueReceived){
                            throw new Exception();
                        }

                        System.out.println(ackWindow);
                        System.out.println("Expected ACK: " + packagesSent);
                        windowCount = 0;

                    }
                    packagesSent++;
                    windowCount++;

                } catch (IOException e) {
                    System.out.println("Failed to write package");
                }catch (Exception e){
                    System.out.println("Failed receive ACK with the defined window");
                }
            }
            packagesSent = 1;
        }
        close();
    }

    private static List<String> getParts(String string) {
        List<String> parts = new ArrayList<>();
        int len = string.length();
        int partitionSize = 3;

        for (int i=0; i<len; i+=partitionSize)
        {
            parts.add(string.substring(i, Math.min(len, i + partitionSize)));
        }
        return parts;
    }

    private void close() {
        try {
            socket.close();
            inServer.close();
            out.close();
            inUser.close();
        } catch (IOException e) {
            System.out.println("Failed to close client");
        }
    }


    public static void main(String[] args) {
        new Client();
    }
}
