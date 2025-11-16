package de.bundestag.service;

import de.bundestag.factory.PortalFactory;
import de.bundestag.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

//die klasse xmlprocessor ist für das einlesen und verarbeiten von bundestags xmlprotokollen zuständig
// sie nimmt entitäten wie fraktionen, abgeordnete, sitzungen, reden und kommentare
public class XMLProcessor {
    // die factory zur erstellung und verwaltung der model objekte
    private PortalFactory factory;

    // eine map zum normalisieren von fraktionsnamen
    private Map<String, String> fraktionMapping = new HashMap<>();

    //konstruktor, der die factory initialisiert und das fraktions mapping aufbaut
    public XMLProcessor(PortalFactory factory) {
        this.factory = factory;
        setupMapping(); // ruft die methode zum aufbau der mappings auf
    }

    //private methode zum initialisieren der fraktionsnamen normalisierung
    private void setupMapping() {
        // diese mappings sorgen dafür,dass unterschiedliche schreibweise auf einen standardnamen abgebildet werden
        fraktionMapping.put("CDUCSU", "CDU/CSU");
        fraktionMapping.put("CDU", "CDU/CSU");
        fraktionMapping.put("CSU", "CDU/CSU");
        fraktionMapping.put("BUNDNIS90DIEGRUNEN", "BÜNDNIS 90/DIE GRÜNEN");
        fraktionMapping.put("BUNDNIS90DIEGRUENEN", "BÜNDNIS 90/DIE GRÜNEN");
        fraktionMapping.put("GRUNEN", "BÜNDNIS 90/DIE GRÜNEN");
        fraktionMapping.put("SPD", "SPD");
        fraktionMapping.put("FDP", "FDP");
        fraktionMapping.put("AFD", "AfD");
        fraktionMapping.put("DIELINKE", "DIE LINKE");
        // zuordnung für nicht parteigebundene oder sitzungsleiter
        fraktionMapping.put("FRAKTIONSLOS", "Fraktionslos");
        fraktionMapping.put("PRASIDENT", "Sitzungsleitung");
        fraktionMapping.put("GASTE", "Sitzungsleitung");
        fraktionMapping.put("UBRIGE", "Fraktionslos");
        fraktionMapping.put("UNABHANGIG", "Fraktionslos");
        fraktionMapping.put("ABG", "Fraktionslos");
    }

    //durchläuft alle xmldateien in einem angegebenen verzeichnis
    public void parseAllXMLFiles(Path directoryPath) {
        try {
            File dir = directoryPath.toFile(); //konvertiert den pfad in ein file-objekt
            // filtert alle dateien, die mit .xml enden
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".xml"));

