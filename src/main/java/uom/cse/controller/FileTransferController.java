package uom.cse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@RestController
public class FileTransferController {

    @GetMapping("/download")
    public FileResponse downloadFile(@RequestParam String filename) throws NoSuchAlgorithmException {
        Random random = new Random();
        int fileSize = random.nextInt(9) + 2; // Generate file size between 2MB and 10MB
        byte[] fileContent = new byte[fileSize * 1024 * 1024];
        random.nextBytes(fileContent);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileContent);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(Integer.toHexString(0xFF & b));
        }

        return new FileResponse(filename, fileSize, hexString.toString(), fileContent);
    }
}

class FileResponse {
    private String filename;
    private int size;
    private String hash;
    private byte[] content;

    public FileResponse(String filename, int size, String hash, byte[] content) {
        this.filename = filename;
        this.size = size;
        this.hash = hash;
        this.content = content;
    }

    // Getters and setters

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
