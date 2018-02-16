package eu.expobank.transactionstore.bloomberg.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "operations")
public class BloombergTransaction extends BaseEntity{
	public BloombergTransaction() {
		super();
	}
}
