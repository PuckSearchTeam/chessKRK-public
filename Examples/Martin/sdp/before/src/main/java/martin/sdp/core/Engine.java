package martin.sdp.core;

import martin.sdp.experimental.Tuner;

/** SDP, the violating version: the engine, hard to change because every application uses it, depends on
 *  an experimental helper that is easy to change. */
public class Engine {
    private final Tuner tuner = new Tuner();

    public int run(int input) {
        return tuner.tune(input) + 1;
    }
}
