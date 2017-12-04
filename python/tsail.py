import json
import subprocess

TSAT_JAR_LOCATION = "../target/tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

def buildMotifs(pathToTimeseries, outputFile, window_size=30, word_size=6, alphabet_size=4, strategy="EXACT", threshold=0.01, numworkers=2):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.cli.TS2SequiturGrammar -d {} -o {} -w {} -p {} -a {} --strategy {} --threshold {} --num-workers {}".format(TSAT_JAR_LOCATION,pathToTimeseries, outputFile, window_size, word_size, alphabet_size, strategy, threshold, numworkers).split())
		return json.loads(open(outputFile).read())
	
def RRA(pathToTimeseries, outputFile, window_size=30, word_size=6, alphabet_size=4, threshold=0.01, discords_num=5):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.GrammarVizAnomaly -i {} -o {} -w {} -p {} -a {} --threshold {} --discords_num {}".format(TSAT_JAR_LOCATION, pathToTimeseries, outputFile, window_size, word_size, alphabet_size, threshold, discords_num).split())
		return json.loads(open(outputFile).read())

def RPM(pathToTraining, pathToTest, outputFile, num_iters):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.GrammarVizRPM --trainD {} --testD {} --model {} --numIters {} ".format(TSAT_JAR_LOCATION, pathToTraining, pathToTest, outputFile, num_iters).split())
		return (json.loads(open("{}.train".format(outputFile)).read()), json.loads(open("{}.test".format(outputFile)).read()))



#motifs = buildMotifs("/home/drew/Desktop/ecg0606_1.csv", "pythonOutTest", window_size=300)
#print(motifs['rules']['1']['ruleIntervals'][0]['endPos'])
#RRA("/home/drew/Desktop/ecg0606_1.csv", "pythonOutTest", window_size=300)
# train, test = RPM("/home/drew/Desktop/TSATtutorial/CBF/CBF_TRAIN_TSAT","/home/drew/Desktop/TSATtutorial/CBF/CBF_TEST_TSAT", "CBFData", 3)
