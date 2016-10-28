import numpy as np
import pickle 
from sklearn.cluster import KMeans
from parse import *

def predict(X):
  k_means = KMeans(init='k-means++', n_clusters=8, n_init=10)
  k_means.fit(X)
  k_means_labels = k_means.labels_
  k_means_cluster_centers = k_means.cluster_centers_
  k_means_labels_unique = np.unique(k_means_labels)
  print k_means_labels 
  print k_means_cluster_centers
  print k_means_labels_unique 


  result = k_means.predict(X)
  # for 1- 3000
  dic1 = {}
  for r in result[1:3000]:
    if r not in dic1:
      dic1[r] = 1
    else:
      count = dic1[r] 
      dic1[r] = count + 1
  print dic1
 
  dic2 = {}  
  for r in result[3001:6000]:
    if r not in dic2:
      dic2[r] = 1
    else:
      count = dic2[r] 
      dic2[r] = count + 1
  print dic2

  dic3 = {}  
  for r in result[6001:9000]:
    if r not in dic3:
      dic3[r] = 1
    else:
      count = dic3[r] 
      dic3[r] = count + 1
  print dic3

  dic4 = {}  
  for r in result[9001:12000]:
    if r not in dic4:
      dic4[r] = 1
    else:
      count = dic4[r] 
      dic4[r] = count + 1
  print dic4

if __name__ == '__main__':
  e = 7
  sam = 3000
  packets1 = parse('tcp_port_80')
  #packets1 = strip(packets1, 54, 154)
  #with open('pkt1.pickle', 'wb') as handle:
  #  pickle.dump(packets1, handle)
  #with open('pkt1.pickle', 'rb') as handle:
  #  packets1 = pickle.load(handle)
  packets1 = strip(packets1, 35, 37, 54, 57 + e)
  packets1 = sample(packets1, sam)
 
  packets2 = parse('ssl')
  #packets2 = strip(packets2, 54, 154)
  #with open('pkt2.pickle', 'wb') as handle:
  #  pickle.dump(packets2, handle)
  #with open('pkt2.pickle', 'rb') as handle:
  #  packets2 = pickle.load(handle)
  packets2 = strip(packets2, 35, 37, 54, 57 + e)
  packets2 = sample(packets2, sam)
 
  packets3 = parse('udp')
  #packets3 = strip(packets3, 42, 142)
  #with open('pkt3.pickle', 'wb') as handle:
  #  pickle.dump(packets3, handle)
  #with open('pkt3.pickle', 'rb') as handle:
  #  packets3 = pickle.load(handle)
  packets3 = strip(packets3, 35, 37, 42, 45 + e)
  packets3 = sample(packets3, sam)
 
  packets4 = parse('udp2')
  #packets4 = strip(packets4, 42, 142)
  #with open('pkt4.pickle', 'wb') as handle:
  #  pickle.dump(packets4, handle)
  #with open('pkt4.pickle', 'rb') as handle:
  #  packets4 = pickle.load(handle)
  packets4 = strip(packets4, 35, 37, 42, 45 + e)
  packets4 = sample(packets4, sam)
 
  X1 = np.array(packets1)
  X2 = np.array(packets2)
  X3 = np.array(packets3)
  X4 = np.array(packets4)

  X = np.concatenate((X1, X2, X3, X4))
  #with open('X.pickle', 'wb') as handle:
  #  pickle.dump(X, handle)
  
  #with open('X.pickle', 'rb') as handle:
  #  X = pickle.load(handle)
  predict(X)
