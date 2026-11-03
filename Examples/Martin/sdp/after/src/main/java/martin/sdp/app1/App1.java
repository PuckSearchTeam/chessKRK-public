package martin.sdp.app1;

import martin.sdp.core.Engine;
import martin.sdp.experimental.Tuner;

public class App1 {
    public int compute() {
        return new Engine(new Tuner()).run(1);
    }
}
