package eu.expobank.transactionstore.bloomberg.service.impl;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.expobank.transactionstore.bloomberg.dao.BloombergTransactionDAO;
import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;
import eu.expobank.transactionstore.bloomberg.service.ProcessDealsService;
import eu.expobank.transactionstore.bloomberg.terminal.TerminalManager;

@Component
public class ProcessDealsServiceImpl implements ProcessDealsService{	
	
	@Autowired
	private TerminalManager manager;
	@Autowired
	private BloombergTransactionDAO dao;
	
	private boolean isActive;
	
	public void run() {
		isActive=true;
		while (isActive) {
//			List<BloombergTransaction> transactions = manager.getTransactionsList(ZonedDateTime.now().minusMinutes(15), ZonedDateTime.now());
//			dao.addBloombergTransactions(transactions);
		}
		
	}

}
