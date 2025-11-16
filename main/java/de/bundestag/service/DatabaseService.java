package de.bundestag.service;

import de.bundestag.factory.PortalFactory;
import de.bundestag.database.Neo4jConnection;
import de.bundestag.model.IEntity;
import de.bundestag.model.CypherQuery; //Importiere die Hilfsklasse

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseService {

    private final PortalFactory factory;
    private final Neo4jConnection dbConnection;
    //Batch Größe: Ein guter Wert,um transaktions Overhead zu reduzieren
    private static final int BATCH_SIZE = 1000;

    public DatabaseService(PortalFactory factory) {
        this.factory = factory;
        //Die Datenbankverbindung wird über die Factory bezogen
        this.dbConnection = factory.getDbConnection();
    }

     //speichert alle geladenen Entitäten in die Neo4jDatenbank, indem alle
     //Abfragen gesammelt und in Batches ausgeführt werden

    public void saveAllEntitiesToDatabase() {
        System.out.println("\n STARTE DATENBANK SPEICHERUNG ");

        // sammle alle Queries aus allen Entitätstypen
        List<CypherQuery> allQueries = collectAllQueries();
        System.out.println("Gesamte Queries gesammelt: " + allQueries.size() + " Abfragen.");

        // führt alle gesammelten Queries in Batches aus
        executeBatchedQueries(allQueries);

        System.out.println("DATENBANK SPEICHERUNG ABGESCHLOSSEN");
    }


     //sammelt alle parametrisierten Queries aus allen Factory Listen in der richtigen Reihenfolge

    private List<CypherQuery> collectAllQueries() {
        List<CypherQuery> allQueries = new ArrayList<>();

        //Sammle in der Reihenfolge der Abhängigkeiten
        allQueries.addAll(collectQueries(factory.getAllFraktionen(), "Fraktionen"));
        allQueries.addAll(collectQueries(factory.getAllSitzungen(), "Sitzungen"));
        allQueries.addAll(collectQueries(factory.getAllAbgeordnete(), "Abgeordnete"));
        allQueries.addAll(collectQueries(factory.getAllReden(), "Reden"));
        allQueries.addAll(collectQueries(factory.getAllKommentare(), "Kommentare"));

        return allQueries;
    }

     //Hilfsmethode:konvertiert eine Collection von Entitäten in eine Liste von CypherQuery Objekten
    private <T extends IEntity> List<CypherQuery> collectQueries(Collection<T> entities, String entityName) {
        System.out.println("Sammle " + entities.size() + " Queries für " + entityName + "...");
        //konvertiert alle Entitäten in Queries und filtert leere Queries heraus
        return entities.stream()
                .map(IEntity::toParameterizedNode)
                .filter(query -> query != null && !query.cypher.isEmpty())
                .collect(Collectors.toList());
    }

     //führt die gesammelten Queries in definierten Batches aus, wobei jeder Batch eine Transaktion bildet

    private void executeBatchedQueries(List<CypherQuery> allQueries) {
        int totalQueries = allQueries.size();

        for (int i = 0; i < totalQueries; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalQueries);
            //erstellt den Batch (Subliste)
            List<CypherQuery> batch = allQueries.subList(i, endIndex);

            try {
                //Aufruf der Methode, die den gesamten Batch in einer Transaktion verarbeitet
                //dies reduziert den Transaktions vverhead drastisch
                dbConnection.executeWriteBatch(batch);
                System.out.printf("Batch %d (Einträge: %d - %d) erfolgreich ausgeführt.%n",
                        (i / BATCH_SIZE) + 1, i + 1, endIndex);
            } catch (Exception e) {
                //fehlerbehandlung für den gesamten Batch
                System.err.printf("FEHLER beim Ausführen von Batch %d (Einträge: %d - %d). Der Batch wurde zurückgerollt. Ursache: %s%n",
                        (i / BATCH_SIZE) + 1, i + 1, endIndex, e.getMessage());
                //Wir stoppen die Verarbeitung,da ein Fehler im Batch aufgetreten ist.
                break;
            }
        }
    }
}