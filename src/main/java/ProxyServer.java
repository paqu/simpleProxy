import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.Map;

/* to test with localhost server as target server set in firefox
   network.proxy.allow_hijacking_localhost", true
 */
public class ProxyServer {


    public static void main(String[] args) throws Exception {
        int port = 9000;


        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());

        System.out.println("Starting server on port: " + port);
        server.start();
    }
    /*
        TODO:
         *refactor reading and writing streams
         *refactor:
            copyHeadersFromServerToClient
            copyHeadersFromClientToServer
            looks like code duplication
         *add mechanism for error handling
     */
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Proxy server used.");
            HttpURLConnection conn;
            URL url;

            BufferedInputStream reader;
            BufferedOutputStream writer;

            Headers requestHeaders   = exchange.getRequestHeaders();
            Headers responseHeaders  = exchange.getResponseHeaders();

            byte [] buffer = new byte[1024];
            int bytesRead = -1;

            url  = exchange.getRequestURI().toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(exchange.getRequestMethod());
            conn.setInstanceFollowRedirects(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            copyHeadersFromClientToServer(conn, requestHeaders);

            reader = new BufferedInputStream(exchange.getRequestBody());
            writer = new BufferedOutputStream(conn.getOutputStream());

            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }

            reader.close();
            writer.close();

            responseHeaders.set("Content-Type", conn.getContentType());
            exchange.sendResponseHeaders(conn.getResponseCode(), conn.getContentLength());

            copyHeadersFromServerToClient(responseHeaders, conn.getHeaderFields());
            responseHeaders.add("Via", InetAddress.getLocalHost().toString());

            reader = new BufferedInputStream(conn.getInputStream());
            writer = new BufferedOutputStream(exchange.getResponseBody());

            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }

            reader.close();
            writer.close();

            conn.disconnect();
        }
    }
    static void copyHeadersFromServerToClient(Headers header, Map<String,List<String>> conn)
    {
        Set <String> keys = conn.keySet();
        for (String key : keys) {
            for (String value : conn.get(key)) {
                if (!"Transfer-Encoding".equalsIgnoreCase(key))
                    header.add(key, value);
            }
        }
    }
    static void copyHeadersFromClientToServer(HttpURLConnection conn, Headers header)
    {
        Set<String> keys = header.keySet();
        for (String key : keys) {
            for (String value : header.get(key)) {
                conn.addRequestProperty(key, value);
            }
        }
    }
}
