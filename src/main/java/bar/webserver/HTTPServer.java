package bar.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Desktop.*;

public class HTTPServer {

    private final int port;
    private boolean isOpen = false;
    private ServerSocket serverSocket;
    private final List<HTTPServerListener> listeners = new ArrayList<>();

    public HTTPServer(int port) {
        this.port = port;
    }

    public void open() throws IOException {
        serverSocket = new ServerSocket(port);
        isOpen = true;

        while (isOpen) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (SocketException e) {
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            for (HTTPServerListener listener : listeners) {
                listener.clientConnected(in, out);
            }

            try {
                out.close();
                in.close();
                clientSocket.close();
            } catch (Exception ignored) {
            }
        }

        serverSocket.close();
    }

    public void close() {
        isOpen = false;
    }

    public void addListener(HTTPServerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HTTPServerListener listener) {
        listeners.add(listener);
    }

    public int getPort() {
        return port;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void openInBrowser() {
        try {
            getDesktop().browse(new URI("http://localhost:" + port));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}