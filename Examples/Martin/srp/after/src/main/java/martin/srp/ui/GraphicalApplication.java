package martin.srp.ui;

import martin.srp.geometry.Rectangle;

/** The graphical application: draws rectangles through the view. */
public class GraphicalApplication {
    public void show(Rectangle rectangle) {
        new RectangleView().draw(rectangle, new Canvas());
    }
}
