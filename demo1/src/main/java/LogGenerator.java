import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogGenerator {
    public static void main(String[] args) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logs.jsonl"))) {
            for (int i = 1; i <= 10000; i++) {
                String level = (i % 3 == 0) ? "ERROR" : "INFO";

                // We want the file to look like: "msg":"Text with \"quoted\" part"
                // In Java, we need triple backslashes to get one backslash in the file
                String msg = "Processing record " + i + ". Data: {\\\"id\\\": " + i + ", \\\"status\\\": \\\"OK\\\"}";
                String ts = "2026-01-25T21:00:" + (i < 10 ? "0" + i : i) + "Z";

                // Construct the JSON line
                String jsonLine = "{\"level\":\"" + level + "\",\"msg\":\"" + msg + "\",\"timestamp\":\"" + ts + "\"}";

                writer.write(jsonLine);
                writer.newLine();
            }
        }
        org.slf4j.LoggerFactory.getLogger(LogGenerator.class).info("Generated logs.jsonl successfully.");
    }
}