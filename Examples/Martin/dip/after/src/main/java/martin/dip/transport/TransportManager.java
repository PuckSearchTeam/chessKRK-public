package martin.dip.transport;

import java.util.List;

/** After the repair: the manager depends on its abstraction; the fleet is injected. */
public class TransportManager {
    private final List<Vehicle> fleet;

    public TransportManager(List<Vehicle> fleet) {
        this.fleet = fleet;
    }

    public void dispatch(String destination, boolean heavy) {
        for (Vehicle vehicle : fleet) {
            if (vehicle.carries(heavy)) {
                vehicle.go(destination);
                return;
            }
        }
    }
}
