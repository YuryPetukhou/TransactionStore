package eu.expobank.transactionstore.bloomberg.dao.impl;

import java.util.List;

import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import eu.expobank.transactionstore.bloomberg.dao.BloombergTransactionDAO;
import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;

@Component
public class BloombergTransactionDAOImpl implements BloombergTransactionDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	@Transactional
	public void addBloombergTransaction(BloombergTransaction transaction) {
		Session session = sessionFactory.getCurrentSession();
		session.save(transaction);
	}

	@Override
	@Transactional
	public void addBloombergTransactions(List<BloombergTransaction> transactions) {
		Session session = sessionFactory.getCurrentSession();
		transactions.forEach(tr -> session.save(tr));
	}

	@Override
	@Transactional
	public List<BloombergTransaction> gatAllTransactions() {
		Session session = sessionFactory.getCurrentSession();
		Query query = session.createQuery("from BloombergTransaction");
		List<BloombergTransaction> transactions = query.getResultList();
		return (List<BloombergTransaction>) transactions;
	}

}
