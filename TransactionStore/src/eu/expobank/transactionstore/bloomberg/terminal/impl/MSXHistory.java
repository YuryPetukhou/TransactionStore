package eu.expobank.transactionstore.bloomberg.terminal.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;

import eu.expobank.transactionstore.bloomberg.config.ApplicationContextContainer;
import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;
import eu.expobank.transactionstore.bloomberg.logger.BloomStoreLogger;

public class MSXHistory implements EventHandler {

	private static final String SERVICE_URI = "//blp/mktdata";

	private static final Name SESSION_STARTED = new Name("SessionStarted");
	private static final Name SESSION_STARTUP_FAILURE = new Name("SessionStartupFailure");
	private static final Name SERVICE_OPENED = new Name("ServiceOpened");
	private static final Name SERVICE_OPEN_FAILURE = new Name("ServiceOpenFailure");

	private static final Name ERROR_INFO = new Name("ErrorInfo");
	private static final Name GET_FILLS_RESPONSE = new Name("GetFillsResponse");
	
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZZZZZ");
	
	
	private BloomStoreLogger logger=(BloomStoreLogger)ApplicationContextContainer.getBean(BloomStoreLogger.class);
	
	private List<BloombergTransaction> transactions;

	private CorrelationID requestID;
	private ZonedDateTime startDateTime;
	private ZonedDateTime finishDateTime;

	public MSXHistory(ZonedDateTime startDateTime) {
		super();
		this.startDateTime = startDateTime;
		this.finishDateTime = ZonedDateTime.now();
		this.transactions =new ArrayList<BloombergTransaction>();
	}

	public MSXHistory(ZonedDateTime startDateTime, ZonedDateTime finishDateTime) {
		super();
		this.startDateTime = startDateTime;
		this.finishDateTime = finishDateTime;
		this.transactions =new ArrayList<BloombergTransaction>();
	}

