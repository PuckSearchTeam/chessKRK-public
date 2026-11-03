package martin.ocp.server;

/** A concrete server. */
public class Server {
    public String serve(String request) {
        return "served " + request;
    }
}
