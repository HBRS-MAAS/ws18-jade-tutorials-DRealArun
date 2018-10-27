package maas;

import java.util.List;
import java.util.Vector;
import maas.tutorials.BookBuyerAgent;

public class Start {
    public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	agents.add("buyer1:maas.tutorials.BookBuyerAgent");
        agents.add("buyer2:maas.tutorials.BookBuyerAgent");
        agents.add("seller1:maas.tutorials.BookSellerAgent");
        agents.add("seller2:maas.tutorials.BookSellerAgent");

    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : agents) {
    		sb.append(a);
    		sb.append(";");
    	}
    	cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }
}
