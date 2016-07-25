package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_TAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_COMMIT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBODockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DockerCommitDaoImpl implements DockerCommitDao {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;
	
	// get the latest tags for an entity
	// for this to return a single record per <entity,tag> it's crucial
	// that the table have a unique key constraint on <entity,tag,createdOn>
	private static final String LATEST_COMMIT_SQL = 
		"SELECT * FROM "+TABLE_DOCKER_COMMIT+" d "+
		" WHERE d."+COL_DOCKER_COMMIT_OWNER_ID+"=? AND d."+
		COL_DOCKER_COMMIT_CREATED_ON+"=(SELECT MAX(d2."+COL_DOCKER_COMMIT_CREATED_ON+
		") FROM "+TABLE_DOCKER_COMMIT+" d2 WHERE "+
			" d2."+COL_DOCKER_COMMIT_OWNER_ID+"=d."+COL_DOCKER_COMMIT_OWNER_ID+" AND "+
			" d2."+COL_DOCKER_COMMIT_TAG+"=d."+COL_DOCKER_COMMIT_TAG+")";
	
	private static final TableMapping<DBODockerCommit> COMMIT_ROW_MAPPER = 
			(new DBODockerCommit()).getTableMapping();
	
	private static final String UPDATE_NODE_ETAG_SQL = 
			"UPDATE "+TABLE_NODE+" SET "+COL_NODE_ETAG+"=? WHERE "+COL_NODE_ID+"=?";
	
	private static final String UPDATE_REVISION_SQL = 
			"UPDATE "+TABLE_REVISION+" r SET r."+COL_REVISION_MODIFIED_BY+"=?, r."+
			COL_REVISION_MODIFIED_ON+"=? WHERE r."+COL_REVISION_OWNER_NODE+"=? "+
			" AND r."+COL_REVISION_NUMBER+"= SELECT n."+COL_CURRENT_REV+" FROM "+
			TABLE_NODE+" n WHERE n."+COL_NODE_ID+"=r."+COL_REVISION_OWNER_NODE;

	@WriteTransactionReadCommitted
	@Override
	public void createDockerCommit(String entityId, long modifiedBy, DockerCommit commit) {
		DBODockerCommit dbo = new DBODockerCommit();
		long nodeId = KeyFactory.stringToKey(entityId);
		dbo.setOwner(nodeId);
		dbo.setTag(commit.getTag());
		dbo.setDigest(commit.getDigest());
		long eventTime = commit.getCreatedOn().getTime();
		dbo.setCreatedOn(eventTime);
		basicDao.createNew(dbo);
		jdbcTemplate.update(UPDATE_NODE_ETAG_SQL, UUID.randomUUID().toString(), nodeId);
		jdbcTemplate.update(UPDATE_REVISION_SQL, modifiedBy, eventTime, nodeId);
	}
	
	@Override
	public List<DockerCommit> listDockerCommits(String entityId) {
		if (entityId==null) throw new IllegalArgumentException("entityId is required.");
		long nodeIdAsLong = KeyFactory.stringToKey(entityId);
		List<DBODockerCommit> dbos = jdbcTemplate.query(LATEST_COMMIT_SQL, COMMIT_ROW_MAPPER, nodeIdAsLong);
		List<DockerCommit> result = new ArrayList<DockerCommit>();
		for (DBODockerCommit dbo : dbos) {
			DockerCommit dto = new DockerCommit();
			dto.setDigest(dbo.getDigest());
			dto.setTag(dbo.getTag());
			dto.setCreatedOn(new Date(dbo.getCreatedOn()));
			result.add(dto);
		}
		return result;
	}

}
