-- ============================================================
-- data.sql — Données initiales Core Financier
-- Exécuté automatiquement au démarrage si spring.jpa.defer-datasource-initialization=true
-- ============================================================

-- 1. Devise de base (XAF, EUR, USD)
INSERT IGNORE INTO devise (code, libelle, symbole, devise_base, actif)
VALUES
    ('XAF', 'Franc CFA BEAC', 'FCFA', true, true),
    ('EUR', 'Euro',            '€',    false, true),
    ('USD', 'Dollar US',       '$',    false, true);

-- NOTE : L'utilisateur admin est créé par DataInitializer.java au démarrage
--        pour garantir que le mot de passe est correctement encodé (BCrypt coût 12).


-- 3. Parametrage initial (seuil PDG + autres configs)
INSERT IGNORE INTO parametrage (cle, valeur, description)
VALUES
    ('SEUIL_APPROBATION_PDG',       '500000', 'Montant en XAF au-dela duquel le PDG doit approuver'),
    ('DEVISE_BASE_CODE',            'XAF',    'Devise de reference pour les operations'),
    ('DUREE_SESSION_MINUTES',       '60',     'Duree de validite du token JWT en minutes'),
    ('MAX_TENTATIVES_CONNEXION',    '5',      'Nombre de tentatives avant blocage du compte'),
    ('DUREE_BLOCAGE_MINUTES',       '30',     'Duree du blocage de compte en minutes'),
    ('INFO_SOCIETE_NOM',            'SODICA SARL', 'Nom officiel de la societe'),
    ('INFO_SOCIETE_ADRESSE',        'Yaounde, Cameroun', 'Adresse du siege social'),
    ('INFO_SOCIETE_TEL',            '+237 6XX XXX XXX', 'Telephone principal de la societe'),
    ('INFO_SOCIETE_EMAIL',          'contact@sodica.cm', 'Email de contact de la societe'),
    ('APP_LOGO_URL',                '',       'URL du logo principal de application'),
    ('INVOICE_LOGO_URL',            '',       'URL du logo utilise sur les factures PDF'),
    ('SEUIL_SOLDE_CRITIQUE',        '100000', 'Solde minimum avant alerte de tresorerie (XAF)'),
    ('NOTIF_EMAIL_ALERTE',          'true',   'Activer les notifications par email pour les alertes'),
    ('APP_VERSION',                 '1.0.0',  'Version actuelle du systeme');

-- 4. Comptes financiers par défaut pour les tests
INSERT IGNORE INTO compte_financier (id, type, numero, libelle, solde, devise_id, actif)
VALUES
    (1, 'BANQUE', 'BQ-001', 'Compte Courant Société Générale', 15000000.00, 1, true),
    (2, 'CAISSE', 'CA-001', 'Caisse Principale siège', 500000.00, 1, true);

INSERT IGNORE INTO compte_bancaire (banque, agence, iban, bic, type_compte, id)
VALUES
    ('Société Générale', 'Agence Yaoundé', 'CM21 1000 1000 1000', 'SGCMCM', 'COURANT', 1);

INSERT IGNORE INTO caisse (emplacement, solde_limite, id)
VALUES
    ('Siège Social Yaoundé', 2000000.00, 2);

