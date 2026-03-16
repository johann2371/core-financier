# CORE FINANCIER — AI CONTEXT FILE
> Colle ce fichier en entier au début de chaque session avec ton IA.
> Dernière mise à jour : mars 2026

---

## 1. PRÉSENTATION DU PROJET

**Nom du projet** : Core Financier  
**Client** : SODICA SARL — société camerounaise de distribution  
**Objectif** : Système de gestion financière (encaissements clients + décaissements fournisseurs) remplaçant Excel/papier  
**Étudiant** : Gouaffo Nanfah Johann Brandon — Projet de fin d'études BTS/Licence à l'IUC (Cameroun)

### Stack technique
| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3.2 — Java 17 — Maven |
| Sécurité | Spring Security + JWT (expiration 1h) |
| Persistance | JPA / Hibernate + MySQL 8 (XAMPP en local) |
| Frontend | Vue.js 3 + Vite + Pinia + Vue Router + Axios |
| PDF | iText (génération reçus et factures) |
| Tests | JUnit 5 + Mockito |
| Conteneurisation | Docker (prod) |

### Devise de base
- Code ISO : **XAF** (Franc CFA BEAC) — symbole FCFA  
- Devises supportées : XAF, EUR, USD  
- TVA camerounaise : **19,25 %**

---

## 2. RÈGLES D'ARCHITECTURE — À RESPECTER ABSOLUMENT

### 2.1 Backend — Principes SOLID + Inversion de dépendance

```
Les controllers NE dépendent PAS des classes d'implémentation.
Ils dépendent UNIQUEMENT des interfaces de service.
```

**Exemple obligatoire :**
```java
// ✅ CORRECT
@RestController
public class DecaissementController {
    private final IDecaissementService decaissementService; // interface
    public DecaissementController(IDecaissementService decaissementService) {
        this.decaissementService = decaissementService;
    }
}

// ❌ INTERDIT
@RestController
public class DecaissementController {
    private final DecaissementServiceImpl decaissementService; // implémentation directe
}
```

### 2.2 Structure des packages backend

