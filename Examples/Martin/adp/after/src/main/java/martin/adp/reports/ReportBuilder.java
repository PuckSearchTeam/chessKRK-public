package martin.adp.reports;

/** After the repair: the cycle is broken by inverting the direction that the decision forbids. */
public class ReportBuilder {
    private final ReportSink sink;

    public ReportBuilder(ReportSink sink) {
        this.sink = sink;
    }

    public void build(String title) {
        sink.export(title, ReportFormat.TABLE);
    }
}
