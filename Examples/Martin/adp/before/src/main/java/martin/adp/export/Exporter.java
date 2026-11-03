package martin.adp.export;

import martin.adp.reports.ReportFormat;

public class Exporter {
    public void export(String content, ReportFormat format) {
        System.out.println(format + ": " + content);
    }
}
