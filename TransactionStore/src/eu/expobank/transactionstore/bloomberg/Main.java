package eu.expobank.transactionstore.bloomberg;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import eu.expobank.transactionstore.bloomberg.config.ApplicationContextContainer;
import eu.expobank.transactionstore.bloomberg.dao.BloombergTransactionDAO;
import eu.expobank.transactionstore.bloomberg.dao.impl.BloombergTransactionDAOImpl;
import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;
import eu.expobank.transactionstore.bloomberg.service.impl.ProcessDealsServiceImpl;
import eu.expobank.transactionstore.bloomberg.terminal.TerminalManager;
import eu.expobank.transactionstore.bloomberg.terminal.impl.TerminalManagerImpl;

public class Main {

	public static void main(String[] args) {
		TerminalManager manager = (TerminalManager)ApplicationContextContainer.getBean(TerminalManagerImpl.class);
		List<Integer> uuids = new ArrayList<Integer>();
		uuids.add(new Integer(21697191));
		List<BloombergTransaction> transactions = manager.getTransactionsList(uuids,ZonedDateTime.now().minusMinutes(15), ZonedDateTime.now());
		transactions.forEach(tr-> System.out.println(tr.getCreatedDateTime()));
	}

}
