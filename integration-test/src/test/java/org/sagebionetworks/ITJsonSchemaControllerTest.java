package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.Organization;

import com.google.common.collect.Sets;

public class ITJsonSchemaControllerTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userId;

	String organizationName;
	CreateOrganizationRequest request;

	Organization organization;

	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		synapse = new SynapseClientImpl();
		userId = SynapseClientHelper.createUser(adminSynapse, synapse);
		SynapseClientHelper.setEndpoints(synapse);
	}

	@BeforeEach
	public void beforeEach() throws SynapseException {
		organizationName = "test.integeration.organization";
		request = new CreateOrganizationRequest();
		request.setOrganizationName(organizationName);
		// ensure we start each test without this organization.
		try {
			Organization org = synapse.getOrganizationByName(organizationName);
			adminSynapse.deleteOrganization(org.getId());
		} catch (SynapseNotFoundException e) {
			// can ignore
		}
	}

	@AfterEach
	public void afterEach() throws SynapseException {
		if (organization != null) {
			try {
				adminSynapse.deleteOrganization(organization.getId());
			} catch (SynapseNotFoundException e) {
				// can ignore
			}
		}
	}

	@Test
	public void testCreateOrganization() throws SynapseException {
		// call under test
		organization = synapse.createOrganization(request);
		assertNotNull(organization);
		assertEquals(organizationName, organization.getName());
		assertNotNull(organization.getId());
		assertEquals("" + userId, organization.getCreatedBy());
	}

	@Test
	public void testGetOrganizationByName() throws SynapseException {
		organization = synapse.createOrganization(request);
		assertNotNull(organization);
		// call under test
		Organization fetched = synapse.getOrganizationByName(organizationName);
		assertEquals(organization, fetched);
	}

	@Test
	public void testDeleteOrganization() throws SynapseException {
		organization = synapse.createOrganization(request);
		assertNotNull(organization);
		// call under test
		synapse.deleteOrganization(organization.getId());

		assertThrows(SynapseNotFoundException.class, () -> {
			synapse.getOrganizationByName(organizationName);
		});
	}

	@Test
	public void testGetOrganizationAcl() throws SynapseException {
		organization = synapse.createOrganization(request);
		assertNotNull(organization);

		// call under test
		AccessControlList acl = synapse.getOrganizationAcl(organization.getId());
		assertNotNull(acl);
	}

	@Test
	public void testUpdateOrganizationAcl() throws SynapseException {
		organization = synapse.createOrganization(request);
		assertNotNull(organization);
		AccessControlList acl = synapse.getOrganizationAcl(organization.getId());
		assertNotNull(acl);
		// grant public read
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		acl.getResourceAccess().add(ra);
		// call under test
		AccessControlList resultAcl = synapse.updateOrganizationAcl(organization.getId(), acl);
		assertNotNull(resultAcl);
		// etag should have changed
		assertNotEquals(acl.getEtag(), resultAcl.getEtag());
	}
}
