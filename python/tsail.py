import json
import subprocess


def buildMotifs(pathToTimeseries, outputFile, window_size=30, word_size=6, alphabet_size=4, strategy="EXACT", threshold=0.01, numworkers=2):
		exitcode = subprocess.call("java -cp ../target/tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar net.seninp.grammarviz.cli.TS2SequiturGrammar -d {} -o {} -w {} -p {} -a {} --strategy {} --threshold {} --num-workers {}".format(pathToTimeseries, outputFile, window_size, word_size, alphabet_size, strategy, threshold, numworkers).split())
		return json.loads(open(outputFile).read())
	
def RRA(pathToTimeseries, outputFile, window_size=30, word_size=6, alphabet_size=4, threshold=0.01, discords_num=5):
		exitcode = subprocess.call("java -cp ../target/tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar net.seninp.grammarviz.GrammarVizAnomaly -i {} -o {} -w {} -p {} -a {} --threshold {} --discords_num {}".format(pathToTimeseries, outputFile, window_size, word_size, alphabet_size, threshold, discords_num).split())
		return json.loads(open(outputFile).read())

def RPM(pathToTraining, pathToTest, outputFile, num_iters):
		exitcode = subprocess.call("java -cp ../target/tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar net.seninp.grammarviz.GrammarVizRPM --trainD {} --testD {} --model {} --numIters {} ".format(pathToTraining, pathToTest, outputFile, num_iters).split())
		return (json.loads(open("{}.train".format(outputFile)).read()), json.loads(open("{}.test".format(outputFile)).read()))

#buildMotifs("/home/drew/Desktop/ecg0606_1.csv", "pythonOutTest", window_size=300)
#RRA("/home/drew/Desktop/ecg0606_1.csv", "pythonOutTest", window_size=300)
#train, test = RPM("/home/drew/Desktop/TSATtutorial/CBF/CBF_TRAIN_TSAT","/home/drew/Desktop/TSATtutorial/CBF/CBF_TEST_TSAT", "CBFData", 3)