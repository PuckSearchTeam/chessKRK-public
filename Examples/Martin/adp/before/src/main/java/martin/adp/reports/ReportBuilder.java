package martin.adp.reports;

import martin.adp.export.Exporter;

/** ADP, the violating version: reports uses export, and export uses reports (ReportFormat): a cycle. */
public class ReportBuilder {
    private final Exporter exporter = new Exporter();

    public void build(String title) {
        exporter.export(title, ReportFormat.TABLE);
    }
}
