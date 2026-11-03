package martin.ocp.server;

import martin.ocp.client.ClientInterface;

/** The server implements the client's interface: a new server needs no change in the client. */
public class Server implements ClientInterface {
    @Override
    public String serve(String request) {
        return "served " + request;
    }
}
