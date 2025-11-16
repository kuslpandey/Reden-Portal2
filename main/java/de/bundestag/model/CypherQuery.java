package de.bundestag.model;

import java.util.Map;

public class CypherQuery {
    public final String cypher;
    public final Map<String, Object> parameters;

    public CypherQuery(String cypher, Map<String, Object> parameters) {
        this.cypher = cypher;
        this.parameters = parameters;
    }

    /**
     * Erstellt einen leeren Query-Container. Nützlich, wenn kein Query ausgeführt werden soll.
     * @return Ein leeres CypherQuery-Objekt.
     */
    public static CypherQuery empty() {
        return new CypherQuery("", java.util.Collections.emptyMap());
    }
}