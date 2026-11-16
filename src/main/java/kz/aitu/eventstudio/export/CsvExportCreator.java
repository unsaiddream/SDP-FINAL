package kz.aitu.eventstudio.export;

public final class CsvExportCreator extends ExportCreator {
    @Override
    protected ReportExporter createExporter() { return new CsvReportExporter(); }
    @Override
    public String extension() { return "csv"; }
}