```
com.corefi
│
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── ApplicationConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── UtilisateurController.java
│   ├── EncaissementController.java
│   ├── DecaissementController.java
│   ├── FactureController.java
│   ├── TiersController.java
│   ├── CompteFinancierController.java
│   └── TableauBordController.java
│
├── service/
│   ├── interfaces/              ← LES CONTROLLERS DÉPENDENT D'ICI
│   │   ├── IAuthService.java
│   │   ├── IUtilisateurService.java
│   │   ├── IEncaissementService.java
│   │   ├── IDecaissementService.java
│   │   ├── IFactureService.java
│   │   ├── ITiersService.java
│   │   ├── ICompteFinancierService.java
│   │   ├── IJournalAuditService.java
│   │   └── ITableauBordService.java
│   │
│   └── impl/                   ← IMPLÉMENTATIONS (jamais injectées dans les controllers)
│       ├── AuthServiceImpl.java
│       ├── UtilisateurServiceImpl.java
│       ├── EncaissementServiceImpl.java
│       ├── DecaissementServiceImpl.java
│       ├── FactureServiceImpl.java
│       ├── TiersServiceImpl.java
│       ├── CompteFinancierServiceImpl.java
│       ├── JournalAuditServiceImpl.java
│       └── TableauBordServiceImpl.java
│
├── repository/
│   ├── UtilisateurRepository.java
│   ├── EncaissementRepository.java
│   ├── DecaissementRepository.java
│   ├── FactureRepository.java
│   ├── LigneFactureRepository.java
│   ├── TiersRepository.java
│   ├── ClientRepository.java
│   ├── FournisseurRepository.java
│   ├── CompteFinancierRepository.java
│   ├── MouvementCompteRepository.java
│   ├── AffectationPaiementRepository.java
│   ├── DeviseRepository.java
│   ├── TauxChangeRepository.java
│   ├── JournalAuditRepository.java
│   └── ParametrageRepository.java
│
├── entity/
│   ├── Utilisateur.java
│   ├── Tiers.java
│   ├── Client.java
│   ├── Fournisseur.java
│   ├── Encaissement.java
│   ├── Decaissement.java
│   ├── Facture.java
│   ├── LigneFacture.java
│   ├── CompteFinancier.java
│   ├── Caisse.java
│   ├── CompteBancaire.java
│   ├── MouvementCompte.java
│   ├── AffectationPaiement.java
│   ├── Devise.java
│   ├── TauxChange.java
│   ├── JournalAudit.java
│   └── Parametrage.java
│
├── dto/
│   ├── request/
│   │   ├── auth/
│   │   │   ├── LoginRequest.java
│   │   │   └── RefreshTokenRequest.java
│   │   ├── utilisateur/
│   │   │   └── UtilisateurCreateRequest.java
│   │   ├── encaissement/
│   │   │   ├── EncaissementCreateRequest.java
│   │   │   └── AffectationRequest.java
│   │   ├── decaissement/
│   │   │   ├── DecaissementCreateRequest.java
│   │   │   ├── ValidationRFRequest.java
│   │   │   ├── ApprobationPDGRequest.java
│   │   │   └── ExecutionCaissierRequest.java
│   │   ├── facture/
│   │   │   ├── FactureCreateRequest.java
│   │   │   └── LigneFactureRequest.java
│   │   └── tiers/
│   │       └── TiersCreateRequest.java
│   │
│   └── response/
│       ├── auth/
│       │   └── AuthResponse.java
│       ├── encaissement/
│       │   └── EncaissementResponse.java
│       ├── decaissement/
│       │   └── DecaissementResponse.java
│       ├── facture/
│       │   └── FactureResponse.java
│       ├── tiers/
│       │   └── TiersResponse.java
│       └── tableaubord/
│           └── TableauBordResponse.java
│
├── exception/
│   ├── GlobalExceptionHandler.java     ← @ControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── SoldeInsuffisantException.java
│   ├── WorkflowException.java          ← statut invalide pour l'action demandée
│   ├── SeuilPDGRequiredException.java
│   └── AccesNonAutoriseException.java
│
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
│
├── enums/
│   ├── Role.java
│   ├── StatutDecaissement.java
│   ├── StatutEncaissement.java
│   ├── StatutFacture.java
│   ├── MoyenPaiement.java
│   ├── TypeTiers.java
│   ├── TypeCompte.java
│   └── TypeMouvement.java
│
└── CoreFinancierApplication.java
```

### 2.3 Profils Spring Boot (application.properties)

```
src/main/resources/
├── application.properties          ← config commune (nom app, etc.)
├── application-dev.properties      ← XAMPP local, logs debug, H2 console off
├── application-test.properties     ← H2 en mémoire, données de test
└── application-prod.properties     ← MySQL prod, logs warn, HTTPS
```

**Contenu type application-dev.properties :**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/core_financier
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
jwt.secret=DEV_SECRET_KEY_CHANGE_IN_PROD
jwt.expiration=3600000
logging.level.com.corefi=DEBUG
```

---

## 3. ENUMS JAVA — CORRESPONDANCE AVEC LA BASE DE DONNÉES

```java
// Role.java
public enum Role {
    COMPTABLE, RESPONSABLE_FINANCIER, PDG, CAISSIER, ADMINISTRATEUR
}

// StatutDecaissement.java
public enum StatutDecaissement {
    BROUILLON,       // Comptable : en cours de saisie
    EN_ATTENTE,      // Soumis, en attente Responsable Financier
    VALIDEE_RF,      // Validé par RF (montant < seuil PDG)
    EN_ATTENTE_PDG,  // Transmis au PDG (montant >= seuil)
    VALIDEE_PDG,     // Approuvé par PDG, en attente Caissier
    EXECUTEE,        // Caissier a exécuté le paiement
    REJETEE_RF,      // Rejeté par le Responsable Financier
    REJETEE_PDG,     // Rejeté par le PDG
    ANNULEE
}

