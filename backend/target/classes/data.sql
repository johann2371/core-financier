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


-- 3. Paramétrage initial (seuil PDG + autres configs)
INSERT IGNORE INTO parametrage (cle, valeur, description)
VALUES
    ('SEUIL_APPROBATION_PDG',       '500000', 'Montant en XAF au-delà duquel le PDG doit approuver'),
    ('DEVISE_BASE_CODE',            'XAF',    'Devise de référence pour les opérations'),
    ('DUREE_SESSION_MINUTES',       '60',     'Durée de validité du token JWT en minutes'),
    ('MAX_TENTATIVES_CONNEXION',    '5',      'Nombre de tentatives avant blocage du compte'),
    ('DUREE_BLOCAGE_MINUTES',       '30',     'Durée du blocage de compte en minutes');
