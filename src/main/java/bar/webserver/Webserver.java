package bar.webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Webserver {

    private final HttpServer server;
    private final String subpath;
    private final int port;

    public Webserver(int port, String subpath) throws IOException {
        this.port = port;
        this.subpath = subpath;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/" + subpath, new MyHandler());
        server.setExecutor(null);
    }

    public void openInBrowser() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + port + "/" + subpath + "?p=" + port));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void open() {
        server.start();
    }

    public void close() {
        server.stop(10);
    }

    private Map<String, String> queryToMap(String query) {
        if (query == null) return null;
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private RequestHandler handler = null;

    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }

    public RequestHandler getHandler() {
        return handler;
    }

    class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = handler.handle(queryToMap(httpExchange.getRequestURI().getQuery()));
            httpExchange.sendResponseHeaders(200, response.length());
            if (response.charAt(0) == '{')
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            else if (response.charAt(0) == '<')
                httpExchange.getResponseHeaders().add("Content-Type", "text/html");
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public interface RequestHandler {
        String handle(Map<String, String> params);
    }
}
