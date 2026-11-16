package kz.aitu.eventstudio.export;

public final class HtmlReportExporter implements ReportExporter {
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    public String render(EventReport report) {
        var event = report.event();
        var quote = report.quote();
        StringBuilder out = new StringBuilder("<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Event Studio report</title><style>body{font:16px system-ui;max-width:900px;margin:48px auto;padding:24px;color:#18334b}table{width:100%;border-collapse:collapse}td,th{text-align:left;border-bottom:1px solid #ccd7df;padding:10px}h1{font-size:32px}@media print{body{margin:0}}</style><body><h1>");
        out.append(escape(event.title())).append("</h1><p>").append(escape(event.date())).append(" | ").append(escape(event.venue())).append(" | ").append(event.status()).append("</p><h2>Service package</h2><ul>");
        for (String line : quote.lines()) out.append("<li>").append(escape(line)).append("</li>");
        out.append("</ul><p>").append(escape(quote.strategy())).append("</p><p>Confirmed guests: ").append(quote.guests())
                .append(" / ").append(event.capacity()).append("</p><p>Per guest: ").append(quote.unitPrice().toPlainString())
                .append(" KZT | Subtotal: ").append(quote.subtotal().toPlainString()).append(" KZT | Discount: ")
                .append(quote.discount().toPlainString()).append(" KZT</p><h2>Total: ").append(quote.total().toPlainString())
                .append(" KZT</h2><h2>Guest list</h2><table><thead><tr><th>Name</th><th>Email</th><th>Seats</th><th>Status</th></tr></thead><tbody>");
        for (var guest : report.guests()) {
            out.append("<tr><td>").append(escape(guest.name())).append("</td><td>").append(escape(guest.email())).append("</td><td>")
                    .append(guest.seats()).append("</td><td>").append(escape(guest.status())).append("</td></tr>");
        }
        return out.append("</tbody></table><p>Totals include confirmed seats only. Cancelled bookings are kept for reference. Prices are estimates in KZT; no payments are collected.</p></body></html>").toString();
    }
}
