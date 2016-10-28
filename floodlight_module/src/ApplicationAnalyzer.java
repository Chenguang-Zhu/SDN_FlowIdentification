package net.floodlightcontroller.csc2229;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwTos;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import sun.misc.BASE64Encoder;

import java.lang.System;

public class ApplicationAnalyzer implements IOFMessageListener, IFloodlightModule
{
	protected IFloodlightProviderService floodlightProvider;
	
	protected static Logger logger;
	
	protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 600; // in seconds 
	protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite 
	protected static short FLOWMOD_PRIORITY = 100; 

	
	@Override
	public String getName() 
	{
		return  ApplicationAnalyzer.class.getSimpleName();
	}
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) 
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() 
	{
		//Tell module dependency relationships to the system.
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException 
	{
		//This method will be called when controller starts, Initiate some variables.
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(ApplicationAnalyzer.class);
	}
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException 
	{
		//bind PACKET_IN message to listener.
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		// We only care about packet-in messages
		if (msg.getType() != OFType.PACKET_IN) 
		{ 
			// Allow the next module to also process this OpenFlow message
			return Command.CONTINUE;
		}
		//It is packet-in massage
		if(msg.getType() == OFType.PACKET_IN)
		{
			//get ethernet frame
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			// get src and dst mac address
			MacAddress srcMacAddr = eth.getSourceMACAddress();
			MacAddress dstMacAddr = eth.getDestinationMACAddress();
			//if its payload is IPv4, get IPv4 packet
			if (eth.getEtherType() == EthType.IPv4) 
	        {
	            //We got an IPv4 packet; get the payload from Ethernet
	            IPv4 ipv4 = (IPv4) eth.getPayload();
	            //get src and dst ip address
	            IPv4Address srcIP = ipv4.getSourceAddress();
	            IPv4Address dstIP = ipv4.getDestinationAddress();
	            //if its payload is TCP, get TCP segment
	            if (ipv4.getProtocol().equals(IpProtocol.TCP))
	            {
	                // We got a TCP packet; get the payload from IPv4
	                TCP tcp = (TCP) ipv4.getPayload();
	                //get source port and dest port
	                TransportPort srcPort = tcp.getSourcePort();
	                TransportPort dstPort = tcp.getDestinationPort();
	                
	                boolean canWePushRule = false;
	                int priority = 0;
	    			OFMessage outMessage;
	    			
	    			logger.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
	    			logger.info("offset="+tcp.getDataOffset());
	    			logger.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
	    			
	    			Data d = (Data)tcp.getPayload();
	    			byte[] data = d.getData();
	    			
//	    			//indentify TCP flow type
//	    			if(dpi_isHttpProtocol(data))
//	    			{
//	    				canWePushRule = true;
//	    				priority = 100;
//	    			}
//	    			else if(dpi_isSSHProtocol(data))
//	    			{
//	    				canWePushRule = true;
//	    				priority = 200;
//	    			}
//	    			else if(dpi_isOpenVPNProtocol(data))
//	    			{
//	    				canWePushRule = true;
//	    				priority = 150;
//	    			}
	    			
	    			if(!canWePushRule)// send packet_out
	    			{
	    				if(srcIP.toString().equals("192.168.3.2"))
	    				{
	    					logger.info("we can't indentify this packet!");
	    				
	    					//outMessage = sendPacketOut(sw, msg);	
		    				
		    				outMessage = sendFlowMod_TCP(sw, msg, eth, srcMacAddr, dstMacAddr, ipv4, srcPort, dstPort, 10);
		    				sw.write(outMessage);
	    				}
	    			}
	    			else //push rule
	    			{
	    				//if this packet is sent by client 192.168.3.2
	    				if(srcIP.toString().equals("192.168.3.2"))
	    				{
	    					logger.info("we can indentify this packet!");
		    				
		    				outMessage = sendFlowMod_TCP(sw, msg, eth, srcMacAddr, dstMacAddr, ipv4, srcPort, dstPort, priority);
		    				sw.write(outMessage);
	    				}
	    				
	    				
	    			}
	    			
	    			
	            }
	            //if its payload is UDP, get UDP segment
	            else if(ipv4.getProtocol().equals(IpProtocol.UDP))
	            {
	            	UDP udp = (UDP) ipv4.getPayload();
	                //get source port and dest port
	                TransportPort srcPort = udp.getSourcePort();
	                TransportPort dstPort = udp.getDestinationPort();
	            }
	            else if(ipv4.getProtocol().equals(IpProtocol.ICMP))
	            {
	            	ICMP icmp = (ICMP) ipv4.getPayload();
	            }
	        }
			
			
			
			
			return Command.CONTINUE;

		}
		
		
        
        return Command.CONTINUE; // This line is to make sure that other listeners can deal with events.
	}
	
	private OFMessage sendPacketOut(IOFSwitch sw, OFMessage msg) 
	{ 
		logger.info("send packet out: "+msg.toString());
		
		OFPacketIn pi = (OFPacketIn) msg;
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0
				? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));

		// set actions
		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
		actionBuilder.setPort(OFPort.FLOOD);
		pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

		// set data if it is included in the packetin
		if (pi.getBufferId() == OFBufferId.NO_BUFFER) 
		{
			byte[] packetData = pi.getData();
			pob.setData(packetData);
		}
		return pob.build();   
	} 
	
	private OFMessage sendFlowMod_TCP(IOFSwitch sw, OFMessage msg, Ethernet eth, MacAddress srcMac, MacAddress dstMac, IPv4 ipv4, TransportPort srcPort, TransportPort dstPort, int priority)
	{ 
		logger.info("TCP message: srcip="+ipv4.getSourceAddress()+", dstip="+ipv4.getDestinationAddress()+", srcport="+srcPort+", dstport="+dstPort);
		
		//set flowtable entry
		OFPacketIn pi = (OFPacketIn) msg;
		OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
		fmb.setBufferId(pi.getBufferId()).setXid(pi.getXid());
		fmb.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT);
		fmb.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT);
		fmb.setPriority(priority);
		
		//set match
		Match.Builder mb = sw.getOFFactory().buildMatch();
		
		mb