// StatutEncaissement.java
public enum StatutEncaissement {
    BROUILLON, EN_ATTENTE, VALIDEE, ANNULEE
}

// StatutFacture.java
public enum StatutFacture {
    BROUILLON, VALIDEE, PARTIELLEMENT_PAYEE, SOLDEE, ANNULEE
}

// MoyenPaiement.java
public enum MoyenPaiement {
    ESPECES, CHEQUE, VIREMENT, CARTE_BANCAIRE, ORANGE_MONEY
}

// TypeTiers.java
public enum TypeTiers { CLIENT, FOURNISSEUR }

// TypeCompte.java
public enum TypeCompte { BANQUE, CAISSE }

// TypeMouvement.java
public enum TypeMouvement { CREDIT, DEBIT }
```

---

## 4. WORKFLOW DÉCAISSEMENT — LOGIQUE MÉTIER CRITIQUE

```
COMPTABLE          RF               PDG (si seuil)     CAISSIER
    │                │                    │                │
    │ createDemande()│                    │                │
    │────────────────>                    │                │
    │  statut: EN_ATTENTE                 │                │
    │                │                   │                │
    │                │ valider/rejeter()  │                │
    │                │ (vérifie solde)    │                │
    │                │                   │                │
    │        si montant < 500 000 FCFA    │                │
    │                │ statut: VALIDEE_RF │                │
    │                │────────────────────────────────────>│
    │                │                   │   executer()   │
    │                │                   │  statut: EXECUTEE
    │                │                   │                │
    │        si montant >= 500 000 FCFA   │                │
    │                │ statut: EN_ATTENTE_PDG              │
    │                │────────────────────>               │
    │                │              approuver/rejeter()   │
    │                │              statut: VALIDEE_PDG   │
    │                │────────────────────────────────────>│
    │                │                   │   executer()   │
    │                │                   │  statut: EXECUTEE
```

**Règle de séparation des tâches :**
- `saisi_par` (COMPTABLE) ≠ `valide_par` (RESPONSABLE_FINANCIER)  
- `valide_par` ≠ `approuve_par_pdg` (PDG)  
- `approuve_par_pdg` ≠ `execute_par` (CAISSIER)  
- Le système doit lever une `WorkflowException` si ces règles sont violées.

**Seuil PDG** : récupéré dynamiquement depuis la table `parametrage` (clé : `SEUIL_APPROBATION_PDG`), **jamais hardcodé**.

---

## 5. WORKFLOW ENCAISSEMENT — LOGIQUE MÉTIER

```
CLIENT ──paye──> CAISSIER (physique, hors système)
                     │
                     ▼
               COMPTABLE (saisie dans le système)
               1. Sélectionner le client
               2. Saisir montant + moyen de paiement
               3. Affecter aux factures impayées
               4. Générer reçu PDF
               statut: VALIDEE directement (pas de workflow de validation)
```

---

## 6. SCHÉMA DE BASE DE DONNÉES

### Tables et relations principales

```
utilisateur (id, nom, prenom, email, password[BCrypt], role[enum], actif,
             tentatives_connexion, bloque_jusqu_a, date_creation, dernier_acces)

tiers (id, type[CLIENT|FOURNISSEUR], code[unique], raison_sociale, telephone,
       email, adresse, ville, pays, devise_defaut_id→devise, actif)
  ├── client (id→tiers, type_client[PARTICULIER|ENTREPRISE], credit_limite, delai_paiement)
  └── fournisseur (id→tiers, numero_compte, iban, delai_paiement_moyen)

devise (id, code[XAF/EUR/USD], libelle, symbole, devise_base, actif)
taux_change (id, devise_source_id→devise, devise_cible_id→devise, taux, date_debut, actif)

