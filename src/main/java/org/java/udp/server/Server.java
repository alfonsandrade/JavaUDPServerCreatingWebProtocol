package org.java.udp.server;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(5000);
        byte[] receiveBuffer = new byte[1024];
        byte[] sendBuffer;

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());

            if (request.startsWith("GET")) {
                String fileName = request.split(" ")[1];
                File file = new File(fileName);

                if (!file.exists()) {
                    String message = "ERROR: File not found";
                    sendBuffer = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(sendPacket);
                    continue;
                }

                // Implementação para enviar o arquivo em pedaços
                // Inclui lógica para checksum e numeração de pedaços
            }
        }
    }
}