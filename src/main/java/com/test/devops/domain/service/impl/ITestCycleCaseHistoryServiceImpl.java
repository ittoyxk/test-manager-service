package com.test.devops.domain.service.impl;

import com.test.devops.domain.entity.TestCycleCaseHistoryE;
import com.test.devops.domain.entity.TestCycleCaseStepE;
import com.test.devops.domain.service.ITestCycleCaseHistoryService;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jialongZuo@hand-china.com on 6/11/18.
 */
@Component
public class ITestCycleCaseHistoryServiceImpl implements ITestCycleCaseHistoryService {
	@Override
	public TestCycleCaseHistoryE insert(TestCycleCaseHistoryE testCycleCaseHistoryE) {
		return testCycleCaseHistoryE.addSelf();
	}

	@Override
	public void delete(List<TestCycleCaseHistoryE> testCycleCaseHistoryE) {
		testCycleCaseHistoryE.forEach(v -> v.deleteSelf());
	}

	@Override
	public List<TestCycleCaseHistoryE> update(List<TestCycleCaseHistoryE> testCycleCaseHistoryE) {
		List<TestCycleCaseHistoryE> list = new ArrayList<>();
		testCycleCaseHistoryE.forEach(v -> list.add(v.updateSelf()));
		return list;
	}

	@Override
	public Page<TestCycleCaseHistoryE> query(TestCycleCaseHistoryE testCycleCaseHistoryE, PageRequest pageRequest) {
		return testCycleCaseHistoryE.querySelf(pageRequest);
	}
}
