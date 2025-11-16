package de.bundestag.factory;

import de.bundestag.database.Neo4jConnection;

import de.bundestag.service.DatabaseService;
import de.bundestag.service.XMLProcessor;
import de.bundestag.model.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PortalFactory {

    // singleton Instanz
    private static PortalFactory instance;

    // zentrale Instanzen/Services
    private final Neo4jConnection dbConnection;
    private final XMLProcessor xmlProcessor;
    private final DatabaseService databaseService;

    //zentrale Daten repositories collection
    protected Map<String, Fraktion> fraktionMap;
    protected Map<String, Abgeordneter> abgeordneterMap;
    protected Map<String, Rede> redeMap;
    protected Map<String, Sitzung> sitzungMap;
    protected Map<String, Kommentar> kommentarMap;
    protected Map<String, Plenarprotokoll> protokollMap;
    protected Map<String, Redner> rednerMap;

    private PortalFactory() {
        System.out.println("PortalFactory wird initialisiert...");

        // Initialisierung von allen Collections
        this.fraktionMap = new ConcurrentHashMap<>();
        this.abgeordneterMap = new ConcurrentHashMap<>();
        this.redeMap = new ConcurrentHashMap<>();
        this.sitzungMap = new ConcurrentHashMap<>();
        this.kommentarMap = new ConcurrentHashMap<>();
        this.protokollMap = new ConcurrentHashMap<>();
        this.rednerMap = new ConcurrentHashMap<>();

        // Datenbankverbindung initialisieren
        this.dbConnection = new Neo4jConnection();

        // Services initialisieren
        this.xmlProcessor = new XMLProcessor(this);
        this.databaseService = new DatabaseService(this);

        System.out.println("PortalFactory initialisiert");
    }

    public static PortalFactory getInstance() {
        if (instance == null) {
            instance = new PortalFactory();
        }
        return instance;
    }

    // Getter für services
    public XMLProcessor getXMLProcessor() { return xmlProcessor; }
    public DatabaseService getDatabaseService() { return databaseService; }
    public Neo4jConnection getDbConnection() { return dbConnection; }


    //Die Signatur ist korrekt und passt zum 4 Argumenten Aufruf in XMLprocessor.parseFraktionen(..., 0)
    public Fraktion createFraktion(String id, String name, String herkunftspartei) {
        //korrekte Konsistenzprüfung, nutzt 'this' oder direkten Map Zugriff
        if (fraktionMap.containsKey(id)) {return fraktionMap.get(id);}

        Fraktion f = new Fraktion(id, name, herkunftspartei);

        fraktionMap.put(id, f);
        return f;
    }

    public Abgeordneter createAbgeordneter(String id, String vorname, String nachname, LocalDate geburtsdatum, String beruf, String funktion) {
        if (abgeordneterMap.containsKey(id)) {
            return abgeordneterMap.get(id);
        }
        Abgeordneter a = new Abgeordneter(id, vorname, nachname, geburtsdatum, beruf, funktion);
        abgeordneterMap.put(id, a);
        return a;
    }

    public Redner createRedner(Abgeordneter abgeordneter, String thema, Duration redezeit) {
        Redner r = new Redner(abgeordneter, thema, redezeit);
        String id = abgeordneter.getId();

        if (rednerMap.containsKey(id)) return rednerMap.get(id);

        rednerMap.put(id, r);
        return r;
    }

    public Sitzung createSitzung(String id, LocalDate datum, LocalTime zeit, String raum, String zugang) {
        if (sitzungMap.containsKey(id)) return sitzungMap.get(id);

        Sitzung s = new Sitzung(id, datum, zeit, raum, zugang);

        sitzungMap.put(id, s);
        return s;
    }

    //Die übergebene ID wird verwendet
    public Rede createRede(String id, LocalDate datum, String titel, Abgeordneter abgeordneter, String text) {
        if (redeMap.containsKey(id)) { return redeMap.get(id); }

        //Die ID wird direkt verwendet da die Logik in XMLprocessor die Eindeutigkeit sichern muss
        Rede r = new Rede(id, datum, titel, abgeordneter, text);
        redeMap.put(id, r);
        return r;
    }

    public Kommentar createKommentar(String id, String autor, String text, LocalDate datum, Rede rede) {
        if (kommentarMap.containsKey(id)) return kommentarMap.get(id);

        Kommentar k = new Kommentar(id, autor, text, datum, rede);

        kommentarMap.put(id, k);
        return k;
    }


     //speichert alle geladenen Daten persistent in Datenbank.

    public void saveAllData() {
        this.databaseService.saveAllEntitiesToDatabase();
    }



    public void associateAbgeordneterToFraktion(Abgeordneter abgeordneter, Fraktion fraktion) {
        if (abgeordneter != null && fraktion != null) {
            abgeordneter.setFraktion(fraktion);
            //annahme ist Fraktion hat addMitglied()
            //fraktion.addMitglied(abgeordneter);
        }
    }

    public Rede getRedeById(String id) { return redeMap.get(id); }
    public Abgeordneter getAbgeordneterById(String id) { return abgeordneterMap.get(id); }
    public Fraktion getFraktionById(String id) { return fraktionMap.get(id); }
    public Sitzung getSitzungById(String id) { return sitzungMap.get(id); }
    public Kommentar getKommentarById(String id) { return kommentarMap.get(id); }


    public Collection<Fraktion> getAllFraktionen() {
        return Collections.unmodifiableCollection(fraktionMap.values());
    }
    public Collection<Abgeordneter> getAllAbgeordnete() {
        return Collections.unmodifiableCollection(abgeordneterMap.values());
    }
    public Collection<Rede> getAllReden() {
        return Collections.unmodifiableCollection(redeMap.values());
    }
    public Collection<Sitzung> getAllSitzungen() {
        return Collections.unmodifiableCollection(sitzungMap.values());
    }
    public Collection<Kommentar> getAllKommentare() {
        return Collections.unmodifiableCollection(kommentarMap.values());
    }

    public Map<String, Fraktion> getFraktionMap() { return fraktionMap; }
    public Map<String, Abgeordneter> getAbgeordneterMap() { return abgeordneterMap; }
    public Map<String, Rede> getRedeMap() { return redeMap; }
    public Map<String, Sitzung> getSitzungMap() { return sitzungMap; }
    public Map<String, Kommentar> getKommentarMap() { return kommentarMap; }
}