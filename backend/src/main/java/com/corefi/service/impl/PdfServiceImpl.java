package com.corefi.service.impl;

import com.corefi.entity.Encaissement;
import com.corefi.entity.Facture;
import com.corefi.entity.LigneFacture;
import com.corefi.entity.Parametrage;
import com.corefi.entity.Tiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.entity.Decaissement;
import com.corefi.repository.EncaissementRepository;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.FactureRepository;
import com.corefi.repository.ParametrageRepository;
import com.corefi.repository.SessionCaisseRepository;
import com.corefi.entity.SessionCaisse;
import com.corefi.enums.MoyenPaiement;
import com.corefi.service.interfaces.IPdfService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements IPdfService {

    private final FactureRepository factureRepository;
    private final EncaissementRepository encaissementRepository;
    private final DecaissementRepository decaissementRepository;
    private final ParametrageRepository parametrageRepository;
    private final SessionCaisseRepository sessionCaisseRepository;

    private String getNomSociete() {
        return parametrageRepository.findByCle("INFO_SOCIETE_NOM")
                .map(Parametrage::getValeur)
                .orElse("SODICA SARL");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] genererFacturePdf(Long factureId) {
        System.out.println("Début génération PDF pour Facture ID: " + factureId);
        Facture facture = factureRepository.findByIdWithDetails(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        System.out.println("Facture trouvée: " + facture.getNumero());

        try {
            // Force l'initialisation des collections lazy
            if (facture.getLignes() != null) {
                System.out.println("Nombre de lignes: " + facture.getLignes().size());
            }
            Tiers client = facture.getTiers();
            if (client != null) {
                System.out.println("Client: " + client.getRaisonSociale());
            }

            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();
            System.out.println("Document ouvert");

            // En-tête de l'entreprise (SODICA SARL)
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph titreApp = new Paragraph(getNomSociete(), fontTitre);
            titreApp.setAlignment(Element.ALIGN_CENTER);
            document.add(titreApp);

            document.add(new Paragraph(" ")); // Espace vide
            
            // Titre Facture
            Font fontFacture = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titreFact = new Paragraph("FACTURE N° " + (facture.getNumero() != null ? facture.getNumero() : "N/A"), fontFacture);
            titreFact.setAlignment(Element.ALIGN_CENTER);
            document.add(titreFact);

            document.add(new Paragraph(" "));

            // Informations générales
            PdfPTable tableInfo = new PdfPTable(2);
            tableInfo.setWidthPercentage(100);
            
            PdfPCell cellG = new PdfPCell();
            cellG.setBorder(Rectangle.NO_BORDER);
            String dateF = facture.getDateFacture() != null ? facture.getDateFacture().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
            cellG.addElement(new Paragraph("Date d'émission: " + dateF));
            cellG.addElement(new Paragraph("Date d'échéance: " + (facture.getDateEcheance() != null ? facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-")));
            
            PdfPCell cellD = new PdfPCell();
            cellD.setBorder(Rectangle.NO_BORDER);
            cellD.addElement(new Paragraph("Client: " + (client != null ? client.getRaisonSociale() : "Divers")));
            cellD.addElement(new Paragraph("Adresse: " + (client != null && client.getAdresse() != null ? client.getAdresse() : "-")));
            cellD.addElement(new Paragraph("Téléphone: " + (client != null && client.getTelephone() != null ? client.getTelephone() : "-")));

            tableInfo.addCell(cellG);
            tableInfo.addCell(cellD);
            document.add(tableInfo);

            document.add(new Paragraph(" "));

            // Tableau des lignes de facture
            PdfPTable tableLignes = new PdfPTable(4);
            tableLignes.setWidthPercentage(100);
            tableLignes.setWidths(new float[] { 4f, 1.5f, 2f, 2f });

            // En-têtes tableau
            Font fontHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            ajouterCelluleEnTete(tableLignes, "Description", fontHead);
            ajouterCelluleEnTete(tableLignes, "Quantité", fontHead);
            ajouterCelluleEnTete(tableLignes, "Prix Unitaire", fontHead);
            ajouterCelluleEnTete(tableLignes, "Total Ligne", fontHead);

            // Contenu
            if (facture.getLignes() != null) {
                for (LigneFacture ligne : facture.getLignes()) {
                    tableLignes.addCell(new Phrase(ligne.getDesignation() != null ? ligne.getDesignation() : ""));
                    tableLignes.addCell(new Phrase(ligne.getQuantite() != null ? ligne.getQuantite().toString() : "0"));
                    tableLignes.addCell(new Phrase(String.format("%.2f", ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : 0.0)));
                    tableLignes.addCell(new Phrase(String.format("%.2f", ligne.getMontantHt() != null ? ligne.getMontantHt() : 0.0)));
                }
            }
            document.add(tableLignes);

            document.add(new Paragraph(" "));

            // Totaux
            PdfPTable tableTotal = new PdfPTable(2);
            tableTotal.setWidthPercentage(100);
            tableTotal.setWidths(new float[] { 7.5f, 2.5f });

            PdfPCell cellVide = new PdfPCell(new Phrase(""));
            cellVide.setBorder(Rectangle.NO_BORDER);
            
            String cur = (facture.getDevise() != null ? facture.getDevise().getCode() : "XAF");

            tableTotal.addCell(cellVide);
            tableTotal.addCell(new Phrase("Montant HT: " + String.format("%.2f", facture.getMontantHt() != null ? facture.getMontantHt() : 0.0) + " " + cur));
            
            tableTotal.addCell(cellVide);
            tableTotal.addCell(new Phrase("Montant TVA: " + String.format("%.2f", facture.getMontantTva() != null ? facture.getMontantTva() : 0.0) + " " + cur));
            
            tableTotal.addCell(cellVide);
            PdfPCell cellTTC = new PdfPCell(new Phrase("Montant TTC: " + String.format("%.2f", facture.getMontantTtc() != null ? facture.getMontantTtc() : 0.0) + " " + cur, fontHead));
            cellTTC.setBorder(Rectangle.TOP);
            tableTotal.addCell(cellTTC);
            
            document.add(tableTotal);

            // Pied de page
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Statut de la facture : " + (facture.getStatut() != null ? facture.getStatut().name() : "N/A"), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            System.out.println("Génération terminée avec succès (" + baos.size() + " octets)");
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("ERREUR GENERATION PDF: " + e.getMessage());
            e.printStackTrace();
            throw new WorkflowException("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public byte[] genererRecuEncaissementPdf(Long encaissementId) {
        Encaissement encaissement = encaissementRepository.findById(encaissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Encaissement introuvable"));

        Tiers client = encaissement.getClient();
        if (client != null) {
            client.getRaisonSociale();
        }

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();

            // En-tête
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph titreApp = new Paragraph(getNomSociete(), fontTitre);
            titreApp.setAlignment(Element.ALIGN_CENTER);
            document.add(titreApp);

            document.add(new Paragraph(" ")); 
            
            // Titre Reçu
            Font fontRecu = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titreRecu = new Paragraph("REÇU D'ENCAISSEMENT N° " + encaissement.getNumero(), fontRecu);
            titreRecu.setAlignment(Element.ALIGN_CENTER);
            document.add(titreRecu);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Informations
            Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Date du paiement: " + encaissement.getDateEncaissement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontTexte));
            document.add(new Paragraph("Moyen de paiement: " + encaissement.getMoyenPaiement().name(), fontTexte));
            
            // Métadonnées de paiement dynamiques
            if (encaissement.getBanqueEmettrice() != null) {
                document.add(new Paragraph("Banque: " + encaissement.getBanqueEmettrice(), fontTexte));
            }
            if (encaissement.getNumeroOperation() != null) {
                document.add(new Paragraph("N° Opération: " + encaissement.getNumeroOperation(), fontTexte));
            }
            if (encaissement.getDateOperation() != null) {
                document.add(new Paragraph("Date Opération: " + encaissement.getDateOperation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontTexte));
            }
            if (encaissement.getTelephone() != null) {
                document.add(new Paragraph("Téléphone (Mobile Money): " + encaissement.getTelephone(), fontTexte));
            }

            if (encaissement.getReference() != null && !encaissement.getReference().isEmpty()) {
                document.add(new Paragraph("Référence / Motif: " + encaissement.getReference(), fontTexte));
            }
            
            document.add(new Paragraph(" "));
            
            String clientNom = client != null ? client.getRaisonSociale() : "Client Divers";
            document.add(new Paragraph("Client: " + clientNom, fontTexte));
            
            document.add(new Paragraph(" "));
            
            Font fontMontant = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            String deviseCode = encaissement.getDevise() != null ? encaissement.getDevise().getCode() : "XAF";
            Paragraph pMontant = new Paragraph("Montant encaissé : " + String.format("%.2f", encaissement.getMontant()) + " " + deviseCode, fontMontant);
            pMontant.setAlignment(Element.ALIGN_RIGHT);
            document.add(pMontant);

            // Pied de page
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Reçu généré électroniquement. Merci de votre confiance.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            throw new WorkflowException("Erreur lors de la génération du reçu PDF : " + e.getMessage());
        }
    }

    private void ajouterCelluleEnTete(PdfPTable table, String texte, Font police) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(texte, police));
        table.addCell(header);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] genererRecuDecaissementPdf(Long decaissementId) {
        Decaissement decaissement = decaissementRepository.findById(decaissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Décaissement introuvable"));

        Tiers fournisseur = decaissement.getFournisseur();
        if (fournisseur != null) {
            fournisseur.getRaisonSociale();
        }

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();

            // En-tête
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph titreApp = new Paragraph(getNomSociete(), fontTitre);
            titreApp.setAlignment(Element.ALIGN_CENTER);
            document.add(titreApp);
            document.add(new Paragraph(" ")); 
            
            // Titre Reçu
            Font fontRecu = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titreRecu = new Paragraph("BON DE DÉCAISSEMENT N° " + decaissement.getNumero(), fontRecu);
            titreRecu.setAlignment(Element.ALIGN_CENTER);
            document.add(titreRecu);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Informations
            Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Date de création: " + decaissement.getDateSaisie().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontTexte));
            if (decaissement.getDateExecution() != null) {
                document.add(new Paragraph("Date d'exécution: " + decaissement.getDateExecution().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontTexte));
            }
            if (decaissement.getMoyenPaiement() != null) {
                document.add(new Paragraph("Moyen de paiement: " + decaissement.getMoyenPaiement().name(), fontTexte));
            }
            
            // Métadonnées de paiement dynamiques
            if (decaissement.getBanqueEmettrice() != null) {
                document.add(new Paragraph("Banque: " + decaissement.getBanqueEmettrice(), fontTexte));
            }
            if (decaissement.getNumeroOperation() != null) {
                document.add(new Paragraph("N° Opération: " + decaissement.getNumeroOperation(), fontTexte));
            }
            if (decaissement.getDateOperation() != null) {
                document.add(new Paragraph("Date Opération: " + decaissement.getDateOperation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontTexte));
            }
            if (decaissement.getTelephone() != null) {
                document.add(new Paragraph("Téléphone (Mobile Money): " + decaissement.getTelephone(), fontTexte));
            }

            document.add(new Paragraph("Motif: " + decaissement.getMotif(), fontTexte));
            
            document.add(new Paragraph(" "));
            
            String fournNom = fournisseur != null ? fournisseur.getRaisonSociale() : "Fournisseur Divers";
            document.add(new Paragraph("Bénéficiaire / Fournisseur: " + fournNom, fontTexte));
            if (decaissement.getBeneficiaire() != null) {
                document.add(new Paragraph("À l'attention de: " + decaissement.getBeneficiaire(), fontTexte));
            }
            
            document.add(new Paragraph(" "));
            
            Font fontMontant = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            String deviseCode = decaissement.getDevise() != null ? decaissement.getDevise().getCode() : "XAF";
            Paragraph pMontant = new Paragraph("Montant payé : " + String.format("%.2f", decaissement.getMontant()) + " " + deviseCode, fontMontant);
            pMontant.setAlignment(Element.ALIGN_RIGHT);
            document.add(pMontant);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Bon de décaissement - " + decaissement.getStatut().name(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            throw new WorkflowException("Erreur lors de la génération du bon PDF : " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] genererRapportCloturePdf(Long sessionId) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable"));

        List<Encaissement> encaissements = encaissementRepository.findBySessionCaisseId(sessionId).stream()
                .filter(e -> e.getMoyenPaiement() == MoyenPaiement.ESPECES)
                .toList();

        List<Decaissement> decaissements = decaissementRepository.findBySessionCaisseId(sessionId).stream()
                .filter(d -> d.getMoyenPaiement() == MoyenPaiement.ESPECES)
                .toList();

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();

            // Header
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph titreApp = new Paragraph(getNomSociete(), fontTitre);
            titreApp.setAlignment(Element.ALIGN_CENTER);
            document.add(titreApp);

            document.add(new Paragraph(" "));

            Font fontType = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titreRapport = new Paragraph("RAPPORT DE CLÔTURE DE CAISSE", fontType);
            titreRapport.setAlignment(Element.ALIGN_CENTER);
            document.add(titreRapport);

            document.add(new Paragraph(" "));

            // Session Info
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11);
            
            document.add(new Paragraph("Caisse: " + session.getCaisse().getLibelle(), fontNormal));
            document.add(new Paragraph("Caissier: " + session.getCaissier().getNom() + " " + session.getCaissier().getPrenom(), fontNormal));
            document.add(new Paragraph("Date Ouverture: " + session.getDateOuverture().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontNormal));
            if (session.getDateFermeture() != null) {
                document.add(new Paragraph("Date Fermeture: " + session.getDateFermeture().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontNormal));
            }
            
            document.add(new Paragraph(" "));

            // Table Encaissements
            document.add(new Paragraph("DÉTAIL DES ENCAISSEMENTS (RECETTES)", fontBold));
            document.add(new Paragraph(" "));
            PdfPTable tableEnc = new PdfPTable(3);
            tableEnc.setWidthPercentage(100);
            tableEnc.setWidths(new float[]{2f, 5f, 3f});
            ajouterCelluleEnTete(tableEnc, "N°", fontBold);
            ajouterCelluleEnTete(tableEnc, "Motif / Client", fontBold);
            ajouterCelluleEnTete(tableEnc, "Montant (XAF)", fontBold);

            for (Encaissement e : encaissements) {
                tableEnc.addCell(new Phrase(e.getNumero(), fontNormal));
                tableEnc.addCell(new Phrase((e.getClient() != null ? e.getClient().getRaisonSociale() + " - " : "") + e.getReference(), fontNormal));
                tableEnc.addCell(new Phrase(String.format("%,.0f", e.getMontant()), fontNormal));
            }
            if (encaissements.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucun mouvement", fontNormal));
                empty.setColspan(3);
                tableEnc.addCell(empty);
            }
            document.add(tableEnc);

            document.add(new Paragraph(" "));

            // Table Décaissements
            document.add(new Paragraph("DÉTAIL DES DÉCAISSEMENTS (DÉPENSES)", fontBold));
            document.add(new Paragraph(" "));
            PdfPTable tableDec = new PdfPTable(3);
            tableDec.setWidthPercentage(100);
            tableDec.setWidths(new float[]{2f, 5f, 3f});
            ajouterCelluleEnTete(tableDec, "N°", fontBold);
            ajouterCelluleEnTete(tableDec, "Motif", fontBold);
            ajouterCelluleEnTete(tableDec, "Montant (XAF)", fontBold);

            for (Decaissement d : decaissements) {
                tableDec.addCell(new Phrase(d.getNumero(), fontNormal));
                tableDec.addCell(new Phrase(d.getMotif(), fontNormal));
                tableDec.addCell(new Phrase(String.format("%,.0f", d.getMontant()), fontNormal));
            }
            if (decaissements.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Aucun mouvement", fontNormal));
                empty.setColspan(3);
                tableDec.addCell(empty);
            }
            document.add(tableDec);

            document.add(new Paragraph(" "));
            document.add(new LineSeparator());
            document.add(new Paragraph(" "));

            // Récapitulatif Final
            PdfPTable tableRecap = new PdfPTable(2);
            tableRecap.setWidthPercentage(60);
            tableRecap.setHorizontalAlignment(Element.ALIGN_RIGHT);

            ajouterRecapRow(tableRecap, "Solde Initial (A):", session.getSoldeInitial());
            ajouterRecapRow(tableRecap, "Total Recettes (+):", encaissements.stream().map(Encaissement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));
            ajouterRecapRow(tableRecap, "Total Dépenses (-):", decaissements.stream().map(Decaissement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));
            document.add(tableRecap);
            
            document.add(new Paragraph(" "));
            
            PdfPTable tableTotal = new PdfPTable(2);
            tableTotal.setWidthPercentage(60);
            tableTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            ajouterRecapRowBold(tableTotal, "Solde Théorique (B):", session.getSoldeFinalTheorique());
            ajouterRecapRowBold(tableTotal, "Solde Réel compté (C):", session.getSoldeFinalReel());
            
            BaseColor ecartColor = session.getEcart().compareTo(BigDecimal.ZERO) == 0 ? BaseColor.BLACK : BaseColor.RED;
            ajouterRecapRowColored(tableTotal, "ÉCART (C - B):", session.getEcart(), ecartColor);
            
            document.add(tableTotal);

            if (session.getMotifEcart() != null && !session.getMotifEcart().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Justification de l'écart:", fontBold));
                document.add(new Paragraph(session.getMotifEcart(), fontNormal));
            }

            // Signatures
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            PdfPTable tableSign = new PdfPTable(2);
            tableSign.setWidthPercentage(100);
            PdfPCell signC = new PdfPCell(new Phrase("Visa Caissier", fontBold));
            signC.setBorder(Rectangle.NO_BORDER);
            signC.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell signR = new PdfPCell(new Phrase("Visa Responsable Financier / PDG", fontBold));
            signR.setBorder(Rectangle.NO_BORDER);
            signR.setHorizontalAlignment(Element.ALIGN_CENTER);
            tableSign.addCell(signC);
            tableSign.addCell(signR);
            document.add(tableSign);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new WorkflowException("Erreur lors de la génération du rapport PDF : " + e.getMessage());
        }
    }

    private void ajouterRecapRow(PdfPTable table, String label, java.math.BigDecimal montant) {
        PdfPCell c1 = new PdfPCell(new Phrase(label));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(String.format("%,.0f XAF", montant)));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void ajouterRecapRowBold(PdfPTable table, String label, java.math.BigDecimal montant) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(String.format("%,.0f XAF", montant), font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void ajouterRecapRowColored(PdfPTable table, String label, java.math.BigDecimal montant, BaseColor color) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color);
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(String.format("%,.0f XAF", montant), font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }
}
