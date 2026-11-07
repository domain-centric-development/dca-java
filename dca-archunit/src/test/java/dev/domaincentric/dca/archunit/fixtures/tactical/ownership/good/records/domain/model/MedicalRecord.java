package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Owned;

public final class MedicalRecord extends BaseAggregateRoot<MedicalRecord, RecordId>
    implements Owned<PatientId> {
  private final RecordId id;
  private final PatientId patient;

  private MedicalRecord(RecordId id, PatientId patient) {
    this.id = id;
    this.patient = patient;
  }

  public static MedicalRecord open(RecordId id, PatientId patient) {
    return new MedicalRecord(id, patient);
  }

  @Override
  public RecordId id() {
    return id;
  }

  public PatientId patient() {
    return patient;
  }
}
