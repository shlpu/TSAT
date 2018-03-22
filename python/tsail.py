import json
import subprocess

TSAT_JAR_LOCATION = "../target/tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

def buildMotifs(pathToTimeseries, outputFile, window_size=30, word_size=6, alphabet_size=4, strategy="EXACT", threshold=0.01, numworkers=2):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.cli.TS2SequiturGrammar -d {} -o {} -w {} -p {} -a {} --strategy {} --threshold {} --num-workers {}".format(TSAT_JAR_LOCATION,pathToTimeseries, outputFile, window_size, word_size, alphabet_size, strategy, threshold, numworkers).split())
		return json.loads(open(outputFile).read())
	
def RRA(pathToTimeseries, outputFile, window_size=30, word_size=6, alphabet_size=4, threshold=0.01, discords_num=5):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.GrammarVizAnomaly -i {} -o {} -w {} -p {} -a {} --threshold {} --discords_num {}".format(TSAT_JAR_LOCATION, pathToTimeseries, outputFile, window_size, word_size, alphabet_size, threshold, discords_num).split())
		return json.loads(open(outputFile).read())

def RPMTrainTest(pathToTraining, pathToTest, outputFile, num_iters):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.GrammarVizRPM --trainD {} --testD {} --model {} --numIters {} --mode {}".format(TSAT_JAR_LOCATION, pathToTraining, pathToTest, outputFile, num_iters, 1).split())
		return (json.loads(open("{}.train".format(outputFile)).read()), json.loads(open("{}.test".format(outputFile)).read()))

def RPMTrain(pathToTraining, outputFile, num_iters):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.GrammarVizRPM --trainD {} --model {} --numIters {} ".format(TSAT_JAR_LOCATION, pathToTraining, outputFile, num_iters,0).split())
		# return the representative patterns
		return json.loads(open("{}.train".format(outputFile)).read())

def RPMTest(pathToTest, modelFile, num_iters):
		exitcode = subprocess.call("java -cp {} net.seninp.grammarviz.GrammarVizRPM --testD {} --model {} --numIters {} --mode {}".format(TSAT_JAR_LOCATION, pathToTest, modelFile, num_iters, 2).split())
		#return the results
		return json.loads(open("{}.test".format(modelFile)).read())



#motifs = buildMotifs("/home/drew/Desktop/ecg0606_1.csv", "pythonOutTest", window_size=300)
#print(motifs['rules']['1']['ruleIntervals'][0]['endPos'])
#jsonData = RRA("/home/dwicke/Desktop/testpcapconnection3.txt", "pythonOutTest", window_size=15,word_size=4,threshold=0.05, discords_num=10)
#print(jsonData)
#train, test = RPMTrainTest("/home/dwicke/Desktop/TSATtutorial/CBF/CBF_TRAIN_TSAT","/home/dwicke/Desktop/TSATtutorial/CBF/CBF_TEST_TSAT", "CBFData", 3)
# train = RPMTrain("/home/dwicke/Desktop/TSATtutorial/CBF/CBF_TRAIN_TSAT", "CBFData", 3)
# test = RPMTest("/home/dwicke/Desktop/TSATtutorial/CBF/CBF_TEST_TSAT", "CBFData", 3)