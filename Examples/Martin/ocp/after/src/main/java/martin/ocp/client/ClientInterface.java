package martin.ocp.client;

/** The abstraction the client owns (Extract Interface into the client's axis). */
public interface ClientInterface {
    String serve(String request);
}
