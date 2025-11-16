package app;

import de.bundestag.factory.PortalFactory;
import de.bundestag.service.XMLProcessor;
import de.bundestag.service.DatabaseService;
import de.bundestag.service.Statistik;

import java.nio.file.Paths;

public class MainApp {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Fehler: XML-Dateipfad fehlt als Argument.");
            System.err.println("Verwendung: java MainApp <pfad-zu-xml-dateien>");
            return;
        }

        PortalFactory factory = PortalFactory.getInstance();
        XMLProcessor processor = factory.getXMLProcessor();

        //initialisiere Service-Objekte

        DatabaseService dbService = new DatabaseService(factory);
        Statistik statistik = new Statistik(factory);

        String directoryPathString = args[0];

        try {
            // XMLVerarbeitung
            System.out.println("\n STARTE PARSING Aufgabe 2");
            processor.parseAllXMLFiles(Paths.get(directoryPathString));
            System.out.println(" Daten aus " + directoryPathString + " erfolgreich verarbeitet.");

            //prüfung der Datenkapselung/Zählungen aus der Factory
            System.out.println("\nPRÜFUNG DER DATENKAPSELUNG (Factory-Zählungen)");
            //verwendung der getAll... Methoden, um die Zählungen aus den internen Collections zu holen.
            System.out.println("Prüfung: Fraktionen in Factory: " + factory.getAllFraktionen().size());
            System.out.println("Prüfung: Abgeordnete in Factory: " + factory.getAllAbgeordnete().size());
            System.out.println("Prüfung: Reden in Factory: " + factory.getAllReden().size());
            System.out.println("Prüfung: Sitzungen in Factory: " + factory.getAllSitzungen().size());
            System.out.println("Prüfung: Kommentare in Factory: " + factory.getAllKommentare().size());

            //Speicherung in Datenbank Aufgabe 3b
            dbService.saveAllEntitiesToDatabase();
            System.out.println(" Alle Daten in Neo4j gespeichert.");

            //statistik Aufgabe 4
            System.out.println("\n STARTE STATISTIKEN Aufgabe 4");

            //Knotenzählungen aus der Datenbank
            statistik.printDataCounts();


            //durchschnittliche Redelänge

            statistik.redeLaengeProPerson();
            statistik.redeLaengeProFraktion();


            //kommentarhäufigkeit

            statistik.kommentarHaeufigkeitProAbgeordneten();
            statistik.kommentarHaeufigkeitProFraktion();


            //erweiterungen zur längsten Sitzung

            //erweiterung nach Zeit
            statistik.redeLaengeProPersonInLaengsterSitzungNachZeit();
            statistik.kommentarHaeufigkeitProAbgeordnetenInLaengsterSitzungNachZeit();

            //4c Erweiterung nach gesamt Redelänge
            statistik.redeLaengeProFraktionInLaengsterSitzungNachLaenge();
            statistik.kommentarHaeufigkeitProFraktionInLaengsterSitzungNachLaenge();


            System.out.println("\n STATISTIKEN ABGESCHLOSSEN ");

        } catch (Exception e) {
            System.err.println("Ein Fehler ist da: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //Sicherstellen dass die Datenbankverbindung geschlossen wird
            if (factory.getDbConnection() != null) {
                factory.getDbConnection().shutdown();
            }
        }
    }
}