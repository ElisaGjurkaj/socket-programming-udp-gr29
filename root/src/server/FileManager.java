package server;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Base64;

public class FileManager {
    private final String baseDir;

    public FileManager(String baseDir) {
        this.baseDir = baseDir;
        createDirectoryIfNotExists(baseDir);
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("U krijua direktoria: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Gabim gjatë krijimit të direktorisë " + directoryPath + ": " + e.getMessage());
        }
    }

    public String listFiles() {
        File dir = new File(baseDir);
        StringBuilder sb = new StringBuilder("Lista e file-ve:\n");
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return sb.append("(Nuk ka file)\n").toString();
        for (File file : files) {
            if (file.isFile()) sb.append(file.getName()).append("\n");
        }
        return sb.toString();
    }

    public String searchFiles(String keyword) {
        if (keyword == null || keyword.isEmpty()) return "Keyword i zbrazët";

        File dir = new File(baseDir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return "Nuk ka file në direktorium";

        StringBuilder sb = new StringBuilder("Rezultatet e kërkimit për '" + keyword + "':\n");
        boolean found = false;
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().contains(keyword.toLowerCase())) {
                sb.append(file.getName()).append("\n");
                found = true;
            }
        }
        if (!found) sb.append("(Nuk u gjet asnjë file)\n");
        return sb.toString();
    }

    public String readFile(String filename) throws IOException {
        Path path = Path.of(baseDir, filename).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) return "Gabim: File nuk ekziston";
        return Files.readString(path);
    }

    public String prepareDownload(String filename) throws IOException {
        Path path = Path.of(baseDir, filename).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) return "Gabim: File nuk ekziston";

        byte[] fileData = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(fileData);

        return "DOWNLOAD:" + filename + ":" + base64;
    }

    public String deleteFile(String filename) {
        File file = new File(baseDir, filename);
        if (file.exists() && file.isFile() && file.delete()) return "File u fshi me sukses: " + filename;
        return "Gabim: Nuk mund të fshihet file";
    }

    public String getFileInfo(String filename) {
        File file = new File(baseDir, filename);
        if (!file.exists()) return "Gabim: File nuk ekziston";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return String.format("Informacion për %s:\nMadhësia: %d bytes\nModifikuar: %s\nPath: %s",
                filename, file.length(), sdf.format(new Date(file.lastModified())), file.getAbsolutePath());
    }
}
