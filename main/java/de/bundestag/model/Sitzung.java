package de.bundestag.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;


 //repräsentiert eine Plenarsitzung des Bundestages
 //implementiert IEntity zur IDVerwaltung

public class Sitzung implements IEntity {

    private final String id;

    //Attributes
    private LocalDate datum;
    private LocalTime zeit;
    private String raumnummer;
    private String zugang;

    //Assoziation zur Speicherung der Reden für die Factory Logik
    private final List<Rede> alleReden;


     //konstruktor akzeptiert die eindeutige ID

    public Sitzung(String id, LocalDate datum, LocalTime zeit, String raumnummer, String zugang) {
        this.id = id;
        this.datum = datum;
        this.zeit = zeit;
        this.raumnummer = raumnummer;
        this.zugang = zugang;
        this.alleReden = new ArrayList<>();
    }

    //methoden für die Assoziation


     //fügt eine Rede zu dieser Sitzung hinzu

    public void addRede(Rede rede) {
        if (rede != null && !this.alleReden.contains(rede)) {
            this.alleReden.add(rede);
        }
    }

    //optionalGetter für die Redenliste
    public List<Rede> getAlleReden() {
        return alleReden;
    }

    //IEntity Methode
    @Override
    public String getId() {
        return this.id;
    }

    //Getter
    public LocalDate getDatum() { return datum; }
    public LocalTime getZeit() { return zeit; }
    public String getRaumnummer() { return raumnummer; }
    public String getZugang() { return zugang; }


     //PERFORMANTE METHODE erzeugt den parametrisierten Cypherbefehl für den Sitzung Knoten.
     //implementiert die Methode aus IEntity

    @Override
    public CypherQuery toParameterizedNode() {

        //definiere den Cypherquery mit Platzhaltern $Parameter
        String cypher = "MERGE (s:Sitzung {id: $id}) " +
                "ON CREATE SET s.datum = $datum, s.zeit = $zeit, s.raumnummer = $raumnummer, s.zugang = $zugang " +
                "ON MATCH SET s.zugang = $zugang"; //aktualisiert den Zugang bei MATCH

        //erstelle die Parameter Map
        Map<String, Object> params = new HashMap<>();
        params.put("id", this.getId());

        //keine Escapierung oder String Konkatenation mehr nötig
        params.put("datum", (datum != null) ? datum.toString() : "NULL");
        params.put("zeit", (zeit != null) ? zeit.toString() : "NULL");
        params.put("raumnummer", this.raumnummer);
        params.put("zugang", this.zugang);

        //relationen (Sitzung hat typischerweise keine ausgehenden Relationen, nur eingehende von Reden)

        return new CypherQuery(cypher, params);
    }

    //toNode Methode
    //d IEntity.toNode() entfernt/ersetzt wurde, muss @Override hier entfernt werden
    //oder die Methode ganz gelöscht werden. Ich behalte sie ohne @Override

     //Implementierung für die alte IEntitysignatur

    public String toNode() {
        // Die Logik ist veraltet und unsicher. Wir geben einen leeren String zurück.
        return "";
    }

    //Aufgabe 3c Methoden
    @Override
    public String toString() {
        return "Sitzung{" +
                "ID='" + getId() + '\'' +
                ", Datum=" + datum +
                ", Zeit=" + zeit +
                ", Raum='" + raumnummer + '\'' +
                '}';
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId());
        json.put("datum", (this.datum != null) ? this.datum.toString() : "NULL");
        json.put("zeit", (this.zeit != null) ? this.zeit.toString() : "NULL");
        json.put("raumnummer", this.raumnummer);
        json.put("zugang", this.zugang);
        return json;
    }
}