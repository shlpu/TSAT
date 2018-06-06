package net.seninp.grammarviz.logic;

public class PackedRuleRecord {

  // class number
  private int classIndex;

  // Sub-sequence Number - i.e. how many sub-sequences
  private int subsequenceNumber;

  private int minLength;

  private int maxLength;


  public int getClassIndex() {
    return classIndex;
  }

  public void setClassIndex(int classIndex) {
    this.classIndex = classIndex;
  }

  public int getSubsequenceNumber() {
    return subsequenceNumber;
  }

  public void setSubsequenceNumber(int subsequenceNumber) {
    this.subsequenceNumber = subsequenceNumber;
  }

  public int getMinLength() {
    return minLength;
  }

  public void setMinLength(int minLength) {
    this.minLength = minLength;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

}
