package org.sagebionetworks.repo.manager.file;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

/**
 * Manages uploading files.
 * 
 * @author John
 *
 */
public interface FileUploadManager {

	/**
	 * Upload all of the files found in a request and capture all of the parameters and meta-data.
	 * 
	 * @param userInfo - The User's information.
	 * @param expectedParams - If any of the expected parameters are missing by the time we get to a file then an IllegalArgumentException will be thrown.
	 * @param itemIterator - Iterates over all of the parts.
	 * @return FileUploadResults
	 * @throws IOException 
	 * @throws FileUploadException 
	 * @throws ServiceUnavailableException 
	 */
	FileUploadResults uploadfiles(UserInfo userInfo, Set<String> expectedParams, FileItemIterator itemIterator) throws FileUploadException, IOException, ServiceUnavailableException;
	
	/**
	 * Upload a file.
	 * @param userId
	 * @param fis
	 * @return
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 */
	public S3FileHandle uploadFile(String userId, FileItemStream fis)	throws IOException, ServiceUnavailableException;

	/**
	 * Get a file handle for a user.
	 * Note: Only the creator of the FileHandle can access it.
	 * @param userInfo
	 * @param handleId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FileHandle getRawFileHandle(UserInfo userInfo, String handleId) throws DatastoreException, NotFoundException;
	
	/**
	 * Delete a file handle
	 * @param userInfo
	 * @param handleId
	 * @throws DatastoreException
	 */
	void deleteFileHandle(UserInfo userInfo, String handleId) throws DatastoreException;

}
