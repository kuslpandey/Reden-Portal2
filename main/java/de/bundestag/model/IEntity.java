package de.bundestag.model;

import org.json.JSONObject;

public interface IEntity {


     //stellt eine eindeutige Kennung für die Persistenz und den Abruf bereit
     //return Die ID von Entität

    String getId();

    // Neue Methode für parametrisierte Queries
    // erzeugt den parametrisierten Cypher-Befehl (Knoten und Relationen) zur Speicherung in der Neo4j-Datenbank
     //nutzt $Parameter anstelle von String-Konkatenation für Sicherheit und Performance
     //return Ein CypherQuery objekt mit dem Query-String und der Parameter Map.

    CypherQuery toParameterizedNode();

    //Für die JSONrepräsentation

     //konvertiert die Entität in ein JSON objekt
     //return Das JSONobject der Entität

    JSONObject toJSON();

    //für die Debug susgabe von 3c
    //erzeugt eine lesbare String-Repräsentation der Entität
     //return Die String repräsentation

    String toString();
}