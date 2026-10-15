package dev.bachi.shiftworkr.seed;

import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.Role;
import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.repository.ShiftTypeRepository;
import dev.bachi.shiftworkr.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production bootstrap: creates ONLY the initial admin from environment secrets.
 * Fails fast when secrets are missing — a live site must never boot unsecured.
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
public class ProdAdminBootstrap implements ApplicationRunner {

  private final StaffMemberRepository staffMembers;
  private final PasswordEncoder passwordEncoder;
  private final ShiftTypeRepository shiftTypes;

  @Value("${ADMIN_EMAIL:}")
  private String adminEmail;

  @Value("${ADMIN_PASSWORD:}")
  private String adminPassword;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (staffMembers.count() > 0) {
      return;
    }
    if (adminEmail.isBlank() || adminPassword.isBlank()) {
      throw new IllegalStateException(
          "ADMIN_EMAIL and ADMIN_PASSWORD environment variables are required for first prod boot");
    }
    ReferenceData.shiftTypes(shiftTypes);
    StaffMember admin = new StaffMember(null, adminEmail.trim(),
        passwordEncoder.encode(adminPassword), "ShiftWorkr", "Admin",
        Role.ADMIN, Qualification.RN, true, 38);
    staffMembers.save(admin);
  }
}
