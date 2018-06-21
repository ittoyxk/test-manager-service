package com.test.devops.domain.repository;

import com.test.devops.domain.entity.TestCycleCaseStepE;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import java.util.List;

/**
 * Created by jialongZuo@hand-china.com on 6/11/18.
 */
public interface TestCycleCaseStepRepository {
	TestCycleCaseStepE insert(TestCycleCaseStepE testCycleCaseStepE);

	void delete(TestCycleCaseStepE testCycleCaseStepE);

	TestCycleCaseStepE update(TestCycleCaseStepE testCycleCaseStepE);

	Page<TestCycleCaseStepE> query(TestCycleCaseStepE testCycleCaseStepE, PageRequest pageRequest);

	List<TestCycleCaseStepE> query(TestCycleCaseStepE testCycleCaseStepE);
}
