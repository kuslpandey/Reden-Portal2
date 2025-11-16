package de.bundestag.database;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import de.bundestag.model.CypherQuery; // 💡 NEU: Import für Batch-Verarbeitung

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * stellt die Verbindung zu einer embedded Neo4j datenbank her
 * und ermöglicht  Ausführen von cypher Abfragen.
 */
public class Neo4jConnection {

    private final DatabaseManagementService managementService;
    private final GraphDatabaseService graphDb;
    private static final String DB_PATH = "data/neo4j-db";

    public Neo4jConnection() {
        File databaseDir = new File(DB_PATH);
        if (!databaseDir.exists()) {
            boolean created = databaseDir.mkdirs();
            if (created) {
                System.out.println(" Db Verzeichnis erstellt: " + DB_PATH);
            }
        }

        Path databasePath = databaseDir.toPath();

        // Initialisierung der embedded datenbank
        this.managementService = new DatabaseManagementServiceBuilder(databasePath)
                .build();

        // startet die Datenbank
        this.graphDb = managementService.database("neo4j");

        registerShutdownHook();
        System.out.println(" embedded Neo4j datenbank initialisiert: " + DB_PATH);

        testConnection();
        //unique constraints hier erstellt für schnelle merge operationen
        createUniqueConstraints();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }));
    }

    /**
     * erstellt  unique constraints für schnelle mergeoperationen
     */
    private void createUniqueConstraints() {
        try (Transaction tx = graphDb.beginTx()) {
            tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (k:Kommentar) REQUIRE k.id IS UNIQUE");
            tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (r:Rede) REQUIRE r.id IS UNIQUE");
            tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (a:Abgeordneter) REQUIRE a.id IS UNIQUE");
            tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (f:Fraktion) REQUIRE f.id IS UNIQUE");
            tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (s:Sitzung) REQUIRE s.id IS UNIQUE");
            tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Protokoll) REQUIRE p.id IS UNIQUE");
            tx.commit();
            System.out.println(" [Index] unique Constraints erstellt.");
        } catch (Exception e) {
            System.err.println(" Fehler beim Erstellung der Constraints: " + e.getMessage());
        }
    }


    /**
     * methode für Abfragen wobei jeder Aufruf eine eigene Transaktion startet.
     * sollte in der Regel durch executeWriteBatch ersetzt werden.
     */
    public void executeWriteQuery(String cypherQuery, Map<String, Object> parameters) {
        try (Transaction tx = graphDb.beginTx()) {
            // WICHTIG: Die .execute() Methode akzeptiert den Cypher-String und die Parameter-Map
            tx.execute(cypherQuery, parameters);
            tx.commit();
        } catch (Exception e) {
            System.err.println("Fehler bei Schreibabfrage für Cypher: " + cypherQuery);
            System.err.println("   Mit Parametern: " + parameters);
            System.err.println("   Ursache: " + e.getMessage());
        }
    }

    /**
     *   Führt eine liste von Queries innerhalb einer
     * einzigenn transaktion aus Batchmodus.
     * @param queries Die Liste der CypherQuery Objekte (Query-String und Parameter-Map).
     */
    public void executeWriteBatch(List<CypherQuery> queries) {
        if (queries == null || queries.isEmpty()) {
            return;
        }

        // die transaktion wird nur einmal für den gesamten Batch geöffnet
        try (Transaction tx = graphDb.beginTx()) {
            for (CypherQuery query : queries) {
                // führt jeden Query mit seinen spezifischen Parameter aus
                tx.execute(query.cypher, query.parameters);
            }
            tx.commit(); // Alle änderungen werden gleichzeitig gespeichert
        } catch (Exception e) {
            // fehlerhafte transaktion wird automatisch zurückgerollt sozusagen
            // wir werfen die ausnahme erneut damit der Databaseservice den Fehler melden kann
            throw new RuntimeException("Fehler beim Ausführen eines Schreib Batches. Ursache: " + e.getMessage(), e);
        }
    }


    // methode für lesende Abfragen
    public List<Map<String, Object>> executeReadQuery(String cypherQuery) {
        List<Map<String, Object>> records = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {
            Result result = tx.execute(cypherQuery);

            while (result.hasNext()) {
                records.add(result.next());
            }
            return records;

        } catch (Exception e) {
            System.err.println("Fehler bei Leseabfrage: " + e.getMessage());
            e.printStackTrace();
            return records;
        }
    }

    public void shutdown() {
        if (managementService != null) {
            managementService.shutdown();
            System.out.println("Neo4j datenbank gestürzt");
        }
    }

    /**
     * testet die Datenbankverbindung
     */
    public void testConnection() {
        try (Transaction tx = graphDb.beginTx()) {
            Result result = tx.execute("RETURN 'Datenbank bereit' AS status");
            if (result.hasNext()) {
                Map<String, Object> row = result.next();
                String status = row.get("status").toString();
                System.out.println( status);
            }
            tx.commit();
        } catch (Exception e) {
            System.err.println("Fehler " + e.getMessage());
        }
    }
}