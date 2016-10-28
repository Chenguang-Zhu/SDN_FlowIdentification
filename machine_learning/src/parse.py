import sys
import re
def parse(cfile):
  packets = []

  lines = None
  with open(cfile) as f:
    lines = f.readlines()
  
  pkt = None
  for l in lines:
    if 'static const unsigned char pkt' in l:
      if pkt is not None:
        packets.append(pkt)
        #print "New packet len: " + str(len(pkt)) 
        #sys.stdout.write('\rTotal num of packets: %d' % (len(packets)))
        #sys.stdout.flush()
      pkt = []
      continue

    seg = l.split(',')
    length = len(seg)
    for i in xrange(8):
      if '0x' not in seg[i]:
        break
      
      rawChars = seg[i].split()[0].strip()
 
      #print 'Appending ' + rawChars  
      curr = int(rawChars, 0)
      if curr > 127 or curr < 0:
        curr1 = str(curr)
      else:
        curr1 = re.escape(str(unichr(curr)))
      pkt.append(curr1)
      if i + 1 == length:
        break

  return packets

'''
startIndex is inclusive.
endIndex is exclusive
'''
def strip(packets, startIndex, endIndex, isRaw=False):
  newPackets = []
  for pkt in packets:
    pkt = pkt[startIndex:endIndex]
    if not isRaw:
      if (len(pkt) < endIndex - startIndex):
        continue
    #print 'Appending ' + str(pkt) 
    newPackets.append(pkt)
  return newPackets
"""
def strip(packets, start1, end1, start2, end2, isRaw=False):
  newPackets = []
  for pkt in packets:
    pkt = pkt[start1:end1] + pkt[start2:end2]
    if not isRaw:
      if (len(pkt) < end2 - start2 + end1 - start1):
        continue
    #print 'Appending ' + str(pkt) 
    newPackets.append(pkt)
  return newPackets
"""

def sample(packets, max_num, min_num=0):
  packets = packets[min_num:max_num]
  #print 'Size after sample: ' + str(len(packets))
  for pkt in packets:
    print "START "+ "|".join(pkt) + "END"
  return packets
 

if __name__ == '__main__':
  packets = parse('c2')
  print "Total packet len: " + str(len(packets)) 
  packets = strip(packets, 54, 64)
  packets = sample(packets, 5000)
  #print packets
