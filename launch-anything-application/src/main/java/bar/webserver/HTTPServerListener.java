package bar.webserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public interface HTTPServerListener {
    void clientConnected(BufferedReader in, BufferedWriter out) throws IOException;
}
