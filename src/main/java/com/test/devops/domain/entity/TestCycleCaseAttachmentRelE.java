package com.test.devops.domain.entity;

import com.test.devops.domain.repository.TestCycleCaseAttachmentRelRepository;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.Blob;
import java.util.List;

/**
 * Created by jialongZuo@hand-china.com on 6/11/18.
 */
@Component
@Scope("prototype")
public class TestCycleCaseAttachmentRelE {

	private Long id;
	private String attachmentType;
	private Long attachmentLinkId;
	private String attachmentName;
	private String url;
	private String comment;
	private Long objectVersionNumber;


	@Autowired
	private TestCycleCaseAttachmentRelRepository testCycleCaseAttachmentRelRepository;

	public List<TestCycleCaseAttachmentRelE> querySelf() {
		return testCycleCaseAttachmentRelRepository.query(this);
	}

	public TestCycleCaseAttachmentRelE addSelf() {
		return testCycleCaseAttachmentRelRepository.insert(this);
	}

	public TestCycleCaseAttachmentRelE updateSelf() {
		return testCycleCaseAttachmentRelRepository.update(this);
	}

	public void deleteSelf() {
		testCycleCaseAttachmentRelRepository.delete(this);
	}

	public String getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(String attachmentType) {

		this.attachmentType = attachmentType;
	}

	public Long getAttachmentLinkId() {
		return attachmentLinkId;
	}

	public void setAttachmentLinkId(Long attachmentLinkId) {
		this.attachmentLinkId = attachmentLinkId;
	}

	public String getAttachmentName() {
		return attachmentName;
	}

	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setObjectVersionNumber(Long objectVersionNumber) {
		this.objectVersionNumber = objectVersionNumber;
	}

	public TestCycleCaseAttachmentRelRepository getTestCycleCaseAttachmentRelRepository() {
		return testCycleCaseAttachmentRelRepository;
	}

	public void setTestCycleCaseAttachmentRelRepository(TestCycleCaseAttachmentRelRepository testCycleCaseAttachmentRelRepository) {
		this.testCycleCaseAttachmentRelRepository = testCycleCaseAttachmentRelRepository;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getObjectVersionNumber() {
		return objectVersionNumber;
	}
}
