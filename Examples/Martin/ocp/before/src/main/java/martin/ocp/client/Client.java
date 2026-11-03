package martin.ocp.client;

import martin.ocp.server.Server;

/** Martin's OCP example, the violating version: the client names the concrete server. */
public class Client {
    private final Server server = new Server();

    public String run(String request) {
        return server.serve(request);
    }
}
