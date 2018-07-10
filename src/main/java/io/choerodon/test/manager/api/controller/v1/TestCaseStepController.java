package io.choerodon.test.manager.api.controller.v1;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.test.manager.api.dto.TestCaseStepDTO;
import io.choerodon.test.manager.app.service.TestCaseStepService;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Created by 842767365@qq.com on 6/11/18.
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/case/step")
public class TestCaseStepController {

    @Autowired
    TestCaseStepService iTestCaseStepService;


	@Permission(level = ResourceLevel.PROJECT)
	@ApiOperation("批量变动测试步骤(添加|修改)")
	@PutMapping("/batchInsert")
	public ResponseEntity<List<TestCaseStepDTO>> changeStep(@RequestBody List<TestCaseStepDTO> testCaseStepDTO) {

		return Optional.ofNullable(iTestCaseStepService.batchInsertStep(testCaseStepDTO))
				.map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
				.orElseThrow(() -> new CommonException("error.testCycleCase.query.cycleId"));
	}

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("变动一个测试步骤(添加|修改)")
    @PutMapping("/change")
    public ResponseEntity<Boolean> changeOneStep(@RequestBody TestCaseStepDTO testCaseStepDTO) {
        iTestCaseStepService.changeStep(testCaseStepDTO);
        return new ResponseEntity<>(true, HttpStatus.NO_CONTENT);
    }


    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("删除测试步骤")
    @DeleteMapping
    public ResponseEntity<Boolean> removeStep(@RequestBody TestCaseStepDTO testCaseStepDTO) {
        iTestCaseStepService.removeStep(testCaseStepDTO);
        return new ResponseEntity<>(true, HttpStatus.NO_CONTENT);
    }

}
