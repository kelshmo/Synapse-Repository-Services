package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.EntityServletTestHelperUtils.HTTPMODE;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Note: In order to use this class you must have the following annotations on
 * your test to get the DispatcherServlet initialized:
 * 
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @ContextConfiguration(locations = { "classpath:test-context.xml" })
 * 
 */
public class EntityServletTestHelper {

	private static HttpServlet dispatcherServlet = null;

	/**
	 * Setup the servlet, default test user, and entity list for test cleanup.
	 * 
	 * Create a Spring MVC DispatcherServlet so that we can test our URL
	 * mapping, request format, response format, and response status code.
	 * 
	 * @throws Exception
	 */
	public EntityServletTestHelper() throws Exception {
		dispatcherServlet = DispatchServletSingleton.getInstance();
	}
	
	/**
	 * Create an entity without an entity type
	 */
	public Entity createEntity(Entity entity, String username, String activityId)
			throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, NameConflictException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.POST, 
				UrlHelpers.ENTITY, username, entity);
		request.setParameter(ServiceConstants.GENERATED_BY_PARAM, activityId);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);
		
		return EntityServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Delete an entity without knowing the type
	 */
	public void deleteEntity(String id, String username) 
			throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.DELETE, 
				UrlHelpers.ENTITY + "/" + id, username, null);
		
		EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.NO_CONTENT);
	}

	/**
	 * Get an entity using only the ID
	 */
	public Entity getEntity(String id, String username) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + "/" + id, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		return EntityServletTestHelperUtils.readResponseEntity(response);
	}
	
	/**
	 * Get an entity bundle using only the ID
	 */
	public EntityBundle getEntityBundle(String id, int mask, String username) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + "/" + id + UrlHelpers.BUNDLE, username, null);
		request.setParameter("mask", "" + mask);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new EntityBundle(EntityServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Get an entity bundle for a specific version using the ID and versionNumber.
	 */
	public EntityBundle getEntityBundleForVersion(String id, Long versionNumber, int mask, String username) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + "/" + id + UrlHelpers.VERSION + "/" + versionNumber + UrlHelpers.BUNDLE, username, null);
		request.setParameter("mask", "" + mask);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new EntityBundle(EntityServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Update an entity.
	 */
	public Entity updateEntity(Entity toUpdate, String username) 
			throws JSONObjectAdapterException, IOException, NotFoundException, DatastoreException, ServletException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.PUT, 
				UrlHelpers.ENTITY + "/" + toUpdate.getId(), username, toUpdate);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Get the annotations for an entity.
	 */
	public Annotations getEntityAnnotations(String id, String username) 
			throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + "/" + id + UrlHelpers.ANNOTATIONS, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), Annotations.class);
	}

	/**
	 * Update the annotations of an entity
	 */
	public Annotations updateAnnotations(Annotations annos, String username) 
			throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.PUT, 
				UrlHelpers.ENTITY + "/" + annos.getId() + UrlHelpers.ANNOTATIONS, username, annos);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), Annotations.class);
	}

	/**
	 * Get the user's permissions for an entity
	 */
	public UserEntityPermissions getUserEntityPermissions(String id, String username) 
			throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + "/" + id + UrlHelpers.PERMISSIONS, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), UserEntityPermissions.class);
	}
	
	/**
	 * Get the user's permissions for an entity.
	 */
	public EntityPath getEntityPath(String id, String username) 
			throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + "/" + id + UrlHelpers.PATH, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), EntityPath.class);
	}
	
	/**
	 * Get the types of entities
	 */
	public BatchResults<EntityHeader> getEntityTypeBatch(List<String> ids, String username) 
			throws ServletException, IOException, NotFoundException, DatastoreException, JSONException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY_TYPE, username, null);
		request.setParameter(ServiceConstants.BATCH_PARAM, StringUtils.join(ids, ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR));
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(new JSONObject(response.getContentAsString()));
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
		results.initializeFromJSONObject(adapter);
		return results;
	}
	
	
	/**
	 * Get the list of all REST resources
	 */
	public RestResourceList getRESTResources() 
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.REST_RESOURCES, null, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), RestResourceList.class);
	}
	
	/**
	 * Get the effective schema for a resource
	 */
	public ObjectSchema getEffectiveSchema(String resourceId) 
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.REST_RESOURCES + UrlHelpers.EFFECTIVE_SCHEMA, null, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), ObjectSchema.class);
	}
	
	/**
	 * Get the full schema for a resource
	 */
	public ObjectSchema getFullSchema(String resourceId) 
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.REST_RESOURCES + UrlHelpers.SCHEMA, null, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), ObjectSchema.class);
	}

	/**
	 * Get the entity registry
	 */
	public EntityRegistry getEntityRegistry() 
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.ENTITY + UrlHelpers.REGISTRY, null, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), EntityRegistry.class);
	}

	/**
	 * Creates a new version of an entity
	 */
	public Versionable createNewVersion(String username, Versionable entity) 
			throws DatastoreException, IOException, ServletException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.PUT, 
				UrlHelpers.ENTITY + "/" + entity.getId() + "/version", username, entity);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), Versionable.class);
	}
	
	/////////////////////////
	// Evaluation Services //
	/////////////////////////
	
	/**
	 * Creates an evaluation
	 */
	public Evaluation createEvaluation(Evaluation eval, String username)
			throws JSONObjectAdapterException, IOException, DatastoreException, NotFoundException, ServletException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.POST, 
				UrlHelpers.EVALUATION, username, eval);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	/**
	 * Gets an evaluation
	 */
	public Evaluation getEvaluation(String username, String evalId) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	/**
	 * Returns whether the user has access rights to the evaluation
	 */
	public Boolean canAccess(String username, String evalId, ACCESS_TYPE accessType) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + "/access", username, null);
		request.addParameter("accessType", accessType.toString());
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter joa = EntityServletTestHelperUtils.readResponseJSON(response);
		return (Boolean) joa.get("result");
	}

	/**
	 * Looks for an evaluation by name
	 */
	public Evaluation findEvaluation(String username, String name) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/name/" + name, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	/**
	 * Gets a paginated list of available evaluations
	 */
	public PaginatedResults<Evaluation> getAvailableEvaluations(String username, String status) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION_AVAILABLE, username, null);
		request.setParameter("limit", "100");
		request.setParameter("offset", "0");
		if (status != null) {
			request.setParameter("status", status);
		}
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter joa = EntityServletTestHelperUtils.readResponseJSON(response);
		PaginatedResults<Evaluation> res = new PaginatedResults<Evaluation>(Evaluation.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public Evaluation updateEvaluation(Evaluation eval, String username) 
			throws JSONObjectAdapterException, IOException, NotFoundException, DatastoreException, ServletException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.PUT, 
				UrlHelpers.EVALUATION + "/" + eval.getId(), username, eval);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public void deleteEvaluation(String evalId, String username) 
			throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.DELETE, 
				UrlHelpers.EVALUATION + "/" + evalId, username, null);
		
		EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.NO_CONTENT);
	}

	
	public PaginatedResults<Evaluation> getEvaluationsByContentSourcePaginated(String username, String projectId, long limit, long offset) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/project/" + projectId, username, null);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "" + offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "" + limit);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter joa = EntityServletTestHelperUtils.readResponseJSON(response);
		PaginatedResults<Evaluation> res = new PaginatedResults<Evaluation>(Evaluation.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	
	public PaginatedResults<Evaluation> getEvaluationsPaginated(String username, long limit, long offset) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION, username, null);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "" + offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "" + limit);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter joa = EntityServletTestHelperUtils.readResponseJSON(response);
		PaginatedResults<Evaluation> res = new PaginatedResults<Evaluation>(Evaluation.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getEvaluationCount(String username) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION_COUNT, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		return Long.parseLong(response.getContentAsString());
	}
	
	public Participant createParticipant(String username, String evalId)
			throws JSONObjectAdapterException, IOException, DatastoreException, NotFoundException, ServletException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.POST, 
				UrlHelpers.EVALUATION + "/" + evalId + "/participant", username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Participant(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public Participant getParticipant(String username, String partId, String evalId) 
			throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		// Make sure we are passing in the ID, not the user name
		Long.parseLong(partId);
		
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + "/participant/" + partId, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Participant(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public void deleteParticipant(String username, String partId, String evalId) 
			throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		// Make sure we are passing in the ID, not the user name
		Long.parseLong(partId);
		
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.DELETE, 
				UrlHelpers.EVALUATION + "/" + evalId + "/participant/" + partId, username, null);
		
		EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.NO_CONTENT);
	}
	
	public PaginatedResults<Participant> getAllParticipants(String username, String evalId) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + "/participant", username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		JSONObjectAdapter joa = EntityServletTestHelperUtils.readResponseJSON(response);
		PaginatedResults<Participant> res = new PaginatedResults<Participant>(Participant.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getParticipantCount(String username, String evalId) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + "/participant/count", username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		return Long.parseLong(response.getContentAsString());
	}
	
	public Submission createSubmission(Submission sub, String username, String entityEtag)
			throws JSONObjectAdapterException, IOException, DatastoreException, NotFoundException, ServletException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.POST, 
				UrlHelpers.SUBMISSION, username, sub);
		request.setParameter(AuthorizationConstants.ETAG_PARAM, entityEtag);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return new Submission(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public Submission getSubmission(String username, String subId) 
			throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.SUBMISSION + "/" + subId, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Submission(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public SubmissionStatus getSubmissionStatus(String username, String subId) 
			throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.SUBMISSION + "/" + subId + "/status", username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new SubmissionStatus(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus subStatus, String username) 
			throws JSONObjectAdapterException, IOException, NotFoundException, DatastoreException, ServletException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.PUT, 
				UrlHelpers.SUBMISSION + "/" + subStatus.getId() + "/status", username, subStatus);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new SubmissionStatus(EntityServletTestHelperUtils.readResponseJSON(response));
	}
	
	public void deleteSubmission(String subId, String username) 
			throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.DELETE, 
				UrlHelpers.SUBMISSION + "/" + subId, username, null);
		
		EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.NO_CONTENT);
	}
	
	public PaginatedResults<Submission> getAllSubmissions(String username, String evalId, SubmissionStatusEnum status) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + "/submission/all", username, null);
		if (status != null) {
			request.setParameter(UrlHelpers.STATUS, status.toString());
		}
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter joa = EntityServletTestHelperUtils.readResponseJSON(response);
		PaginatedResults<Submission> res = new PaginatedResults<Submission>(Submission.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getSubmissionCount(String username, String evalId) 
			throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + "/submission/count", username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return Long.parseLong(response.getContentAsString());
	}

	public AccessControlList getEvaluationAcl(String username, String evalId)
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + UrlHelpers.ACL, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), AccessControlList.class);
	}

	public AccessControlList updateEvaluationAcl(String username, AccessControlList acl)
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.PUT, 
				UrlHelpers.EVALUATION_ACL, username, acl);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), AccessControlList.class);
	}

	public UserEvaluationPermissions getEvaluationPermissions(String username, String evalId)
			throws ServletException, IOException, JSONObjectAdapterException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				UrlHelpers.EVALUATION + "/" + evalId + UrlHelpers.PERMISSIONS, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), UserEvaluationPermissions.class);
	}

	public BatchResults<EntityHeader> getEntityHeaderByMd5(String username, String md5) 
			throws DatastoreException, IOException, ServletException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = EntityServletTestHelperUtils.initRequest(HTTPMODE.GET, 
				"/entity/md5/" + md5, username, null);
		
		MockHttpServletResponse response = EntityServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter adapter = EntityServletTestHelperUtils.readResponseJSON(response);
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
		results.initializeFromJSONObject(adapter);
		return results;
	}
	
	
