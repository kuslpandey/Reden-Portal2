package de.bundestag.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

//die klasse kommentar implementiert das interface ientity und repräsentiert
//einen kommentar, der auf eine bestimmte rede bezieht
public class Kommentar implements IEntity {

    //die eindeutige Erkennung (id) des kommentars final, da sie nach erstellung nicht geändert werden soll
    private final String id;

    //der name des autors des kommentars
    private String autor;
    //der textinhalt des kommentars
    private String text;
    //das Datum der erstellung des Kommentars
    private LocalDate datum;
    //ein verweis auf das Redeobjekt,zu dem dieser kommentar gehört
    private Rede rede;

    //konstruktor zur vollen initialisierung eines kommentarobjekts
    public Kommentar(String id, String autor, String text, LocalDate datum, Rede rede) {
        this.id = id;
        this.autor = autor;
        this.text = text;
        this.datum = datum;
        this.rede = rede;
    }

    //methoden von ientity


      //gibt die eindeutige id des kommentars zurück.
     //return die id.

    @Override
    public String getId() {
        return this.id;
    }

    //holt die Wertegetter

    //gibt den textinhalt des Kommentars zurück
    public String getText() {
        return text;
    }

    //gibt den namen des autors zurück
    public String getAutor() {
        return autor;
    }

    //gibt das Erstellungsdatum zurück
    public LocalDate getDatum() {
        return datum;
    }

    //gibt die zugehörige rede zurück
    public Rede getRede() {
        return rede;
    }


     //erzeugt eine string repräsentation des kommentars für debugging zwecke oder protokoll.
     //return eine string repräsentation mit den wichtigsten Daten

    @Override
    public String toString() {
        return "KommentarDaten{" +
                "ID=" + getId() +
                ", Autor=" + autor +
                ", Datum=" + datum +
                // zeigt nur die ersten 30 zeichen des textes an.
                ", TextStart=" + text.substring(0, Math.min(text.length(), 30)) + "..." +
                '}';
    }

     //konvertiert das kommentar-objekt in ein jsonobject.
      //return ein jsonobject mit den attributen des kommentars.

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.getId());
        json.put("AutorName", this.autor);
        json.put("Inhalt", this.text);
        // konvertiert das localdate in einen string für json oder setzt auf null.
        json.put("Tag", (this.datum != null) ? this.datum.toString() : null);

        // fügt die id der zugehörigen rede hinzu, wenn sie existiert und ientity implementiert.
        if (this.rede != null && this.rede instanceof IEntity) {
            json.put("RedeIDNummer", ((IEntity) this.rede).getId());
        }
        return json;
    }

    /**
     * erzeugt den parametrisierten cypher-code für die neo4j-datenbank.
     * verwendet $parameter statt string-konkatenation (performance und sicherheit).
     * dies ist die korrekte und performante implementierung der ientity-schnittstelle für neo4j.
     * @return ein cypherquery-objekt mit dem query-string und der parameter-map.
     */
    @Override
    public CypherQuery toParameterizedNode() {

        // 1. definiere den cypher-query mit platzhaltern ($parameter)
        // merge (k:kommentar {id: $id}) stellt sicher, dass der knoten erstellt oder gefunden wird.
        // on create set setzt die eigenschaften nur beim erstellen.
        String cypher =
                "MERGE (k:Kommentar {id: $id}) " +
                        "ON CREATE SET k.Autor = $autor, k.Inhalt = $text, k.Datum = $datum";

        // 2. erstelle die parameter-map zur übergabe der werte an die datenbank.
        Map<String, Object> params = new HashMap<>();
        params.put("id", this.getId());
        params.put("autor", this.autor);
        params.put("text", this.text);
        // konvertiert das datum in einen string oder verwendet einen default-wert.
        params.put("datum", (datum != null) ? datum.toString() : "KEINDATUM");

        // 3. füge die relation hinzu, falls eine rede existiert
        if (this.rede != null) {
            // with k übergibt den gerade gemergten kommentar-knoten an den nächsten schritt
            // match sucht die zugehörige rede.
            // merge (k)-[:ist_teil_von]->(r) erstellt die beziehung.
            cypher +=
                    " WITH k " +
                            "MATCH (r:Rede {id: $redeId}) " +
                            "MERGE (k)-[:IST_TEIL_VON]->(r)";

            //fügt die id der rede zu den parametern hinzu
            params.put("redeId", this.rede.getId());
        }

        //erstellt das Cypherquery objekt,das vom datenbankclient verwendet wird
        return new CypherQuery(cypher, params);
    }


     //veraltet implementierung für die alte ientitysignatur (toNode)
     //diese methode ist langsam stringkonkatenation und unsicher cypher injection
     //sie sollte nicht verwendet werden,toParameterizednode ist vorzuziehen

    //Override
    public String toNode() {
        //gibt einen leeren string zurück,da die methode veraltet ist
        return "";
    }
}