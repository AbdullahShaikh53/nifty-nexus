package com.niftynexus.report;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.niftynexus.auth.CurrentUser;
import com.niftynexus.journal.JournalEntry;
import com.niftynexus.journal.JournalRepository;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.OrderRepository;
import com.niftynexus.orders.Trade;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.portfolio.PortfolioService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/** PDF and Excel exports of P&L, orders and journal (FR-59). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final TradeRepository trades; private final OrderRepository orders; private final JournalRepository journal; private final PortfolioService portfolio;
    private final com.niftynexus.user.UserRepository users;
    public ReportController(TradeRepository trades, OrderRepository orders, JournalRepository journal, PortfolioService portfolio, com.niftynexus.user.UserRepository users) {
        this.trades = trades; this.orders = orders; this.journal = journal; this.portfolio = portfolio; this.users = users;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel() throws Exception {
        Long uid = CurrentUser.id();
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle(); org.apache.poi.ss.usermodel.Font f = wb.createFont(); f.setBold(true); bold.setFont(f);
            Sheet s1 = wb.createSheet("Summary");
            Map<String, Object> sum = portfolio.summary(uid);
            int r = 0;
            Row owner = s1.createRow(r++); owner.createCell(0).setCellValue("Trader"); owner.createCell(1).setCellValue(users.findById(uid).map(com.niftynexus.user.User::getName).orElse(""));
            Row dev = s1.createRow(r++); dev.createCell(0).setCellValue("Developed by"); dev.createCell(1).setCellValue("Abdullah Shaikh");
            for (String k : List.of("totalValue", "cash", "invested", "marketValue", "unrealisedPnl", "realisedPnl", "chargesPaid", "overallReturnPct", "tradeCount")) {
                Row row = s1.createRow(r++); Cell c = row.createCell(0); c.setCellValue(k); c.setCellStyle(bold); row.createCell(1).setCellValue(String.valueOf(sum.get(k)));
            }
            Sheet s2 = wb.createSheet("Trades"); header(s2, bold, "ID", "Symbol", "Side", "Fill Price", "Qty", "Charges", "Realised P&L", "Executed At");
            r = 1; for (Trade t : trades.findByUserIdOrderByExecutedAtDesc(uid)) {
                Row row = s2.createRow(r++); row.createCell(0).setCellValue(t.getId()); row.createCell(1).setCellValue(t.getSymbol()); row.createCell(2).setCellValue(t.getSide().name());
                row.createCell(3).setCellValue(t.getFillPrice().doubleValue()); row.createCell(4).setCellValue(t.getFillQuantity().doubleValue()); row.createCell(5).setCellValue(t.getCharges().doubleValue());
                row.createCell(6).setCellValue(t.getRealisedPnl().doubleValue()); row.createCell(7).setCellValue(t.getExecutedAt().toString());
            }
            Sheet s3 = wb.createSheet("Orders"); header(s3, bold, "ID", "Symbol", "Side", "Type", "Qty", "Limit", "Stop-loss", "Target", "Status", "Placed At");
            r = 1; for (Order o : orders.findByUserIdOrderByPlacedAtDesc(uid)) {
                Row row = s3.createRow(r++); row.createCell(0).setCellValue(o.getId()); row.createCell(1).setCellValue(o.getSymbol()); row.createCell(2).setCellValue(o.getSide().name());
                row.createCell(3).setCellValue(o.getOrderType().name()); row.createCell(4).setCellValue(o.getQuantity().doubleValue());
                row.createCell(5).setCellValue(o.getLimitPrice() == null ? "" : o.getLimitPrice().toPlainString()); row.createCell(6).setCellValue(o.getStopLoss() == null ? "" : o.getStopLoss().toPlainString());
                row.createCell(7).setCellValue(o.getTargetPrice() == null ? "" : o.getTargetPrice().toPlainString()); row.createCell(8).setCellValue(o.getStatus().name()); row.createCell(9).setCellValue(o.getPlacedAt().toString());
            }
            Sheet s4 = wb.createSheet("Journal"); header(s4, bold, "Date", "Symbol", "Reason", "Tags", "Notes");
            r = 1; for (JournalEntry j : journal.findByUserIdOrderByCreatedAtDesc(uid)) {
                Row row = s4.createRow(r++); row.createCell(0).setCellValue(j.getCreatedAt().toString()); row.createCell(1).setCellValue(j.getSymbol());
                row.createCell(2).setCellValue(j.getReason() == null ? "" : j.getReason()); row.createCell(3).setCellValue(j.getTags() == null ? "" : j.getTags()); row.createCell(4).setCellValue(j.getNotes() == null ? "" : j.getNotes());
            }
            for (Sheet s : List.of(s1, s2, s3, s4)) for (int i = 0; i < 10; i++) s.autoSizeColumn(i);
            wb.write(out);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nifty-nexus-report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(out.toByteArray());
        }
    }

    private void header(Sheet s, CellStyle bold, String... cols) {
        Row h = s.createRow(0); for (int i = 0; i < cols.length; i++) { Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(bold); }
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf() throws Exception {
        Long uid = CurrentUser.id();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out); doc.open();
        doc.add(new Paragraph("Nifty Nexus - Paper Trading Report", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD)));
        String who = users.findById(uid).map(com.niftynexus.user.User::getName).orElse("");
        doc.add(new Paragraph("Trader: " + who));
        doc.add(new Paragraph("Nifty Nexus - developed by Abdullah Shaikh"));
        doc.add(new Paragraph("Educational paper-trading results. Not investment advice.\n\n"));
        Map<String, Object> sum = portfolio.summary(uid);
        PdfPTable t = new PdfPTable(2); t.setWidthPercentage(100);
        for (String k : List.of("totalValue", "cash", "invested", "marketValue", "unrealisedPnl", "realisedPnl", "chargesPaid", "overallReturnPct", "tradeCount")) { t.addCell(k); t.addCell(String.valueOf(sum.get(k))); }
        doc.add(t);
        doc.add(new Paragraph("\nTrades\n"));
        PdfPTable tt = new PdfPTable(6); tt.setWidthPercentage(100);
        for (String h : List.of("Symbol", "Side", "Price", "Qty", "Charges", "Realised P&L")) tt.addCell(h);
        for (Trade tr : trades.findByUserIdOrderByExecutedAtDesc(uid)) {
            tt.addCell(tr.getSymbol()); tt.addCell(tr.getSide().name()); tt.addCell(tr.getFillPrice().toPlainString()); tt.addCell(tr.getFillQuantity().stripTrailingZeros().toPlainString());
            tt.addCell(tr.getCharges().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()); tt.addCell(tr.getRealisedPnl().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        }
        doc.add(tt); doc.close();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nifty-nexus-report.pdf").contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());
    }
}
