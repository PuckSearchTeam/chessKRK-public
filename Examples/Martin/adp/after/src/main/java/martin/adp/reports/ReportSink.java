package martin.adp.reports;

/** The abstraction reports owns; export implements it, so that export depends on reports and not the reverse. */
public interface ReportSink {
    void export(String content, ReportFormat format);
}
