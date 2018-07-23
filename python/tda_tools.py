# coding: utf-8
# Topological Data Analysis Toolkit
# 
import dionysus as d
import numpy as np
from matplotlib import pyplot as plt
import itertools as it
from scipy.integrate import quad # for integration
import pandas as pd
from sklearn import preprocessing
from matplotlib import cm
from copy import deepcopy
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import StandardScaler
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics.pairwise import pairwise_distances
import matplotlib.colors as col
import time
import tqdm # progress meter
from scipy import signal #resampling
from scipy import stats
import scipy as sp
from scipy import special #gamma for max radius estimate of V-R complex/Chi distribution mean
import multiprocessing as mp
import dill # supplement to pickle

# SIMPLICIAL COMPLEX CONSTRUCTS
# Join
# Inputs: X,Y - lists of Dionysus simplices
# Output: The Join of X and Y, if they are both simplicial complexes then this will be the definition of the Join in textbooks.
#         If used on facets, then the output will be the facets of the Join.
Join = lambda X,Y : list([d.Simplex(s) for s in set([tuple(set(tuple(sigma)).union(set(tuple(tau)))) for sigma, tau in it.product(X,Y)])])

def Link(sigma,X):
    # inputs: 
    # sigma - dionysus simplex
    # X     - list of dionysus simplices 
    link = []
    for tau in X:
        if set(sigma).issubset(set(tau)):
            tau_sub_gamma = set(tau).difference(set(sigma))
            link.append(d.Simplex(list(tau_sub_gamma)))
    return(link)

# Star
# inputs: 
# * sigma - dionysus simplex
# * X     - list of dionysus simplices
# output: list of all simplices of X containing sigma
Star = lambda sigma,X : [d.Simplex(tau) for tau in X if set(sigma).issubset(set(tau))]

def AlexanderDual(X):
    # input: list of dionysus simplices X
    # output: facets of the Alexander Dual 
    V   = set()
    dim = -1
    X   = list(set(X))
    for s in X:
        V   = V.union(set(s))
        dim = max(dim,s.dimension())
    Id  = np.eye(len(V))
    M = np.zeros((len(X),len(V))) # one row per simplex 
    # form simplicial incidence matrix
    for i in range(len(X)):
        for j in X[i]:
            M[i,j] = 1
    # find maximal faces.  Entry (i,j) is the size of intersection of face i and j
    M_max    = np.matmul(M, M.transpose())
    # entry (i,j) is true if face i is subset of face j
    M_max    = M_max == np.sum(M,1).reshape(M.shape[0],1)
    nonfaces = []
    rV       = range(len(V))
    Mt       = M.transpose()
    for ix in range(2,dim+2):
        combsMat = np.vstack([np.sum(Id[list(x)],0) for x in it.combinations(rV,ix)])
        M_max    = np.matmul(combsMat,Mt)
        M_max    = M_max == ix
        if nonfaces ==[]: 
            nonfaces = combsMat[np.sum(M_max,1)==0]
        else:
            nonfaces = np.vstack([nonfaces,combsMat[np.sum(M_max,1)==0]])
    M     = np.array(np.logical_not(nonfaces),dtype='float')
    M_max = np.matmul(M,M.transpose())
    M_max = M_max == np.sum(M,1).reshape(M.shape[0],1)
    M     = M[np.sum(M_max,1)==1]
    maxSimplices = []
    for ix in range(M.shape[0]):
        maxSimplices.append(d.Simplex(list(np.where(M[ix]==1)[0])))
    return(maxSimplices)

def Deletion(v,X):
    # inputs:
    # * v: vertex label (int)
    # * X: dionysus simplex list
    # output:
    # The list of simplices in X, where no simplex contains the vertex v
    return([s for s in X if v not in s])

# SHAPE GENERATORS
fuzzyEllipseGen = lambda n,r1,r2,x,y,sigma : sigma*np.random.normal(size=(n,2))+np.transpose(np.vstack((lambda t:[r1*np.cos(t)+x,r2*np.sin(t)+y])(np.linspace(0,2*np.pi,n))))

fuzzyBentCircleGen = lambda n,r1,r2,x,y,a,sigma : sigma*np.random.normal(size=(n,2))+np.transpose(np.vstack((lambda t:[r1*np.cos(t)+x,r2*np.sin(t)+a*np.square(np.cos(t))+y])(np.linspace(0,2*np.pi,n))))

