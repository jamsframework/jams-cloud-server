# jams-cloud-server → Payara 6 / Jakarta EE 10 / Maven

## Ziel
`jams-cloud-server` auf einem nackten Server reproduzierbar neu aufsetzen: aktuelles
**Payara 6** (Jakarta EE 10, Java 17), Build mit **Maven**, Deployment per **Docker**.
Der **Desktop-Client bleibt komplett unangetastet** (Weg B).

## Grundsatzentscheidungen (fix)
- **Payara 6.x** (Jakarta EE 10) auf **Java 17** — unser aktueller Floor.
- **Maven** statt Ant/NetBeans (konsistent zu jams/jamsmodels; `mvnw`-Build).
- **Weg B — Client isolieren:** Die gesamte Desktop-Seite (jams-cloud-client, -core,
  jams-main, jams-explorer) bleibt auf `javax.*`. Nur der **Server** wird `jakarta.*`.
  Die geteilten Entities (`jams-cloud-core`) werden **beim Server-Build** per
  **Eclipse Transformer** `javax→jakarta` konvertiert. Kein Client-File wird angefasst.
- **EE-Stack vom Container** (JAX-RS/Jersey, JPA/EclipseLink, CDI, EJB, Servlet,
  Bean Validation, JAXB) → Maven-Scope `provided`, nicht ins WAR gepackt.

## Empfehlungen (zur Freigabe)
- **JSF-Admin-UI entfernen.** Ist toter Code: die `web/admin/user/*.xhtml` referenzieren
  `#{userController}`, das im Quellcode nicht existiert; keine Backing-Beans, nur ein
  leeres `faces-config`. Entfällt damit die komplette Faces-4-Migration.
- **Deployment: Docker Compose** — `payara/server-full:6` + `mysql:8`. Reproduzierbar,
  kapselt Java 17/Payara/DB. (Alternative: manuelle Host-Installation + systemd.)
- **DB: MySQL 8** (Connector/J 8, `com.mysql.cj.jdbc.Driver`), **Flyway 10/11**.

## Ausgangslage (recherchiert)
- Server-Source `src/java` (~15 Klassen): `service/*REST.java`, ProcessManager-Trio,
  `Utilities`, `DBInit`, `ApplicationConfig`, `cors/`, dazu `session/`-EJBs.
- javax-Nutzung: ws.rs 58×, persistence 16×, ejb 10×, servlet.http 8×, annotation 2×.
- REST-Facades halten **eigenen** `@PersistenceContext`-EntityManager; die `session/`-EJBs
  (`UserFacade`, `FileFacade`) werden von REST **nicht** genutzt → mit JSF-UI entfernbar.
- `DBInit` fährt Flyway zur Laufzeit (alte 3.x-API: `new Flyway()`, `setInitOnMigrate`).
- Deskriptoren: `web.xml` 3.1, `persistence.xml` 2.1 (EclipseLink), `glassfish-web.xml` /
  `glassfish-resources.xml` (MySQL-Pool). **JNDI-Bug:** `persistence.xml` erwartet
  `jdbc/jamsserver__pm`, `glassfish-resources.xml` definiert `jdbc/jamsserver`.
- Jersey im alten `lib/`: 2.7. Payara 6 liefert Jersey 3.x selbst.

## Schritte

### 1. Maven-Projekt aufsetzen
- `pom.xml` (Packaging `war`, Java 17). Verzeichnisumbau:
  `src/java`→`src/main/java`, `web`→`src/main/webapp`,
  `src/conf/persistence.xml`→`src/main/resources/META-INF/persistence.xml`,
  `db/migration`→`src/main/resources/db/migration`.
- Dependencies:
  - `provided`: `jakarta.platform:jakarta.jakartaee-web-api:10.0.0` (JAX-RS, JPA, EJB,
    Servlet, CDI, Bean Validation, JAXB in einem).
  - `provided` (falls nicht im Container): `jersey-media-multipart` (jakarta) — sonst vom
    Payara bereitgestellt; im Schritt 6 verifizieren.
  - `compile` (ins WAR): `flyway-core` + `flyway-mysql` (10/11), JAMS-Framework-Jars
    `jams-api`/`jams-main` + `jna`/`platform` (Modell-Handling zur Laufzeit).
  - `runtime`: `mysql-connector-j:8` (oder als Payara-JDBC-Ressource; siehe 5).
