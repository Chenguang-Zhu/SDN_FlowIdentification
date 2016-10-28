#!/usr/bin/python

"""
CSC2229-topology
"""

from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.log import setLogLevel, info
from mininet.cli import CLI
from mininet.topo import Topo
from mininet.util import quietRun
from mininet.moduledeps import pathCheck

from sys import exit
import os.path
from subprocess import Popen, STDOUT, PIPE

IPBASE = '192.168.1.0/24'
IPCONFIG_FILE = './IP_CONFIG'
IP_SETTING={}

class CSC2229Topo( Topo ):
    "CSC2229 Topology"
    
    def __init__( self, *args, **kwargs ):
        Topo.__init__( self, *args, **kwargs )

        # Add 1 host, 1switch, 1 NAT host
        clientHost = self.addHost( 'client' )
        mainSwitch = self.addSwitch('sw1')
        natGate = self.addNode('nat_gate', inNamespace=False) # NAT gate is not in namespace

        # Add links
        self.addLink(clientHost, mainSwitch)
        self.addLink(mainSwitch, natGate)


class CSC2229Controller( RemoteController ):
    "CSC2229 Controller"

    def __init__( self, name, ip="127.0.0.1", port=6653, **params ):

        """command: controller command name
           cargs: controller command arguments
           cdir: director to cd to before running controller
           ip: IP address for controller
           port: port for controller to listen at
           params: other params passed to Node.__init__()"""

        RemoteController.__init__( self, name, ip=ip, port=port, **params)


def set_default_route(host):
    info('*** setting default gateway of host %s\n' % host.name)

    if host.name == 'client':
        gwip = IP_SETTING['sw1-eth1']
    print host.name, gwip
    #let packets go out from host's eth0, to sw1-eth1
    host.cmd('route add default gw %s dev %s-eth0' % (gwip, host.name))


def get_ip_setting():
    try:
        with open(IPCONFIG_FILE, 'r') as f:
            for line in f:
                if len(line.split()) == 0:
                  break
                name, ip = line.split()
                print name, ip
                IP_SETTING[name] = ip
            info('*** Successfully loaded ip settings for hosts\n %s\n' % IP_SETTING)
    except EnvironmentError:
        exit("Couldn't load config file for ip addresses, check whether %s exists" % IPCONFIG_FILE)


def CSC2229net():
    "Create a simple network for CSC2229"
    get_ip_setting()
    topo = CSC2229Topo()
    info( '*** Creating network in CSC2229\n' )
    net = Mininet( topo=topo, controller=CSC2229Controller, ipBase=IPBASE )
    net.start()

    #set client and switch
    clientHost, mainSwitch = net.get('client', 'sw1')
    client_intf = clientHost.defaultIntf()
    client_intf.setIP('%s/24' % IP_SETTING['client'])

    #Setup VLan Interface for natGate
    natGate = net.get('nat_gate')
    natGate_intf = natGate.defaultIntf()
    natGate_intf.setIP('%s/24' % IP_SETTING['sw1-eth1'])

    for host in [clientHost]:
        set_default_route(host)
    CLI( net )
    net.stop()


if __name__ == '__main__':
    setLogLevel( 'debug' )
    CSC2229net()
