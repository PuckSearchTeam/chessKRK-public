package martin.sdp.experimental;

import martin.sdp.core.Tuning;

public class Tuner implements Tuning {
    @Override
    public int tune(int value) {
        return value * 2;
    }
}
