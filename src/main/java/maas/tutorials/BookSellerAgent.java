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
import java.io.File;
import java.io.IOException;
import java.net.*;

@SuppressWarnings("serial")
public class BookSellerAgent extends Agent {
	private int MAX_BOOKS = 8; // Including ebooks
	private String[] inventory_names = new String[MAX_BOOKS];
	private int[] inventory_prices = new int[MAX_BOOKS];
	private int[] inventory_quantity = new int[MAX_BOOKS];
	private String source_path = "";
	private String relative_path = "/src/main/java/maas/tutorials/SellerInventory.csv";
	String name = "";
	// The catalogue of books for sale (maps the title of a book to its price)
	private Hashtable catalogue;
	private Hashtable remaining_inventory;

	// Convert string to integer using following post,
	// https://javahungry.blogspot.com/2014/02/how-to-convert-string-to-int-in-java-without-using-integer-parseint-method-code-with-example.html
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
		source_path =  System.getProperty("user.dir");
		String fileToParse = source_path + relative_path;
		File tmpfile = new File(fileToParse);
		boolean exists = tmpfile.exists();
		if (exists == false) {
			System.out.println("INVENTORY FILE DOES NOT EXIT ! exiting ...");
			doDelete();
		}
        BufferedReader fileReader = null;
 		StringBuilder st = new StringBuilder();
        st.append("==================================================================================================================================\n");
        st.append("                                                "+arg+ " Catalogue                                                                \n");
        st.append("==================================================================================================================================\n");
         
        //Delimiter used in CSV file
        final String DELIMITER = ",";
        try
        {
            String line = "";
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
	                	st.append(""+(count+1)+")");
	                	st.append(" Title : "+inventory_names[count]);
	                	inventory_prices[count] = stringToint(tokens[i+1]);
	                	st.append(" ,Price : "+inventory_prices[count]);
	                	inventory_quantity[count] = stringToint(tokens[i+2]);
	                	if (inventory_names[count].contains("E-Book")) {
	                		st.append(" ,Quantity : Unlimited");
	                		st.append(" ,Type : Softcopy/ebook\n");
	                	} else {
	                		st.append(" ,Quantity : "+inventory_quantity[count]);
	                		st.append(" ,Type : Hardcopy/Paperback\n");
	                	}
	                	count += 1;
	                }
            	}
            }
            st.append("__________________________________________________________________________________________________________________________________\n");
            System.out.println(st.toString());
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
	
	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");
		// Create the catalogue
		name = getAID().getLocalName();
		getInventory(name);
		catalogue = new Hashtable();
		remaining_inventory = new Hashtable();
		// Update the catalogue
		for (int i = 0; i < inventory_names.length; ++i) {
			catalogue.put(inventory_names[i], new Integer(inventory_prices[i]));
			remaining_inventory.put(inventory_names[i], new Integer(inventory_quantity[i]));
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

	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		StringBuilder st = new StringBuilder();
		st.append("==================================================================================================================================\n");
        st.append("                                                "+name+ " Inventory                                                                \n");
        st.append("==================================================================================================================================\n");
        for(int i=0; i< inventory_names.length; i++) {
        	st.append(""+(i+1)+")");
        	String title = inventory_names[i];
        	st.append(" Title : "+title);
        	st.append(" ,   Price : "+catalogue.get(title));
        	if (title.contains("E-Book")) {
        		st.append(" ,   Quantity : Unlimited");
        		st.append(" ,   Type : Softcopy/ebook\n");
        	} else {
        		st.append(" ,   Quantity : "+remaining_inventory.get(title));
        		st.append(" ,   Type : Hardcopy/Paperback\n");
        	}
        }
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		st.append("Seller Agent "+name+" will terminate now !\n");
		st.append("__________________________________________________________________________________________________________________________________\n");
		System.out.println(st.toString());
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
				// System.out.println(getAID().getName()+"checking "+title+" "+price+" "+msg.getSender().getName());
				if (price != null) {
					// The requested book is available for sale. Reply with the price
					// System.out.println("\t"+getAID().getLocalName()+" Proposing price"+ title+" "+price+" "+msg.getSender().getName());
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					// The requested book is NOT available for sale.
					// System.out.println("\t"+getAID().getLocalName()+" Refusing price"+ title+" "+price+" "+msg.getSender().getName());
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

				Integer price = (Integer) catalogue.get(title);
				Integer quantity = (Integer) remaining_inventory.get(title);
				// if (title.contains("E-Book")) {
				// 	// System.out.println("Book Details: " + title + " Price: " + price + " No's: Unlimited");
				// } else {
				// 	// System.out.println("Book Details: " + title + " Price: " + price + " No's: " + quantity);
				// }
				if ((price != null) && (quantity != null)) {
					reply.setPerformative(ACLMessage.INFORM);
					// System.out.println(title+" sold to agent "+msg.getSender().getName());
					String name_buyer = msg.getSender().getName().split("@")[0];
					// System.out.println("<----------------"+name+" sold the book "+title+" to "+name_buyer);
					if (!title.contains("E-Book")) {
						remaining_inventory.put(title, quantity-1);
						// System.out.println("Number of Books Remaining:" + remaining_inventory.get(title));
						if (quantity == 0) {
							catalogue.remove(title);
							remaining_inventory.remove(title);
						}
					}
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