	@Override
	public void processEvent(Event event, Session session) {

		try {
			switch (event.eventType().intValue()) {
			case Event.EventType.Constants.SESSION_STATUS:
				processSessionEvent(event, session);
				break;
			case Event.EventType.Constants.SERVICE_STATUS:
				processServiceEvent(event, session);
				break;
			case Event.EventType.Constants.RESPONSE:
				processResponseEvent(event, session);
				break;
			default:
				processMiscEvents(event, session);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean processSessionEvent(Event event, Session session) throws Exception {

		System.out.println("Processing " + event.eventType().toString());
		logger.info("Processing " + event.eventType().toString());
		MessageIterator msgIter = event.messageIterator();

		while (msgIter.hasNext()) {
			Message msg = msgIter.next();
			if (msg.messageType().equals(SESSION_STARTED)) {
				System.out.println("Session started...");
				logger.info("Session started...");
				session.openService(SERVICE_URI);
			} else if (msg.messageType().equals(SESSION_STARTUP_FAILURE)) {
				System.err.println("Error: Session startup failed");
				logger.error("Error: Session startup failed");
				return false;
			}
		}
		return true;
	}

	private boolean processServiceEvent(Event event, Session session) {

		System.out.println("Processing " + event.eventType().toString());

		MessageIterator msgIter = event.messageIterator();

		while (msgIter.hasNext()) {

			Message msg = msgIter.next();

			if (msg.messageType().equals(SERVICE_OPENED)) {

				System.out.println("Service opened...");
				logger.info("Service opened...");
				Service service = session.getService(SERVICE_URI);

				Request request = service.createRequest("GetFills");

				request.set("FromDateTime", startDateTime.format(DATE_TIME_FORMATTER));
				request.set("ToDateTime", finishDateTime.format(DATE_TIME_FORMATTER));

				Element scope = request.getElement("Scope");

				// scope.setChoice("Team");
				// scope.setChoice("TradingSystem");
				scope.setChoice("Uuids");

				// scope.setElement("Team", "SEXEGROUP");
				// scope.setElement("TradingSystem",false);

				scope.getElement("Uuids").appendValue(8049857);

				/*
				 * scope.getElement("Uuids").appendValue(14348220);
				 * scope.getElement("Uuids").appendValue(8639067);
				 * scope.getElement("Uuids").appendValue(4674574);
				 */

				// Element filter = request.getElement("FilterBy");

				// filter.setChoice("Basket");
				// filter.setChoice("Multileg");
				// filter.setChoice("OrdersAndRoutes");

				// filter.getElement("Basket").appendValue("TESTRJC");
				// filter.getElement("Multileg").appendValue("mymlegId");
				// Element newOrder = filter.getElement("OrdersAndRoutes").appendElement();
				// newOrder.setElement("OrderId",4292580);
				// newOrder.setElement("RouteId",1);

				System.out.println("Request: " + request.toString());
				logger.info("Request: " + request.toString());
				requestID = new CorrelationID();

				// Submit the request
				try {
					session.sendRequest(request, requestID);
				} catch (Exception ex) {
					System.err.println("Failed to send the request");
					logger.error("Failed to send the request");
					return false;
				}

			} else if (msg.messageType().equals(SERVICE_OPEN_FAILURE)) {
				System.err.println("Error: Service failed to open");
				logger.error("Error: Service failed to open");
				return false;
			}
		}
		return true;
	}

	private boolean processResponseEvent(Event event, Session session) throws Exception {
		System.out.println("Received Event: " + event.eventType().toString());
		logger.info("Received Event: " + event.eventType().toString());
		MessageIterator msgIter = event.messageIterator();

		while (msgIter.hasNext()) {
			Message msg = msgIter.next();

			if (event.eventType() == Event.EventType.RESPONSE && msg.correlationID() == requestID) {

				System.out.println("Message Type: " + msg.messageType());
				logger.info("Message Type: " + msg.messageType());
				if (msg.messageType().equals(ERROR_INFO)) {
					Integer errorCode = msg.getElementAsInt32("ERROR_CODE");
					String errorMessage = msg.getElementAsString("ERROR_MESSAGE");
					System.out.println("ERROR CODE: " + errorCode + "\tERROR MESSAGE: " + errorMessage);
					logger.error("ERROR CODE: " + errorCode + "\tERROR MESSAGE: " + errorMessage);
				} else if (msg.messageType().equals(GET_FILLS_RESPONSE)) {

					Element fills = msg.getElement("Fills");
					int numFills = fills.numValues();

					for (int i = 0; i < numFills; i++) {
						Element fill = fills.getValueAsElement(i);

						String account = fill.getElementAsString("Account");
						Double amount = fill.getElementAsFloat64("Amount");
						String assetClass = fill.getElementAsString("AssetClass");
						int basketId = fill.getElementAsInt32("BasketId");
						String bbgid = fill.getElementAsString("BBGID");
						String blockId = fill.getElementAsString("BlockId");
						String broker = fill.getElementAsString("Broker");
						String clearingAccount = fill.getElementAsString("ClearingAccount");
						String clearingFirm = fill.getElementAsString("ClearingFirm");
						Datetime contractExpDate = fill.getElementAsDate("ContractExpDate");
						int correctedFillId = fill.getElementAsInt32("CorrectedFillId");
						String currency = fill.getElementAsString("Currency");
						String cusip = fill.getElementAsString("Cusip");
						Datetime dateTimeOfFill = fill.getElementAsDate("DateTimeOfFill");
						String exchange = fill.getElementAsString("Exchange");
						int execPrevSeqNo = fill.getElementAsInt32("ExecPrevSeqNo");
						String execType = fill.getElementAsString("ExecType");
						String executingBroker = fill.getElementAsString("ExecutingBroker");
						int fillId = fill.getElementAsInt32("FillId");
						double fillPrice = fill.getElementAsFloat64("FillPrice");
						double fillShares = fill.getElementAsFloat64("FillShares");
						String investorId = fill.getElementAsString("InvestorID");
						boolean isCFD = fill.getElementAsBool("IsCfd");
						String isin = fill.getElementAsString("Isin");
						boolean isLeg = fill.getElementAsBool("IsLeg");
						String lastCapacity = fill.getElementAsString("LastCapacity");
						String lastMarket = fill.getElementAsString("LastMarket");
						double limitPrice = fill.getElementAsFloat64("LimitPrice");
						String liquidity = fill.getElementAsString("Liquidity");
						String localExchangeSymbol = fill.getElementAsString("LocalExchangeSymbol");
						String locateBroker = fill.getElementAsString("LocateBroker");
						String locateId = fill.getElementAsString("LocateId");
						boolean locateRequired = fill.getElementAsBool("LocateRequired");
						String multiLedId = fill.getElementAsString("MultilegId");
						String occSymbol = fill.getElementAsString("OCCSymbol");
						String orderExecutionInstruction = fill.getElementAsString("OrderExecutionInstruction");
						String orderHandlingInstruction = fill.getElementAsString("OrderHandlingInstruction");
						int orderId = fill.getElementAsInt32("OrderId");
						String orderInstruction = fill.getElementAsString("OrderInstruction");
						String orderOrigin = fill.getElementAsString("OrderOrigin");
						String orderReferenceId = fill.getElementAsString("OrderReferenceId");
						int originatingTraderUUId = fill.getElementAsInt32("OriginatingTraderUuid");
						String reroutedBroker = fill.getElementAsString("ReroutedBroker");
						double routeCommissionAmount = fill.getElementAsFloat64("RouteCommissionAmount");
						double routeCommissionRate = fill.getElementAsFloat64("RouteCommissionRate");
						String routeExecutionInstruction = fill.getElementAsString("RouteExecutionInstruction");
						String routeHandlingInstruction = fill.getElementAsString("RouteHandlingInstruction");
						int routeId = fill.getElementAsInt32("RouteId");
						double routeNetMoney = fill.getElementAsFloat64("RouteNetMoney");
						String routeNotes = fill.getElementAsString("RouteNotes");
						double routeShares = fill.getElementAsFloat64("RouteShares");
						String securityName = fill.getElementAsString("SecurityName");
						String sedol = fill.getElementAsString("Sedol");
						Datetime settlementDate = fill.getElementAsDate("SettlementDate");
						String side = fill.getElementAsString("Side");
						double stopPrice = fill.getElementAsFloat64("StopPrice");
						String strategyType = fill.getElementAsString("StrategyType");
						String ticker = fill.getElementAsString("Ticker");
						String tif = fill.getElementAsString("TIF");
						String traderName = fill.getElementAsString("TraderName");
						int traderUUId = fill.getElementAsInt32("TraderUuid");
						String type = fill.getElementAsString("Type");
						double userCommissionAmount = fill.getElementAsFloat64("UserCommissionAmount");
						double userCommissionRate = fill.getElementAsFloat64("UserCommissionRate");
						double userFees = fill.getElementAsFloat64("UserFees");
						double userNetMoney = fill.getElementAsFloat64("UserNetMoney");
						String yellowKey = fill.getElementAsString("YellowKey");

						System.out.println("OrderId: " + orderId + "\tFill ID: " + fillId + "\tDate/Time: "
								+ dateTimeOfFill.toString() + "\tShares: " + fillShares + "\tPrice: " + fillPrice);

					}
				}

				session.stop();
			}
		}
		return true;
	}

	private boolean processMiscEvents(Event event, Session session) throws Exception {
		System.out.println("Processing " + event.eventType().toString());
		logger.info("Processing " + event.eventType().toString());
		MessageIterator msgIter = event.messageIterator();
		while (msgIter.hasNext()) {
			Message msg = msgIter.next();
			System.out.println("MESSAGE: " + msg);
			logger.info("MESSAGE: " + msg);
		}
		return true;
	}

	public List<BloombergTransaction> getTransactions() {
		return transactions;
	}

}
