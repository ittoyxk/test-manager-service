package com.test.devops.domain.service;

import com.test.devops.domain.entity.TestCaseStepE;

import java.util.List;

/**
 * Created by jialongZuo@hand-china.com on 6/11/18.
 */
public interface ITestCaseStepService {

	List<TestCaseStepE> query(TestCaseStepE testCaseStepE);

	void removeStep(TestCaseStepE testCaseStepE);

	List<TestCaseStepE> batchInsertStep(List<TestCaseStepE> testCaseStepES);

	TestCaseStepE changeStep(TestCaseStepE testCaseStepE);

}
