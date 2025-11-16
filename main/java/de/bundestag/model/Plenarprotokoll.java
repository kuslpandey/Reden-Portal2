package de.bundestag.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

 //repräsentiert ein Plenarprotokoll zb Bundestagssitzung
 //diese Klasse implementiert IEntity um in Neo4j eindeutig identifiziert zu werden

public class Plenarprotokoll implements IEntity {

    //attribute (basierend auf DTD)
    private final String id;
    private final int wahlperiode;
    private final int sitzungsNr;
    private final LocalDate sitzungDatum;
    private final LocalTime sitzungStartUhrzeit;
    private final LocalTime sitzungEndeUhrzeit;
    private final String sitzungOrt; // DTD: FIXED "Berlin"

    //assoziationen
    private final Sitzung sitzung; //Verweis auf Sitzungsdaten
    private final List<Rede> alleReden; //liste aller Reden in der Sitzung


     // Konstruktor für vollständige Initialisierung
    public Plenarprotokoll(int wahlperiode,
                           int sitzungsNr,
                           LocalDate datum,
                           LocalTime start,
                           LocalTime ende,
                           Sitzung sitzung,
                           List<Rede> reden) {

        this.wahlperiode = wahlperiode;
        this.sitzungsNr = sitzungsNr;
        this.sitzungDatum = datum;
        this.sitzungStartUhrzeit = start;
        this.sitzungEndeUhrzeit = ende;
        this.sitzungOrt = "Berlin"; // FIXED laut DTD
        this.id = wahlperiode + "-" + sitzungsNr; // Eindeutige ID

        this.sitzung = sitzung;
        this.alleReden = (reden != null) ? reden : new ArrayList<>();
    }

    //IEntity Methode
    @Override
    public String getId() {
        return id;
    }

    //Getter
    public int getWahlperiode() {
        return wahlperiode;
    }

    public int getSitzungsNr() {
        return sitzungsNr;
    }

    public LocalDate getSitzungDatum() {
        return sitzungDatum;
    }

    public LocalTime getSitzungStartUhrzeit() {
        return sitzungStartUhrzeit;
    }

    public LocalTime getSitzungEndeUhrzeit() {
        return sitzungEndeUhrzeit;
    }

    public String getSitzungOrt() {
        return sitzungOrt;
    }

    public Sitzung getSitzung() {
        return sitzung;
    }

    public List<Rede> getAlleReden() {
        return alleReden;
    }


     //string Repräsentation für Debugging.

    @Override
    public String toString() {
        return "Plenarprotokoll{" +
                "ID='" + getId() + '\'' +
                ", WP=" + wahlperiode +
                ", Sitzung=" + sitzungsNr +
                ", Datum=" + sitzungDatum +
                ", Redenanzahl=" + alleReden.size() +
                '}';
    }


     //JSON Repräsentation von Objekten

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId());
        json.put("wahlperiode", this.wahlperiode);
        json.put("sitzungs_nr", this.sitzungsNr);
        json.put("datum", (this.sitzungDatum != null) ? this.sitzungDatum.toString() : null);
        json.put("start_zeit", (this.sitzungStartUhrzeit != null) ? this.sitzungStartUhrzeit.toString() : null);
        json.put("ende_zeit", (this.sitzungEndeUhrzeit != null) ? this.sitzungEndeUhrzeit.toString() : null);
        json.put("ort", this.sitzungOrt);

        //Assoziationen nur IDs der assoziierten Entitäten speichern
        if (this.sitzung instanceof IEntity) {
            json.put("sitzung_id", ((IEntity) this.sitzung).getId());
        }

        json.put("reden_anzahl", this.alleReden.size());
        return json;
    }


     //PERFORMANTE METHODEe rzeugt den parametrisierten Cypher befehl für den Protokoll knoten und relationen.
     //implementiert die Methode aus IEntity.

    @Override
    public CypherQuery toParameterizedNode() {

        //definiere den CypherQuery mit Platzhaltern/$Parameter
        String cypher = "MERGE (p:Protokoll {id: $id}) " +
                "ON CREATE SET p.wahlperiode = $wahlperiode, " +
                "p.sitzungsNr = $sitzungsNr, " +
                "p.datum = $datum, " +
                "p.start = $start, " +
                "p.ende = $ende, " +
                "p.ort = $ort " +
                "ON MATCH SET p.ende = $ende"; //endezeit bei match aktualisieren

        //erstelle die ParameterMap
        Map<String, Object> params = new HashMap<>();
        params.put("id", this.getId());
        params.put("wahlperiode", this.wahlperiode);
        params.put("sitzungsNr", this.sitzungsNr);

        //Datums/Zeitwerte als String übergeben
        params.put("datum", (this.sitzungDatum != null) ? this.sitzungDatum.toString() : "NULL");
        params.put("start", (this.sitzungStartUhrzeit != null) ? this.sitzungStartUhrzeit.toString() : "NULL");
        params.put("ende", (this.sitzungEndeUhrzeit != null) ? this.sitzungEndeUhrzeit.toString() : "NULL");
        params.put("ort", this.sitzungOrt);


        //relation zur Sitzung hinzufügen falls vorhanden
        if (this.sitzung instanceof IEntity) {
            String sitzungId = ((IEntity) this.sitzung).getId();

            cypher += " WITH p " +
                    "MATCH (s:Sitzung {id: $sitzungId}) " +
                    "MERGE (p)-[:ENTHAELT_SITZUNG]->(s)";

            params.put("sitzungId", sitzungId);
        }

        return new CypherQuery(cypher, params);
    }


     //veraltet erzeugt den Cypher merge Befehl für den Knoten und Relationen.

    public String toNode() {
        // Die Logik ist veraltet und unsicher. Wir geben einen leeren String zurück.
        return "";
    }
}