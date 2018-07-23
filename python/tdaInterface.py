
# coding: utf-8

# In[1]:


from tda_tools import *
import os
from sklearn.preprocessing import StandardScaler
from sklearn.preprocessing import MinMaxScaler
from sklearn.utils import resample
from sklearn.model_selection import train_test_split
import datetime
import json
import glob
from random import shuffle
import argparse
get_ipython().run_line_magic('matplotlib', 'inline')


# In[2]:


def consolidate(y):
    indexNonZero = np.nonzero(y)[0]
    start = indexNonZero[0]
    # first do a pass over the data to consoldate it 
    for i in range(1,len(indexNonZero)):
        if indexNonZero[i] - start < 10:
            y[start] = y[start] + y[indexNonZero[i]]
            y[indexNonZero[i]] = 0
        else:
            start = indexNonZero[i]
    return y


# In[3]:


def cubicInterpolate(y):
    
    y = consolidate(y)
    indexNonZero = np.nonzero(y)[0]
 
    if len(indexNonZero) <= 1:
#         print('num non zero = {}'.format(len(indexNonZero)))
        return y
    
    # then set the mid points (need two values because otherwise we get the runge effect)
    for i in range(len(indexNonZero) - 1):
        mid = int((indexNonZero[i + 1] - indexNonZero[i]) / 2)
        y[indexNonZero[i] + mid - 1] = -1.
        y[indexNonZero[i] + mid] = -1.

    # change the zeros to nans
    y[y==0] = np.nan
    # change the -1 to 0
    y[y==-1] = 0
    # now do the interpolation
    ySeries = pd.Series(y)
    iySeries = ySeries.interpolate(method='cubic')
    interpVals = np.array(iySeries.values)
    interpVals = interpVals[~np.isnan(interpVals)]
    return interpVals # return the numpy array


# In[4]:


def multiToUniVar(mvTS, window, dt, pdim, maxrad):
    minLength = min([len(x) for x in mvTS])
    for ts in mvTS:
        newTS = ts[:minLength]
        shortenedAndLogDiff.append(np.log(np.divide(newTS[1:], newTS[0:-1])))
#                 plt.figure(figsize=(12,6),dpi=90)
#                 plt.plot(np.log(np.divide(newTS[1:], newTS[0:-1])),label="bytes")
    stacked = np.stack(shortenedAndLogDiff, axis=-1)
    norms2 = streamingLandscapePnorms(stacked,window, dt, pdim, dim=1,maxrad=maxrad)
    scaler = MinMaxScaler(feature_range=(0,1))
    norms2normed = scaler.fit_transform(np.array([x[1] for x in norms2]).reshape(-1,1))
#     if np.count_nonzero(norms2normed) > 0:
#         plt.figure(figsize=(12,6),dpi=90)
#         plt.plot(norms2normed,label="bytes")
    return norms2normed


# In[5]:


def parseJson(d, data_out, numLines, shouldConsolidate, window, dt, pdim, maxrad):
    minLen = 1000
    
    
    if numLines <= 0:
        numLines = len(d)
    
    for i in range(numLines):
        mvTS = []
        #print("{}/{} j = {} val = {}".format(i, len(d), len(d[i]['timeSeries']), d[i]['timeSeries'][2]['data']))
        
        for j in range(len(d[i]['timeSeries'])):
            
            # I'm going to add 1 so that the log won't be undefined at log(0)
            var = np.array(d[i]['timeSeries'][j]['data'])
            
            if np.count_nonzero(var) > 1:
                # then we have some values so interpolate
                if shouldConsolidate:
                    var = consolidate(var) #cubicInterpolate(var)
                if np.count_nonzero(var) > 1:
                    print(i)
                    var = var + 1 # need to add one otherwise the double zeros will mess with the log
                    mvTS.append(var)
        
        # this was the min length other than all zeros which can be a thing...
        shortenedAndLogDiff = []
        if len(mvTS) == 1:
            norms2normed = np.array(mvTS[0])
        elif len(mvTS) > 1:
            norms2normed = multiToUniVar(mvTS, window, dt, pdim, maxrad)
            
        if len(mvTS) > 0:
            val ="{}".format(norms2normed.reshape(1,-1))
            val = val.strip('[]')
            val = " ".join(val.splitlines())
            label = d[i]['label']
            data_out.write("{} {}\n".format(label, val))


# In[ ]:


#first create one file with all the data converted to univariate time series
parser = argparse.ArgumentParser(description='Topological Data Analysis tool to convert multivariate time series to univariate time series.')
parser.add_argument('dataset', metavar='MVData', help='The json file containing the multivariate time series dataset')
parser.add_argument('univarDataset', metavar='UVData', help='The name for the univariate time series data file created by this tool.')
parser.add_argument('numLines', metavar='numL', help='The number of examples to read from the multivariate time series file', type=int)
parser.add_argument('window', help='The width of the window to compute persistence on, will take up to this number of samples',type=int)
parser.add_argument('dt', help='number of samples to skip between points.',type=int)
parser.add_argument('p', help='integer (type of L^p norm to compute)', default=2, type=int)
parser.add_argument('maxrad', help='max distance between pairwise points to consider for the Rips complex', default=1.0, type=float)
parser.add_argument('--shouldConsolidate', help='If true will merge values in the time series that are less than 10 time steps appart', type=bool, action="store_true")

args = parser.parse_args()

multivarFile = os.path.abspath(args.dataset)
univarFile = os.path.abspath(args.univarDataset)
numLines = args.numLines
consolidate = args.shouldConsolidate
window = args.window
dt = args.dt
pdim = args.p
maxrad = args.maxrad

# then do this with the requested files.
    with open(univarFile, 'w') as data_out:
        data_out.write("#\n")
        with open(multivarFile) as json_data:
            d = json.load(json_data)
            parseJson(d, data_out,numLines, window, dt, pdim, maxrad)

