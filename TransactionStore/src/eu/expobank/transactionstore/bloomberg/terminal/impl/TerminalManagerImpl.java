package eu.expobank.transactionstore.bloomberg.terminal.impl;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

import eu.expobank.transactionstore.bloomberg.config.ApplicationContextContainer;
import eu.expobank.transactionstore.bloomberg.entity.BloombergTransaction;
import eu.expobank.transactionstore.bloomberg.logger.BloomStoreLogger;
import eu.expobank.transactionstore.bloomberg.terminal.TerminalManager;

@Component
public class TerminalManagerImpl implements TerminalManager {
	private static final String SERVICE_URI = "//blp/emsx.history";

	private static final Name SESSION_STARTED = new Name("SessionStarted");
	private static final Name SESSION_STARTUP_FAILURE = new Name("SessionStartupFailure");
	private static final Name SERVICE_OPENED = new Name("ServiceOpened");
	private static final Name SERVICE_OPEN_FAILURE = new Name("ServiceOpenFailure");

	private static final Name ERROR_INFO = new Name("ErrorInfo");
	private static final Name GET_FILLS_RESPONSE = new Name("GetFillsResponse");

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZZZZZ");

	private List<BloombergTransaction> transactions;

	private CorrelationID requestID;
	@Autowired
	private SessionOptions sessionOptions;
	@Autowired
	private BloomStoreLogger logger;
	@Autowired
	private BloombergCommServiceStarter starter;

	@Override
	public List<BloombergTransaction> getTransactionsList(List<Integer> uuids, ZonedDateTime startDateTime,
			ZonedDateTime finishDateTime) {
		if (!starter.isBloombergProcessRunning()) {
			starter.startBloombergProcessIfNecessary();
		}
		Session session = new Session(sessionOptions);
		List<BloombergTransaction> transactions = new ArrayList<BloombergTransaction>();
		try {
			if (!session.start()) {
				System.err.println("Error: Session startup failed");
				logger.error("Error: Session startup failed");
				return transactions;
			}
			System.out.println("Session started...");
			logger.info("Session started...");
			if (!session.openService(SERVICE_URI)) {
				System.err.println("Error: Service start failed");
				logger.error("Error: Service start failed");
			}
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
			uuids.forEach(uuid -> scope.getElement("Uuids").appendValue(uuid));

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
				boolean continueToLoop = true;
				while (continueToLoop) {
					Event event = session.nextEvent();
					switch (event.eventType().intValue()) {
					case Event.EventType.Constants.RESPONSE: // final event
						continueToLoop = false; // fall through
					case Event.EventType.Constants.PARTIAL_RESPONSE:
						handleResponseEvent(event);
						break;
					default:
						handleOtherEvent(event);
						break;
					}
				}
			} catch (Exception ex) {
				System.err.println("Failed to send the request");
				logger.error("Failed to send the request");
			}
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		return transactions;
	}

	private void handleResponseEvent(Event event) throws Exception {
		System.out.println("EventType =" + event.eventType());
		MessageIterator iter = event.messageIterator();
		while (iter.hasNext()) {
			Message message = iter.next();
			System.out.println("correlationID=" + message.correlationID());
			System.out.println("messageType =" + message.messageType());
			message.print(System.out);

			if (event.eventType() == Event.EventType.RESPONSE && message.correlationID() == requestID) {

				System.out.println("Message Type: " + message.messageType());
				logger.info("Message Type: " + message.messageType());
				if (message.messageType().equals(ERROR_INFO)) {
					Integer errorCode = message.getElementAsInt32("ERROR_CODE");
					String errorMessage = message.getElementAsString("ERROR_MESSAGE");
					System.out.println("ERROR CODE: " + errorCode + "\tERROR MESSAGE: " + errorMessage);
					logger.error("ERROR CODE: " + errorCode + "\tERROR MESSAGE: " + errorMessage);
				} else if (message.messageType().equals(GET_FILLS_RESPONSE)) {

					Element fills = message.getElement("Fills");
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

			}
		}

	}

	private void handleOtherEvent(Event event) throws Exception {
		System.out.println("EventType=" + event.eventType());
		MessageIterator iter = event.messageIterator();
		while (iter.hasNext()) {
			Message message = iter.next();
			System.out.println("correlationID=" + message.correlationID());
			System.out.println("messageType=" + message.messageType());
			message.print(System.out);
			if (Event.EventType.Constants.SESSION_STATUS == event.eventType().intValue()
					&& "SessionTerminated" == message.messageType().toString()) {
				System.out.println("Terminating: " + message.messageType());
				System.exit(1);
			}
		}
	}
}
