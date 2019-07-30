package io.choerodon.test.manager.app.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import io.choerodon.agile.api.vo.ProductVersionDTO;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.test.manager.api.vo.TestIssuesUploadHistoryVO;
import io.choerodon.test.manager.api.vo.TestFileLoadHistoryVO;
import io.choerodon.test.manager.app.service.TestCaseService;
import io.choerodon.test.manager.app.service.TestFileLoadHistoryService;
import io.choerodon.test.manager.infra.dto.TestCycleDTO;
import io.choerodon.test.manager.infra.dto.TestFileLoadHistoryDTO;
import io.choerodon.test.manager.infra.dto.TestIssueFolderDTO;
import io.choerodon.test.manager.infra.enums.TestFileLoadHistoryEnums;
import io.choerodon.test.manager.infra.mapper.TestCycleMapper;
import io.choerodon.test.manager.infra.mapper.TestFileLoadHistoryMapper;
import io.choerodon.test.manager.infra.mapper.TestIssueFolderMapper;

@Component
public class TestFileLoadHistoryServiceImpl implements TestFileLoadHistoryService {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestFileLoadHistoryMapper testFileLoadHistoryMapper;

    @Autowired
    private TestIssueFolderMapper testIssueFolderMapper;

    @Autowired
    private TestCycleMapper cycleMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<TestFileLoadHistoryVO> queryIssues(Long projectId) {
        TestFileLoadHistoryVO testFileLoadHistoryVO = new TestFileLoadHistoryVO();
        testFileLoadHistoryVO.setCreatedBy(DetailsHelper.getUserDetails().getUserId());
        testFileLoadHistoryVO.setProjectId(projectId);

        List<TestFileLoadHistoryVO> historyDTOS = modelMapper.map(testFileLoadHistoryMapper
                        .queryDownloadFile(modelMapper.map(testFileLoadHistoryVO, TestFileLoadHistoryDTO.class)),
                new TypeToken<List<TestFileLoadHistoryVO>>() {
                }.getType());

        historyDTOS.stream().filter(v -> v.getSourceType().equals(1L)).forEach(v -> v
                .setName(testCaseService.getProjectInfo(v.getLinkedId()).getName()));
        historyDTOS.stream().filter(v -> v.getSourceType().equals(2L)).forEach(v ->
                v.setName(Optional.ofNullable(testCaseService.getVersionInfo(v.getProjectId())
                        .get(v.getLinkedId())).map(ProductVersionDTO::getName).orElse("版本已被删除")));
        historyDTOS.removeIf(v -> v.getSourceType().equals(3L));
        historyDTOS.stream().filter(v -> v.getSourceType().equals(4L)).forEach(v -> v.setName(Optional
                .ofNullable(testIssueFolderMapper.selectByPrimaryKey(v.getLinkedId()))
                .map(TestIssueFolderDTO::getName).orElse("文件夹已被删除")));

        return historyDTOS;
    }

    @Override
    public List<TestFileLoadHistoryVO> queryCycles(Long projectId) {
        TestCycleDTO testCycleDTO = new TestCycleDTO();
        TestFileLoadHistoryVO testFileLoadHistoryVO = new TestFileLoadHistoryVO();
        testFileLoadHistoryVO.setCreatedBy(DetailsHelper.getUserDetails().getUserId());
        testFileLoadHistoryVO.setProjectId(projectId);
        testFileLoadHistoryVO.setSourceType(3L);

        List<TestFileLoadHistoryVO> historyDTOS = modelMapper.map(queryDownloadFileByParameter(modelMapper
                .map(testFileLoadHistoryVO, TestFileLoadHistoryDTO.class)), new TypeToken<List<TestFileLoadHistoryVO>>() {
        }.getType());

        historyDTOS.stream().forEach(v -> {
            testCycleDTO.setCycleId(v.getLinkedId());
            v.setName(Optional.ofNullable(cycleMapper.selectOne(testCycleDTO)).map(TestCycleDTO::getCycleName).orElse("循环已被删除"));
        });
        return historyDTOS;
    }

    @Override
    public TestIssuesUploadHistoryVO queryLatestImportIssueHistory(Long projectId) {
        TestFileLoadHistoryDTO testFileLoadHistoryE = new TestFileLoadHistoryDTO();
        TestFileLoadHistoryDTO testFileLoadHistoryDTO = new TestFileLoadHistoryDTO();

        testFileLoadHistoryDTO.setCreatedBy(DetailsHelper.getUserDetails().getUserId());
        testFileLoadHistoryDTO.setActionType(TestFileLoadHistoryEnums.Action.UPLOAD_ISSUE.getTypeValue());
        testFileLoadHistoryDTO = queryLatestHistory(testFileLoadHistoryDTO);
        if (testFileLoadHistoryDTO == null) {
            return null;
        }

        TestIssuesUploadHistoryVO testIssuesUploadHistoryVO = modelMapper.map(testFileLoadHistoryE, TestIssuesUploadHistoryVO.class);

        TestIssueFolderDTO testIssueFolderDTO = new TestIssueFolderDTO();
        testIssueFolderDTO.setFolderId(testFileLoadHistoryE.getLinkedId());
        testIssueFolderDTO = testIssueFolderMapper.selectByPrimaryKey(testFileLoadHistoryDTO.getLinkedId());

        if (!ObjectUtils.isEmpty(testIssueFolderDTO)) {
            testIssuesUploadHistoryVO.setVersionName(testCaseService.getVersionInfo(projectId)
                    .get(testIssueFolderDTO.getVersionId()).getName());
        }

        return testIssuesUploadHistoryVO;
    }

    private List<TestFileLoadHistoryDTO> queryDownloadFileByParameter(TestFileLoadHistoryDTO testFileLoadHistoryE) {
        List<TestFileLoadHistoryDTO> res = testFileLoadHistoryMapper.select(testFileLoadHistoryE);
        Collections.sort(res, Comparator.comparing(TestFileLoadHistoryDTO::getCreationDate));
        return res;
    }

    @Override
    public TestFileLoadHistoryDTO queryLatestHistory(TestFileLoadHistoryDTO testFileLoadHistoryDTO) {
        List<TestFileLoadHistoryDTO> testFileLoadHistoryDTOS = testFileLoadHistoryMapper.queryLatestHistory(testFileLoadHistoryDTO);
        if (testFileLoadHistoryDTOS == null || testFileLoadHistoryDTOS.isEmpty()) {
            return null;
        }
        return modelMapper.map(testFileLoadHistoryDTOS.get(0), TestFileLoadHistoryDTO.class);
    }
}
