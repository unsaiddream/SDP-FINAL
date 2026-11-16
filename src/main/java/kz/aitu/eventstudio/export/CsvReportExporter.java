package kz.aitu.eventstudio.export;

public final class CsvReportExporter implements ReportExporter {
    private static String cell(String value) {
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r\n".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static void row(StringBuilder out, String... values) {
        out.append(java.util.Arrays.stream(values).map(CsvReportExporter::cell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
    }

    @Override
    public String render(EventReport report) {
        StringBuilder out = new StringBuilder();
        row(out, "Event", "Date", "Venue", "Status", "Package", "Extras", "Pricing", "Confirmed guests", "Unit price KZT", "Subtotal KZT", "Discount KZT", "Total KZT");
        var event = report.event();
        var quote = report.quote();
        row(out, event.title(), event.date(), event.venue(), event.status().name(), event.packageTier().name(),
                event.extras().stream().map(e -> e.label()).collect(java.util.stream.Collectors.joining("; ")), quote.strategy(),
                String.valueOf(quote.guests()), quote.unitPrice().toPlainString(), quote.subtotal().toPlainString(), quote.discount().toPlainString(), quote.total().toPlainString());
        row(out, "Participant", "Email", "Seats", "Booking status");
        for (var guest : report.guests()) row(out, guest.name(), guest.email(), String.valueOf(guest.seats()), guest.status());
        return out.toString();
    }
}