compte_financier (id, type[BANQUE|CAISSE], numero[unique], libelle, solde, devise_id→devise, actif)
  ├── caisse (id→compte_financier, emplacement, solde_limite, responsable_id→utilisateur)
  └── compte_bancaire (id→compte_financier, banque, agence, iban, bic, type_compte)

facture (id, type[VENTE|ACHAT], numero[unique], date_facture, date_echeance,
         tiers_id→tiers, montant_ht, montant_tva, montant_ttc,
         statut[BROUILLON|VALIDEE|PARTIELLEMENT_PAYEE|SOLDEE|ANNULEE],
         devise_id, cree_par→utilisateur, valide_par→utilisateur)
  └── ligne_facture (id, facture_id→facture, numero_ligne, designation,
                     quantite, unite, prix_unitaire, taux_tva[19.25], montant_ht, montant_tva, montant_ttc)
      ⚠️ 3 triggers MySQL recalculent automatiquement les totaux facture sur INSERT/UPDATE/DELETE

encaissement (id, numero[unique], date_encaissement, client_id→tiers,
              montant, devise_id→devise, taux_change, moyen_paiement[enum],
              compte_financier_id→compte_financier, numero_recu, reference,
              statut[BROUILLON|EN_ATTENTE|VALIDEE|ANNULEE] DEFAULT VALIDEE,
              saisi_par→utilisateur, valide_par→utilisateur)

decaissement (id, numero[unique], date_decaissement, fournisseur_id→tiers,
              montant, devise_id→devise, taux_change, moyen_paiement[enum],
              compte_financier_id→compte_financier, beneficiaire, reference,
              statut[BROUILLON|EN_ATTENTE|VALIDEE_RF|EN_ATTENTE_PDG|VALIDEE_PDG|EXECUTEE|REJETEE_RF|REJETEE_PDG|ANNULEE],
              saisi_par→utilisateur,         date_saisie,
              valide_par→utilisateur,        date_validation,      motif_rejet_rf,
              seuil_pdg_requis[boolean],
              approuve_par_pdg→utilisateur,  date_approbation_pdg, motif_rejet_pdg,
              execute_par→utilisateur,       date_execution,       reference_execution)

affectation_paiement (id, transaction_id, transaction_type[ENCAISSEMENT|DECAISSEMENT],
                      facture_id→facture, montant_affecte, affecte_par→utilisateur)

mouvement_compte (id, compte_financier_id→compte_financier, date_operation,
                  type_mouvement[CREDIT|DEBIT], montant_debit, montant_credit,
                  solde_apres, transaction_id, transaction_type)

details_cheque (id, transaction_id, transaction_type, numero_cheque, banque_emettrice, date_emission)
details_virement (id, transaction_id, transaction_type, numero_virement, banque_emettrice, banque_receptrice)
details_orange_money (id, transaction_id, transaction_type, numero_telephone, nom_titulaire, reference_transaction)

journal_audit (id, utilisateur_id→utilisateur, action[CREATE|UPDATE|DELETE|LOGIN|VALIDATE|APPROVE|EXECUTE|REJECT],
               entite, entite_id, anciennes_valeurs[JSON], nouvelles_valeurs[JSON], adresse_ip, date_action)

parametrage (id, cle[unique], valeur, description, modifie_par→utilisateur)
  Données initiales :
  - SEUIL_APPROBATION_PDG = 500000
  - DEVISE_BASE_CODE = XAF
  - DUREE_SESSION_MINUTES = 60
  - MAX_TENTATIVES_CONNEXION = 5
  - DUREE_BLOCAGE_MINUTES = 30
