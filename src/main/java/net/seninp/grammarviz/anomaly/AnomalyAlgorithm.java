package net.seninp.grammarviz.anomaly;

/**
 * The anomaly discovery algorithm selector.
 * 
 * @author psenin
 *
 */
public enum AnomalyAlgorithm {
  BRUTEFORCE(0), HOTSAX(1), RRA(2), RRAPRUNED(3), RRASAMPLED(4), EXPERIMENT(5);

  private final int index;

  AnomalyAlgorithm(int index) {
    this.index = index;
  }

  public int index() {
    return index;
  }



  @Override
  public String toString() {
    switch (this.index) {
    case 0:
      return "BRUTEFORCE";
    case 1:
      return "HOTSAX";
    case 2:
      return "RRA";
    case 3:
      return "RRAPRUNED";
    case 4:
      return "RRASAMPLED";
    case 5:
      return "EXPERIMENT";
    default:
      throw new RuntimeException("Unknown index");
    }
  }
}
