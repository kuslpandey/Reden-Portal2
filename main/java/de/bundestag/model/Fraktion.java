package de.bundestag.model;

import org.json.JSONObject; // wird benutzt, um daten als json zu speichern
import java.util.ArrayList; // um eine liste zu machen
import java.util.Collections; // hilft, die liste zu schützen
import java.util.HashMap; // neu für die parameter der Datenbankabfrage (map)
import java.util.List;
import java.util.Map;   // neu für die parameter der datenbankabfrage (map)
//annahme die klasse Abgeordneter existiert im de.bundestag.model paket
//die Fraktion ist eine gruppe von abgeordneten wie eine Partei im bundestag

public class Fraktion extends AbstractEntity {

    //attributes private felder
    private String name; //der Name der Fraktion
    private String herkunftspartei; //von welcher partei kommt die fraktion

    //dieliste speichert alle Abgeordneten, die dazu gehören
    private final List<Abgeordneter> mitglieder;
    private int mitgliederanzahl; // wie viele mitglieder es gibt (wird gespeichert)


     //erstellen wir eine neue fraktion

    public Fraktion(String id, String name, String herkunftspartei) {
        super(id); //ruft den konstruktor der elternklasse auf
        this.name = name;
        this.herkunftspartei = herkunftspartei;
        this.mitglieder = new ArrayList<>(); //erstellt eine leere liste
        this.mitgliederanzahl = 0; //fängt mit null mitgliedern an
    }

    //methoden zur verwaltung der abgeordneten


     //fügt einen Abgeordneten der fraktion hinzu

    public void addMitglied(Abgeordneter abgeordneter) {
        //prüft ob der Abgeordnete da ist und noch nicht in der liste ist
        if (abgeordneter != null && !this.mitglieder.contains(abgeordneter)) {
            this.mitglieder.add(abgeordneter);
            //die Mitgliederanzahl wird auf die neue größe der liste gesetzt
            this.mitgliederanzahl = this.mitglieder.size();
        }
    }

    // getter um werte auszulesen

    public String getName() {
        return name;
    }

    public String getHerkunftspartei() {
        return herkunftspartei;
    }

    public int getMitgliederanzahl() {
        return mitgliederanzahl;
    }

    public List<Abgeordneter> getMitglieder() {
        //gibt die liste zurück aber man kann sie von außen nicht ändern
        return Collections.unmodifiableList(this.mitglieder);
    }

    //Implementierung der IEntity methoden für die datenbank
    //erstellt den cypherbefehl für die datenbank

    @Override
    public CypherQuery toParameterizedNode() {

        //der cypherbefehl mit platzhaltern ($id, $name, etc.)
        String cypher = "MERGE (f:Fraktion {id: $id}) " +
                "ON CREATE SET f.name = $name, f.herkunftspartei = $herkunftspartei, f.mitgliederanzahl = $mitgliederAnzahl " +
                "ON MATCH SET f.mitgliederanzahl = $mitgliederAnzahl"; // die mitgliederanzahl wird immer aktualisiert

        //eine map um die Platzhalter mit werten zu füllen
        Map<String, Object> params = new HashMap<>();
        params.put("id", this.getId());

        //die Werte werden in die map gepackt
        params.put("name", this.name);
        params.put("herkunftspartei", this.herkunftspartei);
        params.put("mitgliederAnzahl", this.mitgliederanzahl);

        return new CypherQuery(cypher, params); // gibt befehl und werte zurück
    }

     //veraltete methode wird nicht mehr benutzt

    public String toNode() {
        //gibt einfach nur einen leeren Text zurück
        return "";
    }

     //wandelt die Fraktion in ein jsonobjekt um

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId());
        json.put("name", this.name);
        json.put("herkunftspartei", this.herkunftspartei);
        json.put("mitglieder_anzahl", this.mitgliederanzahl);
        return json;
    }


     //macht einen einfachen text für die ausgabe

    @Override
    public String toString() {
        return "Fraktion{" +
                "ID='" + this.getId() + '\'' +
                ", Name='" + name + '\'' +
                ", Herkunft='" + herkunftspartei + '\'' +
                ", Mitglieder=" + mitgliederanzahl +
                '}';
    }
}