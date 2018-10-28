package maas.tutorials;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.Random;
import java.util.*;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent {
	// The title of the book to buy
	Random rand = new Random();
	private int MIN_ORDERS = 3;
	// private String[] targetBookTitles =  {"Book1","Book5"};
	private String targetBookTitle = "";
	// The list of known seller agents
	private AID[] sellerAgents;
	// Counter 
	private Integer counter = 0;
	private Integer choice = 0;
	private Integer bookType = 0;
	private static boolean shutdownRequested = false;
	private List<Orders> status = new ArrayList<Orders>();

	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
		try {
 			Thread.sleep(6000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
		final String DELIMITER = "@";
		String name = getAID().getName().split(DELIMITER)[0];
		// Add a TickerBehaviour that schedules a request to seller agents every minute
		addBehaviour(new TickerBehaviour(this, 600) {
			protected void onTick() {
				// choice = rand.nextInt(7) + 1;
				// targetBookTitle = "Book"+choice;
				// System.out.println("Trying to buy "+targetBookTitle);
				// Update the list of seller agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("book-selling");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					// System.out.println("Found the following seller agents:");
					sellerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						sellerAgents[i] = result[i].getName();
						// System.out.println(sellerAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}

				// Perform the request
				myAgent.addBehaviour(new RequestPerformer());
			}
		} );
	}
	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		final String DELIMITER = "@";
		String name = getAID().getName().split(DELIMITER)[0];
		StringBuilder st = new StringBuilder();
		st.append("**********************************************************************************************************************************\n");
        st.append("********************************************** "+name+ " Order Status ***************************************************************\n");
        st.append("**********************************************************************************************************************************\n");
		for(int i=0; i<status.size(); i++) {
			st.append(""+(i+1)+")");
			st.append(" Title :"+status.get(i).title);
			st.append("    ,Price :"+status.get(i).price);
			st.append("    ,Order Status :"+status.get(i).orderStatus);
			st.append("    ,Supplier :"+status.get(i).supplier.split(DELIMITER)[0]);
			if (status.get(i).title.contains("E-Book")) {
				st.append("    ,Type : Softcopy/ebook\n");
			} else {
				st.append("    ,Type : Hardcopy/Paperback\n");
			}
		}
		st.append("Buyer Agent "+name+" will terminate now !\n");
		st.append("**********************************************************************************************************************************");
		System.out.println(st.toString());
	}

	private class Orders {
		private String title;
		private String orderStatus;
		private String supplier;
		private String price;
	}
	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller 
	   agents the target book.
	 */
	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // The agent who provides the best offer 
		private int bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private Orders currentOrder;
		private boolean flag = false;
		private int refuseCnt = 0;

		public void action() {
			switch (step) {
			case 0:
				choice = rand.nextInt(7) + 1;
				bookType = rand.nextInt(2);
				if (bookType == 0) {
					targetBookTitle = "Book"+choice;
				} else {
					targetBookTitle = "E-Book"+choice;
				}
				refuseCnt = 0;
				repliesCnt = 0;
				bestSeller = null; // The agent who provides the best offer 
				bestPrice = 0;  // The best offered price

				final String DELIMITER = "@";
				String name = getAID().getName().split(DELIMITER)[0];
				// System.out.println("____________________________________________________________________________");
				// System.out.println(""+name+" is searching for "+targetBookTitle+" ---------------->");
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				} 
				cfp.setContent(targetBookTitle);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				// System.out.println("\t Message Received"+repliesCnt+" "+refuseCnt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int price = Integer.parseInt(reply.getContent());
						// System.out.println("proposals Received"+price);
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							// System.out.println("Update Price"+bestPrice);
							bestSeller = reply.getSender();
						}
					} else if (reply.getPerformative() == ACLMessage.REFUSE) {
						// System.out.println("proposals Refused");
						refuseCnt++;
						// currentOrder = new Orders();
						// currentOrder.title = targetBookTitle;
						// currentOrder.supplier = "Cannot be found";
						// currentOrder.price = "Cannot be found";
						// currentOrder.orderStatus = "Failed";
						// status.add(currentOrder);
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						// We received all replies
						// System.out.println("\tReceived all replies");
						if (refuseCnt >= sellerAgents.length) {
							// System.out.println("\tEveryone refused");
							// System.out.println(""+targetBookTitle+" could not be located");
							currentOrder = new Orders();
							currentOrder.title = targetBookTitle;
							currentOrder.supplier = "Cannot be found";
							currentOrder.price = "Cannot be found";
							currentOrder.orderStatus = "Failed";
							status.add(currentOrder);
							step = 0;
						} else { 
							step = 2;
						} 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(targetBookTitle);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						// System.out.println(targetBookTitle+" successfully purchased from agent "+reply.getSender().getName());
						// System.out.println("Price = "+bestPrice);
						counter += 1;
						flag = true;
					} else {
						// System.out.println("Attempt failed: requested book already sold.");
					}
					if (counter >= MIN_ORDERS) {
						step = 4;
						name = getAID().getName().split("@")[0];
						if (!shutdownRequested) {
							myAgent.addBehaviour(new shutdown());
							shutdownRequested = true;
						}
						// myAgent.doDelete();
						// System.out.println("******************BUYER : "+getAID().getName()+" is done******************");
					} else {
						step = 0;
					}
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && bestSeller == null) {
				currentOrder = new Orders();
				currentOrder.title = targetBookTitle;
				currentOrder.supplier = "Not Available";
				currentOrder.price = "Not Available";
				currentOrder.orderStatus = "Failed";
				status.add(currentOrder);
				// System.out.println("Attempt failed: "+targetBookTitle+" not available for sale");
			} else if (flag){
				flag = false;
				currentOrder = new Orders();
				currentOrder.title = targetBookTitle;
				currentOrder.supplier = bestSeller.getName();
				currentOrder.price = "" + bestPrice;
				currentOrder.orderStatus = "Success";
				status.add(currentOrder);
			}
			// System.out.println("\t Truth Condition "+((step == 2 && bestSeller == null) || (step == 4)));
			return ((step == 2 && bestSeller == null) || (step == 4));
		}
	}  // End of inner class RequestPerformer

    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			try {
	 			Thread.sleep(1000);
	 		} catch (InterruptedException e) {
	 			//e.printStackTrace();
	 		}
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
			    myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
			    myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
			    //LOGGER.error(e);
			}

		}
	}
}
