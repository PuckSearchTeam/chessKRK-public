package martin.dip.transport;

import martin.dip.vehicles.Car;
import martin.dip.vehicles.Truck;

/** The DIP example, the violating version: the high-level manager names the concrete vehicles. */
public class TransportManager {
    private final Car car = new Car();
    private final Truck truck = new Truck();

    public void dispatch(String destination, boolean heavy) {
        if (heavy) {
            truck.haul(destination);
        } else {
            car.drive(destination);
        }
    }
}
