package martin.srp.ui;

import martin.srp.geometry.Rectangle;

/** The graphical application: draws rectangles. */
public class GraphicalApplication {
    public void show(Rectangle rectangle) {
        rectangle.draw(new Canvas());
    }
}
