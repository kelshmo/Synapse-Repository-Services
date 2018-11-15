package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;

public interface ObjectTypeManager {

	/**
	 * Change the given object's DataType.
	 * @param userInfo
	 * @param id
	 * @param entity
	 * @param dataType
	 * @return
	 */
	DataTypeResponse changeObjectsDataType(UserInfo userInfo, String objectId, ObjectType objectType, DataType dataType);

}
