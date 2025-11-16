package de.bundestag.model;

public abstract class AbstractEntity implements IEntity {
    private final String id;

    protected AbstractEntity(String id) {
        //gute Praxis eine ID sollte nicht null sein
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Die Entität ID darf nicht leer sein.");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    //Ergänzungen für Collections von 2e

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        //vergleicht die Klasse da IDs nur innerhalb eines Entitätstyps eindeutig sein müssen
        if (o == null || getClass() != o.getClass()) return false;

        AbstractEntity that = (AbstractEntity) o;

        //entitäten sind gleich  wenn ihre IDs gleich sind
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        //der Hashcode basiert ausschließlich auf der finalen ID
        return id.hashCode();
    }
}
