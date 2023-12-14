package org.java.udp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {

    enum RxResponse {
        NEXT_PACKET, RESEND, FILE_ERROR, ALL_DONE
    }

    enum SysState {
        CMD_INPUT, SENDING_FILE
    }

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3000;
    private static final String ENCODING = "UTF-8";
    private static FileUtils rxFile = new FileUtils("Server-Received.txt");
    private static SysState sysState = SysState.CMD_INPUT;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverIp = InetAddress.getByName(HOST);

            while (true) {
                try {
                    if (SysState.CMD_INPUT == sysState) {
                        String ans = getUserInput("\n> ");
                        String[] splittedAns = ans.split(" ");
                        String cmd = splittedAns[0];

                        if ("GET".equals(cmd)) {
                            String filePath = splittedAns[1];
                            int ack = 0;  // Initialize ACK to 0 for the first request
                            String request = "GET:" + filePath + ":" + ack;
                            byte[] dataToSend = request.getBytes(ENCODING);
                            socket.send(new DatagramPacket(dataToSend, dataToSend.length, serverIp, PORT));
                        } else if ("EXIT".equals(cmd)) {
                            System.out.println("Exiting.");
                            break;
                        }
                    }

                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String dataReceived = new String(packet.getData(), 0, packet.getLength(), ENCODING);
                    System.out.println("Received data: " + dataReceived);

                    String[] msgParts = dataReceived.split(":");
                    if (msgParts.length >= 5) {
                        RxResponse response = handleReceivedFile(msgParts);
                        if (response == RxResponse.NEXT_PACKET) {
                            // Request next packet
                            int ack = rxFile.getAck();
                            String request = "GET:" + rxFile.getFilename() + ":" + ack;
                            byte[] dataToSend = request.getBytes(ENCODING);
                            socket.send(new DatagramPacket(dataToSend, dataToSend.length, serverIp, PORT));
                        } else if (response == RxResponse.ALL_DONE) {
                            System.out.println("File transfer completed.");
                            break;
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Communication error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("Client failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getUserInput(String prompt) {
        System.out.print(prompt);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static RxResponse handleReceivedFile(String[] msg) throws IOException {
        if (msg.length < 5) return RxResponse.FILE_ERROR;

        String fileName = msg[1];
        int seqNum = Integer.parseInt(msg[2]);
        int fileSize = Integer.parseInt(msg[3]);
        String receivedSha256 = msg[4];
        String packData = msg[5]; // Directly use the data without Base64 decoding

        String realSha256;
        try {
            realSha256 = toSHA256(packData);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return RxResponse.FILE_ERROR;
        }

        if (!receivedSha256.equals(realSha256)) {
            return RxResponse.RESEND;
        }

        if (seqNum == 0) {
            rxFile.replaceAll(fileName, fileSize, packData, seqNum + packData.length());
        } else if (seqNum == rxFile.getAck()) {
            rxFile.addData(packData);
            rxFile.setAck(seqNum + packData.length());
        } else {
            return RxResponse.RESEND;
        }

        if (rxFile.getAck() >= fileSize) {
            rxFile.saveFile();
            return RxResponse.ALL_DONE;
        }

        return RxResponse.NEXT_PACKET;
    }
    private static String toSHA256(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
