package eu.expobank.transactionstore.bloomberg.dao;

import java.util.List;

import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;

public interface BloombergTransactionDAO {
	void addBloombergTransaction (BloombergTransaction transaction);
	void addBloombergTransactions (List<BloombergTransaction> transactions);
	List<BloombergTransaction> gatAllTransactions ();
}