```

---

## 7. SÉCURITÉ — RÈGLES IMPORTANTES

- Authentification : **JWT** — expiration 1 heure — BCrypt coût 12
- Blocage : après **5 tentatives** échouées → compte bloqué **30 minutes** (colonnes `tentatives_connexion` + `bloque_jusqu_a`)
- RBAC : `@PreAuthorize` sur chaque endpoint selon le rôle
- Chaque action sensible doit être enregistrée dans `journal_audit`

### Matrice des autorisations par endpoint

| Endpoint | COMPTABLE | RF | PDG | CAISSIER | ADMIN |
|---|---|---|---|---|---|
| POST /encaissements | ✅ | ❌ | ❌ | ❌ | ✅ |
| POST /decaissements | ✅ | ❌ | ❌ | ❌ | ✅ |
| PUT /decaissements/{id}/valider-rf | ❌ | ✅ | ❌ | ❌ | ✅ |
| PUT /decaissements/{id}/approuver-pdg | ❌ | ❌ | ✅ | ❌ | ✅ |
| PUT /decaissements/{id}/executer | ❌ | ❌ | ❌ | ✅ | ✅ |
| GET /tableau-bord | ✅ | ✅ | ✅ | ✅ | ✅ |
| GET /journal-audit | ❌ | ❌ | ❌ | ❌ | ✅ |
| POST /utilisateurs | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 8. STRUCTURE FRONTEND — VUE.JS 3

```
src/
├── main.js
├── App.vue
│
├── router/
│   └── index.js              ← routes protégées par rôle (navigation guards)
│
├── stores/                   ← Pinia
│   ├── auth.store.js         ← token JWT, utilisateur connecté, rôle
│   ├── encaissement.store.js
│   ├── decaissement.store.js
│   ├── facture.store.js
│   ├── tiers.store.js
│   └── tableaubord.store.js
│
├── services/                 ← appels API Axios (1 fichier par domaine)
│   ├── api.js                ← instance Axios avec intercepteur JWT
│   ├── auth.service.js
│   ├── encaissement.service.js
│   ├── decaissement.service.js
│   ├── facture.service.js
│   ├── tiers.service.js
│   └── tableaubord.service.js
│
├── views/                    ← pages principales (1 par grande fonctionnalité)
│   ├── LoginView.vue
│   ├── DashboardView.vue
│   ├── encaissement/
│   │   ├── EncaissementListView.vue
│   │   └── EncaissementFormView.vue
│   ├── decaissement/
│   │   ├── DecaissementListView.vue
│   │   ├── DecaissementFormView.vue
│   │   └── DecaissementDetailView.vue  ← workflow boutons selon rôle
│   ├── facture/
│   │   ├── FactureListView.vue
│   │   └── FactureFormView.vue
│   ├── tiers/
│   │   ├── TiersListView.vue
│   │   └── TiersFormView.vue
│   └── admin/
│       ├── UtilisateurListView.vue
│       └── JournalAuditView.vue
│
└── components/               ← composants réutilisables
    ├── layout/
    │   ├── AppSidebar.vue    ← menu selon rôle connecté
    │   ├── AppHeader.vue
    │   └── AppFooter.vue
    ├── common/
    │   ├── AppTable.vue
    │   ├── AppModal.vue
    │   ├── AppBadgeStatut.vue  ← badge coloré selon statut
    │   └── AppAlert.vue
    └── workflow/
        ├── BoutonValiderRF.vue    ← visible seulement si rôle = RF
        ├── BoutonApprouverPDG.vue ← visible seulement si rôle = PDG
        └── BoutonExecCaissier.vue ← visible seulement si rôle = CAISSIER
