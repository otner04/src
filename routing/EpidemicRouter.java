/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;



/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouter extends ActiveRouter {

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * 
	 * @param s The settings object
	 */
	
	public static final double SET_INIT = 0.3;
	
	List<Integer> list = new ArrayList<Integer>(); // すれ違ったIDを保存
	private Map<Integer,Coord> locus= new LinkedHashMap<>(); //時間と座標を格納
//	private Map<Integer,Coord> cp= new LinkedHashMap<>(); //時間と座標を格納
	List<Coord> cp = new ArrayList<Coord>(); // 未来予想図
	
	Map<Integer,Coord> l1 = new LinkedHashMap<>();
	Map<Integer,Coord> l2 = new LinkedHashMap<>();

	
    Map<Integer,Map<Integer,Coord>> week = new LinkedHashMap<>(); //週ごとに格納
    private int i = 1;

	
	public EpidemicRouter(Settings s) {
		super(s);
		// TODO: read&use epidemic router specific settings (if any)
	}

	/**
	 * Copy constructor.
	 * 
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicRouter(EpidemicRouter r) {
		super(r);
		// TODO: copy epidemic settings here (if any)
	}
	
	public boolean jaccard(List<Integer> list_a,List<Integer> list_b) {
		Set<Integer> intersection = new HashSet<Integer>(list_a);
		intersection.retainAll(new HashSet<Integer>(list_b)); //積集合を作成
		int len_int = intersection.size();
		
		Set<Integer> union = new HashSet<Integer>(list_a);
		union.addAll(new HashSet<Integer>(list_b)); //和集合を作成
		int len_uni = union.size();
		
		double jaccard = (double)len_int/(double)len_uni;
		
		
		if(jaccard<SET_INIT) return true;
		else return false;
	}
	
	public boolean dice(List<Integer> list_a,List<Integer> list_b) {
		Set<Integer> intersection = new HashSet<Integer>(list_a);
		intersection.retainAll(new HashSet<Integer>(list_b)); //積集合を作成
		int len_int = intersection.size()*2; //積集合の要素*2
		int list_sum = list_a.size()+list_b.size();
		
		
		double dice = (double)len_int/(double)list_sum;
		
		
		if(dice<SET_INIT) return true;
		else return false;
	}
	
	public boolean simpson(List<Integer> list_a,List<Integer> list_b) {
		Set<Integer> intersection = new HashSet<Integer>(list_a);
		intersection.retainAll(new HashSet<Integer>(list_b)); //積集合を作成
		int len_int = intersection.size();
		int list_min=list_a.size();
		if(list_b.size()>0) list_min = Math.min(list_a.size(),list_b.size());
		
		
		
		
		double simpson = (double)len_int/(double)list_min;
		
		
		if(simpson<SET_INIT) return true;
		else return false;
	}
	
	
	public List<Coord> reg(Map<Integer,Map<Integer,Coord>> week, Integer i) {
//		Map<Integer,Coord> cp= new LinkedHashMap<>(); //時間と座標を格納
		List<Coord> cp = new ArrayList<Coord>(); // すれ違ったIDを保存
		int count=0;
		
//		for(Map<Integer,Coord> k:week.values()) {
//			System.out.println(k.size());
//		}

		
		l1 = week.get(week.size());
		
		l2 = week.get(week.size()-1);

//		System.out.println(l1.size());

		for(Coord q:l1.values()) {
			cp.add(q);
		}
		
		for(Coord p:l2.values()) {
			double newX = 2*cp.get(count).getX()-p.getX();
			double newY = 2*cp.get(count).getY()-p.getY();
			cp.add(count,new Coord(newX,newY));
			count+=1;
		}
		
		
		
		
		
		return cp;
	}
	
	
	public boolean Rpredict(List<Coord> cp,List<Coord> cp2) {
		int simcount=0;
//		for(int n = 0;n<cp.size()||n<cp2.size();n++) {
//			if(cp.size()>=6&&cp2.size()>=6&&cp2.get(n).distance(cp.get(n))<=100) {
//				simcount+=1;
//			}
//			
//		}
//		System.out.println("cp:"+cp.size()+"     cp2:"+cp2.size());
		
		if((double)simcount/(double)cp.size()>=0.5) {
			return true;
		}else return false;
		
	}
	
	

	@Override
	public void update() {
//		int n;
		if(!locus.containsKey(SimClock.getIntTime())) {
			locus.put(SimClock.getIntTime(), this.getHost().getLocation());
//			if(this.getHost().getAddress()==0)System.out.println(locus.size());
//			if(this.getHost().getAddress()==0)System.out.println(SimClock.getIntTime());
			if(locus.size()>35) {
				week.put(i, locus);
				if(week.size()>=2) {
//					cp=reg(week,i);
//					if(this.getHost().getAddress()==0)System.out.println("予測値のリストの数："+cp.size());

				}
//				for(Map<Integer,Coord> k:week.values()) {
//					System.out.println(k.size());
//				}
				if(this.getHost().getAddress()==0)System.out.println(week.size()+" "+i);
				i+=1;
				locus.clear();
				}
			
		}
		
		
		

		
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		
		tryOtherMessages();

	}
	
	
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/*
		 * for all connected hosts collect all messages that have a higher
		 * probability of delivery by the other host
		 */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			EpidemicRouter othRouter = (EpidemicRouter) other.getRouter();
			
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
				
			}
			if(!list.contains(othRouter.getHost().getAddress())) {
				list.add(othRouter.getHost().getAddress());
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if (dice(list,othRouter.list)) { //
					// the other node has higher probability of delivery
					messages.add(new Tuple<Message, Connection>(m, con));
				}
			}
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		return tryMessagesForConnected(messages); // try to send messages
	}
	
	

	@Override
	public EpidemicRouter replicate() {
		return new EpidemicRouter(this);
	}
	
	
	

}