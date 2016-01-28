package org.sagebionetworks.repo.web.service.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionReplyManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionServiceImpl implements DiscussionService{
	@Autowired
	private UserManager userManager;
	@Autowired
	private ForumManager forumManager;
	@Autowired
	private DiscussionThreadManager threadManager;
	@Autowired
	private DiscussionReplyManager replyManager;

	@Override
	public Forum getForumMetadata(Long userId, String projectId) {
		UserInfo user = userManager.getUserInfo(userId);
		return forumManager.getForumMetadata(user, projectId);
	}

	@Override
	public DiscussionThreadBundle createThread(Long userId,
			CreateDiscussionThread toCreate) throws IOException {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.createThread(user, toCreate);
	}

	@Override
	public DiscussionThreadBundle getThread(Long userId, String threadId) {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.getThread(user, threadId);
	}

	@Override
	public DiscussionThreadBundle updateThreadTitle(Long userId, String threadId, UpdateThreadTitle title) {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.updateTitle(user, threadId, title);
	}

	@Override
	public DiscussionThreadBundle updateThreadMessage(Long userId, String threadId,
			UpdateThreadMessage message) throws IOException {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.updateMessage(user, threadId, message);
	}

	@Override
	public void markThreadAsDeleted(Long userId,
			String threadId) {
		UserInfo user = userManager.getUserInfo(userId);
		threadManager.markThreadAsDeleted(user, threadId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreads(Long userId,
			String forumId, Long limit, Long offset, DiscussionThreadOrder order,
			Boolean ascending) {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.getThreadsForForum(user, forumId, limit, offset, order, ascending);
	}

	@Override
	public DiscussionReplyBundle createReply(Long userId, CreateDiscussionReply toCreate) throws IOException {
		UserInfo user = userManager.getUserInfo(userId);
		return replyManager.createReply(user, toCreate);
	}

	@Override
	public DiscussionReplyBundle getReply(Long userId, String replyId) {
		UserInfo user = userManager.getUserInfo(userId);
		return replyManager.getReply(user, replyId);
	}

	@Override
	public DiscussionReplyBundle updateReplyMessage(Long userId, String replyId, UpdateReplyMessage message) throws IOException {
		UserInfo user = userManager.getUserInfo(userId);
		return replyManager.updateReplyMessage(user, replyId, message);
	}

	@Override
	public void markReplyAsDeleted(Long userId, String replyId) {
		UserInfo user = userManager.getUserInfo(userId);
		replyManager.markReplyAsDeleted(user, replyId);
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getReplies(Long userId,
			String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending) {
		UserInfo user = userManager.getUserInfo(userId);
		return replyManager.getRepliesForThread(user, threadId, limit, offset, order, ascending);
	}

	@Override
	public MessageURL getThreadUrl(Long userId, String threadId) {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.getMessageUrl(user, threadId);
	}

	@Override
	public MessageURL getReplyUrl(Long userId, String replyId) {
		UserInfo user = userManager.getUserInfo(userId);
		return replyManager.getMessageUrl(user, replyId);
	}
}