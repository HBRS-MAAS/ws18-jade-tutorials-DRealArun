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
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@SuppressWarnings("serial")
public class BookSellerAgent extends Agent {
	private int MAX_BOOKS = 4;
	private String[] inventory_names = new String[MAX_BOOKS];
	private int[] inventory_prices = new int[MAX_BOOKS];
	private int[] inventory_quantity = new int[MAX_BOOKS];
	// private String[] inventory_names = {"Book1","Book2","Book3","Book4"};
	// private int[] inventory_prices = {40,30,20,10};
	// private int[] inventory_quantity = {40,30,20,30};
	public static int stringToint( String str ){
        int i = 0, number = 0;
        boolean isNegative = false;
        int len = str.length();
        if( str.charAt(0) == '-' ){
            isNegative = true;
            i = 1;
        }
        while( i < len ){
            number *= 10;
            number += ( str.charAt(i++) - '0' );
        }
        if( isNegative )
        number = -number;
        return number;
    }   

	public void getInventory(String arg) {
		String fileToParse = "/home/arun/Workspace/Third Semester/MultiAgent/ws18-jade-tutorials-DRealArun/src/main/java/maas/tutorials/SellerInventory.csv";
        BufferedReader fileReader = null;
         
        //Delimiter used in CSV file
        final String DELIMITER = ",";
        try
        {
            String line = "";
            System.out.println("I am here"+arg);
            //Create the file reader
            fileReader = new BufferedReader(new FileReader(fileToParse));
             
            //Read the file line by line
            while ((line = fileReader.readLine()) != null)
            {
                //Get all tokens available in line
                boolean isFound = line.contains(arg);
                if (isFound){
                	String[] tokens = line.split(DELIMITER);
	                int count = 0;
	                for(int i=1; i<tokens.length; i=i+3) {
	                	inventory_names[count] = tokens[i];
	                	inventory_prices[count] = stringToint(tokens[i+1]);
	                	inventory_quantity[count] = stringToint(tokens[i+2]);
	                	count += 1;
	                } 
            	}
            }
            // System.out.println(inventory_names[0]);
            // System.out.println(inventory_prices[0]);
            // System.out.println(inventory_quantity[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

	}
	// The catalogue of books for sale (maps the title of a book to its price)
	private Hashtable catalogue;
	private Hashtable remaining_inventory;
	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");
		// Create the catalogue
		final String DELIMITER = "@";
		String name = getAID().getName().split(DELIMITER)[0];
		// System.out.println("Name is "+name);
		getInventory(name);
		catalogue = new Hashtable();
		remaining_inventory = new Hashtable();
		// Update the catalogue
		for (int i = 0; i < inventory_names.length; ++i) {
			catalogue.put(inventory_names[i], new Integer(inventory_prices[i]));
			remaining_inventory.put(inventory_names[i], 5);
		}

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName(getAID().getName()+"-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
    //     try {
 	// 		Thread.sleep(3000);
 	// 	} catch (InterruptedException e) {
 	// 		//e.printStackTrace();
 	// 	}
		// addBehaviour(new shutdown());

	}
	// private class inventory {
		// private String[] inventory_names = [];
		// private int[] inventory_prices = [];
		// private int[] inventory_quantity = [];
	// }
	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Seller-agent "+getAID().getName()+" terminating.");
	}

    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
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
	/**
		Inner class OfferRequestsServer.
		This is the behaviour used by Book-seller agents to serve incoming requests 
		for offer from buyer agents.
		If the requested book is in the local catalogue the seller agent replies 
		with a PROPOSE message specifying the price. Otherwise a REFUSE message is
		sent back.
	*/
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.get(title);
				if (price != null) {
					// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Book-seller agents to serve incoming 
	   offer acceptances (i.e. purchase orders) from buyer agents.
	   The seller agent removes the purchased book from its catalogue 
	   and replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				// Integer price = (Integer) catalogue.remove(title);
				Integer price = (Integer) catalogue.get(title);
				Integer quantity = (Integer) remaining_inventory.get(title);
				System.out.println("Book Details: " + title + " Price: " + price + " No's: " + quantity);
				if ((price != null) && (quantity != null)) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent "+msg.getSender().getName());
					remaining_inventory.put(title, quantity-1);
					if (quantity == 0) {
						catalogue.remove(title);
					}
					System.out.println("Number of Books Remaining:" + remaining_inventory.get(title));
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
}
