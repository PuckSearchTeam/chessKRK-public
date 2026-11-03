package martin.sdp.core;

/** After the repair: the engine depends on its own abstraction; the helper is injected. */
public class Engine {
    private final Tuning tuning;

    public Engine(Tuning tuning) {
        this.tuning = tuning;
    }

    public int run(int input) {
        return tuning.tune(input) + 1;
    }
}