///////////////////////////////////////////////////////////////////////////////
// TODO End of methods refactored to use the EntityServletTestHelperUtils    //
///////////////////////////////////////////////////////////////////////////////

	
	/**
	 * Get the migration counts
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCounts getMigrationTypeCounts(String userId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/counts";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, MigrationTypeCounts.class);
	}
	
	/**
	 * Get the RowMetadata for a given Migration type.
	 * This is used to get all metadata from a source stack during migation.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public RowMetadataResult getRowMetadata(String userId, MigrationType type, long limit, long offset) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/rows";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		request.setParameter("limit", ""+limit);
		request.setParameter("offset", ""+offset);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, RowMetadataResult.class);
	}
	
	/**
	 * Get the RowMetadata for a given Migration type.
	 * This is used to get all metadata from a source stack during migation.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public RowMetadataResult getRowMetadataDelta(String userId, MigrationType type, IdList list) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/delta";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(list);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, RowMetadataResult.class);
	}
	
	/**
	 * Get the RowMetadata for a given Migration type.
	 * This is used to get all metadata from a source stack during migation.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeList getPrimaryMigrationTypes(String userId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/primarytypes";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, MigrationTypeList.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus startBackup(String userId, MigrationType type, IdList list) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/backup";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(list);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, BackupRestoreStatus.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus startRestore(String userId, MigrationType type, RestoreSubmission sub) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/restore";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(sub);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, BackupRestoreStatus.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus getBackupRestoreStatus(String userId, String daemonId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/status";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("daemonId", daemonId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, BackupRestoreStatus.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCount deleteMigrationType(String userId, MigrationType type, IdList list) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/delete";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(list);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, MigrationTypeCount.class);
	}
	
	/**
	 * 
	 * @param userId
	 * @param toCreate
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage createWikiPage(String userId, String ownerId, ObjectType ownerType, WikiPage toCreate) throws ServletException,
			IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wiki";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		String body;
		body = EntityFactory.createJSONStringForEntity(toCreate);
		request.setContent(body.getBytes("UTF-8"));
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Delete a wikipage.
	 * @param key
	 * @param userName
	 * @throws ServletException
	 * @throws IOException
	 */
	public void deleteWikiPage(WikiPageKey key, String userName) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key);
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
	}
	
	/**
	 * Get a wiki page.
	 * @param key
	 * @param useranme
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public WikiPage getWikiPage(WikiPageKey key, String useranme) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key);
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, useranme);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Get the root wiki page.
	 * @param ownerId
	 * @param ownerType
	 * @param userName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType, String userName) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wiki";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Simple helper for creating a URI for a WikiPage using its key
	 * @param key
	 * @return
	 */
	public String createURI(WikiPageKey key) {
		return "/"+key.getOwnerObjectType().name().toLowerCase() + "/" + key.getOwnerObjectId() + "/wiki/"+key.getWikiPageId();
	}
	
	/**
	 * Update a wiki page.
	 * @param userName
	 * @param id
	 * @param entity
	 * @param wiki
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage updateWikiPage(String userId, String ownerId, ObjectType ownerType, WikiPage wiki) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wiki/"+wiki.getId();
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		String body;
		body = EntityFactory.createJSONStringForEntity(wiki);
		request.setContent(body.getBytes("UTF-8"));
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
		
	}
	/**
	 * Get the paginated results of a wiki header.
	 * @param userName
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String userName, String ownerId, ObjectType ownerType) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wikiheadertree";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(response.getContentAsString());
		PaginatedResults<WikiHeader> result = new PaginatedResults<WikiHeader>(WikiHeader.class);
		result.initializeFromJSONObject(adapter);
		return result;
	}
	
	/**
	 * Get the paginated results of a wiki header.
	 * @param userName
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public FileHandleResults getWikiFileHandles(String userName, WikiPageKey key) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key)+"/attachmenthandles";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the file handles for the current version.
	 * @param userName
	 * @param entityId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public FileHandleResults geEntityFileHandlesForCurrentVersion(String userName, String entityId) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/entity/"+entityId+"/filehandles";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the file handles for a given version.
	 * @param userName
	 * @param entityId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public FileHandleResults geEntityFileHandlesForVersion(String userName, String entityId, Long versionNumber) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/entity/"+entityId+"/version/"+versionNumber+"/filehandles";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File.
	 * @param userName
	 * @param key
	 * @param fileName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getWikiAttachmentFileURL(String userName, WikiPageKey key, String fileName, Boolean redirect) throws ServletException, IOException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key)+"/attachment";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.setParameter("fileName", fileName);
		if(redirect != null){
			request.setParameter("redirect", redirect.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		return handleRedirectReponse(redirect, response);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File.
	 * @param userName
	 * @param key
	 * @param fileName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getWikiAttachmentPreviewFileURL(String userName, WikiPageKey key, String fileName, Boolean redirect) throws ServletException, IOException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key)+"/attachmentpreview";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.setParameter("fileName", fileName);
		if(redirect != null){
			request.setParameter("redirect", redirect.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		return handleRedirectReponse(redirect, response);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File.
	 * @param userName
	 * @param entityId
	 * @param redirect - Defaults to null, which will follow the redirect.  When set to FALSE, a call will be made without a redirect.
	 * @param preview - Defaults to null, wich will get the File and not the preview of the File.  When set to TRUE, the URL of the preview will be returned.
	 * @param versionNumber - Defaults to null, wich will get the file for the current version.  When set to a version number, the file (or preview) assocaited
	 * with that version number will be returned.
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	private URL getEntityFileURL(String userName, String entityId, Boolean redirect, Boolean preview, Long versionNumber) throws ServletException, IOException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String suffix = "/file";
		if(Boolean.TRUE.equals(preview)){
			// This is a preview request.
			suffix = "/filepreview";
		}
		String version = "";
		if(versionNumber != null){
			version = "/version/"+versionNumber;
		}
		String uri = "/entity/"+entityId+version+suffix;
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		if(redirect != null){
			request.setParameter("redirect", redirect.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		return handleRedirectReponse(redirect, response);
	}
	
	/**
	 * 
	 * @param redirect
	 * @param response
	 * @return
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	private URL handleRedirectReponse(Boolean redirect,	MockHttpServletResponse response) throws MalformedURLException,
			UnsupportedEncodingException {
		// Redirect response is different than non-redirect.
		if(redirect == null || Boolean.TRUE.equals(redirect)){
			if (response.getStatus() != HttpStatus.TEMPORARY_REDIRECT.value()) {
				throw new ServletTestHelperException(response);
			}
			// Get the redirect location
			return new URL(response.getRedirectedUrl());
		}else{
			// Redirect=false
			if (response.getStatus() != HttpStatus.OK.value()) {
				throw new ServletTestHelperException(response);
			}
			// Get the redirect location
			return new URL(response.getContentAsString());
		}
	}
	/**
	 * Get the file URL for the current version.
	 * @param userName
	 * @param entityId
	 * @param redirect
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getEntityFileURLForCurrentVersion(String userName, String entityId, Boolean redirect) throws ServletException, IOException {
		Boolean preview = null;
		Long versionNumber = null;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
	
	/**
	 * Get the file preview URL for the current version.
	 * @param userName
	 * @param entityId
	 * @param redirect
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getEntityFilePreviewURLForCurrentVersion(String userName, String entityId, Boolean redirect) throws ServletException, IOException {
		Boolean preview = Boolean.TRUE;
		Long versionNumber = null;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
	/**
	 * 
	 * @param userName
	 * @param id
	 * @param versionNumber
	 * @param redirect
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public URL getEntityFileURLForVersion(String userName, String entityId, Long versionNumber, Boolean redirect) throws ServletException, IOException {
		Boolean preview = null;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
	
	/**
	 * 
	 * @param userName
	 * @param id
	 * @param versionNumber
	 * @param redirect
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public URL getEntityFilePreviewURLForVersion(String userName, String entityId, Long versionNumber, Boolean redirect) throws ServletException, IOException {
		Boolean preview = Boolean.TRUE;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
}
