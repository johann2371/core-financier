package com.corefi.service.impl;

import com.corefi.entity.*;
import com.corefi.enums.StatutDecaissement;
import com.corefi.enums.StatutFacture;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IRapportService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RapportServiceImpl implements IRapportService {

    private final EncaissementRepository encaissementRepository;
    private final DecaissementRepository decaissementRepository;
    private final FactureRepository factureRepository;

    @Override
    public Map<String, Object> genererBilanMensuel(int mois, int annee) {
        YearMonth ym = YearMonth.of(annee, mois);
        Map<String, Object> bilan = new LinkedHashMap<>();

        // Chiffre d'Affaires (Encaissements du mois)
        BigDecimal totalCA = encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && YearMonth.from(e.getDateEncaissement()).equals(ym))
                .map(Encaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        bilan.put("totalCA", totalCA);

        // Total Dépenses (Décaissements exécutés du mois)
        List<Decaissement> decMois = decaissementRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutDecaissement.EXECUTEE)
                .filter(d -> d.getDateExecution() != null && YearMonth.from(d.getDateExecution()).equals(ym))
                .collect(Collectors.toList());

        BigDecimal totalDepenses = decMois.stream()
                .map(Decaissement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        bilan.put("totalDepenses", totalDepenses);

        // Nombre d'opérations
        long nbEnc = encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && YearMonth.from(e.getDateEncaissement()).equals(ym))
                .count();
        long nbDec = decMois.size();
        bilan.put("nbEncaissements", nbEnc);
        bilan.put("nbDecaissements", nbDec);

        // Top 10 Clients (par encaissements de la période)
        Map<String, BigDecimal> clientStats = new LinkedHashMap<>();
        encaissementRepository.findAll().stream()
                .filter(e -> e.getDateEncaissement() != null && YearMonth.from(e.getDateEncaissement()).equals(ym))
                .filter(e -> e.getClient() != null)
                .forEach(e -> {
                    String nom = e.getClient().getRaisonSociale();
                    clientStats.put(nom, clientStats.getOrDefault(nom, BigDecimal.ZERO).add(e.getMontant()));
                });

        List<Map<String, Object>> topClients = clientStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(entry -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nom", entry.getKey());
                    m.put("total", entry.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        bilan.put("topClients", topClients);

        // Répartition des dépenses par catégorie
        Map<String, BigDecimal> repartition = new LinkedHashMap<>();
        decMois.forEach(d -> {
            String cat = d.getCategorie() != null ? d.getCategorie().name() : "Autre";
            repartition.put(cat, repartition.getOrDefault(cat, BigDecimal.ZERO).add(d.getMontant()));
        });
        bilan.put("repartitionDepenses", repartition);

        return bilan;
    }

    @Override
    public byte[] genererBilanMensuelPdf(int mois, int annee) {
        Map<String, Object> bilan = genererBilanMensuel(mois, annee);
        YearMonth ym = YearMonth.of(annee, mois);
        String moisLabel = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            // Fonts
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(30, 41, 59));
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(148, 163, 184));
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(30, 41, 59));
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(71, 85, 105));
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(30, 41, 59));
            Font greenFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(5, 150, 105));
            Font redFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(220, 38, 38));

            // === En-tête ===
            Paragraph title = new Paragraph("BILAN MENSUEL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph(moisLabel.substring(0, 1).toUpperCase() + moisLabel.substring(1) + " " + annee, subtitleFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(25);
            document.add(period);

            document.add(new Paragraph("SODICA - Rapport financier mensuel", subtitleFont));
            document.add(new Paragraph(" "));

            // === Métriques Principales ===
            BigDecimal totalCA = (BigDecimal) bilan.get("totalCA");
            BigDecimal totalDepenses = (BigDecimal) bilan.get("totalDepenses");
            BigDecimal resultatNet = totalCA.subtract(totalDepenses);

            PdfPTable metricsTable = new PdfPTable(3);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingBefore(10);
            metricsTable.setSpacingAfter(20);

            addMetricCell(metricsTable, "Chiffre d'Affaires", formatMontant(totalCA) + " FCFA", new BaseColor(16, 185, 129));
            addMetricCell(metricsTable, "Total Depenses", formatMontant(totalDepenses) + " FCFA", new BaseColor(245, 158, 11));
            addMetricCell(metricsTable, "Resultat Net", (resultatNet.signum() >= 0 ? "+" : "") + formatMontant(resultatNet) + " FCFA",
                    resultatNet.signum() >= 0 ? new BaseColor(16, 185, 129) : new BaseColor(220, 38, 38));

            document.add(metricsTable);

            // === Nombre d'opérations ===
            document.add(new Paragraph("Operations du mois : " + bilan.get("nbEncaissements") + " encaissements, " + bilan.get("nbDecaissements") + " decaissements", normalFont));
            document.add(new Paragraph(" "));

            // === Top 10 Clients ===
            document.add(new Paragraph("TOP 10 CLIENTS", headerFont));
            document.add(new Paragraph(" "));

            List<Map<String, Object>> topClients = (List<Map<String, Object>>) bilan.get("topClients");
            if (topClients != null && !topClients.isEmpty()) {
                PdfPTable clientTable = new PdfPTable(3);
                clientTable.setWidthPercentage(100);
                clientTable.setWidths(new float[]{1, 4, 2});

                addTableHeader(clientTable, "#", boldFont);
                addTableHeader(clientTable, "Client", boldFont);
                addTableHeader(clientTable, "Montant", boldFont);

                int rank = 1;
                for (Map<String, Object> client : topClients) {
                    addTableCell(clientTable, String.valueOf(rank++), normalFont);
                    addTableCell(clientTable, String.valueOf(client.get("nom")), normalFont);
                    addTableCell(clientTable, formatMontant((BigDecimal) client.get("total")) + " FCFA", boldFont);
                }
                document.add(clientTable);
            } else {
                document.add(new Paragraph("Aucun encaissement client pour cette periode.", normalFont));
            }

            document.add(new Paragraph(" "));

            // === Répartition des Dépenses ===
            document.add(new Paragraph("REPARTITION DES DEPENSES PAR CATEGORIE", headerFont));
            document.add(new Paragraph(" "));

            Map<String, BigDecimal> repartition = (Map<String, BigDecimal>) bilan.get("repartitionDepenses");
            if (repartition != null && !repartition.isEmpty()) {
                PdfPTable depTable = new PdfPTable(3);
                depTable.setWidthPercentage(100);
                depTable.setWidths(new float[]{3, 2, 1});

                addTableHeader(depTable, "Categorie", boldFont);
                addTableHeader(depTable, "Montant", boldFont);
                addTableHeader(depTable, "%", boldFont);

                for (Map.Entry<String, BigDecimal> entry : repartition.entrySet()) {
                    addTableCell(depTable, entry.getKey(), normalFont);
                    addTableCell(depTable, formatMontant(entry.getValue()) + " FCFA", normalFont);
                    double pct = totalDepenses.signum() > 0
                            ? entry.getValue().doubleValue() / totalDepenses.doubleValue() * 100
                            : 0;
                    addTableCell(depTable, String.format("%.1f%%", pct), boldFont);
                }
                document.add(depTable);
            } else {
                document.add(new Paragraph("Aucune depense pour cette periode.", normalFont));
            }

            // === Footer ===
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Genere automatiquement par CoreFi le " + LocalDate.now(), subtitleFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la generation du PDF du bilan mensuel", e);
        }
    }

    private void addMetricCell(PdfPTable table, String label, String value, BaseColor accentColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPadding(10);
        cell.setBackgroundColor(new BaseColor(249, 250, 251));

        Paragraph labelP = new Paragraph(label, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(100, 116, 139)));
        labelP.setSpacingAfter(4);
        cell.addElement(labelP);

        Paragraph valueP = new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, accentColor));
        cell.addElement(valueP);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new BaseColor(241, 245, 249));
        cell.setPadding(8);
        cell.setBorderWidth(0);
        cell.setBorderWidthBottom(1);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderWidth(0);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColor(new BaseColor(241, 245, 249));
        table.addCell(cell);
    }

    private String formatMontant(BigDecimal montant) {
        if (montant == null) return "0";
        return String.format("%,.0f", montant.doubleValue()).replace(",", " ");
    }
}
