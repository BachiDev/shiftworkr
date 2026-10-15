package dev.bachi.shiftworkr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ward")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ward {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, unique = true, length = 20)
  private String code;

  @Column(nullable = false)
  private boolean active = true;

  public Ward(String name, String code) {
    this.name = name;
    this.code = code;
  }
}
