import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;


/* to test with localhost server as target server set in firefox
   network.proxy.allow_hijacking_localhost", true
 */
public class ProxyServer {

    private static ArrayList<String> blacklist;
    private static StatsManager stats;

    public static void main(String[] args) throws Exception {
        int port = 9000;

        try {
            blacklist = getBlackList("src/main/resources/blacklist.txt");
            stats = new StatsManager("src/main/resources/stats.csv");
            stats.readFromFile();
            System.out.println(stats);
            for (String domain : blacklist) {
                System.out.println(domain);
            }
        } catch (FileNotFoundException e) {
            System.err.println("File Not Found");
            return;
        } catch (IOException e) {
            System.err.println("Access to file error");
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());

        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            OutputStream out = null;
            byte [] requestedBody = null;
            byte [] responseBody = null;
            HttpURLConnection conn = null;
            URI uri = exchange.getRequestURI();
            Headers requestHeader   = exchange.getRequestHeaders();
            Headers responseHeaders = exchange.getResponseHeaders();

            if (blacklist.contains(uri.getHost())) {
                handleError(exchange, 403, "Forbidden");
                return;
            }

            try {
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod(exchange.getRequestMethod());
                conn.setInstanceFollowRedirects(false);
                copyHeadersFromClientToServer(conn, requestHeader);
                requestedBody = exchange.getRequestBody().readAllBytes();

                if (requestedBody.length > 0) {
                    conn.setDoOutput(true);
                    out = conn.getOutputStream();
                    out.write(requestedBody);
                    out.close();
                }

                try {
                    responseBody = conn.getInputStream().readAllBytes();
                } catch (IOException e) {
                    responseBody = conn.getErrorStream().readAllBytes();
                }

                copyHeadersFromServerToClient(responseHeaders, conn.getHeaderFields());
                responseHeaders.add("Via", InetAddress.getLocalHost().toString());
                exchange.sendResponseHeaders(conn.getResponseCode(), responseBody.length);
                out = exchange.getResponseBody();
                out.write(responseBody);
                out.close();

                stats.update(uri.getHost(), responseBody.length + requestedBody.length);
                stats.writeToFile();

            } catch (MalformedURLException e) {
                handleError(exchange, 400, "Bad Request");
            } catch (IOException e) {
                handleError(exchange, 500, "Internal error");
            } catch (Exception e) {
                System.out.println("Other error");
                e.printStackTrace();
            }
        }

    }
    static void copyHeadersFromServerToClient(Headers dst, Map<String,List<String>> src)
    {
        for (String key : src.keySet()) {
            for (String value : src.get(key)) {
                if (key != null && value != null && !"Transfer-Encoding".equalsIgnoreCase(key)) {
                    dst.add(key, value);
                }
            }
        };
    }
    static void copyHeadersFromClientToServer(HttpURLConnection dst, Headers src)
    {
        for (String key : src.keySet()) {
            for (String value : src.get(key)) {
                dst.addRequestProperty(key, value);
            }
        }
    }
    static void handleError (HttpExchange exchange, int err_code, String err_msg) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(err_code, err_msg.length());
        OutputStream os = exchange.getResponseBody();
        os.write(err_msg.getBytes());
        os.close();
    }

    static ArrayList<String> getBlackList(String filename) throws FileNotFoundException, IOException
    {
        ArrayList<String> list = new ArrayList<String>();
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new FileReader(filename));

            String line;
            while ((line = inputStream.readLine()) != null) {
                list.add(line);
            }

        } finally {
            if (inputStream != null)
                inputStream.close();
        }
        return list;

    }
}
