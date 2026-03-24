package com.corefi.service.impl;

import com.corefi.entity.Encaissement;
import com.corefi.entity.Facture;
import com.corefi.entity.LigneFacture;
import com.corefi.entity.Tiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.entity.Decaissement;
import com.corefi.repository.EncaissementRepository;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.FactureRepository;
import com.corefi.service.interfaces.IPdfService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements IPdfService {

    private final FactureRepository factureRepository;
    private final EncaissementRepository encaissementRepository;
    private final DecaissementRepository decaissementRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] genererFacturePdf(Long factureId) {
        System.out.println("Début génération PDF pour Facture ID: " + factureId);
        Facture facture = factureRepository.findById(factureId)
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
            Paragraph titreApp = new Paragraph("SODICA SARL", fontTitre);
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
            Paragraph titreApp = new Paragraph("SODICA SARL", fontTitre);
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
            Paragraph titreApp = new Paragraph("SODICA SARL", fontTitre);
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
}
