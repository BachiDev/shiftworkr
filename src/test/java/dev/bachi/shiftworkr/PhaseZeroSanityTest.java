package dev.bachi.shiftworkr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseZeroSanityTest {
  @Test
  void projectInfo() {
    assertThat("shiftworkr").isNotBlank();
  }
}
