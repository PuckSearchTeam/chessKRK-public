package martin.sdp.app2;

import martin.sdp.core.Engine;
import martin.sdp.experimental.Tuner;

public class App2 {
    public int compute() {
        return new Engine(new Tuner()).run(2);
    }
}