# PLOTTING FUNCTIONS
def saturate_points(X2D,radius,figsize=(20,20),color='g',edgecolor='black',facecolor='green'):
    # input: Nx2 numpy float array of 2-d points (one in each row)
    # output: matplotlib (fig,ax) with the plot of bubbles of radius 'radius' around the points from the input
    myfig = plt.gcf()
    ax    = plt.gca()
    ax.figure.set_size_inches(*figsize)
    ax.set_aspect('equal')
    plt.scatter(X2D[:,0],X2D[:,1],color=color)
    for ix in range(X2D.shape[0]):
        C = plt.Circle(X2D[ix,:],radius,edgecolor=edgecolor,facecolor=facecolor,fill=True,alpha=.2,linewidth=2)
        ax.add_artist(C)
    # use ax.figure.show() to display plot from terminal
    return(myfig,ax)

def plot_landscapes( landscape_array, rstart, rend, N ):
    # inputs:
    # landscape_array: m x n numpy array, the kth row is the kth landscape function, from output of dgm2landscape below
    # rstart (float): earliest rips parameter
    # rend   (float): latest rips parameter 
    # N (int): top-N landscape functions to plot
    # output: matplotlib (fig,ax) object to plot landscapes
    thisfig = plt.gcf()
    thisax  = plt.gca()
    thisax.figure.set_size_inches(6,4)
    thisax.figure.set_dpi(120)
    thisax.set_xticks(range(0,landscape_array.shape[1],landscape_array.shape[1]//20))
    thisax.set_xticklabels(["{:.2f}".format(tm) for tm in np.linspace(rstart,rend,20)],orientation='vertical')
    for ix in range(N):
        thisax.plot(landscape_array[ix,:],label="$\lambda_{}$".format('{'+str(ix+1)+'}'))
    thisax.legend()
    return(thisfig,thisax) #run thisax.figure.show() to show it inline in a jupyter notebook
    
    
    
    
# PERSISTENCE LANDSCAPES

# dgm2array
# inputs:
# * dgm: dionysus persistence diagram of a specified dimension, obtained by running dionysus' init_diagrams method against the result of fill_rips and homology_persistent methods.
# output:
# N x 2 array, each row (x,y) is a birth/death pair for the persistent homology.  Note- it is possible y=np.inf (infinity) if the feature existed at the beginning and never fades.
dgm2array = lambda dgm : np.array([(xy.birth,xy.death) for xy in dgm])

def dgm2landscape( dgm, n_points = 100, t_min=None,t_max=None ):
    # inputs:
    # dgm - numpy array with birth/death pairs in each row (output of dgm2array)
    # n_points - number of sample points on the birth axis to sample.
    #            The characteristic length scale of the smallest feature is implicitly
    #            selected via this parameter; (max{x,y}-min{x,y})/n_points 
    # t_min (float) - minimum birth time to consider 
    # t_max (float) -maximum death time to consider
    # output:
    # m x n_points array, the kth row is the kth landscape function 
    if dgm.shape[0]==0:
        return(np.zeros((1,n_points)),0,0)
    if t_min:
        bstart = t_min
    else:
        bstart = np.min(dgm)
    if t_max:
        bend = t_max
    else:
        bend = np.max(dgm)
    if bend == np.inf:
        #print("REE")
        #bend=np.max(dgm[dgm<np.inf])
        bend                    = np.max(dgm[~np.isinf(dgm)])
        dgm[dgm[:,1] == np.inf] = bend
    t = np.linspace(bstart,bend,n_points)
    f_birthdeath = np.zeros((dgm.shape[0],n_points),dtype='float32')
    for ix in range(t.size):
        # light up rows that have (b,d) intervals with t inside
        # also disregard rows that have infinity since they will
        # contribute 0 to the landscape function by definition
        #range_mask = (dgm[:,0]<=t[ix]) & (dgm[:,1]>=t[ix]) & (dgm[:,1]<np.inf)
        range_mask = (dgm[:,0]<=t[ix]) & (dgm[:,1]>=t[ix])
        if np.any(range_mask):
            dgm_inrange = dgm[range_mask,:]
            if dgm_inrange.shape[0]==1:
                # if there is only one row, there is only one (b,d) pair to consider persistence of
                f_birthdeath[range_mask,ix] = np.min( np.hstack([t[ix]-dgm_inrange[0,0],dgm_inrange[0,1]-t[ix]]) )
            else:
                # in this case, find cur_time-birth or death-cur_time (whichever is smaller) to estimate the persistence of each feature
                f_birthdeath[range_mask,ix] = np.transpose(np.min(np.vstack([t[ix]-dgm_inrange[:,0],dgm_inrange[:,1]-t[ix]]),0))
    # now that the functions f_(b,d) have been computed for every birth/death pair, just have to sort the columns so that lambda_k will be the kth row of the matrix
    landscapes = np.sort(f_birthdeath,axis=0)[::-1]
    return(landscapes,bstart,bend)

def LpNorms( landscapes, rmin, rmax, landscapes2=None, p=1 ):
    # inputs:
    # landscapes: numpy array, output from dgm2landscape
    # rmin (float): min rips parameter, from dgm2landscape
    # rmax (float): max ....
    # landscapes2: another numpy array output from dgm2landscape in order to compute the Lp-distance between this and landscapes
    #              For instance, in two consecutive overlapping time windows of a timeseries, use this to see how fast persistence is changing
    # p (int or float >=1): which L^p norm to compute 
    # output:
    # numpy array the shape of landscapes, if landscapes2 is null, or the max of the two if both are given.  It has the Lp Norms of the landscape functions, or if two arrays are given, the distance between.
    dt      = rmax-rmin
    n       = landscapes.shape[1]
    landFun = lambda x,ix : np.max([0,landscapes[ix,int(x)]])
    if landscapes2 is not None:
        nScapes = max(landscapes.shape[0],landscapes2.shape[0])
        if landscapes.shape[0]<nScapes:
            landscapes = np.vstack([landscapes,np.zeros((nScapes-landscapes.shape[0],n))])
        elif landscapes2.shape[0]<nScapes:
            landscapes2 = np.vstack([landscapes2,np.zeros((nScapes-landscapes2.shape[0],n))])
        landFun2 = lambda x,ix : np.max([0,landscapes2[ix,int(x)]])
        quadInts = np.zeros((nScapes,))
    else:
        quadInts = np.zeros((landscapes.shape[0],))
        nScapes  = landscapes.shape[0]
    for ix in range(nScapes):
        if landscapes2 is not None:
            quadInt = quad(lambda x:np.power(np.abs(landFun(x,ix)-landFun2(x,ix)),p),0,n-1,full_output=1)[0]*(rmax-rmin)/100
        else:
            quadInt = quad(lambda x:np.power(landFun(x,ix),p),0,n-1,full_output=1)[0]*(rmax-rmin)/100
        pQuadInt = np.power(quadInt,1/p)
        quadInts[ix] = pQuadInt
    return(quadInts)

def LandscapePnorm( ts, p, ts2=None, dim=1, maxrad = 1, t_min=None, t_max=None, n_points=100 ):
    # compute L^p norm of all landscapes from multidimensional timeseries (or just set of points)
    # input:
    # ts - m x d numpy array, one timeseries (or just point coordinates) per column (d series), m samples per series 
    # p - integer (type of L^p norm to compute)
    # ts2 - m x d numpy array, one timeseries (or just point coordinates) per column (d series), m samples per series
    #     if this is set, then the L^p distance will be returned
    # dim - dimension of the persistence diagram from which to derive the landscape
    # maxrad - max distance between pairwise points to consider for the Rips complex 
    # t_min, t_max (int or None) - same as inputs to dgm2landscape; if universal start/stop radii are desired for computing landscapes
    #                              setting these to fixed values might reduce computation time a little bit
    # n_points (int) - same as input to dgm2landscape; number of data points for landscape functions
    # output:
    # float32 - the L^p norm of the landscapes or difference
    # need to go one dim up, above the one we want, as otherwise get free hom group at the top of the chains.
    frips  = d.fill_rips(np.array(ts,dtype='float32'),dim+1,maxrad)
    mrips  = d.homology_persistence(frips)
    dgms   = d.init_diagrams(mrips, frips)
    # acquire birth/death pairs
    if len(dgms)<=dim:
        # set the only birth/death pair to (0.0,0.0) if no gens found
        bd = np.zeros((1,2))
    else:
        bd     = dgm2array(dgms[dim])
    # acquire landscape functions
    landscapes,rstart,rend = dgm2landscape(bd,t_min=t_min,t_max=t_max,n_points=n_points)
    if ts2 is not None:
        frips2 = d.fill_rips(np.array(ts2,dtype='float32'),dim+1,maxrad)
        mrips2 = d.homology_persistence(frips2)
        dgms2  = d.init_diagrams(mrips2, frips2)
        if len(dgms2)<=dim:
            # set the only birth/death pair to (0.0,0.0) if no gens found
            bd2 = np.zeros((1,2))
        else:
            bd2     = dgm2array(dgms2[dim])
        landscapes2,rstart2,rend2 = dgm2landscape(bd2,t_min=t_min,t_max=t_max,n_points=n_points)
        # compute L^p distances
        lpnorms = LpNorms(landscapes,min(rstart,rstart2),max(rend,rend2),landscapes2=landscapes2,p=p)
    else:
        # compute L^p norms
        lpnorms = LpNorms(landscapes,rstart,rend,p=p)
    normie = LandscapeLpNorm(lpnorms,p=p)
    return(normie) 

# utility function needed for streamingLandscapePnorms
def worker_bee(inQ,outQ,local_ts,local_ts2,p,dim,maxrad,t_min,t_max,n_points):
    ix = inQ.get()
    normie = LandscapePnorm( local_ts, p, ts2=local_ts2, dim=dim, maxrad = maxrad, t_min=t_min, t_max=t_max, n_points=n_points )
    outQ.put((ix,normie))
    inQ.task_done()
    #print(ix,' done ',normie)
    return()

# this is useful for timeseries analysis...
def streamingLandscapePnorms( ts, w, dt, p, delta=False, dim=1, maxrad = 1, t_min=None, t_max=None, n_points=100, cpus=None ):
    # compute L^p norm of all landscapes from multidimensional timeseries (or just set of points)
    # rows = features
    # input:
    # ts - m x d numpy array, one timeseries (or just point coordinates) per column (d series), m samples per series 
    # w - width of window to compute persistence on, will take up to this number of samples
    # dt - number of samples to skip between points.
    # p - integer (type of L^p norm to compute)
    # delta - if true, will compute the distance between L^p norms on the window stepping schedule defined by dt
    # dim - dimension of the persistence diagram from which to derive the landscape
    # maxrad - max distance between pairwise points to consider for the Rips complex 
    # t_min/t_max/n_points: same as in LandscapePnorm
    # cpus (int) - number of CPU processes to use (uber fast).
    # output:
    # v_x (numpy array): indices where samples were taken from ts
    # s   (numpy array): the norms or distances computed at the corresponding index 
    t0 = time.time()
    results = []
    if cpus is None:
        threads = mp.cpu_count()
    else: 
        threads = cpus
    n_ts = ts.shape[0]
    n_tsMinusw = n_ts-w
    # skip, hop, jump cpus
    idx  = np.arange(0 if not delta else dt,n_tsMinusw,threads*dt)
    idxLast = idx[-1]
    #manager = mp.Manager()
    outQ = mp.Queue()
    inQ  = mp.JoinableQueue()
    ### START MULTIPROCESSING ###
    #if __name__=='__main__':
    for ix in tqdm.tqdm(idx,ncols=80):
        for thread_idx in np.arange(ix,min(ix+threads*dt,n_tsMinusw),dt):
            inQ.put(thread_idx)
        for thread_idx in np.arange(ix,min(ix+threads*dt,n_tsMinusw),dt):
            #print(thread_idx)
            #inQ.put(thread_idx)
            thread = mp.Process( target = worker_bee, args = (inQ,outQ,\
                        ts[thread_idx:thread_idx+w],None if not delta else \
                        ts[thread_idx-dt:thread_idx+w-dt],p,dim,maxrad,t_min,t_max,n_points ))
            thread.start()
        #print("*"*40)
        inQ.join()
        if ix%10==0 or ix==idxLast:
            results = results + sorted([outQ.get() for i in range(outQ.qsize())])
    ### END MULTIPROCESSING ###
    dtime = time.time()-t0
    print("{} results in {:.2f}(s), {:.2f} results/sec".format(len(results),dtime,len(results)/dtime))
    return(results)
# this is the analogue of the Wasserstein p-distance, as defined on persistence diagrams 
def LandscapeLpNorm( LpNorms, p = 1 ):
    # input: LpNorms (numpy array), output from function LpNorms as above
    # output: L^p norm of landscape functions from the data (float)
    if 1<p<np.inf:
        normie = np.power(np.sum(np.power(LpNorms,p)),1/p)
    elif p==1:
        normie = np.sum(LpNorms)
    elif p==np.inf:
        normie = np.max(LpNorms)
    else:
        print("Error, p must be 1<=p<=np.inf")
        return(None)
    return(normie)

# HELPER FUNCTIONS
def mean_chi( dof ):
    # returns expected norm of vector whose components follow an N(0,1) distribution using the relation between the gamma function and the chi distribution
    return( special.gamma( (dof+1)/2 )/special.gamma( dof/2 ) * np.sqrt(2) )

def mean_dist( dof ):
    # returns expected distance between two RVs which are vectors whose components follow an N(0,1) distribution
    return( 2*mean_chi(dof) )



        



    
