package martin.adp.export;

import martin.adp.reports.ReportFormat;
import martin.adp.reports.ReportSink;

public class Exporter implements ReportSink {
    @Override
    public void export(String content, ReportFormat format) {
        System.out.println(format + ": " + content);
    }
}
