package martin.ocp.client;

/** After the repair: the client uses its interface; the server is injected (Introduce Parameter). */
public class Client {
    private final ClientInterface server;

    public Client(ClientInterface server) {
        this.server = server;
    }

    public String run(String request) {
        return server.serve(request);
    }
}
