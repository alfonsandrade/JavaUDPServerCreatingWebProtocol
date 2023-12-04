package org.java.udp.server;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        String request = "GET /caminho_do_arquivo";
        byte[] sendBuffer = request.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, 5000);
        socket.send(sendPacket);

        byte[] receiveBuffer = new byte[1024];

        // Implementação para receber o arquivo em pedaços
        // Inclui lógica para verificar checksum e solicitar retransmissão de pedaços, se necessário
    }
}

