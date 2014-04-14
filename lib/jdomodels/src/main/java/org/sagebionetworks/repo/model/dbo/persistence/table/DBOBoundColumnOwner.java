package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The of the bound columns.  This controls bound column migration.
 * 
 * @author jmhill
 *
 */
@Table(name = TABLE_BOUND_COLUMN_OWNER)
public class DBOBoundColumnOwner implements MigratableDatabaseObject<DBOBoundColumnOwner, DBOBoundColumnOwner> {

	private static TableMapping<DBOBoundColumnOwner> tableMapping = AutoTableMapping.create(DBOBoundColumnOwner.class);

	@Field(name = COL_BOUND_OWNER_OBJECT_ID, nullable = false, primary=true, backupId = true)
	private Long objectId;
	
	@Field(name = COL_BOUND_OWNER_ETAG, nullable = false, varchar = 256)
	private String etag;
	
	@Override
	public TableMapping<DBOBoundColumnOwner> getTableMapping() {
		return tableMapping;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.BOUND_COLUMN_OWNER;
	}

	@Override
	public MigratableTableTranslation<DBOBoundColumnOwner, DBOBoundColumnOwner> getTranslator() {
		return new MigratableTableTranslation<DBOBoundColumnOwner, DBOBoundColumnOwner>(){

			@Override
			public DBOBoundColumnOwner createDatabaseObjectFromBackup(
					DBOBoundColumnOwner backup) {
				return backup;
			}

			@Override
			public DBOBoundColumnOwner createBackupFromDatabaseObject(
					DBOBoundColumnOwner dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOBoundColumnOwner> getBackupClass() {
		return DBOBoundColumnOwner.class;
	}

	@Override
	public Class<? extends DBOBoundColumnOwner> getDatabaseObjectClass() {
		return DBOBoundColumnOwner.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		List<MigratableDatabaseObject> seconday = new LinkedList<MigratableDatabaseObject>();
		seconday.add(new DBOBoundColumn());
		seconday.add(new DBOBoundColumnOrdinal());
		return seconday;
	}
}
