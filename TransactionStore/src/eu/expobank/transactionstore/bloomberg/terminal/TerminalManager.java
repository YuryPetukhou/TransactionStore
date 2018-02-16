package eu.expobank.transactionstore.bloomberg.terminal;

import java.time.ZonedDateTime;
import java.util.List;

import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;

public interface TerminalManager {
		
	List<BloombergTransaction> getTransactionsList(List<Integer> uuids,ZonedDateTime startDateTime, ZonedDateTime finishDateTime);
}
