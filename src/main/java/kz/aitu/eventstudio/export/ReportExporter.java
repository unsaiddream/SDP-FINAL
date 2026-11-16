package kz.aitu.eventstudio.export;

public interface ReportExporter {
    String render(EventReport report);
}
