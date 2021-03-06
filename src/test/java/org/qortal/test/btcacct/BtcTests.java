package org.qortal.test.btcacct;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.store.BlockStoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qortal.crosschain.BTC;
import org.qortal.crosschain.BTCP2SH;
import org.qortal.repository.DataException;
import org.qortal.test.common.Common;

public class BtcTests extends Common {

	@Before
	public void beforeTest() throws DataException {
		Common.useDefaultSettings(); // TestNet3
	}

	@After
	public void afterTest() {
		BTC.resetForTesting();
	}

	@Test
	public void testGetMedianBlockTime() throws BlockStoreException {
		System.out.println(String.format("Starting BTC instance..."));
		BTC btc = BTC.getInstance();
		System.out.println(String.format("BTC instance started"));

		long before = System.currentTimeMillis();
		System.out.println(String.format("Bitcoin median blocktime: %d", btc.getMedianBlockTime()));
		long afterFirst = System.currentTimeMillis();

		System.out.println(String.format("Bitcoin median blocktime: %d", btc.getMedianBlockTime()));
		long afterSecond = System.currentTimeMillis();

		long firstPeriod = afterFirst - before;
		long secondPeriod = afterSecond - afterFirst;

		System.out.println(String.format("1st call: %d ms, 2nd call: %d ms", firstPeriod, secondPeriod));

		assertTrue("2nd call should be quicker than 1st", secondPeriod < firstPeriod);
		assertTrue("2nd call should take less than 5 seconds", secondPeriod < 5000L);
	}

	@Test
	public void testFindP2shSecret() {
		// This actually exists on TEST3 but can take a while to fetch
		String p2shAddress = "2N8WCg52ULCtDSMjkgVTm5mtPdCsUptkHWE";

		List<byte[]> rawTransactions = BTC.getInstance().getAddressTransactions(p2shAddress);

		byte[] expectedSecret = "This string is exactly 32 bytes!".getBytes();
		byte[] secret = BTCP2SH.findP2shSecret(p2shAddress, rawTransactions);

		assertNotNull(secret);
		assertTrue("secret incorrect", Arrays.equals(expectedSecret, secret));
	}

	@Test
	public void testBuildSpend() {
		BTC btc = BTC.getInstance();

		String xprv58 = "tprv8ZgxMBicQKsPdahhFSrCdvC1bsWyzHHZfTneTVqUXN6s1wEtZLwAkZXzFP6TYLg2aQMecZLXLre5bTVGajEB55L1HYJcawpdFG66STVAWPJ";

		String recipient = "2N8WCg52ULCtDSMjkgVTm5mtPdCsUptkHWE";
		long amount = 1000L;

		Transaction transaction = btc.buildSpend(xprv58, recipient, amount);
		assertNotNull(transaction);

		// Check spent key caching doesn't affect outcome

		transaction = btc.buildSpend(xprv58, recipient, amount);
		assertNotNull(transaction);
	}

	@Test
	public void testGetWalletBalance() {
		BTC btc = BTC.getInstance();

		String xprv58 = "tprv8ZgxMBicQKsPdahhFSrCdvC1bsWyzHHZfTneTVqUXN6s1wEtZLwAkZXzFP6TYLg2aQMecZLXLre5bTVGajEB55L1HYJcawpdFG66STVAWPJ";

		Long balance = btc.getWalletBalance(xprv58);

		assertNotNull(balance);

		System.out.println(BTC.format(balance));

		// Check spent key caching doesn't affect outcome

		Long repeatBalance = btc.getWalletBalance(xprv58);

		assertNotNull(repeatBalance);

		System.out.println(BTC.format(repeatBalance));

		assertEquals(balance, repeatBalance);
	}

}
