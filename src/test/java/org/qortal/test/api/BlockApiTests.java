package org.qortal.test.api;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.qortal.account.PrivateKeyAccount;
import org.qortal.api.resource.BlocksResource;
import org.qortal.block.GenesisBlock;
import org.qortal.repository.DataException;
import org.qortal.repository.Repository;
import org.qortal.repository.RepositoryManager;
import org.qortal.test.common.ApiCommon;
import org.qortal.test.common.BlockUtils;
import org.qortal.test.common.Common;
import org.qortal.utils.Base58;

public class BlockApiTests extends ApiCommon {

	private BlocksResource blocksResource;

	@Before
	public void buildResource() {
		this.blocksResource = (BlocksResource) ApiCommon.buildResource(BlocksResource.class);
	}

	@Test
	public void testResource() {
		assertNotNull(this.blocksResource);
	}

	@Test
	public void testGetBlock() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			byte[] signatureBytes = GenesisBlock.getInstance(repository).getSignature();
			String signature = Base58.encode(signatureBytes);

			assertNotNull(this.blocksResource.getBlock(signature));
		}
	}

	@Test
	public void testGetBlockTransactions() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			byte[] signatureBytes = GenesisBlock.getInstance(repository).getSignature();
			String signature = Base58.encode(signatureBytes);

			assertNotNull(this.blocksResource.getBlockTransactions(signature, null, null, null));
			assertNotNull(this.blocksResource.getBlockTransactions(signature, 1, 1, true));
		}
	}

	@Test
	public void testGetHeight() {
		assertNotNull(this.blocksResource.getHeight());
	}

	@Test
	public void testGetBlockHeight() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			byte[] signatureBytes = GenesisBlock.getInstance(repository).getSignature();
			String signature = Base58.encode(signatureBytes);

			assertNotNull(this.blocksResource.getHeight(signature));
		}
	}

	@Test
	public void testGetBlockByHeight() {
		assertNotNull(this.blocksResource.getByHeight(1));
	}

	@Test
	public void testGetBlockByTimestamp() {
		assertNotNull(this.blocksResource.getByTimestamp(System.currentTimeMillis()));
	}

	@Test
	public void testGetBlockRange() {
		assertNotNull(this.blocksResource.getBlockRange(1, 1));
	}

	@Test
	public void testGetBlockSigners() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			PrivateKeyAccount mintingAccount = Common.getTestAccount(repository, "alice-reward-share");
			BlockUtils.mintBlock(repository);

			List<String> addresses = Arrays.asList(aliceAddress, mintingAccount.getAddress(), bobAddress);

			assertNotNull(this.blocksResource.getBlockSigners(Collections.emptyList(), null, null, null));
			assertNotNull(this.blocksResource.getBlockSigners(addresses, null, null, null));
			assertNotNull(this.blocksResource.getBlockSigners(Collections.emptyList(), 1, 1, true));
			assertNotNull(this.blocksResource.getBlockSigners(addresses, 1, 1, true));
		}
	}

	@Test
	public void testGetBlockSummariesBySigner() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			BlockUtils.mintBlock(repository);

			assertNotNull(this.blocksResource.getBlockSummariesBySigner(aliceAddress, null, null, null));
			assertNotNull(this.blocksResource.getBlockSummariesBySigner(aliceAddress, 1, 1, true));
		}
	}

}
