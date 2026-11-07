package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model.MedicalRecord;
import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model.PatientId;
import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model.RecordId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.util.Optional;

/** The scoped lookup takes the owner first: the parameter order is free. */
public interface MedicalRecordRepository extends Repository<MedicalRecord, RecordId> {
  Optional<MedicalRecord> findByIdForPatient(PatientId patient, RecordId id);
}
