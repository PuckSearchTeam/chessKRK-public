# Martin's examples as code

Each directory holds an example of a principle from Robert C. Martin's *Agile Software Development* (2003; the
paper's Section 3), in two versions: `before/`, the violating one, and `after/`, the repaired one. Three render
Martin's own examples, two class diagrams and a C listing (`srp`, `ocp`, `shapes`); the five others (`dip`, `isp`, `ccp`, `adp`, `sdp`, marked * below) are ours,
written to the descriptions his chapters give. Each version is a small Java program under `src/main/java` with a
`decouple.wld` that states the context of Section 3, the axes of change and their order, from which Puck generates
the clause. For his three, Martin is the oracle for which version is which; for ours, his description is. Puck
must report the violation on `before/` and nothing on `after/`.

| Directory | Principle | Example | Context, and the clause it generates | Repair (Table 3 of the paper) |
|---|---|---|---|---|
| `srp` | SRP | `Rectangle` with `area()` and `draw()` | `speed geometry < ui`, `draw` tagged ui, foreign to `Rectangle`: `hide ui from geometry` | Extract Class: `RectangleView` in ui |
| `ocp` | OCP | `Client` uses the concrete `Server` | `speed client < servers`: `hide servers from client` | Extract Interface in the client's axis, Introduce Parameter |
| `shapes` | OCP | `drawAllShapes` tests each kind of shape | `speed rendering < shapes`: `hide shapes from rendering` | Replace Conditional with Polymorphism: `Shape` owned by rendering |
| `dip` | DIP* | `TransportManager` names `Car` and `Truck` | `speed transport < vehicles`: `hide vehicles from transport` | Extract Interface `Vehicle` in transport, Introduce Parameter |
| `isp` | ISP* | `UI` serving the deposit, withdrawal and transfer transactions | `speed transaction < deposit, withdrawal, transfer < ui`, each `request…` method tagged with its transaction, foreign to `UI`: the containment clause (3 edges), and `hide ui from …` for the 6 uses, which are DIP's | Extract Interface per transaction, in its package |
| `ccp` | CCP* | `ShapeRenderer` in the package of the shapes | `speed shapes < ui`, the class tagged ui, foreign to the package: `hide ui from shapes` | Move Class to a package of ui |
| `adp` | ADP* | `reports` and `export` use each other | unordered, no order given; the optional block is the written decision `hide export from reports;` | inversion: `ReportSink` owned by reports |
| `sdp` | SDP* | `core`, used by every application, uses `experimental` | `speed core, experimental < apps`; `difficulty experimental < core`: `hide experimental from core` | Extract Interface `Tuning` in core, Introduce Parameter |

SAP has no example of its own: it is a design choice, and the `after/` of `dip` and `sdp` show it, the
interfaces sitting in the hard-to-change axis.

Run them all with `scripts/puck-both.sh Examples/Martin/<example>/<version>` from the repository root; the
kept logs are in `docs/puck-results/martin/`. Every version compiles with `javac` alone (no build file, no
dependency).
