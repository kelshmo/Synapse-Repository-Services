package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Data Binding Object for the Thread table
 * @author kimyentruong
 *
 */
public class DBOThread  implements MigratableDatabaseObject<DBOThread, DBOThread> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_THREAD_ID, true).withIsBackupId(true),
		new FieldColumn("forumId", COL_THREAD_FORUM_ID),
		new FieldColumn("title", COL_THREAD_TITLE),
		new FieldColumn("etag", COL_THREAD_ETAG),
		new FieldColumn("createdOn", COL_THREAD_CREATED_ON),
		new FieldColumn("createdBy", COL_THREAD_CREATED_BY),
		new FieldColumn("modifiedOn", COL_THREAD_MODIFIED_ON),
		new FieldColumn("messageKey", COL_THREAD_MESSAGE_KEY)
	};

	private Long id;
	private Long forumId;
	private byte[] title;
	private String etag;
	private Date createdOn;
	private Long createdBy;
	private Date modifiedOn;
	private String messageKey;

	@Override
	public String toString() {
		return "DBOThread [id=" + id + ", forumId=" + forumId + ", title="
				+ Arrays.toString(title) + ", etag=" + etag + ", createdOn="
				+ createdOn + ", createdBy=" + createdBy + ", modifiedOn="
				+ modifiedOn + ", messageKey=" + messageKey + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((forumId == null) ? 0 : forumId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((messageKey == null) ? 0 : messageKey.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + Arrays.hashCode(title);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOThread other = (DBOThread) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (forumId == null) {
			if (other.forumId != null)
				return false;
		} else if (!forumId.equals(other.forumId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (messageKey == null) {
			if (other.messageKey != null)
				return false;
		} else if (!messageKey.equals(other.messageKey))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (!Arrays.equals(title, other.title))
			return false;
		return true;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getForumId() {
		return forumId;
	}

	public void setForumId(Long forumId) {
		this.forumId = forumId;
	}

	public byte[] getTitle() {
		return title;
	}

	public void setTitle(byte[] title) {
		this.title = title;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	@Override
	public TableMapping<DBOThread> getTableMapping() {
		return new TableMapping<DBOThread>() {

			@Override
			public DBOThread mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOThread dbo = new DBOThread();
				dbo.setId(rs.getLong(COL_THREAD_ID));
				dbo.setForumId(rs.getLong(COL_THREAD_FORUM_ID));
				Blob blob = rs.getBlob(COL_THREAD_TITLE);
				dbo.setTitle(blob.getBytes(0, (int) blob.length()));
				dbo.setEtag(rs.getString(COL_THREAD_ETAG));
				dbo.setCreatedOn(new Date(rs.getLong(COL_THREAD_CREATED_ON)));
				dbo.setCreatedBy(rs.getLong(COL_THREAD_CREATED_BY));
				dbo.setModifiedOn(new Date(rs.getLong(COL_THREAD_MODIFIED_ON)));
				dbo.setMessageKey(rs.getString(COL_THREAD_MESSAGE_KEY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_THREAD;
			}

			@Override
			public String getDDLFileName() {
				return DDL_THREAD;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOThread> getDBOClass() {
				return DBOThread.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.THREAD;
	}

	@Override
	public MigratableTableTranslation<DBOThread, DBOThread> getTranslator() {
		return new MigratableTableTranslation<DBOThread, DBOThread>(){

			@Override
			public DBOThread createDatabaseObjectFromBackup(DBOThread backup) {
				return backup;
			}

			@Override
			public DBOThread createBackupFromDatabaseObject(DBOThread dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOThread> getBackupClass() {
		return DBOThread.class;
	}

	@Override
	public Class<? extends DBOThread> getDatabaseObjectClass() {
		return DBOThread.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
