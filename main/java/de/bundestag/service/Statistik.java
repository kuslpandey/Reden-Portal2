package de.bundestag.service;

import de.bundestag.factory.PortalFactory;
import de.bundestag.database.Neo4jConnection;

import java.util.List;
import java.util.Map;

//die klasse statistik enthält methoden zur berechnung und ausgabe von statistiken
// basierend auf den daten die in der neo4j datenbank gespeichert sind
public class Statistik {

    //verbindung zur datenbank
    private final Neo4jConnection dbConn; // referenz auf die datenbankverbindung

    //konstruktor: speichert die datenbankverbindung
    public Statistik(PortalFactory factory) {
        this.dbConn = factory.getDbConnection(); // holt die verbindung über die factory
    }

    //Datenzählungen aus der datenbank abrufen
     //ruft die anzahl der knoten für alle labels aus der datenbank ab

    public void printDataCounts() {
        System.out.println("\n--- datenzählung (aus neo4j) ---"); //überschrift ausgeben

        //map der labels,die gezählt werden sollen
        Map<String, String> labels = Map.of(
                "Abgeordnete", "Abgeordneter", // anzeige name -> knoten label
                "Fraktionen", "Fraktion",
                "Sitzungen", "Sitzung",
                "Reden", "Rede",
                "Kommentare", "Kommentar"
        );

        //durchläuft jedes label in der map
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            String bezeichnung = entry.getKey(); // der anzeige name
            String label = entry.getValue(); //der tatsächliche neo4j labelname

            // cypher abfrage zum zählen der knoten mit dem aktuellen label
            String query = String.format("MATCH (n:%s) RETURN count(n) AS Count", label);
            // führt die leseabfrage aus
            List<Map<String, Object>> result = dbConn.executeReadQuery(query);

            if (!result.isEmpty()) { // prüft, ob ergebnisse vorhanden sind
                // extrahiert die anzahl/count aus dem Ergebnis
                long count = (long) result.get(0).get("Count");
                //gibt das ergebnis formatiert aus
                System.out.printf("anzahl %-12s: %d\n", bezeichnung, count);
            }
        }
    }

     //aufgabe 4a ermittelt die durchschnittliche redelänge pro jedem Abgeordneten

    public void redeLaengeProPerson() {
        System.out.println("\n 4a durchschnittliche redelänge pro jedem abgeordneten");

        //cypherabfrage: holt den durchschnitt der redelänge/zeichen pro abgeordneten
        //limit 20 entfernt,um alle abgeordneten anzuzeigen
        String query = """
            MATCH (r:Rede)-[:WURDE_GEHALTEN_VON]->(a:Abgeordneter)
            // filtert reden ohne text oder mit leerem text und abgeordnete ohne namen
            WHERE r.text IS NOT NULL AND size(r.text) > 0 AND a.name IS NOT NULL
            RETURN 
                a.name AS Name, // name des abgeordneten
                a.id AS AbgeordnetenId, // id des abgeordneten
                avg(size(r.text)) AS AvgLaenge, // berechnet die durchschnittliche länge des redetextes
                count(r) AS CountReden // zählt die anzahl der gehaltenen reden
            ORDER BY AvgLaenge DESC // sortiert nach durchschnittlicher länge absteigend
        """;

        //führt die leseabfrage aus
        List<Map<String, Object>> results = dbConn.executeReadQuery(query);

        //gibt die tabellen header aus
        System.out.printf("%-25s ! %18s ! %s\n", "name", "durchschnitt (zch.)", "anzahl");
        System.out.println("--------------------------!---------------------!---------");

        if (results.isEmpty()) { //prüft,ob ergebnisse leer sind
            System.out.println("keine ergebnisse gefunden");
            return;
        }

        for (Map<String, Object> record : results) { //iteriert über ergebnisse
            String name = (String) record.get("Name");
            // konvertiert die nummer (long oder double von neo4j) in double
            double avg = ((Number) record.get("AvgLaenge")).doubleValue();
            long count = (long) record.get("CountReden");
            //gibt die daten formatiert aus
            System.out.printf("%-25s ! %18.2f ! %6d\n", name, avg, count);
        }
    }


    public void redeLaengeProFraktion() {
        System.out.println("\n 4a durchschnittliche redelänge pro fraktion ");

        // cypherabfrage: holt den durchschnitt der redeläng/ezeichen pro fraktion
        String query = """
            MATCH (r:Rede)-[:WURDE_GEHALTEN_VON]->(a:Abgeordneter)-[:IST_MITGLIED_VON]->(f:Fraktion) // matcht rede -> abgeordneter -> fraktion
            WHERE r.text IS NOT NULL AND size(r.text) > 0 // filtert nach vorhandenem redetext
            RETURN 
                f.name AS Fraktion, // name der fraktion
                avg(size(r.text)) AS AvgLaenge, // durchschnittliche redelänge
                count(r) AS CountReden // gesamtzahl der reden der fraktion
            ORDER BY AvgLaenge DESC // sortiert nach durchschnittlicher länge
        """;

        //führt die leseabfrage aus
        List<Map<String, Object>> results = dbConn.executeReadQuery(query);

        //gibt die tabellen header aus
        System.out.printf("%-15s ! %18s ! %s\n", "fraktion", "durchschnitt (zch.)", "anzahl");
        System.out.println("----------------!---------------------!---------");

        if (results.isEmpty()) { //prüft ob ergebnisse leer sind
            System.out.println("keine ergebnisse gefunden");
            return;
        }

        for (Map<String, Object> record : results) { // iteriert über ergebnisse
            String fraktion = (String) record.get("Fraktion");
            // konvertiert die nummer in double
            double avg = ((Number) record.get("AvgLaenge")).doubleValue();
            long count = (long) record.get("CountReden");
            // gibt die daten formatiert aus
            System.out.printf("%-15s ! %18.2f ! %6d\n", fraktion, avg, count);
        }
    }



     //aufgabe 4b  ermittelt die durchschnittliche kommentar-häufigkeit pro rede, gruppiert nach jedem abgeordneten

    public void kommentarHaeufigkeitProAbgeordneten() {
        System.out.println("\n4b kommentar häufigkeit pro jedem abgeordneten (durchschnitt pro rede)");

        //berechnet count(k) * 1.0 / count(r) für den durchschnitt
        String cypher = """
            MATCH (a:Abgeordneter)<-[:WURDE_GEHALTEN_VON]-(r:Rede) // matcht abgeordneter <- rede
            OPTIONAL MATCH (k:Kommentar)-[:IST_TEIL_VON]->(r) // optional matcht kommentar -> rede
            WHERE a.name IS NOT NULL // filtert abgeordnete mit namen
            RETURN 
                a.name AS Name, 
                COUNT(r) AS RedeAnzahl, // gesamtzahl der reden pro abgeordneten
                // berechnung des durchschnitts: gesamtkommentare / gesamtzahl reden
                COUNT(k) * 1.0 / COUNT(r) AS AvgKommentareProRede // *1.0 erzwingt double-division
            ORDER BY AvgKommentareProRede DESC // sortiert nach kommentar-durchschnitt
        """;

        try {
            List<Map<String, Object>> result = dbConn.executeReadQuery(cypher); //führt die abfrage aus

            if (result.isEmpty()) { // prüft, ob ergebnisse leer sind
                System.out.println("keine daten zur kommentar häufigkeit pro abgeordneten gefunden");
                return;
            }

            //zeigt den durchschnitt und die redenanzahl
            System.out.printf("%-10s ! %-25s ! %s\n", "avg. kom.", "name", "anzahl reden");
            System.out.println("-----------!---------------------------!--------------");

            for (Map<String, Object> row : result) { // iteriert über ergebnisse
                String name = (String) row.get("Name");
                Double avg = ((Number) row.get("AvgKommentareProRede")).doubleValue(); //durchschnitt
                Long count = (Long) row.get("RedeAnzahl"); //Anzahl der reden

                // gibt die daten formatiert aus
                System.out.printf("%-10.2f ! %-25s ! %12d\n", avg, name, count);
            }
        } catch (Exception e) {
            System.err.println("fehler bei abfrage der kommentarhäufigkeit pro abgeordneten: " + e.getMessage());
        }
    }

     //aufgabe 4b ermittelt die durchschnittliche kommentar häufigkeit pro rede, gruppiert nach jeder fraktion

    public void kommentarHaeufigkeitProFraktion() {
        System.out.println("\n--- 4(b) kommentar-häufigkeit pro jede fraktion (durchschnitt pro rede) ---");

        // berechnet count(k) * 1.0 / count(r) für den durchschnitt
        String cypher = """
            MATCH (f:Fraktion)<-[:IST_MITGLIED_VON]-(a:Abgeordneter)<-[:WURDE_GEHALTEN_VON]-(r:Rede) // matcht fraktion <- abgeordneter <- rede
            OPTIONAL MATCH (k:Kommentar)-[:IST_TEIL_VON]->(r) // optional matcht kommentar -> rede
            RETURN 
                f.name AS FraktionName, // name der fraktion
                COUNT(r) AS RedeAnzahl, // gesamtzahl der reden pro fraktion
                // berechnung des durchschnitts: gesamtkommentare / gesamtzahl reden
                COUNT(k) * 1.0 / COUNT(r) AS AvgKommentareProRede // *1.0 erzwingt double-division
            ORDER BY AvgKommentareProRede DESC // sortiert nach kommentar-durchschnitt
        """;

        try {
            List<Map<String, Object>> result = dbConn.executeReadQuery(cypher); //führt die abfrage aus

            if (result.isEmpty()) { //prüft, ob ergebnisse leer sind
                System.out.println("keine daten zur kommentar-häufigkeit pro fraktion gefunden");
                return;
            }

            //zeigt den durchschnitt und die redenanzahl
            System.out.printf("%-10s ! %-15s ! %s\n", "avg. kom.", "fraktion", "anzahl reden");
            System.out.println("-----------!-----------------!--------------");

            for (Map<String, Object> row : result) { //iteriert über ergebnisse
                String name = (String) row.get("FraktionName"); // fraktionsname
                Double avg = ((Number) row.get("AvgKommentareProRede")).doubleValue(); // durchschnitt
                Long count = (Long) row.get("RedeAnzahl"); //Anzahl der reden

                //gibt die daten formatiert aus
                System.out.printf("%-10.2f ! %-15s ! %12d\n", avg, name, count);
            }
        } catch (Exception e) {
            System.err.println("fehler bei der abfrage der kommentarhäufigkeit pro fraktion: " + e.getMessage());
        }
    }



     //ermittelt die längste sitzung bezüglich der Zeit

    public String getLaengsteSitzungNachZeitId() {
        System.out.println("\n 4(c) ermittle längste sitzung (nach zeit/redenanzahl)");

        // korrektur: nutzt die korrekte relation (r:rede)-[:gehorte_zu_sitzung]->(s)
        String cypher = """
            MATCH (r:Rede)-[:GEHORTE_ZU_SITZUNG]->(s:Sitzung) // matcht rede -> sitzung
            RETURN 
                s.id AS SitzungsId, // gibt die sitzungs-id zurück
                s.datum AS Datum, // gibt das sitzungsdatum zurück
                COUNT(r) AS RedeAnzahl // zählt die reden pro sitzung
            ORDER BY RedeAnzahl DESC // sortiert nach der anzahl der reden (als proxy für zeit)
            LIMIT 1 // nur die längste sitzung
        """;

        List<Map<String, Object>> result = dbConn.executeReadQuery(cypher); //führt die abfrage aus

        if (result.isEmpty()) { // prüft ob Ergebnisse leer sind
            System.out.println("keine sitzungen gefunden");
            return null;
        }

        Map<String, Object> longestSession = result.get(0); // das Ergebnis der längsten sitzung
        String sitzungsId = (String) longestSession.get("SitzungsId"); // die id

        //gibt das ergebnis formatiert aus
        System.out.printf("➡ längste sitzung (nach redenanzahl): id=%s, datum=%s, reden=%d\n",
                sitzungsId, longestSession.get("Datum"), longestSession.get("RedeAnzahl"));

        return sitzungsId; //gibt die id zurück
    }

     //ermittelt die längste sitzung bezüglich der gesamtlänge aller reden

    public String getLaengsteSitzungNachGesamtRedelaengeId() {
        System.out.println("\n 4(c) ermittle längste sitzung (nach gesamt redelänge) ");


        String cypher = """
            MATCH (r:Rede)-[:GEHORTE_ZU_SITZUNG]->(s:Sitzung) // matcht rede -> sitzung
            WHERE r.text IS NOT NULL AND size(r.text) > 0 // nur reden mit text
            RETURN 
                s.id AS SitzungsId, // gibt die sitzungs-id zurück
                s.datum AS Datum, // gibt das sitzungsdatum zurück
                SUM(size(r.text)) AS GesamtLaenge // summiert die länge aller redetexte
            ORDER BY GesamtLaenge DESC // sortiert nach gesamtlänge
            LIMIT 1 // nur die längste sitzung
        """;

        List<Map<String, Object>> result = dbConn.executeReadQuery(cypher); //führt die abfrage aus

        if (result.isEmpty()) { //prüft ob ergebnisse leer sind
            System.out.println("keine sitzungen mit reden gefunden");
            return null;
        }

        Map<String, Object> longestSession = result.get(0); //das Ergebnis der längsten sitzung
        String sitzungsId = (String) longestSession.get("SitzungsId"); //die id
        // konvertiert die nummer in double
        Double gesamtLaenge = ((Number) longestSession.get("GesamtLaenge")).doubleValue();

        //gibt das ergebnis formatiert aus
        System.out.printf("️ längste sitzung (nach gesamt redelänge): id=%s, datum=%s, länge=%.2f zeichen\n",
                sitzungsId, longestSession.get("Datum"), gesamtLaenge);

        return sitzungsId; //gibt die id zurück
    }


     // aufgabe 4c erweitert 4a um die längste sitzung

    public void redeLaengeProPersonInLaengsterSitzungNachZeit() {
        String sitzungsId = getLaengsteSitzungNachZeitId(); //holt die id der längsten sitzung
        if (sitzungsId == null) return; //bricht ab, wenn keine id gefunden

        //überschrift mit der gefundenen sitzungs id
        System.out.println("\n 4(c) avg. redelänge pro abgeordneten in sitzung " + sitzungsId + " (längste nach zeit/redenanzahl) ---");

        // korrektur: nutzt die korrekte Relation (r:rede)-[:gehorte_zu_sitzung]->(s)
        // cypher abfrage, die auf die gefundene sitzungs-id filtert
        String query = String.format("""
            MATCH (r:Rede)-[:GEHORTE_ZU_SITZUNG]->(s:Sitzung {id: '%s'}) // filtert nach sitzung id
            MATCH (r)-[:WURDE_GEHALTEN_VON]->(a:Abgeordneter) // matcht rede -> abgeordneter
            WHERE r.text IS NOT NULL AND size(r.text) > 0 AND a.name IS NOT NULL // filterkriterien
            RETURN 
                a.name AS Name,
                avg(size(r.text)) AS AvgLaenge,
                count(r) AS CountReden
            ORDER BY AvgLaenge DESC
            LIMIT 10 // zeigt nur die top 10
        """, sitzungsId);

        List<Map<String, Object>> results = dbConn.executeReadQuery(query); // führt die abfrage aus
        printAbgeordnetenStatistik(results); // gibt die ergebnisse aus
    }

     //erweitert 4b um die längste sitzung (zeit-proxy: redenanzahl)

    public void kommentarHaeufigkeitProAbgeordnetenInLaengsterSitzungNachZeit() {
        String sitzungsId = getLaengsteSitzungNachZeitId(); //holt die id der längsten sitzung
        if (sitzungsId == null) return; //bricht ab,wenn keine id gefunden

        //überschrift mit der gefundenen sitzungs id
        System.out.println("\n4(c) kommentar häufigkeit pro abgeordneten in sitzung " + sitzungsId + " (längste nach zeit/redenanzahl) ---");

        //korrektur: nutzt die korrekte relationen (r:rede)-[:gehorte_zu_sitzung]->(s) und (k)-[:ist_teil_von]->(r)
        // cypher abfrage, die die anzahl der kommentare pro abgeordneten in der sitzung zählt
        String cypher = String.format("""
            MATCH (r:Rede)-[:GEHORTE_ZU_SITZUNG]->(s:Sitzung {id: '%s'}) // filtert nach sitzung id
            MATCH (r)-[:WURDE_GEHALTEN_VON]->(a:Abgeordneter) // matcht rede -> abgeordneter
            OPTIONAL MATCH (k:Kommentar)-[:IST_TEIL_VON]->(r) // optional matcht kommentar -> rede
            WHERE a.name IS NOT NULL
            RETURN 
                a.name AS Name, 
                COUNT(k) AS KommentarAnzahl // zählt die kommentare (nicht den durchschnitt pro rede)
            ORDER BY KommentarAnzahl DESC
            LIMIT 10 // zeigt nur die top 10
        """, sitzungsId);

        List<Map<String, Object>> results = dbConn.executeReadQuery(cypher); // führt die abfrage aus
        printKommentarStatistik(results); //gibt die Ergebnisse aus
    }


     //aufgabe 4c - erweitert 4a um die längste sitzung (gesamt redelänge)

    public void redeLaengeProFraktionInLaengsterSitzungNachLaenge() {
        String sitzungsId = getLaengsteSitzungNachGesamtRedelaengeId(); // holt die id der längsten sitzung
        if (sitzungsId == null) return; //bricht ab,wenn keine id gefunden

        //überschrift mit der gefundenen sitzungs-id
        System.out.println("\n 4c avg redelänge pro fraktion in sitzung " + sitzungsId + " (längste nach gesamt redelänge)");

        //korrektur: nutzt die korrekte relation (r:rede)-[:gehorte_zu_sitzung]->(s)
        // cypher abfrage, die den durchschnitt pro fraktion in der sitzung berechnet
        String query = String.format("""
            MATCH (r:Rede)-[:GEHORTE_ZU_SITZUNG]->(s:Sitzung {id: '%s'}) // filtert nach sitzung id
            MATCH (r)-[:WURDE_GEHALTEN_VON]->(a:Abgeordneter)-[:IST_MITGLIED_VON]->(f:Fraktion) // matcht bis zur fraktion
            WHERE r.text IS NOT NULL AND size(r.text) > 0
            RETURN 
                f.name AS Fraktion,
                avg(size(r.text)) AS AvgLaenge,
                count(r) AS CountReden
            ORDER BY AvgLaenge DESC
        """, sitzungsId);

        List<Map<String, Object>> results = dbConn.executeReadQuery(query); //führt die abfrage aus
        printFraktionsStatistik(results); // gibt die ergebnisse aus
    }

     //aufgabe 4c erweitert 4b um die längste sitzung (gesamt redelänge)

    public void kommentarHaeufigkeitProFraktionInLaengsterSitzungNachLaenge() {
        String sitzungsId = getLaengsteSitzungNachGesamtRedelaengeId(); // holt die id der längsten sitzung
        if (sitzungsId == null) return; //bricht ab,wenn keine id gefunden

        // überschrift mit der gefundenen sitzungs id
        System.out.println("\n4(c) kommentar häufigkeit pro fraktion in sitzung " + sitzungsId + " (längste nach gesamt redelänge) ---");

        //korrektur: nutzt die korrekte relationen (r:rede)-[:gehorte_zu_sitzung]->(s) und (k)-[:ist_teil_von]->(r)
        // cypher abfrage, die die anzahl der kommentare pro fraktion in der sitzung zählt
        String cypher = String.format("""
            MATCH (r:Rede)-[:GEHORTE_ZU_SITZUNG]->(s:Sitzung {id: '%s'}) // filtert nach sitzung id
            MATCH (r)-[:WURDE_GEHALTEN_VON]->(a:Abgeordneter)-[:IST_MITGLIED_VON]->(f:Fraktion) // matcht bis zur fraktion
            OPTIONAL MATCH (k:Kommentar)-[:IST_TEIL_VON]->(r) // optional matcht kommentar -> rede
            RETURN 
                f.name AS FraktionName,
                COUNT(k) AS KommentarAnzahl // zählt die gesamtanzahl der kommentare
            ORDER BY KommentarAnzahl DESC
        """, sitzungsId);

        List<Map<String, Object>> results = dbConn.executeReadQuery(cypher); // führt die abfrage aus
        printFraktionsKommentarStatistik(results); // gibt die ergebnisse aus
    }


    //private hilfsmethode zur formatierung und ausgabe der abgeordneten statistik
    private void printAbgeordnetenStatistik(List<Map<String, Object>> results) {
        //gibt die tabellen-header aus
        System.out.printf("%-25s ! %18s ! %s\n", "name", "durchschnitt (zch.)", "anzahl");
        System.out.println("---------------------------!---------------------!---------");
        for (Map<String, Object> record : results) {
            String name = (String) record.get("Name");
            // casts prüfen, da cypher zahlen oft als double zurückgibt
            double avg = ((Number) record.get("AvgLaenge")).doubleValue();
            long count = (long) record.get("CountReden");
            System.out.printf("%-25s ! %18.2f ! %6d\n", name, avg, count);
        }
    }

    // private Hilfsmethode zur formatierung und ausgabe der fraktions statistik (redelänge)
    private void printFraktionsStatistik(List<Map<String, Object>> results) {
        // gibt die tabellen-header aus
        System.out.printf("%-15s ! %18s ! %s\n", "fraktion", "durchschnitt (zch.)", "anzahl");
        System.out.println("----------------!---------------------!---------");
        for (Map<String, Object> record : results) {
            String fraktion = (String) record.get("Fraktion");
            double avg = ((Number) record.get("AvgLaenge")).doubleValue();
            long count = (long) record.get("CountReden");
            System.out.printf("%-15s ! %18.2f ! %6d\n", fraktion, avg, count);
        }
    }

    // private hilfsmethode zur formatierung und ausgabe der kommentar statistik pro abgeordneten (gesamtanzahl in sitzung)
    private void printKommentarStatistik(List<Map<String, Object>> results) {
        // diese methode wird nur für die 4(c) ausgaben verwendet,wo die gesamtanzahl der kommentare pro abgeordneten in der sitzung benötigt wird
        // gibt die tabellen header aus
        System.out.printf("%-12s | %s\n", "kommentare", "name");
        System.out.println("-------------!---------------------------");
        for (Map<String, Object> row : results) {
            String name = (String) row.get("Name");
            Long anzahl = (Long) row.get("KommentarAnzahl");
            System.out.printf("%-12d ! %s\n", anzahl, name);
        }
    }

    //private hilfsmethode zur formatierung und ausgabe der kommentar statistik pro fraktion (gesamtanzahl in sitzung)
    private void printFraktionsKommentarStatistik(List<Map<String, Object>> results) {
        // gibt die tabellen header aus
        System.out.printf("%-12s ! %s\n", "kommentare", "fraktion");
        System.out.println("-------------!---------------------");
        for (Map<String, Object> row : results) {
            String name = (String) row.get("FraktionName");
            Long anzahl = (Long) row.get("KommentarAnzahl");
            System.out.printf("%-12d ! %s\n", anzahl, name);
        }
    }
}