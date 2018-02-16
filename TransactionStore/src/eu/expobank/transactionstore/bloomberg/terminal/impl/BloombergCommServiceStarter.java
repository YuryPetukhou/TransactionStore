package eu.expobank.transactionstore.bloomberg.terminal.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.expobank.transactionstore.bloomberg.logger.BloomStoreLogger;

@Component
public class BloombergCommServiceStarter {
	
	
	private final static String BBCOMM_PROCESS  = "bbcomm.exe";
	private final static String BBCOMM_FOLDER  = "C:/blp/DAPI";

	@Autowired
	private BloomStoreLogger logger;
	/**
	 * 
	 * @return true if the bbcomm process is running
	 */
	public boolean isBloombergProcessRunning() {
	    return ShellUtils.isProcessRunning(BBCOMM_PROCESS);
	}

	/**
	 * Starts the bbcomm process, which is required to connect to the Bloomberg data feed
	 * @return true if bbcomm was started successfully, false otherwise
	 */
	public boolean startBloombergProcessIfNecessary() {
	    if (isBloombergProcessRunning()) {
	        logger.info(BBCOMM_PROCESS + " is started");
	        return true;
	    }

	    Callable<Boolean> startBloombergProcess = getStartingCallable();
	    return getResultWithTimeout(startBloombergProcess, 10, TimeUnit.SECONDS);
	}

	private Callable<Boolean> getStartingCallable() {
	    return new Callable<Boolean>() {
	        @Override
	        public Boolean call() throws Exception {
	        	System.out.println("Starting " + BBCOMM_PROCESS + " manually");
	            logger.info("Starting " + BBCOMM_PROCESS + " manually");
	            ProcessBuilder pb = new ProcessBuilder(BBCOMM_FOLDER+"/"+BBCOMM_PROCESS);
	            pb.directory(new File(BBCOMM_FOLDER));
	            pb.redirectErrorStream(true);
	            Process p = pb.start();
	            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            String line;
	            while ((line = reader.readLine()) != null) {
	                if (line.toLowerCase().contains("started")) {
	                    logger.info(BBCOMM_PROCESS + " is started");
	                    System.out.println(BBCOMM_PROCESS + " is started");
	                    return true;
	                }
	            }
	            return false;
	        }
	    };

	}

	private boolean getResultWithTimeout(Callable<Boolean> startBloombergProcess, int timeout, TimeUnit timeUnit) {
	    ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

	        @Override
	        public Thread newThread(Runnable r) {
	            Thread t = new Thread(r, "Bloomberg - bbcomm starter thread");
	            t.setDaemon(true);
	            return t;
	        }
	    });
	    Future<Boolean> future = executor.submit(startBloombergProcess);

	    try {
	        return future.get(timeout, timeUnit);
	    } catch (InterruptedException ignore) {
	        Thread.currentThread().interrupt();
	        return false;
	    } catch (ExecutionException | TimeoutException e) {
	        logger.error("Could not start bbcomm", e);
	        System.out.println("Could not start bbcomm");
	        return false;
	    } finally {
	        executor.shutdownNow();
	        try {
	            if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
	                logger.warn("bbcomm starter thread still running");
	                System.out.println("bbcomm starter thread still running");
	            }
	        } catch (InterruptedException ex) {
	            Thread.currentThread().interrupt();
	        }
	    }
	}
}