//		.setExact(MatchField.ETH_SRC, srcMac)
//		.setExact(MatchField.ETH_DST, dstMac)
		.setExact(MatchField.ETH_TYPE, eth.getEtherType())
		.setExact(MatchField.IPV4_SRC, ipv4.getSourceAddress())
		.setExact(MatchField.IPV4_DST, ipv4.getDestinationAddress())
		.setExact(MatchField.IP_PROTO, ipv4.getProtocol()); 
//		.setExact(MatchField.IP_PROTO, IpProtocol.UDP); //hard code
		
		if (mb.get(MatchField.IP_PROTO).equals(IpProtocol.TCP)) 
		{
			TCP tcp = (TCP) ipv4.getPayload();
			mb.setExact(MatchField.TCP_SRC, tcp.getSourcePort()).setExact(MatchField.TCP_DST, tcp.getDestinationPort());
		}
		
		if (mb.get(MatchField.IP_PROTO).equals(IpProtocol.UDP)) 
		{
			UDP udp = (UDP) ipv4.getPayload();
			mb.setExact(MatchField.UDP_SRC, udp.getSourcePort()).setExact(MatchField.UDP_DST, udp.getDestinationPort());
		}
		
//		TransportPort srcP = TransportPort.of(19233);//hard code skype=19233  bittorrent=
//		mb.setExact(MatchField.UDP_SRC, srcP);//hard code

		Match m = mb.build();
		fmb.setMatch(m);
		
		assert(mb.get(MatchField.IPV4_DST) != null);
		logger.info("-----------"+mb.get(MatchField.ETH_SRC));
		logger.info("-----------"+mb.get(MatchField.ETH_DST));
		logger.info("-----------"+mb.get(MatchField.IPV4_SRC));
		logger.info("-----------"+mb.get(MatchField.IPV4_DST));
		logger.info("-----------"+mb.get(MatchField.TCP_SRC));
		logger.info("-----------"+mb.get(MatchField.TCP_DST));
		logger.info("-----------"+mb.get(MatchField.ETH_TYPE));
		logger.info("-----------"+mb.get(MatchField.IP_PROTO));
		
		logger.info("-----------"+m.get(MatchField.ETH_SRC));
		logger.info("-----------"+m.get(MatchField.ETH_DST));
		logger.info("-----------"+m.get(MatchField.IPV4_SRC));
		logger.info("-----------"+m.get(MatchField.IPV4_DST));
		logger.info("-----------"+m.get(MatchField.TCP_SRC));
		logger.info("-----------"+m.get(MatchField.TCP_DST));
		logger.info("-----------"+m.get(MatchField.ETH_TYPE));
		logger.info("-----------"+m.get(MatchField.IP_PROTO));

		// set actions
		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
		//actionBuilder.setPort(OFPort.FLOOD);
		
		//set outface
		if(pi.getInPort().getPortNumber() == 1)
		{
			OFPort outface = OFPort.of(2);
			actionBuilder.setPort(outface);
		}
		else if(pi.getInPort().getPortNumber() == 2)
		{
			OFPort outface = OFPort.of(1);
			actionBuilder.setPort(outface);
		}
		
		logger.info("=================================================");
		//logger.info(sw.getPorts().toString());
		logger.info("incoming from :"+pi.getInPort());
		logger.info("=================================================");
		
		// set tos action
		//OFActionSetNwTos.Builder tosBuilder = sw.getOFFactory().actions().buildSetNwTos();
		//tosBuilder.setNwTos(tos);// can we see the result of setting tos value?
		
		fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));//we haven't set tos action yet.
		
		logger.info("we write a flow rule !!!");
		logger.info("---------------------------------------------------------------------");
		//logger.info("-----------"+fmb.getMatch().createBuilder().get(MatchField.ETH_SRC));
		//logger.info("-----------"+fmb.getMatch().createBuilder().get(MatchField.ETH_DST));
		logger.info("---------------------------------------------------------------------");
		
		OFFlowAdd add = fmb.build();
		
		logger.info("-----------"+add.getMatch().get(MatchField.ETH_SRC));
		logger.info("-----------"+add.getMatch().get(MatchField.ETH_DST));
		logger.info("-----------"+add.getMatch().get(MatchField.IPV4_SRC));
		logger.info("-----------"+add.getMatch().get(MatchField.IPV4_DST));
		logger.info("-----------"+add.getMatch().get(MatchField.TCP_SRC));
		logger.info("-----------"+add.getMatch().get(MatchField.TCP_DST));

		return add;
	}
	
	//judge if http
	private boolean dpi_isHttpProtocol(byte[] data)
	{
		logger.info("data length = "+data.length);
		String dataStr = "";
		
		dataStr = new String(data);
		
		
		logger.info("++++++++++++++++++++++++++++++++++++++++++++++");
		logger.info(dataStr);
		logger.info("++++++++++++++++++++++++++++++++++++++++++++++");
		
	    /* If it's got HTTP in the request (HTTP/1.1) then it's HTTP */
	    if (dataStr.indexOf("HTTP") != -1)
	    {
	        return true;
	    }
	    
	    if (dataStr.indexOf("OPTIONS") != -1 || dataStr.indexOf("GET") != -1 || dataStr.indexOf("HEAD") != -1
	    	|| dataStr.indexOf("POST") != -1 || dataStr.indexOf("PUT") != -1 || dataStr.indexOf("DELETE") != -1
	    	|| dataStr.indexOf("TRACE") != -1 || dataStr.indexOf("CONNECT") != -1) 
	    {
	    	return true;
	    }
		return false;
	}
	
	//judge if SSH
	private boolean dpi_isSSHProtocol(byte[] data)
	{
		String dataStr = new String(data);
		if (dataStr.length() < 4)
		{
			return false; 
		}
		if(dataStr.startsWith("SSH-"))
		{
			return true;
		}
		return false;
	}
	
	//judge if OpenVPN
	private boolean dpi_isOpenVPNProtocol(byte[] data)
	{
		String dataStr = new String(data); 
		 
		if (dataStr.length() < 2) 
		{
			return false;
		}
			
		byte[] buf = new byte[2];
		for (int i=0;i<2;i++)
		{
			buf[i] = data[i];
		}
		
		ByteArrayInputStream bintput = new ByteArrayInputStream(buf);
		DataInputStream dintput = new DataInputStream(bintput);
		int packetLen = 0;
		try 
		{
			packetLen = dintput.readInt();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return (packetLen == dataStr.length()-2);
	}
	
	public boolean judgeByPortnumber(IOFSwitch sw, OFMessage msg, FloodlightContext cntx)
	{
		/* Retrieve the deserialized packet in message */
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        
        /* Various getters and setters are exposed in Ethernet */
        MacAddress srcMac = eth.getSourceMACAddress();
        VlanVid vlanId = VlanVid.ofVlan(eth.getVlanID());

        /* 
         * Check the ethertype of the Ethernet frame and retrieve the appropriate payload.
         * Note the shallow equality check. EthType caches and reuses instances for valid types.
         */
        if (eth.getEtherType() == EthType.IPv4) 
        {
            /* We got an IPv4 packet; get the payload from Ethernet */
            IPv4 ipv4 = (IPv4) eth.getPayload();

            /* Various getters and setters are exposed in IPv4 */
            byte[] ipOptions = ipv4.getOptions();
            IPv4Address dstIp = ipv4.getDestinationAddress();

            /* 
             * Check the IP protocol version of the IPv4 packet's payload.
             * Note the deep equality check. Unlike EthType, IpProtocol does
             * not cache valid/common types; thus, all instances are unique.
             */

            if (ipv4.getProtocol().equals(IpProtocol.TCP))
            {
                /* We got a TCP packet; get the payload from IPv4 */
                TCP tcp = (TCP) ipv4.getPayload();

                /* Various getters and setters are exposed in TCP */
                TransportPort srcPort = tcp.getSourcePort();
                TransportPort dstPort = tcp.getDestinationPort();
                short flags = tcp.getFlags();
                
                /* Your logic here! */
                if(dstPort.getPort() == 80)//HTTP
                {
                	logger.info("Client sent a HTTP!!!");
                }
                else if(dstPort.getPort() == 23)//telnet
                {
                	logger.info("Client sent a telnet!!!");
                }
                else if(dstPort.getPort() == 21)//FTP-control port
                {
                	logger.info("Client sent a ftp control!!!");
                }
                else if(dstPort.getPort() == 25)//SMTP
                {
                	logger.info("Client sent a SMTP!!!");
                }
                else if(dstPort.getPort() == 35)//DNS
                {
					 logger.info("Client sent a DNS!!!");
				}
                
                
                //Serialize the TCP packet:
                byte[] byte_sequence = tcp.serialize();
                
                //TCP default header length = 5, so usually data is from byte[5].
                byte[] application_data = new byte[byte_sequence.length-5];
                for(int i=0; i<application_data.length;i++)
                {
                	application_data[i] = byte_sequence[i+5];
                }
                
                

            } 
            else if (ipv4.getProtocol().equals(IpProtocol.UDP)) 
            {
                /* We got a UDP packet; get the payload from IPv4 */
                UDP udp = (UDP) ipv4.getPayload();

                /* Various getters and setters are exposed in UDP */
                TransportPort srcPort = udp.getSourcePort();
                TransportPort dstPort = udp.getDestinationPort();

                /* Your logic here! */
                
             
                

            }
            else if (ipv4.getProtocol().equals(IpProtocol.ICMP))
            {
            	ICMP icmp = (ICMP) ipv4.getPayload();
            	logger.info("ICMP!!!!!!!!!"+ "code:" + icmp.getIcmpCode() + "type:" + icmp.getIcmpType());
            }

            

        } 
        else if (eth.getEtherType() == EthType.ARP)
        {
            /* We got an ARP packet; get the payload from Ethernet */
            ARP arp = (ARP) eth.getPayload();
            

  

        } 
        else
        {
            /* Unhandled ethertype */
        }
        
        return false;
	}
}
