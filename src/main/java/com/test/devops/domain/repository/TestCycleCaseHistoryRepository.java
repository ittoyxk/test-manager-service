package com.test.devops.domain.repository;

import com.test.devops.domain.entity.TestCycleCaseHistoryE;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import java.util.List;

/**
 * Created by jialongZuo@hand-china.com on 6/11/18.
 */
public interface TestCycleCaseHistoryRepository {
	TestCycleCaseHistoryE insert(TestCycleCaseHistoryE testCycleCaseHistoryE);

	void delete(TestCycleCaseHistoryE testCycleCaseHistoryE);

	TestCycleCaseHistoryE update(TestCycleCaseHistoryE testCycleCaseHistoryE);

	Page<TestCycleCaseHistoryE> query(TestCycleCaseHistoryE testCycleCaseHistoryE, PageRequest pageRequest);

	List<TestCycleCaseHistoryE> query(TestCycleCaseHistoryE testCycleCaseHistoryE);
}