- `mvnw`/`.mvn` aus dem jams-Repo übernehmen (gleiche Wrapper-Logik).

### 2. Weg B — geteilte Entities transformieren
- `jams-cloud-core`, `jams-api`, `jams-main` kommen als `1.0-SNAPSHOT`-Dependencies aus
  dem lokalen `~/.m2` (durch `mvnw install` im jams-Repo).
- **`org.eclipse.transformer:transformer-maven-plugin`** transformiert das
  `jams-cloud-core`-Artefakt `javax→jakarta` (persistence, xml.bind, validation) und legt
  ein `jams-cloud-core-jakarta`-Artefakt an, gegen das der Server compiliert/packt.
  Paket-/Klassennamen (`jams.server.entities.*`) bleiben gleich → Server-Source importiert
  unverändert, nur die Annotations-Pakete wandern. **jams-Repo bleibt unberührt.**

### 3. Server-Source `javax → jakarta`
- Imports in `src/main/java`: `javax.ws.rs`→`jakarta.ws.rs`, `javax.persistence`→
  `jakarta.persistence`, `javax.ejb`→`jakarta.ejb`, `javax.servlet`→`jakarta.servlet`,
  `javax.annotation`→`jakarta.annotation` (inkl. fully-qualified Referenzen in
  `AbstractFacade`, `ApplicationConfig`).
- Deskriptoren: `web.xml`→6.0 (jakarta-Namespace), `persistence.xml`→3.0 (jakarta),
  EclipseLink als Provider, MySQL8-Platform.
- **JNDI-Bug fixen**: eine konsistente `jta-data-source` (`jdbc/jamsserver`).

### 4. Tote JSF-Schicht entfernen
- Löschen: `web/admin/`, `web/index.xhtml`, `web/template.xhtml`, `web/resources/`,
  `web/WEB-INF/faces-config.xml`, Faces-Servlet aus `web.xml`, `resources/Bundle.properties`,
  Paket `src/java/jams/server/session/` (nach Ref-Check).
- Minimaler Ersatz: schlichte Landing-/Health-Seite oder ein `GET /webresources/info`.

### 5. DB + Flyway modernisieren
- `DBInit` auf Flyway-10-API umschreiben
  (`Flyway.configure().dataSource(url,user,pw).baselineOnMigrate(true).load().migrate()`).
- MySQL-8-Ressource: Connection-Pool/Datasource als `glassfish-resources.xml` (Payara liest
  es beim Deploy) **oder** `@DataSourceDefinition`; Treiber `com.mysql.cj.jdbc.Driver`.
- `settings.properties`: heute aus CWD gelesen. Auf Env-Vars/MicroProfile-Config umstellen
  (Docker-freundlich) oder definiert in den Payara-Domain-Ordner legen.

### 6. Payara-6-Packaging & Docker-Deploy
- `mvnw package` → `target/jams-cloud-server.war`.
- `docker-compose.yml`: `payara/server-full:6` + `mysql:8`; WAR per Autodeploy/asadmin,
  MySQL-Treiber + JDBC-Ressource via Payara-Postboot-Commands; Volumes für
  upload/tmp/exec + `settings`/Env.
- Verifizieren: Container hoch, REST-Endpoints (`/jamscloud/webresources/...`) antworten,
  Persistenz gegen MySQL, Flyway-Migration läuft.

### 7. Verifikation & Commit
- End-to-End: Login/User, File-Upload, Job-Anlage gegen den containerisierten Server.
  (Voller Modell-Lauf braucht Java-17-Modellausführung — kommt durch den Java-17-Container
  automatisch, `command[0]="java"`.)
- Knapper englischer Commit.

## Blast-Radius Client
**Null Client-Files.** Nur `jams-cloud-server` + ein Transformer-Schritt gegen das
`jams-cloud-core`-Artefakt. jams/jamsmodels-Repos werden nicht verändert.

## Nicht in dieser Phase
- `command[0]` in Linux/Win64ProcessManager konfigurierbar (Env/Settings) — optional,
  da der Container-`java` bereits Java 17 ist.
- Push nach GitHub (`jamsframework`) nach Abnahme.
- Härtung: Secrets aus `settings.properties` raus, HTTPS/Reverse-Proxy, DB-Backups.
