package martin.dip.vehicles;

import martin.dip.transport.Vehicle;

public class Car implements Vehicle {
    @Override
    public boolean carries(boolean heavy) {
        return !heavy;
    }

    @Override
    public void go(String destination) {
        System.out.println("car to " + destination);
    }
}