            if (files != null) { //prüft, ob dateien gefunden wurden
                System.out.println("found " + files.length + " xml files");
                for (File file : files) { // iteriert über die gefundenen dateien
                    parseXMLFile(file); //verarbeitet jede datei einzeln
                }
            }
        } catch (Exception e) {
            System.out.println("error reading files: " + e.getMessage());
            e.printStackTrace(); // gibt den stack trace aus
        }
    }

    // verarbeitet eine einzelne xml-datei
    private void parseXMLFile(File xmlFile) {
        try {
            System.out.println("processing: " + xmlFile.getName());

            // standard-dom-parser-setup
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile); // parst die xml datei
            doc.getDocumentElement().normalize(); //normalisiert das Dokument

            //Beibehaltung der korrekten reihenfolge für abhängigkeiten
            //Fraktionen und abgeordnete müssen vor sitzungen und reden erstellt werden
            parseFraktionen(doc); // parst alle fraktionen
            parseAbgeordnete(doc); //parst alle abgeordneten
            parseSitzungen(doc); //parst die sitzungsmetadaten
            parseRedner(doc); //parst die redner-metadaten (thema, redezeit)
            parseReden(doc); //parst die eigentlichen reden und kommentare
            // parsekommentare(doc); //logik ist in parsereden verschoben,methode ist redundant

        } catch (Exception e) {
            System.out.println("error with file " + xmlFile.getName() + ": " + e.getMessage());
            e.printStackTrace(); //gibt den stack-trace bei einem fehler aus
        }
    }

    //normalisiert einen fraktionsnamen in einen standardisierten namen
    private String normalizeFraktion(String fraktionName) {
        if (fraktionName == null || fraktionName.isEmpty()) { // prüft auf null oder leerstring
            return "";
        }

        //konvertiert den namen in großbuchstaben und entfernt sonderzeichen/umlaute für den map lookup
        String key = fraktionName.toUpperCase()
                .replace("Ä", "AE")
                .replace("Ö", "OE")
                .replace("Ü", "UE")
                .replace("ß", "SS")
                .replaceAll("[^A-Z0-9]", ""); // entfernt alle nicht alphanumerischen zeichen

        String mapped = fraktionMapping.get(key); //sucht den standardisierten namen
        if (mapped != null) {
            //gibt den standardisierten namen zurück, wenn er in der map gefunden wird
            return mapped;
        }

        // fallbacks für lange oder unklare namen
        if (fraktionName.length() > 20 || fraktionName.contains("Abgeordneter")) {
            return "Fraktionslos"; // lange namen werden oft als fraktionslos behandelt
        }

        // gibt den namen in großbuchstaben zurück, wenn kein mapping gefunden wurde
        return fraktionName.toUpperCase();
    }

    //parst fraktionen aus den <redner> elementen
    private void parseFraktionen(Document doc) {
        NodeList rednerNodes = doc.getElementsByTagName("redner"); // holt alle <redner>-elemente

        //streamt über alle redner elemente
        getElementStream(rednerNodes)
                .forEach(rednerElement -> {
                    //sucht das verschachtelte <name> element
                    Element nameElement = (Element) rednerElement.getElementsByTagName("name").item(0);
                    if (nameElement == null) return; //überspringt, wenn kein name element gefunden

                    String fraktionName = getElementText(nameElement, "fraktion"); //extrahiert den fraktionsnamen

                    if (!fraktionName.isEmpty()) {
                        String normalizedId = normalizeFraktion(fraktionName); //normalisiert den namen
                        //erstellt eine neue fraktion, wenn sie noch nicht in der factory existiert
                        if (factory.getFraktionById(normalizedId) == null) {
                            factory.createFraktion(normalizedId, fraktionName, ""); //erstellt die fraktion
                        }
                    }
                });
    }

    // parst abgeordnete aus den <redner> elementen
    private void parseAbgeordnete(Document doc) {
        NodeList rednerNodes = doc.getElementsByTagName("redner"); //holt alle <redner> elemente

        getElementStream(rednerNodes)
                .forEach(rednerElement -> {
                    String id = getAttribute(rednerElement, "id"); // eindeutige abgeordneten id

                    // sucht das verschachtelte <name> element
                    Element nameElement = (Element) rednerElement.getElementsByTagName("name").item(0);
                    if (nameElement == null) return; // überspringt, wenn kein name-element gefunden

                    String vorname = getElementText(nameElement, "vorname"); // extrahiert vorname
                    String nachname = getElementText(nameElement, "nachname"); //extrahiert nachname
                    String fraktionId = getElementText(nameElement, "fraktion"); //extrahiert fraktion

                    // fallback logik: versucht,die fraktion aus dem nachnamen zu extrahieren
                    //falls das <fraktion> tag leer ist  was typisch für präsidenten sind
                    if (fraktionId.isEmpty() && !nachname.isEmpty()) {
                        String nachnameUpper = nachname.toUpperCase();
                        //prüft auf parteikürzel in klammern
                        if (nachnameUpper.contains("(SPD)")) fraktionId = "SPD";
                        else if (nachnameUpper.contains("(CDU/CSU)")) fraktionId = "CDU/CSU";
                        else if (nachnameUpper.contains("(GRÜNEN)")) fraktionId = "BÜNDNIS 90/DIE GRÜNEN";
                        else if (nachnameUpper.contains("(FDP)")) fraktionId = "FDP";
                        else if (nachnameUpper.contains("(LINKE)")) fraktionId = "DIE LINKE";
                        else if (nachnameUpper.contains("(AFD)")) fraktionId = "AfD";
                    }

                    //normalisiert die gefundene fraktions id
                    if (!fraktionId.isEmpty()) {
                        fraktionId = normalizeFraktion(fraktionId);
                    }

                    // erstellt den abgeordneten,falls er noch nicht existiert
                    if (!id.isEmpty() && factory.getAbgeordneterById(id) == null) {
                        LocalDate geburtsdatum = null; // iese infos fehlen im protokoll
                        String beruf = ""; //diese infos fehlen im protokoll
                        String funktion = ""; //diese infos fehlen im protokoll

                        Abgeordneter abgeordneter = factory.createAbgeordneter(id, vorname, nachname, geburtsdatum, beruf, funktion);

                        //assoziiert den abgeordneten mit der fraktion
                        Fraktion fraktion = factory.getFraktionById(fraktionId);
                        if (fraktion != null) {
                            factory.associateAbgeordneterToFraktion(abgeordneter, fraktion);
                        } else if (!fraktionId.isEmpty() && !fraktionId.equals("Fraktionslos")) {
                            System.out.println("warning: fraktion " + fraktionId + " not found for " + id); //gibt warnung aus
                        }
                    }
                });
    }

    // parst redner objekte, die die metadaten der rede enthalten (thema, redezeit)
    private void parseRedner(Document doc) {
        NodeList rednerNodes = doc.getElementsByTagName("redner"); // holt alle <redner>elemente

        getElementStream(rednerNodes)
                .forEach(rednerElement -> {
                    String abgeordneterId = getAttribute(rednerElement, "id"); //id des abgeordneten
                    String thema = getElementText(rednerElement, "thema"); //thema der rede
                    String redezeitStr = getElementText(rednerElement, "redezeit"); //redezeit als string
                    Duration redezeit = parseDuration(redezeitStr); //konvertiert in duration

                    Abgeordneter abgeordneter = factory.getAbgeordneterById(abgeordneterId);
                    //erstellt das redner objekt nur,wenn der abgeordnete bereits existiert
                    if (abgeordneter != null) {
                        factory.createRedner(abgeordneter, thema, redezeit);
                    }
                });
    }

    // parst die sitzungsmetadaten aus dem wurzelelement des dokuments
    private void parseSitzungen(Document doc) {
        Element root = doc.getDocumentElement(); // das wurzelelement des xml-dokuments

        //extrahiert wahlperiode und sitzungsnummer
        String wp = getAttribute(root, "wahlperiode");
        String nr = getAttribute(root, "sitzung-nr");

        //erzeugt eine eindeutige sitzungs id
        String id = "WP" + wp + "_S" + nr;

        //extrahiert weitere sitzungsdaten
        String datumStr = getAttribute(root, "sitzung-datum");
        String zeitStr = getAttribute(root, "sitzung-start-uhrzeit");

        String raum = getAttribute(root, "sitzung-ort");
        String zugang = "Öffentlich"; // standardwert

        //konvertiert datum und zeitobjekte
        LocalDate datum = parseLocalDate(datumStr);
        LocalTime zeit = parseLocalTime(zeitStr);

        //erstellt das sitzungs objekt, wenn es noch nicht existiert
        if (!id.isEmpty() && factory.getSitzungById(id) == null) {
            factory.createSitzung(id, datum, zeit, raum, zugang);
        }
    }

    //behebt den fehler der falschen idextraktion und der fehlenden textaggregation
    //diese methode verarbeitet die eigentlichen reden und die darin verschachtelten kommentare

    private void parseReden(Document doc) {
        NodeList redeNodes = doc.getElementsByTagName("rede"); // holt alle <rede> elemente
        // nur <rede> nodes verarbeiten, da diese die gesamte redestruktur enthalten
        if (redeNodes.getLength() == 0) {
            return; // bricht ab,wenn keine reden gefunden
        }

        // sitzungsdaten aus dem dokument header extrahieren
        Element root = doc.getDocumentElement();
        String docDatumStr = getAttribute(root, "sitzung-datum");
        String docWahlperiode = getAttribute(root, "wahlperiode");
        String docSitzungsNr = getAttribute(root, "sitzung-nr");

        //die bereits erstellte sitzung abrufen
        Sitzung sitzung = factory.getSitzungById("WP" + docWahlperiode + "_S" + docSitzungsNr);
        LocalDate docDatum = parseLocalDate(docDatumStr);

        getElementStream(redeNodes) // streamt über alle <rede> elemente
                .forEach(redeElement -> {
                    // id des redners abgeordneter extrahieren
                    //die abgeordneten id liegt im nested <redner> tag
                    NodeList nestedRednerNodes = redeElement.getElementsByTagName("redner");
                    if (nestedRednerNodes.getLength() == 0) return; //ohne redner kein redeobjekt

                    Element internalRednerElement = (Element) nestedRednerNodes.item(0);
                    String rednerId = getAttribute(internalRednerElement, "id"); //korrekte abgeordneter id

                    //id der rede extrahieren
                    String currentRedeId = getAttribute(redeElement, "id"); // id der <rede> node
                    if (currentRedeId.isEmpty()) return; //id ist erforderlich

                    Abgeordneter abgeordneter = factory.getAbgeordneterById(rednerId); //holt den abgeordneten

                    if (abgeordneter != null) {

                        String titel = getElementText(redeElement, "thema"); // extrahiert das thema

                        // fix:aggregiert den gesamten text aus <p> tags innerhalb der <rede> node
                        String text = extractSpeechText(redeElement); //ruft den vollständigen redetext ab

                        //fallback für titel, falls 'thema' fehlt
                        if (titel.isEmpty()) {
                            titel = "rede von " + abgeordneter.getVorname() + " " + abgeordneter.getNachname();
                        }

                        //rede objekt erstellen oder abrufen
                        Rede rede = factory.getRedeById(currentRedeId);
                        if (rede == null) {
                            // nutzt die rede id und den aggregierten text
                            rede = factory.createRede(currentRedeId, docDatum, titel, abgeordneter, text);
                        }

                        //assoziation zur sitzung hinzufügen
                        if (sitzung != null) {
                            if (rede.getSitzung() == null) {
                                rede.setSitzung(sitzung); // setzt die sitzung
                            }
                            sitzung.addRede(rede); //fügt die rede zur sitzung hinzu
                        }

                        //zuweisung zum abgeordneten
                        abgeordneter.addRede(rede); //fügt die rede zum abgeordneten hinzu


                        // ommentare für diese rede verarbeiten
                        NodeList kommentarNodes = redeElement.getElementsByTagName("kommentar"); //holt kommentare
                        final Rede finalRede = rede; //
                        // finale referenz für den lambda-ausdruck

                        getElementStream(kommentarNodes)
                                .forEach(kommentarElement -> {
                                    String kommentarText = kommentarElement.getTextContent().trim(); // text des kommentars
                                    String autor = "unbekannt"; // standardautor
                                    LocalDate datum = docDatum; //datum der sitzung
                                    //erzeugt eine pseudo id für den kommentar da er keine eigene hat
                                    String kommentarId = finalRede.getId() + "_" + kommentarText.hashCode(); //hash basierte id

                                    if (!kommentarText.isEmpty() && factory.getKommentarById(kommentarId) == null) {
                                        Kommentar k = factory.createKommentar(
                                                kommentarId,
                                                autor,
                                                kommentarText,
                                                datum,
                                                finalRede // verknüpfung zur rede
                                        );
                                        finalRede.addKommentar(k); //fügt den kommentar zur rede hinzu
                                    }
                                });
                    }
                });
    }

    //neue hilfsmethode: aggregiert den tatsächlichen redetext aus allen <p>elementen innerhalb einer <rede>node
    //param redeelement das <rede>element
    //return der gesamte redetext

    private String extractSpeechText(Element redeElement) {
        StringBuilder fullText = new StringBuilder(); // verwendet stringbuilder für effiziente textaggregation
        NodeList childNodes = redeElement.getChildNodes(); //alle kind nodes des <rede> elements

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i); // holt die aktuelle node

            // wir suchen nur nach <p>tags
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("p")) {
                //den textinhalt des absatzes hinzufügen,gefolgt von einem zeilenumbruch
                fullText.append(node.getTextContent().trim()).append("\n");
            }
        }

        return fullText.toString().trim(); //gibt den gesammelten text zurück und entfernt überflüssige leerzeichen
    }


    private void parseKommentare(Document doc) {
        //logik wurde nach parsereden verschoben,da kommentare direkt zu einer rede gehören
    }

    //utility methods

    //konvertiert eine nodelist von elementen in einen stream von elementen
    private Stream<Element> getElementStream(NodeList nodeList) {
        List<Element> elements = new ArrayList<>(); //liste für die elemente
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) { //prüft, ob es ein elementknoten ist
                elements.add((Element) node); //fügt das element hinzu
            }
        }
        return elements.stream(); // gibt den stream zurück
    }

    // holt den wert eines attributs von einem element
    private String getAttribute(Element element, String attributeName) {
        if (element != null && element.hasAttribute(attributeName)) {
            return element.getAttribute(attributeName); // gibt den attributwert zurück
        }
        return ""; // gibt leeren string zurück, wenn attribut nicht existiert oder element null ist
    }

    //holt den textinhalt des ersten kindelements mit dem angegebenen tagnamen
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName); //sucht alle tags mit dem namen
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim(); // gibt den textinhalt des ersten elements zurück
        }
        return ""; //gibt leeren string zurück, wenn kein element gefunden
    }

    // parst einen datums-string im format dd.mm.yyyy in ein localdate objekt
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"); // definiert das erwartete format
            return LocalDate.parse(dateStr, formatter); //parst das datum
        } catch (DateTimeParseException e) {
            System.err.println("could not parse date string: " + dateStr);
            return null;
        }
    }

    //parst einen zeitstring in ein localtime objekt
    private LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        try {
            // versucht das standardformat h:mm
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            return LocalTime.parse(timeStr, formatter);
        } catch (DateTimeParseException e) {
            try {
                //versucht das sekundärformat h:mm:ss
                DateTimeFormatter secondaryFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
                return LocalTime.parse(timeStr, secondaryFormatter);
            } catch (DateTimeParseException e2) {
                System.err.println("could not parse time string: " + timeStr);
                return null;
            }
        }
    }

    //parst einen string in ein durationobjekt
    private Duration parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return Duration.ZERO;
        }
        try {
            //die java.time.duration.parse methode erfordert das iso 8601-format
            return Duration.parse(durationStr);
        } catch (Exception e) {
            //gibt null zurück, wenn das format unbekannt ist
            return Duration.ZERO;
        }
    }
}