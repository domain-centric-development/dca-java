package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.bad.records.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.bad.records.domain.model.MedicalRecord;
import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.bad.records.domain.model.PatientId;
import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.bad.records.domain.model.RecordId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

/**
 * Only the inherited findById loads a record; the question by owner answers yes or no and does not
 * count as a lookup.
 */
public interface MedicalRecordRepository extends Repository<MedicalRecord, RecordId> {
  boolean existsByIdForPatient(RecordId id, PatientId patient);
}
