package de.bundestag.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * Repräsentiert den Abgeordneten (Parlamentarier) als Stammdaten-Entität.
 * Erbt von AbstractEntity und enthält die statischen Informationen.
 */
// ️ WICHTIG: Stellen Sie sicher, dass AbstractEntity die IEntity implementiert,
// oder fügen Sie 'implements IEntity' hier hinzu, falls AbstractEntity das nicht tut.
// Da der Fehler auf die fehlende Methode hinweist, muss die Methode hier implementiert werden.
public class Abgeordneter extends AbstractEntity {

    // Stammdaten
    private final String vorname;
    private final String nachname;
    private final LocalDate geburtsdatum;
    private final String beruf;
    private final String funktion;

    // Assoziation zur Fraktion (n:1)
    private Fraktion fraktion;

    // NEU: Assoziation zu Reden (1:n)
    private final List<Rede> reden = new ArrayList<>();


    /**
     * Öffentlicher Konstruktor für die PortalFactory.
     */
    public Abgeordneter(String id, String vorname, String nachname, LocalDate geburtsdatum, String beruf, String funktion) {
        super(id);
        this.vorname = vorname;
        this.nachname = nachname;
        this.geburtsdatum = geburtsdatum;
        this.beruf = beruf;
        this.funktion = funktion;
    }

    // --- Getter ---
    public String getNachname() { return nachname; }
    public String getVorname() { return vorname; }
    public String getName() { return vorname + " " + nachname; }
    public Fraktion getFraktion() { return fraktion; }
    public String getFunktion() { return funktion; }
    public LocalDate getGeburtsdatum() { return geburtsdatum; } // Hinzugefügt, falls benötigt
    public String getBeruf() { return beruf; } // Hinzugefügt, falls benötigt

    // NEU: Getter für die Liste der Reden (gibt eine unveränderliche Kopie zurück)
    public List<Rede> getReden() { return Collections.unmodifiableList(reden); }


    // --- Setter (für die Zuordnung durch die Factory) ---
    public void setFraktion(Fraktion fraktion) {
        this.fraktion = fraktion;
    }

    /**
     * NEU: Fügt eine Rede des Abgeordneten hinzu.
     * Behebt den Fehler im XMLProcessor.
     */
    public void addRede(Rede rede) {
        if (rede != null && !this.reden.contains(rede)) {
            this.reden.add(rede);
        }
    }


    // --- Persistenz (Aufgabe 3c) ---

    /**
     *  PERFORMANTE METHODE: Erzeugt den parametrisierten Cypher-Befehl für den Knoten und die Relation zur Fraktion.
     * Implementiert die Methode aus IEntity.
     */
    //@Override // @Override muss entfernt werden, wenn AbstractEntity toNode() nicht implementiert.
    public CypherQuery toParameterizedNode() {

        // 1. Definiere den Cypher-Query mit Platzhaltern ($Parameter)
        String cypher = "MERGE (a:Abgeordneter {id: $id}) " +
                "ON CREATE SET a.name = $name, a.geburtsdatum = $geburtsdatum, a.funktion = $funktion " +
                "ON MATCH SET a.funktion = $funktion"; // Aktualisiert die Funktion bei MATCH

        // 2. Erstelle die Parameter-Map
        Map<String, Object> params = new HashMap<>();
        params.put("id", this.getId());
        // KEINE String-Escapierung mehr nötig!
        params.put("name", this.getName());
        params.put("geburtsdatum", (this.geburtsdatum != null) ? this.geburtsdatum.toString() : "NULL");
        params.put("funktion", (this.funktion != null) ? this.funktion : "NULL");

        // 3. Relation zur Fraktion hinzufügen (falls vorhanden)
        if (this.fraktion != null && this.fraktion.getId() != null) {
            String fraktionId = this.fraktion.getId();

            cypher += " WITH a " +
                    "MATCH (f:Fraktion {id: $fraktionId}) " + // Fraktion muss existieren
                    "MERGE (a)-[:IST_MITGLIED_VON]->(f)";

            params.put("fraktionId", fraktionId);
        }

        return new CypherQuery(cypher, params);
    }

    /**
     * Veraltet: Erzeugt den Cypher-MERGE-Befehl für den Knoten und die Relation zur Fraktion.
     * Wird nur beibehalten, falls die alte IEntity-Signatur nicht bereinigt wurde.
     */
    //@Override // @Override muss entfernt werden, da toNode() nicht mehr in IEntity ist.
    public String toNode() {
        // Die Logik ist veraltet und unsicher. Wir geben einen leeren String zurück.
        return "";
    }

    /**
     * Erzeugt die JSON-Repräsentation des Objekts.
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId());
        json.put("name", this.getName());
        json.put("geburtsdatum", this.geburtsdatum != null ? this.geburtsdatum.toString() : "NULL");
        json.put("beruf", this.beruf);
        json.put("funktion", this.funktion);

        if (this.fraktion != null) {
            json.put("fraktion_id", this.fraktion.getId());
        }
        return json;
    }

    /**
     * String-Repräsentation für Debugging.
     */
    @Override
    public String toString() {
        return "Abgeordneter{" +
                "ID='" + getId() + '\'' +
                ", Name='" + getName() + '\'' +
                ", Fraktion='" + (fraktion != null ? fraktion.getName() : "N/A") + '\'' +
                ", Funktion='" + (funktion != null ? funktion : "N/A") + '\'' +
                ", Reden: " + reden.size() +
                '}';
    }
}