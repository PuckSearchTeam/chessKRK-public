package martin.dip.transport;

/** The abstraction the manager owns; the vehicles depend on it, not the reverse. */
public interface Vehicle {
    boolean carries(boolean heavy);

    void go(String destination);
}
