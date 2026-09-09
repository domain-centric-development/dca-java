package acme.ledger.domain.model;
public final class Entry extends dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot<Entry,EntryId> { private final EntryId id; public Entry(EntryId id){this.id=id;} public EntryId id(){return id;} public void change(){} }
