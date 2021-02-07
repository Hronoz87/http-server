package server;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    final Map<String, String> params = new HashMap<>();

    public Server() {
    }

    public void serverStart() {
        try (final var serverSocket = new ServerSocket(24000)) {
            ExecutorService executeIt = Executors.newFixedThreadPool(64);
            System.out.println("Server run...");
            while (true) {
                try {
                    executeIt.submit(() -> {
                        try {
                            event(serverSocket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void event(ServerSocket serverSocket) throws IOException {

        try (
                final var socket = serverSocket.accept();
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            {
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    return;
                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    return;
                }

                setQueryParams(path);

                final var filePath = Path.of(".", "public", path);
                final var mimeType = Files.probeContentType(filePath);

                if (path.equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                    return;
                }

                final var length = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> setQueryParams(String path) throws URISyntaxException {
        List<NameValuePair> data = URLEncodedUtils.parse(new URI(path), String.valueOf(StandardCharsets.UTF_8));
        for (NameValuePair nvp : data) {
            params.put(nvp.getName(), nvp.getValue());
        }
        return params;
    }

    public String getQueryParam(String name) {
        return params.get(name);
    }

    public Map<String, String> getQueryParams() {
        for(Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(key + "=" + value);
        }return params;
    }
}

