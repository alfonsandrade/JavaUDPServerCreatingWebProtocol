package org.java.udp.server;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Server {
    private static final int PORT = 3000;
    private static final String ENCODING = "UTF-8";
    private static final int DATA_BUFF_SIZE = 768;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("[NEW MESSAGE] " + packet.getAddress().getHostAddress() + ": " + msg);

                String[] parts = msg.split(":");
                if (parts.length == 3 && "GET".equals(parts[0])) {
                    String filePath = parts[1];
                    int ack = Integer.parseInt(parts[2]);
                    byte[] response = handleGETRequest(filePath, ack);
                    socket.send(new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort()));
                }
            }
        } catch (IOException e) {
            System.out.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] handleGETRequest(String filePath, int ack) {
        FileUtils fileUtils = new FileUtils(filePath);
        if (!fileUtils.fileExists()) {
            return "ERROR: File not found\r\n".getBytes(StandardCharsets.UTF_8);
        }

        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            int end = Math.min(fileData.length, ack + DATA_BUFF_SIZE);
            if (ack < fileData.length) {
                byte[] partialData = Arrays.copyOfRange(fileData, ack, end);
                return makeResponseLine(filePath, fileData.length, ack, partialData);
            } else {
                return makeResponseLine(filePath, fileData.length, ack, new byte[0]);
            }
        } catch (IOException e) {
            return "ERROR: File read error\r\n".getBytes(StandardCharsets.UTF_8);
        }
    }

    private static byte[] makeResponseLine(String fileName, int fileSize, int ack, byte[] partialData) {
        String sha256 = toSHA256(partialData);
        return String.format("FILE:%s:%d:%d:%s:%s\r\n",
                        fileName, ack, fileSize, sha256, new String(partialData, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String toSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
