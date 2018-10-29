package maas;

import java.util.List;
import java.util.Vector;
import maas.tutorials.BookBuyerAgent;

public class Start {
    public static void main(String[] args) {
    	List<String> agents = new Vector<>();
        int MAX_SELLERS = 3; // Maximum number of Book sellers = 3
        int MAX_BUYERS = 20; // Maximum number of Book Buyers = 20

        for(int i=1;i<=MAX_BUYERS;i++){
    	   agents.add("buyer"+i+":maas.tutorials.BookBuyerAgent");
        }

        for(int j=1;j<=MAX_SELLERS;j++){
            agents.add("seller"+j+":maas.tutorials.BookSellerAgent");
        }

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
