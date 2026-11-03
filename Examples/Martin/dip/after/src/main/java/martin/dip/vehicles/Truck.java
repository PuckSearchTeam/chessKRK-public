package martin.dip.vehicles;

import martin.dip.transport.Vehicle;

public class Truck implements Vehicle {
    @Override
    public boolean carries(boolean heavy) {
        return heavy;
    }

    @Override
    public void go(String destination) {
        System.out.println("truck to " + destination);
    }
}
