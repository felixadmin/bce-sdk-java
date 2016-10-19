package com.baidubce.services.ses;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import redis.clients.jedis.Jedis;

import com.baidubce.BceServiceException;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.conf.SesConf;
import com.baidubce.services.ses.SesClient;
import com.baidubce.services.ses.SesClientConfiguration;
import com.baidubce.services.ses.model.DeleteVerifiedDomainRequest;
import com.baidubce.services.ses.model.GetVerifiedDomainResponse;

@RunWith(Parameterized.class)
public class SesClientDeleteVerifiedDomainNorQaTest {
	private SesClient client;
	private Jedis jedis;

	@Parameter(0)
	public int tag;
	@Parameter(1)
	public String inDomainName;
	@Parameter(2)
	public String inStatus;
	@Parameter(3)
	public int inStatusCode;
	@Parameter(4)
	public String inMailDomainCert;

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ 0, "tom0.com", "success:none", 0, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD==" },
				{ 1, "tom1.com", "failed:none", 1, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD==" },
				{ 2, "tom2.com", "temporaryfailure:none", 2, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD==" },
				{ 3, "tom3.com", "pending:none", 3, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD==" },
				{ 4, "tom4.com", "notstarted:none", 4, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD=="},
				{ 5, "tom5.com", "none:none", 5, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD==" },
				{ 6, "notExist.com", "none:none", 5, "ZDUyYzAyMGE2OGM1MmY5NGEyZGM4OD=="},
				});
	}

	@Before
	public void setUp() throws InterruptedException {
		this.client = new SesClient(
				new SesClientConfiguration()
						.withCredentials(
								new DefaultBceCredentials(SesConf.SES_AK,
										SesConf.SES_SK)).withEndpoint(
								SesConf.SES_ENDPOINT));
		this.jedis = new Jedis(SesConf.SES_REDIS_HOST, SesConf.SES_REDIS_PORT);
		switch (tag) {
		case 6: {
			jedis.hdel(SesConf.DOMAIN_CERT_INFO_KEY, inDomainName, inMailDomainCert);
			jedis.hdel(SesConf.MAIL_CERT_STATUS_KEY, inDomainName);
			break;
		}
		default: {
			jedis.hset(SesConf.DOMAIN_CERT_INFO_KEY, inDomainName, inMailDomainCert);
			jedis.hset(SesConf.MAIL_CERT_STATUS_KEY, inDomainName, inStatus);
		}
		}
		Thread.sleep(100);
	}

	@After
	public void tearDown() {
		this.client.shutdown();
		jedis.hdel(SesConf.DOMAIN_CERT_INFO_KEY, inDomainName);
		jedis.hdel(SesConf.MAIL_CERT_STATUS_KEY, inDomainName);
		this.jedis.quit();
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void test() throws InterruptedException {
		switch(tag){
		case 6:{
			expectedException.expect(BceServiceException.class);
	    	expectedException.expectMessage("input identity not verified");
	    	this.client.deleteVerifiedDomain(inDomainName);
	    	Thread.sleep(1000);
		}
		default:{
			this.client.deleteVerifiedDomain(inDomainName);
			Thread.sleep(1000);

			expectedException.expect(BceServiceException.class);
			expectedException.expectMessage("domain not verified");
			GetVerifiedDomainResponse gResponse = this.client
					.getVerifiedDomain(inDomainName);
			Thread.sleep(1000);
			}
		}
	}

	@Test
	public void testRequest() throws InterruptedException {
		switch(tag){
		case 6:{
			expectedException.expect(BceServiceException.class);
	    	expectedException.expectMessage("input identity not verified");
	    	this.client.deleteVerifiedDomain(new DeleteVerifiedDomainRequest()
			.withDomainName(inDomainName));
	    	Thread.sleep(1000);
		}
		default:{
			this.client.deleteVerifiedDomain(new DeleteVerifiedDomainRequest()
				.withDomainName(inDomainName));
			Thread.sleep(1000);
			
			expectedException.expect(BceServiceException.class);
	    	expectedException.expectMessage("domain not verified");
			GetVerifiedDomainResponse gResponse = this.client
					.getVerifiedDomain(inDomainName);
			Thread.sleep(1000);
		}
			
		}
	}

}
