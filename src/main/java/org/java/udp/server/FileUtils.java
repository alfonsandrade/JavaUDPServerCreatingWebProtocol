package org.java.udp.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtils {
    private String filename;
    private int fileSize;
    private StringBuilder fileData;
    private int ack;

    public FileUtils(String filename) {
        this.filename = filename;
        this.fileSize = 0;
        this.fileData = new StringBuilder();
        this.ack = 0;
    }

    public void replaceAll(String fileName, int fileSize, String packData, int ack) {
        this.filename = fileName;
        this.fileSize = fileSize;
        this.fileData = new StringBuilder(packData);
        this.ack = ack;
    }

    public void addData(String packData) {
        this.fileData.append(packData);
    }

    public void setAck(int ack) {
        this.ack = ack;
    }

    public int getAck() {
        return this.ack;
    }

    public String getFilename() {
        return this.filename;
    }

    public int getFileSize() throws IOException {
        return Files.readAllBytes(Paths.get(filename)).length;
    }

    public void saveFile() {
        try {
            if (!filename.isEmpty()) {
                Files.write(Paths.get(filename), fileData.toString().getBytes(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean fileExists() {
        return Files.exists(Paths.get(filename));
    }

    public byte[] getFractionedFileData(int start, int size) throws IOException {
        byte[] fileData = Files.readAllBytes(Paths.get(filename));
        if (start < fileData.length) {
            int end = Math.min(fileData.length, start + size);
            byte[] partialData = new byte[end - start];
            System.arraycopy(fileData, start, partialData, 0, partialData.length);
            return partialData;
        }
        return new byte[0];
    }

    public void resetAll() {
        this.filename = "";
        this.fileSize = 0;
        this.fileData = new StringBuilder();
        this.ack = 0;
    }
}
