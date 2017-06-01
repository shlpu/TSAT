#!/usr/bin/python

import argparse
import os

parser = argparse.ArgumentParser(description='Converts RPM style data to GrammarViz style data.')
parser.add_argument('rpm', metavar='RPM_Data', help='The text file containing RPM sytle data')
parser.add_argument('gv', metavar='GrammarViz_Data', help='The text file for output of GrammarViz sytle data')

args = parser.parse_args()

rpm = os.path.abspath(args.rpm)
gv = os.path.abspath(args.gv)

if not os.path.isfile(rpm):
    print "RPM File " + rpm + " does not exist."
    exit()

gvOutput = []

with open(rpm, "r") as rpmFile:
    firstLine = True
    for line in rpmFile:
        lineSplit = line.split()
        if firstLine:
            for _ in range(0, len(lineSplit)):
                gvOutput.append("")
            firstLine = False
        for i in range(0, len(lineSplit)):
            gvOutput[i] += lineSplit[i] + ' '
    

with open(gv, "w") as gvFile:
    firstLine = True
    for line in gvOutput:
        if firstLine:
            gvFile.write('# ' + line + '\n')
            firstLine = False
        else:
            gvFile.write(line + '\n')
