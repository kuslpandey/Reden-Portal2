package de.bundestag.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; // NEU: Für Parameter-Map
import java.util.List;
import java.util.Map;   // NEU: Für Parameter-Map
import org.json.JSONObject;

// HINWEIS: Alle Modell-Abhängigkeiten im selben Paket müssen nicht explizit importiert werden (außerhalb des Codes).
// Wir stellen sicher, dass alle benötigten Klassen (CypherQuery, IEntity) referenziert werden können.


public class Rede implements IEntity {

    // Einfache Variablen für die Daten
    private LocalDate datum;
    private String id;
    private String ueberschrift;
    private Abgeordneter rednerPerson;
    private String textInhalt;
    private Sitzung sitzung;

    // NEU: Liste für die Kommentare (zur Erfüllung der Aufgabenstellung)
    private final List<Kommentar> kommentare;

    /**
     * Macht ein neues Rede-Objekt.
     */
    public Rede(String id, LocalDate datum, String ueberschrift, Abgeordneter rednerPerson, String textInhalt) {
        this.id = id;
        this.datum = datum;
        this.ueberschrift = ueberschrift;
        this.rednerPerson = rednerPerson;
        this.textInhalt = textInhalt;
        this.sitzung = null;
        this.kommentare = new ArrayList<>();
    }

    // --- Setter/Getter ---
    public void setSitzung(Sitzung s) {
        this.sitzung = s;
    }

    public Sitzung getSitzung() {
        return sitzung;
    }

    // --- NEUE Methoden zur Verwaltung der Kommentare ---
    public void addKommentar(Kommentar k) {
        if (k != null) {
            this.kommentare.add(k);
        }
    }

    public List<Kommentar> getKommentare() {
        return Collections.unmodifiableList(this.kommentare);
    }

    // --- Methoden zum Holen der Daten ---
    public String getText() {
        return textInhalt;
    }

    public String getRednername() {
        if (rednerPerson != null) {
            return rednerPerson.getName();
        }
        return "Unbekannt";
    }

    public int getTextLength() {
        if (this.textInhalt != null) {
            return this.textInhalt.length();
        }
        return 0;
    }

    // --- IEntity Methoden ---
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "RedeEintrag{" +
                "Nummer=" + getId() +
                ", Titel=" + ueberschrift +
                ", Datum=" + datum +
                ", Redner=" + getRednername() +
                ", Kommentare=" + kommentare.size() +
                '}';
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id_nummer", this.id);
        json.put("datum_tag", (this.datum != null) ? this.datum.toString() : null);
        json.put("titel_text", this.ueberschrift);
        json.put("text_zeichen_anzahl", this.getTextLength());
        json.put("kommentare_anzahl", this.kommentare.size());

        if (this.rednerPerson != null) {
            json.put("redner_person_id", this.rednerPerson.getId());
        }
        return json;
    }

    /**
     * Erzeugt den parametrisierten Code für die Neo4j Datenbank (PERFORMANCE-VERBESSERUNG).
     * Implementiert die Methode aus IEntity.
     */
    @Override
    public CypherQuery toParameterizedNode() {

        // 1. Definiere den Cypher-Query mit Platzhaltern ($Parameter)
        String cypher = "MERGE (r:Rede {id: $id}) " +
                "ON CREATE SET r.ueberschrift = $ueberschrift, r.datum = $datum, r.text = $text, r.kommentar_anzahl = $kommentarAnzahl " +
                "ON MATCH SET r.kommentar_anzahl = $kommentarAnzahl "; // Aktualisiert den Zähler immer

        // 2. Erstelle die Parameter-Map
        Map<String, Object> params = new HashMap<>();
        params.put("id", this.getId());
        params.put("ueberschrift", this.ueberschrift);
        params.put("datum", (datum != null) ? datum.toString() : "KEINDATUM");
        params.put("text", this.textInhalt); // KEINE Escapierung mehr nötig!
        params.put("kommentarAnzahl", this.kommentare.size());

        // 3. Füge die Relationen hinzu

        // Verbindung zum Abgeordneten-Knoten
        if (this.rednerPerson != null) {
            cypher += " WITH r " +
                    "MATCH (p:Abgeordneter {id: $rednerId}) " +
                    "MERGE (r)-[:WURDE_GEHALTEN_VON]->(p)";
            params.put("rednerId", this.rednerPerson.getId());
        }

        // Verbindung zur Sitzung
        if (this.sitzung != null) {
            cypher += " WITH r " +
                    "MATCH (s:Sitzung {id: $sitzungId}) " +
                    "MERGE (r)-[:GEHORTE_ZU_SITZUNG]->(s)";
            params.put("sitzungId", this.sitzung.getId());
        }

        return new CypherQuery(cypher, params);
    }

    /**
     * Veraltet: Implementierung für die alte IEntity-Signatur.
     * Muss vorhanden sein, falls IEntity nicht bereinigt wurde.
     * WICHTIG: Die @Override Annotation muss entfernt werden, wenn IEntity toNode() nicht mehr deklariert!
     */
    public String toNode() {
        // Diese Methode wird nur beibehalten, wenn die IEntity-Schnittstelle noch nicht bereinigt wurde.
        // Sie sollte NICHT mehr verwendet werden.
        return "";
    }
}