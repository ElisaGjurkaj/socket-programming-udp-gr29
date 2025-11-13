package common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        else if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        else return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

}