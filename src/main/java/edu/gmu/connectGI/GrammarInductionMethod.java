package edu.gmu.connectGI;

public enum GrammarInductionMethod {

	SEQUITUR(0), REPAIR(1);

	private final int index;

	GrammarInductionMethod(int index) {
		this.index = index;
	}

	/**
	 * Gets the integer index of the instance.
	 * 
	 * @return integer key of the instance.
	 */
	public int index() {
		return index;
	}

	/**
	 * Given a integer value returns the Grammar Induction Method the correspond with it.
	 *
	 * @param value - integer value corresponding to a Grammar Induction Method.
	 * @throws RuntimeException - Unknown index value.
	 * @return A Grammar Induction Method enum value.
	 */
	public static GrammarInductionMethod forValue(int value) {
		switch (value) {
		case 0:
			return GrammarInductionMethod.SEQUITUR;
		case 1:
			return GrammarInductionMethod.REPAIR;
		default:
			throw new RuntimeException("Unknown index:" + value);
		}
	}

}
