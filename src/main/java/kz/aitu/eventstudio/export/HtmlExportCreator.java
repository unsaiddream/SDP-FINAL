package kz.aitu.eventstudio.export;

public final class HtmlExportCreator extends ExportCreator {
    @Override
    protected ReportExporter createExporter() { return new HtmlReportExporter(); }
    @Override
    public String extension() { return "html"; }
}
