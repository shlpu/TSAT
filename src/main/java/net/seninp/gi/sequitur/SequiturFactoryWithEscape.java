package net.seninp.gi.sequitur;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

public class SequiturFactoryWithEscape {

	/** Chunking/Sliding switch action key. */
	protected static final String USE_SLIDING_WINDOW_ACTION_KEY = "sliding_window_key";

	private static final NormalAlphabet normalA = new NormalAlphabet();

	private static SAXProcessor sp = new SAXProcessor();

	// logging stuff
	//
	private static Logger consoleLogger;
	private static Level LOGGING_LEVEL = Level.INFO;

	static {
		consoleLogger = (Logger) LoggerFactory.getLogger(SequiturFactory.class);
		consoleLogger.setLevel(LOGGING_LEVEL);
	}

	/**
	 * Disabling the constructor.
	 */
	private SequiturFactoryWithEscape() {
		assert true;
	}

	public static SAXRule runSequitur(String inputString, int[] startingPositions, ArrayList<Integer> saxWordsIndexes,
                                      int windowSize) {
		consoleLogger.trace("digesting the string " + inputString);

		SAXRule.numRules = new AtomicInteger(0);
		SAXRule.theRules.clear();
		SAXSymbol.theDigrams.clear();
		SAXSymbol.theSubstituteTable.clear();

		SAXRule resRule = new SAXRule();

		StringTokenizer st = new StringTokenizer(inputString, " ");

		int currentPosition = 0;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();

			SAXTerminal symbol = new SAXTerminal(token, currentPosition);

			resRule.last().insertAfter(symbol);
			if ((!checkIntersection(currentPosition, startingPositions, windowSize, saxWordsIndexes))
					&& (!checkIntersection(currentPosition - 1, startingPositions, windowSize, saxWordsIndexes))) {
				resRule.last().p.check();
			}
			currentPosition++;
		}
		return resRule;
	}

	public static Boolean checkIntersection(int currentPosition, int[] startingPositions, int windowSize,
			ArrayList<Integer> saxWordsIndexes) {
		if ((currentPosition < 0) || (currentPosition >= saxWordsIndexes.size())) {
			return Boolean.valueOf(false);
		}
		int start = saxWordsIndexes.get(currentPosition).intValue();
		int end = start + windowSize - 1;
		int[] arrayOfInt;
		int j = (arrayOfInt = startingPositions).length;
		for (int i = 0; i < j; i++) {
			int startP = arrayOfInt[i];
			if ((startP >= start) && (startP <= end)) {
				return Boolean.valueOf(true);
			}
			if (startP > end) {
				break;
			}
		}
		return Boolean.valueOf(false);
	}

}
