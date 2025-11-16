package de.bundestag.model;

import java.time.Duration;

//die Klasse Redner repräsentiert eine Person die im Bundestag eine Rede hält
//sie enthält Informationen über den Abgeordneten,das Thema und die Dauer der Rede
public class Redner {

    //ein Verweis auf das AbgeordnetenObjekt, das den Redner darstellt
    private Abgeordneter abgeordneter;
    //der Titel oder Gegenstand der Rede
    private String thema;
    //Die Dauer der Rede gespeichert als java.time.Duration
    private Duration redezeit;

    //konstruktor zur Initialisierung eines Rednerobjekts mit allen notwendigen Daten
    public Redner(Abgeordneter abgeordneter, String thema, Duration redezeit) {
        this.abgeordneter = abgeordneter;
        this.thema = thema;
        this.redezeit = redezeit;
    }

    //gibt den Namen des Abgeordneten zurück, der die Rede hält
    //dieser wird über das getAbgeordneter().getName() des verknüpften Abgeordnetenobjekts abgerufen
    public String getName() {
        return abgeordneter.getName();
    }

    //getter Methode für das Abgeordnetenobjekt
    public Abgeordneter getAbgeordneter() {
        return abgeordneter;
    }

    //getterMethode für das Thema der Rede
    public String getThema() {
        return thema;
    }

    //gettermethode für die Dauer der Rede
    public Duration getRedezeit() {
        return redezeit;
    }

    //setter Methode um das Thema der Rede zu ändern
    public void setThema(String neuesThema) {
        thema = neuesThema;
    }

    //Settermethode um die Dauer der Rede zu ändern
    public void setRedezeit(Duration neueRedezeit) {
        redezeit = neueRedezeit;
    }

    //eine Methode, die eine String Repräsentation des Redners erstellt
    //die möglicherweise für die Speicherung in einer Datenbank verwendet
    public String toNode() {
        //hier mach ich was mit dem redner für die datenbank
        // ber ich weiß noch nicht genau was
        // ielleicht muss das später noch geändert werden
        //momentan wird nur eine einfache string repräsentation zurückgegeben
        String result = "Redner: " + getName() + " Thema: " + thema;
        return result;
    }
}