```

---

## 9. CONVENTIONS DE CODE

### Backend
- **Nommage** : camelCase pour variables/méthodes, PascalCase pour classes
- **DTOs** : toujours utiliser des DTOs entre controller ↔ service (jamais les entités JPA directement)
- **Transactions** : `@Transactional` sur les méthodes de service qui modifient la BD
- **Audit** : chaque méthode de service qui crée/modifie/valide une entité doit appeler `IJournalAuditService.enregistrer(...)`
- **Validation** : utiliser `@Valid` + annotations Bean Validation sur les DTOs Request (`@NotNull`, `@NotBlank`, `@Positive`, etc.)

### Frontend
- **Composants** : `<script setup>` — Composition API uniquement (pas Options API)
- **Axios** : toujours passer par `api.js` (intercepteur qui ajoute le token JWT automatiquement)
- **Gestion erreurs** : intercepteur Axios global qui catch 401 (redirection login) et 403 (message accès refusé)
- **Navigation guards** : vérifier le rôle avant chaque route protégée

---

## 10. DONNÉES DE TEST (utilisateurs par défaut)

| Email | Mot de passe | Rôle |
|-------|-------------|------|
| admin@corefi.cm | Password123! | ADMINISTRATEUR |
| j.mbarga@corefi.cm | Password123! | COMPTABLE |
| m.nguema@corefi.cm | Password123! | COMPTABLE |
| p.nkomo@corefi.cm | Password123! | RESPONSABLE_FINANCIER |
| s.biya@corefi.cm | Password123! | PDG |
| c.eto@corefi.cm | Password123! | CAISSIER |

---

## 11. ENDPOINTS REST PRINCIPAUX

```
POST   /api/auth/login
POST   /api/auth/refresh

GET    /api/utilisateurs
POST   /api/utilisateurs
PUT    /api/utilisateurs/{id}

GET    /api/tiers
POST   /api/tiers
GET    /api/tiers/{id}
PUT    /api/tiers/{id}

GET    /api/factures
POST   /api/factures
GET    /api/factures/{id}
PUT    /api/factures/{id}/valider

GET    /api/encaissements
POST   /api/encaissements
GET    /api/encaissements/{id}

GET    /api/decaissements
POST   /api/decaissements
GET    /api/decaissements/{id}
PUT    /api/decaissements/{id}/soumettre        ← COMPTABLE : BROUILLON → EN_ATTENTE
PUT    /api/decaissements/{id}/valider-rf       ← RF : EN_ATTENTE → VALIDEE_RF ou EN_ATTENTE_PDG
PUT    /api/decaissements/{id}/approuver-pdg    ← PDG : EN_ATTENTE_PDG → VALIDEE_PDG
PUT    /api/decaissements/{id}/executer         ← CAISSIER : VALIDEE_RF|VALIDEE_PDG → EXECUTEE
PUT    /api/decaissements/{id}/rejeter-rf       ← RF : EN_ATTENTE → REJETEE_RF
PUT    /api/decaissements/{id}/rejeter-pdg      ← PDG : EN_ATTENTE_PDG → REJETEE_PDG

GET    /api/comptes-financiers
GET    /api/tableau-bord/kpis
GET    /api/journal-audit

GET    /api/parametrage
PUT    /api/parametrage/{cle}
```

---

## 12. INSTRUCTIONS SPÉCIALES POUR L'IA

> Si tu génères du code pour ce projet, respecte impérativement ces règles :

1. **Controllers → Interfaces uniquement** : `IDecaissementService`, jamais `DecaissementServiceImpl`
2. **Statuts décaissement** : utiliser l'enum `StatutDecaissement` — ne jamais comparer des Strings
3. **Seuil PDG** : toujours lire depuis `ParametrageRepository` (clé `SEUIL_APPROBATION_PDG`), jamais hardcoder `500000`
4. **Audit** : après chaque création/validation/rejet/exécution → appel `journalAuditService.enregistrer(...)`
5. **Séparation des tâches** : vérifier que `saisi_par ≠ valide_par ≠ approuve_par_pdg ≠ execute_par`
6. **DTOs** : ne jamais retourner une entité JPA directement depuis un controller
7. **Transactions** : `@Transactional` sur toutes les méthodes de service qui modifient des données
8. **Frontend** : Composition API (`<script setup>`) — jamais Options API
9. **Profil actif** : en développement local XAMPP → profil `dev` (`-Dspring.profiles.active=dev`)
10. **TVA** : 19,25 % (valeur par défaut dans `LigneFacture`)
