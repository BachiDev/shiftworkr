package dev.bachi.shiftworkr.security;

import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.repository.StaffMemberRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffUserDetailsService implements UserDetailsService {

  private final StaffMemberRepository staffMembers;

  public StaffUserDetailsService(StaffMemberRepository staffMembers) {
    this.staffMembers = staffMembers;
  }

  @NullMarked
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    StaffMember staff = staffMembers.findByEmailIgnoreCase(email)
        .filter(candidate -> candidate.isActive())
        .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + email));
    // roles(..) adds the ROLE_ prefix: ADMIN -> ROLE_ADMIN, NURSE -> ROLE_NURSE.
    return User.withUsername(staff.getEmail())
        .password(staff.getPasswordHash())
        .roles(staff.getRole().name())
        .build();
  }
}
