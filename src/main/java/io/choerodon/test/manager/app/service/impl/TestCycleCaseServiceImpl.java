package io.choerodon.test.manager.app.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.choerodon.agile.api.vo.ProductVersionDTO;
import io.choerodon.agile.api.vo.SearchDTO;
import io.choerodon.agile.api.vo.UserDO;
import io.choerodon.agile.infra.common.utils.RankUtil;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.test.manager.api.vo.*;
import io.choerodon.test.manager.app.assembler.TestCaseAssembler;
import io.choerodon.test.manager.app.service.*;
import io.choerodon.test.manager.infra.dto.*;
import io.choerodon.test.manager.infra.enums.TestAttachmentCode;
import io.choerodon.test.manager.infra.enums.TestCycleCaseDefectCode;
import io.choerodon.test.manager.infra.enums.TestStatusType;
import io.choerodon.test.manager.infra.mapper.*;
import io.choerodon.test.manager.infra.util.ConvertUtils;
import io.choerodon.test.manager.infra.util.DBValidateUtil;
import io.choerodon.test.manager.infra.util.PageUtil;
import io.choerodon.test.manager.infra.util.VerifyUpdateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Created by 842767365@qq.com on 6/11/18.
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class TestCycleCaseServiceImpl implements TestCycleCaseService {

    private final static double AVG_NUM = 500.00;

    @Autowired
    private TestCycleCaseDefectRelService testCycleCaseDefectRelService;

    @Autowired
    private TestCycleCaseAttachmentRelService attachmentRelService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestStatusService testStatusService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestCycleMapper cycleMapper;

    @Autowired
    private TestCycleCaseMapper testCycleCaseMapper;

    @Autowired
    private TestStatusMapper testStatusMapper;

    @Autowired
    private TestCycleCaseDefectRelMapper testCycleCaseDefectRelMapper;

    @Autowired
    private TestCycleCaseStepMapper testCycleCaseStepMapper;

    @Autowired
    private TestCaseStepMapper testCaseStepMapper;

    @Autowired
    private TestPlanMapper testPlanMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TestCycleCaseStepService testCycleCaseStepService;

    @Autowired
    private TestAttachmentMapper testAttachmentMapper;

    @Autowired
    private TestCaseAssembler testCaseAssembler;

    @Autowired
    private TestCycleCaseAttachmentRelService testCycleCaseAttachmentRelService;

    @Autowired
    private TestIssueFolderMapper testIssueFolderMapper;

    @Autowired
    private TestCycleCaseHistoryMapper testCycleCaseHistory;

    @Autowired
    private TestCycleMapper testCycleMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private TestCycleService testCycleService;

    @Autowired
    private TestIssueFolderService testIssueFolderService;

    @Autowired
    private TestCaseAttachmentService testCaseAttachmentService;

    @Autowired
    private VerifyUpdateUtil verifyUpdateUtil;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long cycleCaseId, Long projectId) {
        TestCycleCaseVO dto = new TestCycleCaseVO();
        dto.setExecuteId(cycleCaseId);
        delete(modelMapper.map(dto, TestCycleCaseDTO.class));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchDelete(TestCycleCaseVO testCycleCaseVO, Long projectId) {
        List<TestCycleCaseDTO> list = testCycleCaseMapper.select(modelMapper.map(testCycleCaseVO, TestCycleCaseDTO.class));
        if (!ObjectUtils.isEmpty(list)) {
            list.forEach(v -> delete(v.getExecuteId(), projectId));
        }
    }

    @Override
    public PageInfo<TestCycleCaseVO> queryByCycle(TestCycleCaseVO dto, Pageable pageable, Long projectId, Long organizationId) {

        //找到所有的子阶段
        List<TestCycleDTO> testCycleES = cycleMapper.queryChildCycle(modelMapper.map(dto, TestCycleDTO.class));
        //装配值进DTO中
        List<TestCycleCaseVO> testCycleCaseVOS = new ArrayList<>();
        testCycleCaseVOS.add(dto);
        testCycleES.forEach(v -> {
                    TestCycleCaseVO tempDTO = new TestCycleCaseVO();
                    tempDTO.setAssignedTo(dto.getAssignedTo());
                    tempDTO.setLastUpdatedBy(dto.getLastUpdatedBy());
                    tempDTO.setCycleId(v.getCycleId());
                    tempDTO.setCycleName(v.getCycleName());
                    testCycleCaseVOS.add(tempDTO);
                }
        );

        PageInfo<TestCycleCaseDTO> serviceEPage = queryByFatherCycle(modelMapper.map(testCycleCaseVOS,
                new TypeToken<List<TestCycleCaseDTO>>() {
                }.getType()), pageable);

        PageInfo<TestCycleCaseVO> dots = PageUtil.buildPageInfoWithPageInfoList(serviceEPage, modelMapper.map(serviceEPage.getList(), new TypeToken<List<TestCycleCaseVO>>() {
        }.getType()));
        List<TestCycleCaseVO> cycleCaseDTOS = dots.getList();
        Long[] issues = cycleCaseDTOS.stream().map(TestCycleCaseVO::getIssueId).toArray(Long[]::new);

        if (!ObjectUtils.isEmpty(dto.getSearchDTO())) {
            //先去敏捷筛选issue
            Map map = new HashMap<String, Long[]>();
            map.put("issueIds", issues);
            dto.getSearchDTO().setOtherArgs(map);
            Map<Long, IssueInfosVO> filterMap = testCaseService.getIssueInfoMap(projectId, dto.getSearchDTO(), false, organizationId);
            List<TestCycleCaseVO> filterCase = new ArrayList<>();
            //根据筛选出来的issueId,进行本地筛选
            for (TestCycleCaseVO caseDTO : cycleCaseDTOS) {
                if (filterMap.keySet().contains(caseDTO.getIssueId())) {
                    filterCase.add(caseDTO);
                }
            }
            dots.setList(filterCase);
        }

        populateCycleCaseWithDefect(dots.getList(), projectId, organizationId, true);
        populateUsers(dots.getList());
        return dots;
    }

    @Override
    public PageInfo<TestCycleCaseVO> queryByCycleWithFilterArgs(Long cycleId, Pageable pageable, Long projectId, TestCycleCaseVO testCycleCaseVO) {
        testCycleCaseVO = Optional.ofNullable(testCycleCaseVO).orElseGet(TestCycleCaseVO::new);
        testCycleCaseVO.setCycleId(cycleId);
        PageInfo<TestCycleCaseDTO> serviceEPage = queryWithPageRequest(modelMapper.map(testCycleCaseVO, TestCycleCaseDTO.class), pageable);
        PageInfo<TestCycleCaseVO> dots = PageUtil.buildPageInfoWithPageInfoList(serviceEPage, modelMapper.map(serviceEPage.getList(), new TypeToken<List<TestCycleCaseVO>>() {
        }.getType()));
        populateUsers(dots.getList());
        return dots;
    }

    @Override
    public List<TestCycleCaseVO> queryByIssuse(Long issueId, Long projectId, Long organizationId) {
        List<TestCycleCaseVO> dto = modelMapper.map(testCycleCaseMapper.queryByIssue(issueId), new TypeToken<List<TestCycleCaseVO>>() {
        }.getType());
        if (ObjectUtils.isEmpty(dto)) {
            return new ArrayList<>();
        }
        populateCycleCaseWithDefect(dto, projectId, organizationId, false);
        populateUsers(dto);
        populateVersionBuild(projectId, dto);
        return dto;
    }


    /**
     * 查询issues的cycleCase 在生成报表处使用
     *
     * @param issueIds
     * @param projectId
     * @return
     */
    @Override
    public List<TestCycleCaseVO> queryInIssues(Long[] issueIds, Long projectId, Long organizationId) {
        if (issueIds == null || issueIds.length == 0) {
            return new ArrayList<>();
        }
        Assert.notEmpty(issueIds, "erorr.query.cycle.in.issues.issueIds.not.null");
        List<TestCycleCaseVO> dto = modelMapper.map(testCycleCaseMapper.queryInIssues(issueIds), new TypeToken<List<TestCycleCaseVO>>() {
        }.getType());
        if (dto == null || dto.isEmpty()) {
            return new ArrayList<>();
        }
        populateCycleCaseWithDefect(dto, projectId, organizationId, false);
        return dto;
    }

    @Override
    public List<TestCycleCaseVO> queryCaseAllInfoInCyclesOrVersions(Long[] cycleIds, Long[] versionIds, Long projectId, Long organizationId) {
        Assert.notNull(cycleIds, "error.query.case.in.versions.project.not.be.null");

        if (!(ObjectUtils.isEmpty(cycleIds) ^ ObjectUtils.isEmpty(versionIds))) {
            Assert.notEmpty(cycleIds, "erorr.query.cycle.in.issues.issueIds.not.null");
        }

        List<TestCycleCaseVO> dto = modelMapper.map(testCycleCaseMapper.queryCaseAllInfoInCyclesOrVersions(cycleIds, versionIds), new TypeToken<List<TestCycleCaseVO>>() {
        }.getType());
        populateCycleCaseWithDefect(dto, projectId, organizationId, false);
        populateUsers(dto);
        return dto;
    }

    /**
     * 将实例查询的Issue信息和缺陷关联的Issue信息合并到一起，为了减少一次外部调用。
     *
     * @param testCycleCaseVOS
     * @param projectId
     */
    private void populateCycleCaseWithDefect(List<TestCycleCaseVO> testCycleCaseVOS, Long projectId, Long organizationId, Boolean needDetails) {
        List<TestCycleCaseDefectRelVO> list = new ArrayList<>();
        for (TestCycleCaseVO v : testCycleCaseVOS) {
            List<TestCycleCaseDefectRelVO> defects = v.getDefects();
            Optional.ofNullable(defects).ifPresent(list::addAll);
            Optional.ofNullable(v.getSubStepDefects()).ifPresent(list::addAll);
        }

        Long[] issueLists = Stream.concat(list.stream().map(TestCycleCaseDefectRelVO::getIssueId),
                testCycleCaseVOS.stream().map(TestCycleCaseVO::getIssueId)).filter(Objects::nonNull).distinct()
                .toArray(Long[]::new);
        if (ObjectUtils.isEmpty(issueLists)) {
            return;
        }
        Map<Long, IssueInfosVO> defectMap = testCaseService.getIssueInfoMap(projectId, issueLists, needDetails, organizationId);
        list.forEach(v -> v.setIssueInfosVO(defectMap.get(v.getIssueId())));
        testCycleCaseVOS.forEach(v -> v.setIssueInfosVO(defectMap.get(v.getIssueId())));
    }


    private void populateVersionBuild(Long projectId, List<TestCycleCaseVO> dto) {
        Map<Long, ProductVersionDTO> map = testCaseService.getVersionInfo(projectId);
        if (ObjectUtils.isEmpty(map)) {
            return;
        }

        for (TestCycleCaseVO cases : dto) {
            TestCycleDTO testCycleDTO = new TestCycleDTO();
            testCycleDTO.setCycleId(cases.getCycleId());

//            List<TestCycleCaseDTO> list = queryWithAttachAndDefect(modelMapper.map(dto, TestCycleCaseDTO.class), PageRequest.of(1, 1));
//            DBValidateUtil.executeAndvalidateUpdateNum(list::size, 1, "error.cycle.case.query.not.found");

//            Long versionId = modelMapper.map(list.get(0), TestCycleCaseVO.class).getVersionId();
            Long versionId = cycleMapper.selectOne(testCycleDTO).getVersionId();
            Assert.notNull(versionId, "error.version.id.not.null");
            Optional.ofNullable(map.get(versionId)).ifPresent(v -> cases.setVersionName(v.getName()));
        }

    }

    @Override
    public TestCycleCaseVO queryOne(Long cycleCaseId, Long projectId, Long cycleId, Long organizationId) {
        TestCycleCaseVO sourceTestCycleCaseVO = new TestCycleCaseVO();
        sourceTestCycleCaseVO.setCycleId(cycleId);
        //找到所有的子阶段
        List<TestCycleDTO> testCycleES = cycleMapper.queryChildCycle(modelMapper.map(sourceTestCycleCaseVO, TestCycleDTO.class));

        TestCycleCaseVO testCycleCaseVO = new TestCycleCaseVO();
        testCycleCaseVO.setExecuteId(cycleCaseId);

        List<TestCycleCaseDTO> list = queryWithAttachAndDefect(modelMapper.map(testCycleCaseVO, TestCycleCaseDTO.class), PageRequest.of(1, 1));
        DBValidateUtil.executeAndvalidateUpdateNum(list::size, 1, "error.cycle.case.query.not.found");

        TestCycleCaseDTO testCycleCaseDTO = list.get(0);
        TestCycleCaseVO vo = modelMapper.map(testCycleCaseDTO, TestCycleCaseVO.class);
        if (testCycleCaseDTO.getCaseAttachment() != null && !testCycleCaseDTO.getCaseAttachment().isEmpty()) {
            vo.setCaseAttachment(modelMapper.map(testCycleCaseDTO.getCaseAttachment(), new TypeToken<List<TestCycleCaseAttachmentRelVO>>() {
            }.getType()));
        }
        if (testCycleCaseDTO.getCaseDefect() != null && !testCycleCaseDTO.getCaseDefect().isEmpty()) {
            vo.setCaseDefect(modelMapper.map(testCycleCaseDTO.getCaseDefect(), new TypeToken<List<TestCycleCaseDefectRelVO>>() {
            }.getType()));
        }
        if (testCycleCaseDTO.getCycleCaseStep() != null && !testCycleCaseDTO.getCycleCaseStep().isEmpty()) {
            vo.setCycleCaseStep(modelMapper.map(testCycleCaseDTO.getCycleCaseStep(), new TypeToken<List<TestCycleCaseStepVO>>() {
            }.getType()));
        }
        if (testCycleCaseDTO.getSubStepDefects() != null && !testCycleCaseDTO.getSubStepDefects().isEmpty()) {
            vo.setSubStepDefects(modelMapper.map(testCycleCaseDTO.getSubStepDefects(), new TypeToken<List<TestCycleCaseDefectRelDTO>>() {
            }.getType()));
        }
        testCycleCaseDefectRelService.populateDefectAndIssue(vo, projectId, organizationId);
        userService.populateTestCycleCaseDTO(vo);

        if (!testCycleES.isEmpty()) {
            if (vo.getLastExecuteId() == null) {
                Sort sort = Sort.by(new Sort.Order(Sort.Direction.DESC, "rank"), new Sort.Order(Sort.Direction.ASC, "cycle_id"));
                PageRequest pageable = PageRequest.of(1, 400, sort);
                updateExecuteId(testCycleES, pageable, projectId, vo.getCycleId(), organizationId, vo, 0L);
            }
            if (vo.getNextExecuteId() == null) {
                Pageable pageable = PageRequest.of(1, 400);
                List<Sort.Order> sort = new ArrayList<>();
                sort.add(new Sort.Order(Sort.Direction.ASC, "cycle_id"));
                sort.add(new Sort.Order(Sort.Direction.ASC, "rank"));
                pageable.getSort();
                updateExecuteId(testCycleES, pageable, projectId, vo.getCycleId(), organizationId, vo, 1L);
            }
        }
        return vo;
    }

    private void updateExecuteId(List<TestCycleDTO> testCycleES, Pageable pageable, Long projectId, Long cycleId, Long organizationId, TestCycleCaseVO dto, Long flag) {
        Long stageId = getTestStageId(testCycleES, flag, cycleId);
        TestCycleCaseVO tmpTestCycleCaseVO = new TestCycleCaseVO();
        Long targetExecuteId;

        if (stageId != 0) {
            tmpTestCycleCaseVO.setCycleId(stageId);
            PageInfo<TestCycleCaseVO> testCycleCaseDTOPage = queryByCycle(tmpTestCycleCaseVO, pageable, projectId, organizationId);
            if (flag == 0) {
                targetExecuteId = testCycleCaseDTOPage.getList().get(0).getExecuteId();
                dto.setLastExecuteId(targetExecuteId);
            } else {
                targetExecuteId = testCycleCaseDTOPage.getList().get(0).getExecuteId();
                dto.setNextExecuteId(targetExecuteId);
            }
        }
    }

    private Long getTestStageId(List<TestCycleDTO> testCycleES, Long flag, Long currentCycleId) {
        int index = 0;
        Long result;
        for (TestCycleDTO testCycleE : testCycleES) {
            if (testCycleE.getCycleId().equals(currentCycleId)) {
                break;
            }
            index++;
        }
        if (flag == 0L) {
            if (checkoutListLength(testCycleES.size(), index - 1)) {
                result = testCycleES.get(index - 1).getCycleId();
            } else return 0L;
        } else {
            if (checkoutListLength(testCycleES.size(), index + 1)) {
                result = testCycleES.get(index + 1).getCycleId();
            } else return 0L;
        }
        return result;
    }

    private boolean checkoutListLength(int size, int index) {
        return index >= 0 && index < size;
    }

    private void populateUsers(List<TestCycleCaseVO> users) {
        List<Long> usersId = new ArrayList<>();
        users.stream().forEach(v -> {
            Optional.ofNullable(v.getAssignedTo()).ifPresent(usersId::add);
            Optional.ofNullable(v.getLastUpdatedBy()).ifPresent(usersId::add);
        });
        List<Long> ids = usersId.stream().distinct().filter(v -> !v.equals(Long.valueOf(0))).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(ids)) {
            Map<Long, UserDO> userMaps = userService.query(ids.toArray(new Long[ids.size()]));
            users.forEach(v -> {
                Optional.ofNullable(userMaps.get(v.getAssignedTo())).ifPresent(v::setAssigneeUser);
                Optional.ofNullable(userMaps.get(v.getLastUpdatedBy())).ifPresent(v::setLastUpdateUser);

            });
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TestCycleCaseVO create(TestCycleCaseVO testCycleCaseVO, Long projectId) {
        if (ObjectUtils.isEmpty(testCycleCaseVO.getExecutionStatus())) {
            testCycleCaseVO.setExecutionStatus(testStatusService.getDefaultStatusId(TestStatusType.STATUS_TYPE_CASE));
        }
        testCycleCaseVO.setLastRank(testCycleCaseMapper.getLastedRank(testCycleCaseVO.getCycleId()));
        return runTestCycleCase(testCycleCaseVO, projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<TestCycleCaseVO> batchCreateForAutoTest(List<TestCycleCaseVO> list, Long projectId) {
        if (list == null || list.isEmpty()) {
            throw new CommonException("error.cycle.case.list.empty");
        }
        Date now = new Date();
        List<TestCycleCaseDTO> testCycleCaseDTOS = new ArrayList<>();
        for (TestCycleCaseVO testCycleCaseVO : list) {
            if (testCycleCaseVO == null || testCycleCaseVO.getExecuteId() != null) {
                throw new CommonException("error.cycle.case.insert.executeId.should.be.null");
            }
            TestCycleCaseDTO testCycleCaseDTO = modelMapper.map(testCycleCaseVO, TestCycleCaseDTO.class);
            testCycleCaseDTO.setCreationDate(now);
            testCycleCaseDTO.setLastUpdateDate(now);
            testCycleCaseDTO.setProjectId(projectId);
            testCycleCaseDTOS.add(testCycleCaseDTO);
        }
        testCycleCaseMapper.batchInsertTestCycleCases(testCycleCaseDTOS);

        return modelMapper.map(testCycleCaseDTOS, new TypeToken<List<TestCycleCaseVO>>() {
        }.getType());
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public TestCycleCaseVO changeOneCase(TestCycleCaseVO testCycleCaseVO, Long projectId) {
        TestCycleCaseVO dto = modelMapper.map(changeStep(projectId, testCycleCaseVO), TestCycleCaseVO.class);
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchChangeCase(Long projectId, List<TestCycleCaseVO> cycleCaseDTOS) {
        for (TestCycleCaseVO cycleCaseDTO : cycleCaseDTOS) {
            testStatusService.populateStatus(cycleCaseDTO);
            TestCycleCaseVO dto = modelMapper.map(changeStep(projectId, cycleCaseDTO), TestCycleCaseVO.class);
            userService.populateTestCycleCaseDTO(dto);
        }
    }


    @Override
    public List<Long> getActiveCase(Long range, Long projectId, String day) {
        List<Long> caseCountList = new ArrayList<>();
        LocalDate date = LocalDate.parse(day);
        for (int i = range.intValue() - 1; i >= 0; i--) {
            caseCountList.add(new RedisAtomicLong("summary:" + projectId + ":" + date.minusDays(i)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    , redisTemplate.getConnectionFactory()).get());
        }
        return caseCountList;
    }

    @Override
    public Long countCaseNotRun(Long projectId) {
        List<Long> cycleIds = getCycleIdByProjectId(projectId);
        return ObjectUtils.isEmpty(cycleIds) ? 0L : testCycleCaseMapper.countCaseNotRun(cycleIds.stream().toArray(Long[]::new));
    }

    @Override
    public Long countCaseNotPlain(Long projectId) {
        List<Long> cycleIds = getCycleIdByProjectId(projectId);
        return ObjectUtils.isEmpty(cycleIds) ? 0L : testCycleCaseMapper.countCaseNotPlain(cycleIds.stream().toArray(Long[]::new));
    }

    @Override
    public Long countCaseSum(Long projectId) {
        List<Long> cycleIds = getCycleIdByProjectId(projectId);
        return ObjectUtils.isEmpty(cycleIds) ? 0L : testCycleCaseMapper.countCaseSum(cycleIds.stream().toArray(Long[]::new));
    }


    public void delete(TestCycleCaseDTO testCycleCaseDTO) {
        Optional.ofNullable(testCycleCaseMapper.select(testCycleCaseDTO)).ifPresent(m ->
                m.forEach(this::deleteCaseWithSubStep));
    }

    private void deleteCaseWithSubStep(TestCycleCaseDTO testCycleCaseDTO) {
        deleteByTestCycleCase(testCycleCaseDTO);
        attachmentRelService.delete(testCycleCaseDTO.getExecuteId(), TestAttachmentCode.ATTACHMENT_CYCLE_CASE);
        deleteLinkedDefect(testCycleCaseDTO.getExecuteId());
        testCycleCaseMapper.delete(testCycleCaseDTO);
    }

    private void deleteLinkedDefect(Long executeId) {
        TestCycleCaseDefectRelDTO testCycleCaseDefectRelDTO = new TestCycleCaseDefectRelDTO();
        testCycleCaseDefectRelDTO.setDefectLinkId(executeId);
        testCycleCaseDefectRelDTO.setDefectType(TestCycleCaseDefectCode.CYCLE_CASE);
        testCycleCaseDefectRelMapper.select(testCycleCaseDefectRelDTO).forEach(v -> testCycleCaseDefectRelMapper.delete(v));
    }

    private void deleteByTestCycleCase(TestCycleCaseDTO testCycleCaseDTO) {
        TestCycleCaseStepDTO testCycleCaseStepDTO = new TestCycleCaseStepDTO();
        testCycleCaseStepDTO.setExecuteId(testCycleCaseDTO.getExecuteId());
        deleteStep(testCycleCaseStepDTO);
    }

    private void deleteStep(TestCycleCaseStepDTO testCycleCaseStepDTO) {
        Optional.ofNullable(testCycleCaseStepMapper.select(testCycleCaseStepDTO)).ifPresent(
                m -> m.forEach(v -> {
                    attachmentRelService.delete(v.getExecuteStepId(), TestAttachmentCode.ATTACHMENT_CYCLE_STEP);
                    deleteLinkedDefect(v.getExecuteStepId());
                })
        );
        testCycleCaseStepMapper.delete(testCycleCaseStepDTO);
    }

    private PageInfo<TestCycleCaseDTO> queryByFatherCycle(List<TestCycleCaseDTO> testCycleCaseES, Pageable pageable) {
        List<TestCycleCaseDTO> converts = modelMapper.map(testCycleCaseES, new TypeToken<List<TestCycleCaseDTO>>() {
        }.getType());
        List<TestCycleCaseDTO> dtos = queryByFatherCycleWithDataBase(converts, pageable);
        Long total = 0L;
        for (TestCycleCaseDTO convert : converts) {
            total += testCycleCaseMapper.queryWithAttachAndDefect_count(convert);
        }
        if (dtos.isEmpty() && total != 0L) {
            pageable = PageRequest.of(((total.intValue() / pageable.getPageSize()) - 1), pageable.getPageSize(), pageable.getSort());
            dtos = queryByFatherCycleWithDataBase(converts, pageable);
        }
        Page page = new Page<>(pageable.getPageNumber(), pageable.getPageSize());
        page.setTotal(total);
        page.addAll(dtos);
        return page.toPageInfo();
    }

    private List<TestCycleCaseDTO> queryByFatherCycleWithDataBase(List<TestCycleCaseDTO> converts, Pageable pageable) {
        return testCycleCaseMapper.queryByFatherCycleWithAttachAndDefect(converts,
                (pageable.getPageNumber() - 1) * pageable.getPageSize(),
                pageable.getPageSize(), PageUtil.sortToSql(pageable.getSort()));
    }

    private PageInfo<TestCycleCaseDTO> queryWithPageRequest(TestCycleCaseDTO testCycleCaseDTO, Pageable pageable) {
        List<TestCycleCaseDTO> dto = queryWithAttachAndDefect(testCycleCaseDTO, pageable);
        return new PageInfo<>(Optional.ofNullable(dto).orElseGet(ArrayList::new));
    }

    public List<TestCycleCaseDTO> queryWithAttachAndDefect(TestCycleCaseDTO convert, Pageable pageable) {
        return testCycleCaseMapper.queryWithAttachAndDefect(convert, (pageable.getPageNumber() - 1) * pageable.getPageSize(), pageable.getPageSize());
    }

    @Override
    public ExecutionStatusVO queryExecuteStatus(Long projectId, Long planId, Long cycleId) {
        Long total = 0L;
        Set<Long> cycleIds = new HashSet<>();
        // 查询文件夹下所有的目录
        if (!ObjectUtils.isEmpty(cycleId)) {
            cycleIds.addAll(queryCycleIds(cycleId, planId));
        }
        // 查询项目下自定义和默认状态
        TestStatusDTO testStatusDTO = new TestStatusDTO();
        testStatusDTO.setProjectId(projectId);
        testStatusDTO.setStatusType("CYCLE_CASE");
        List<TestStatusDTO> testStatusDTOList = testStatusMapper.queryAllUnderProject(testStatusDTO);
        testStatusDTOList.stream().forEach(e -> e.setCount(0L));
        List<TestStatusDTO> testStatusDTOS = testCycleCaseMapper.queryExecutionStatus(planId, cycleIds);
        if (!CollectionUtils.isEmpty(testStatusDTOS)) {
            for (TestStatusDTO test : testStatusDTOS) {
                total += test.getCount();
                testStatusDTOList.forEach(status -> {
                    if (test.getStatusId().equals(status.getStatusId())) {
                        status.setCount(test.getCount());
                    }
                });
            }
        }
        List<TestStatusVO> testStatusVOList = modelMapper.map(testStatusDTOList, new TypeToken<List<TestStatusVO>>() {
        }.getType());

        return new ExecutionStatusVO(total, testStatusVOList);
    }

    @Override
    public void update(TestCycleCaseVO testCycleCaseVO) {
        TestCycleCaseDTO testCycleCaseDTO = modelMapper.map(testCycleCaseVO, TestCycleCaseDTO.class);
        if (!StringUtils.isEmpty(testCycleCaseVO.getLastRank()) || !StringUtils.isEmpty(testCycleCaseVO.getNextRank())) {
            testCycleCaseDTO.setRank(RankUtil.Operation.UPDATE.getRank(testCycleCaseVO.getLastRank(), testCycleCaseVO.getNextRank()));
        }
        baseUpdate(testCycleCaseDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCaseAndStep(Long projectId,TestCycleCaseUpdateVO testCycleCaseUpdateVO) {
        List<TestCycleCaseStepUpdateVO> testCycleCaseStepVOList = testCycleCaseUpdateVO.getTestCycleCaseStepUpdateVOS();
        List<TestCycleCaseStepVO> newTestCycleCaseStepVOS = modelMapper.map(testCycleCaseStepVOList, new TypeToken<List<TestCycleCaseStepVO>>() {
        }.getType());
        TestCycleCaseStepDTO testCycleCaseStepDTO = new TestCycleCaseStepDTO();
        testCycleCaseStepDTO.setExecuteId(testCycleCaseUpdateVO.getExecuteId());
        List<TestCycleCaseStepDTO> oldTestCycleCaseStepDTOS = testCycleCaseStepMapper.select(testCycleCaseStepDTO);
        List<Long> noldStepIds = oldTestCycleCaseStepDTOS.stream().map(TestCycleCaseStepDTO::getExecuteStepId).collect(Collectors.toList());
        newTestCycleCaseStepVOS.forEach(newcCycle -> {
            if (!noldStepIds.contains(newcCycle.getExecuteStepId())) {
                //添加步骤
                testCycleCaseStepService.create(newcCycle);
            }else {
                //更新步骤
                testCycleCaseStepService.update(newcCycle);
            }
        });
        List<Long> newIds = newTestCycleCaseStepVOS.stream().map(TestCycleCaseStepVO::getExecuteStepId).collect(Collectors.toList());
        oldTestCycleCaseStepDTOS.forEach(oldeCycle -> {
            if (!newIds.contains(oldeCycle.getExecuteStepId())) {
                //删除步骤
                testCycleCaseStepService.delete(oldeCycle.getExecuteStepId());
            }
        });
        //3.更新执行用例
        TestCycleCaseDTO testCycleCaseDTO = modelMapper.map(testCycleCaseUpdateVO, TestCycleCaseDTO.class);
        baseUpdate(testCycleCaseDTO);
    }

    @Override
    public PageInfo<TestFolderCycleCaseVO> listAllCaseByCycleId(Long projectId, Long planId, Long cycleId, Pageable pageable, SearchDTO searchDTO) {
        // 查询文件夹下所有的目录
        Set<Long> cycleIds = new HashSet<>();
        if(!ObjectUtils.isEmpty(cycleId)){
            cycleIds.addAll(queryCycleIds(cycleId, planId));
        }
        // 查询文件夹下的的用例
        PageInfo<TestCycleCaseDTO> caseDTOPageInfo = PageHelper.startPage(pageable.getPageNumber(), pageable.getPageSize()).doSelectPageInfo(() ->
                testCycleCaseMapper.queryFolderCycleCase(planId, cycleIds, searchDTO));
        List<TestFolderCycleCaseVO> testFolderCycleCaseVOS = caseDTOPageInfo.getList().stream().map(testCaseAssembler::setAssianUser).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(testFolderCycleCaseVOS)){
            return new PageInfo<TestFolderCycleCaseVO>();
        }

        List<Long> executedIds = testFolderCycleCaseVOS.stream().map(TestFolderCycleCaseVO::getExecuteId).collect(Collectors.toList());
        //对比是否更新
        List<Long> caseIds = testFolderCycleCaseVOS.stream().map(TestFolderCycleCaseVO::getCaseId).collect(Collectors.toList());

        Map<Long, CaseCompareVO> cycleCaseMap = testCycleCaseMapper.queryTestCaseMap(executedIds);
        Map<Long, CaseCompareVO> caseMap = testCaseMapper.queryTestCaseMap(caseIds, executedIds);
        List<TestCaseDTO> testCaseDTOS = testCaseService.listByCaseIds(projectId, caseIds);
        Map<Long, TestCaseDTO> testCaseMap = testCaseDTOS.stream().collect(Collectors.toMap(TestCaseDTO::getCaseId, Function.identity()));
        testFolderCycleCaseVOS.forEach(cycleCase -> {
            Long caseId = cycleCase.getCaseId();
            TestCaseDTO testCaseDTO = testCaseMap.get(caseId);
            if (ObjectUtils.isEmpty(testCaseDTO)) {
                return;
            }
            Boolean hasChange = false;
            CaseCompareVO cycleCaseVo = cycleCaseMap.get(caseId);
            CaseCompareVO caseVo = caseMap.get(caseId);
            Boolean changeCase = false;
            Boolean changeStep = false;
            Boolean changeAttach = false;
            if (cycleCase.getVersionNum() < testCaseDTO.getVersionNum()) {
                if (!ObjectUtils.isEmpty(caseVo)) {
                    if (!Objects.equals(JSON.toJSON(cycleCaseVo.getTestCase()), JSON.toJSON(caseVo.getTestCase()))) {
                        hasChange = true;
                        changeCase = true;
                    }
                    if (!Objects.equals(JSON.toJSON(cycleCaseVo.getCaseStep()), JSON.toJSON(cycleCaseVo.getCaseStep()))) {
                        hasChange = true;
                        changeStep = true;
                    }
                    if (!Objects.equals(JSON.toJSON(cycleCaseVo.getCaseAttach()), JSON.toJSON(cycleCaseVo.getCaseAttach()))) {
                        hasChange = true;
                        changeAttach = true;
                    }
                }
            }
            cycleCase.setChangeCase(changeCase);
            cycleCase.setChangeStep(changeStep);
            cycleCase.setChangeAttach(changeAttach);
            cycleCase.setHasChange(hasChange);

        });

        PageInfo<TestFolderCycleCaseVO> testFolderCycleCaseVOPageInfo = modelMapper.map(caseDTOPageInfo, PageInfo.class);
        testFolderCycleCaseVOPageInfo.setList(testFolderCycleCaseVOS);
        return testFolderCycleCaseVOPageInfo;
    }

    @Override
    public TestCycleCaseInfoVO queryCycleCaseInfo(Long executeId, Long projectId, Long planId, Long cycleId, SearchDTO searchDTO) {
        Set<Long> cycleIds = new HashSet<>();
        if (!ObjectUtils.isEmpty(cycleId)) {
            cycleIds.addAll(queryCycleIds(cycleId, planId));
        }
        // 查询循环下的用例
        List<TestCycleCaseDTO> testCycleCaseDTOS = testCycleCaseMapper.queryFolderCycleCase(planId, cycleIds, searchDTO);
        int index = 0;
        TestCycleCaseDTO testCycleCaseDTO = null;
        for (TestCycleCaseDTO cyclecase : testCycleCaseDTOS) {
            if (cyclecase.getExecuteId().equals(executeId)) {
                testCycleCaseDTO = cyclecase;
                index = testCycleCaseDTOS.indexOf(cyclecase);
            }
        }
        if(ObjectUtils.isEmpty(testCycleCaseDTO)){
            throw new CommonException("error.cycle.case.not.exist");
        }
        TestCycleCaseInfoVO testCycleCaseInfoVO = modelMapper.map(testCycleCaseDTO, TestCycleCaseInfoVO.class);
        TestPlanDTO testPlanDTO = testPlanMapper.selectByPrimaryKey(planId);
        testCycleCaseInfoVO.setExecutorDate(testCycleCaseDTO.getLastUpdateDate());
        testCycleCaseInfoVO.setPlanStatus(testPlanDTO.getStatusCode());
        previousNextId(index,testCycleCaseDTOS,testCycleCaseInfoVO);
        return testCaseAssembler.cycleCaseExtraInfo(testCycleCaseInfoVO);
    }

    @Override
    public void batchInsertByTestCase(Map<Long, TestCycleDTO> testCycleMap, List<TestCaseDTO> testCaseDTOS) {
        List<Long> caseIds = testCaseDTOS.stream().map(TestCaseDTO::getCaseId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(caseIds)) {
            return;
        }
        // 获取case关联的步骤
        List<TestCaseStepDTO> testCaseStepDTOS = testCaseStepMapper.listByCaseIds(caseIds);
        Map<Long, List<TestCaseStepDTO>> caseStepMap = testCaseStepDTOS.stream().collect(Collectors.groupingBy(TestCaseStepDTO::getIssueId));
        // 获取case关联的附件
        List<TestCaseAttachmentDTO> attachmentDTOS = testAttachmentMapper.listByCaseIds(caseIds);
        Map<Long, List<TestCaseAttachmentDTO>> attachmentMap = attachmentDTOS.stream().collect(Collectors.groupingBy(TestCaseAttachmentDTO::getCaseId));
        // 插入
        Long defaultStatusId = testStatusService.getDefaultStatusId(TestStatusType.STATUS_TYPE_CASE);
        List<TestCycleCaseDTO> testCycleCaseDTOS = caseToCycleCase(testCaseDTOS, testCycleMap, defaultStatusId);
        List<List<TestCycleCaseDTO>> lists = ConvertUtils.averageAssign(testCycleCaseDTOS, (int) Math.ceil(testCycleCaseDTOS.size() / AVG_NUM == 0 ? 1 : testCycleCaseDTOS.size() / AVG_NUM));

        List<TestCycleCaseDTO> testCycleCaseDTOList = new ArrayList<>();
        lists.forEach(v -> {
            bathcInsert(v);
            testCycleCaseDTOList.addAll(v);
        });
        // 同步步骤
        testCycleCaseStepService.batchInsert(testCycleCaseDTOList, caseStepMap);
        // 同步附件
        testCycleCaseAttachmentRelService.batchInsert(testCycleCaseDTOList, attachmentMap);
    }

    @Override
    public TestCycleCaseUpdateVO queryCaseAndStep(Long executeId) {
        TestCycleCaseDTO testCycleCaseDTO = testCycleCaseMapper.selectByPrimaryKey(executeId);
        return testCaseAssembler.dtoToUpdateVO(testCycleCaseDTO);
    }

    @Override
    public void batchAssignCycleCase(Long projectId, Long userId, List<Long> cycleCaseId) {
        if (CollectionUtils.isEmpty(cycleCaseId)) {
            throw new CommonException("error cycleCase id is null ");
        }
        testCycleCaseMapper.batchAssign(userId, cycleCaseId);
    }

    @Override
    public void baseUpdate(TestCycleCaseDTO testCycleCaseDTO) {
       if( testCycleCaseMapper.updateByPrimaryKeySelective(testCycleCaseDTO)!=1){
           throw new CommonException("error.update.cycle.case");
       }
    }


    @Override
    public void batchDeleteByExecuteIds(List<Long> executeIds) {
        if (CollectionUtils.isEmpty(executeIds)) {
            return;
        }
        // 删除步骤
        testCycleCaseStepMapper.batchDeleteByExecutIds(executeIds);
        // 删除附件信息
        testCycleCaseAttachmentRelService.batchDeleteByExecutIds(executeIds);
        // 删除测试执行
        testCycleCaseMapper.batchDeleteByExecutIds(executeIds);

        testCycleCaseDefectRelMapper.batchDeleteByExecutIds(executeIds);
        // 删除日志
        testCycleCaseHistory.batchDeleteByExecutIds(executeIds);
    }

    @Override
    public CaseChangeVO selectUpdateCompare(Long projectId, Long executeId) {
        TestCycleCaseDTO testCycleCaseDTO = testCycleCaseMapper.selectByPrimaryKey(executeId);
        Long caseId = testCycleCaseDTO.getCaseId();
        TestCycleDTO testCycleDTO = cycleMapper.selectByPrimaryKey(testCycleCaseDTO.getCycleId());
        TestCycleCaseVO testCycleCaseVO = dtoToVo(testCycleCaseDTO, testCycleDTO);
        TestCaseInfoVO testCaseInfoVO = testCaseService.queryCaseRep(caseId);
        CaseChangeVO caseChangeVO = new CaseChangeVO();
        caseChangeVO.setTestCycleCase(testCycleCaseVO);
        caseChangeVO.setTestCase(testCaseInfoVO);
        return caseChangeVO;

    }

    @Override
    public void updateCompare(Long projectId, CaseCompareRepVO caseCompareRepVO) {
        TestCycleCaseDTO testCycleCaseDTO = testCycleCaseMapper.selectByPrimaryKey(caseCompareRepVO.getExecuteId());
        TestCaseDTO testCaseDTO = testCaseMapper.selectByPrimaryKey(caseCompareRepVO.getCaseId());
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        if (!caseCompareRepVO.getSyncToCase()) {
            TestCycleCaseDTO testCycleCase = new TestCycleCaseDTO();
            if (caseCompareRepVO.getChangeCase()) {
                testCycleCase.setSummary(testCaseDTO.getSummary());
                testCycleCase.setDescription(testCaseDTO.getDescription());
                // 更新执行的文件夹名
                testCycleService.syncByCaseFolder(testCaseDTO.getFolderId(), testCycleCaseDTO.getCycleId());
                testCycleCase.setVersionNum(testCaseDTO.getVersionNum());
            }
            if (caseCompareRepVO.getChangeAttach()) {
                testCycleCaseAttachmentRelService.snycByCase(testCycleCaseDTO, testCaseDTO);
                testCycleCase.setVersionNum(testCaseDTO.getVersionNum());
            }
            if (caseCompareRepVO.getChangeStep()) {
                testCycleCaseStepService.snycByCase(testCycleCaseDTO, testCaseDTO);
                testCycleCase.setVersionNum(testCaseDTO.getVersionNum());
            }
            testCycleCase.setExecuteId(testCycleCaseDTO.getExecuteId());
            testCycleCase.setObjectVersionNumber(testCycleCaseDTO.getObjectVersionNumber());
            testCycleCase.setLastUpdatedBy(userDetails.getUserId());
            baseUpdate(testCycleCase);
        } else {
            if (caseCompareRepVO.getChangeCase()) {
                TestCaseRepVO testCaseRepVO = new TestCaseRepVO();
                testCaseRepVO.setCaseId(testCaseDTO.getCaseId());
                testCaseRepVO.setSummary(testCaseDTO.getSummary());
                testCaseRepVO.setDescription(testCaseDTO.getDescription());
                testCaseRepVO.setObjectVersionNumber(testCaseDTO.getObjectVersionNumber());
                List<String> fieldList = verifyUpdateUtil.verifyUpdateData((JSONObject) JSON.toJSON(testCaseRepVO), testCaseRepVO);
                testCaseService.updateCase(testCaseDTO.getProjectId(), testCaseRepVO, fieldList.toArray(new String[fieldList.size()]));
                //同步执行的文件夹名到用例
                TestIssueFolderDTO testIssueFolderDTO = testIssueFolderMapper.selectByPrimaryKey(testCaseDTO.getFolderId());
                testIssueFolderDTO.setName(cycleMapper.selectByPrimaryKey(testCycleCaseDTO.getCycleId()).getCycleName());
                testIssueFolderService.update(modelMapper.map(testIssueFolderDTO, TestIssueFolderVO.class));
                testCycleCaseDTO.setVersionNum(testCaseDTO.getVersionNum() + 1);
                baseUpdate(testCycleCaseDTO);
            }
            if (caseCompareRepVO.getChangeAttach()) {
                List<TestCycleCaseAttachmentRelVO> testCycleCaseAttachmentRelVOS = testCycleCaseAttachmentRelService.listByExecuteId(caseCompareRepVO.getExecuteId());
                List<String> collect = testCycleCaseAttachmentRelVOS.stream().map(TestCycleCaseAttachmentRelVO::getAttachmentName).collect(Collectors.toList());
                testCaseAttachmentService.deleteByCaseId(caseCompareRepVO.getCaseId(), collect);
                if (CollectionUtils.isEmpty(testCycleCaseAttachmentRelVOS)) {
                    return;
                }
                List<TestCaseAttachmentDTO> caseAttachDTOS = testCycleCaseAttachmentRelVOS.stream().map(v -> cycleAttachVoToDTO(testCaseDTO, v, userDetails)).collect(Collectors.toList());
                testCaseAttachmentService.batchInsert(caseAttachDTOS, collect);
            }
            if (caseCompareRepVO.getChangeStep()) {
                testCaseStepMapper.deleteByCaseId(caseCompareRepVO.getCaseId());
                List<TestCycleCaseStepDTO> cycleCaseStepDTOS = testCycleCaseStepMapper.querListByexecuteId(caseCompareRepVO.getExecuteId());
                if (CollectionUtils.isEmpty(cycleCaseStepDTOS)) {
                    return;
                }
                List<TestCaseStepDTO> stepDTOS = cycleCaseStepDTOS.stream().map(v -> testCaseAssembler.cycleStepToCaseStep(v, testCaseDTO, userDetails)).collect(Collectors.toList());
                testCaseStepMapper.batchInsertTestCaseSteps(stepDTOS);
            }
        }
    }

    @Override
    public void ignoreUpdate(Long projectId, Long executedId) {
        TestCycleCaseDTO testCycleCaseDTO = testCycleCaseMapper.selectByPrimaryKey(executedId);
        TestCaseDTO testCaseDTO = testCaseMapper.selectByPrimaryKey(testCycleCaseDTO.getCaseId());
        testCycleCaseDTO.setVersionNum(testCaseDTO.getVersionNum());
        baseUpdate(testCycleCaseDTO);
    }

    @Override
    public void importCase(Long projectId, Long cycleId, Map<Long, CaseSelectVO> map, Long planId) {
        // 校验是不是底层文件夹
        checkImport(cycleId);
        if (CollectionUtils.isEmpty(map)) {
            return;
        }
        List<TestCaseDTO> testCaseDTOS = testCaseMapper.listByProject(projectId);
        Map<Long, TestCaseDTO> caseMap = testCaseDTOS.stream().collect(Collectors.toMap(TestCaseDTO::getCaseId, Function.identity()));
        Map<Long, List<TestCaseDTO>> folderCaseMap = testCaseDTOS.stream().collect(Collectors.groupingBy(TestCaseDTO::getFolderId));
        List<Long> existCaseIds = testCycleCaseMapper.listByPlanId(planId);
        List<TestCaseDTO> list = new ArrayList<>();
        for (Long key : map.keySet()) {
            List<Long> insertCaseIds = new ArrayList<>();
            CaseSelectVO caseSelectVO = map.get(key);
            List<TestCaseDTO> caseList = folderCaseMap.get(key);
            if (CollectionUtils.isEmpty(caseList)) {
                return;
            }
            List<Long> collect = caseList.stream().map(TestCaseDTO::getCaseId).collect(Collectors.toList());
            if (!caseSelectVO.getCustom()) {
                // 去掉文件夹下已经导入的用例
                collect.removeAll(existCaseIds);
                insertCaseIds.addAll(collect);
            } else {

                if (!CollectionUtils.isEmpty(caseSelectVO.getSelected())) {
                    insertCaseIds.addAll(caseSelectVO.getSelected());

                } else if (!CollectionUtils.isEmpty(caseSelectVO.getUnSelected())) {
                    collect.removeAll(existCaseIds);
                    collect.removeAll(caseSelectVO.getUnSelected());
                    insertCaseIds.addAll(collect);
                }

            }
            insertCaseIds.forEach(v -> {
                list.add(caseMap.get(v));
            });
        }
        Map<Long, TestCycleDTO> cycleMap = new HashMap<>();
        TestCycleDTO testCycleDTO = cycleMapper.selectByPrimaryKey(cycleId);
        cycleMap.put(testCycleDTO.getFolderId(), testCycleDTO);
        batchInsertByTestCase(cycleMap, list);
    }

    @Override
    public void cloneCycleCase(Map<Long, Long> cycleMapping, List<Long> cycIds) {
        List<TestCycleCaseDTO> testCycleCaseDTOS = testCycleCaseMapper.listByCycleIds(cycIds);
        if(CollectionUtils.isEmpty(testCycleCaseDTOS)){
            return;
        }
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Map<Long, Long> olderCycleCaseMap = testCycleCaseDTOS.stream().collect(Collectors.toMap(TestCycleCaseDTO::getCycleId, TestCycleCaseDTO::getExecuteId));
        List<Long> olderExecuteId = new ArrayList<>();
        testCycleCaseDTOS.stream().map(v -> {
            olderExecuteId.add(v.getExecuteId());
            v.setCycleId(cycleMapping.get(v.getCycleId()));
            v.setCreatedBy(userDetails.getUserId());
            v.setLastUpdatedBy(userDetails.getUserId());
            v.setExecuteId(null);
            return  v;
        });
        List<List<TestCycleCaseDTO>> lists = ConvertUtils.averageAssign(testCycleCaseDTOS, (int) Math.ceil(testCycleCaseDTOS.size() / AVG_NUM == 0 ? 1 : testCycleCaseDTOS.size() / AVG_NUM));
        List<TestCycleCaseDTO> testCycleCaseDTOList = new ArrayList<>();
        lists.forEach(v -> {
            bathcInsert(v);
            testCycleCaseDTOList.addAll(v);
        });
        Map<Long,Long> caseIdMap = new HashMap<>();
        testCycleCaseDTOList.stream().forEach(v ->{
            caseIdMap.put(olderCycleCaseMap.get(v.getCycleId()),v.getExecuteId());
        });

        // 复制步骤
        testCycleCaseStepService.cloneStep(caseIdMap,olderExecuteId);
        // 复制附件
        testCycleCaseAttachmentRelService.cloneAttach(caseIdMap,olderExecuteId);

        // 复制缺陷
        testCycleCaseDefectRelService.cloneDefect(caseIdMap,olderExecuteId);
    }

    private void checkImport(Long cycleId) {
        TestCycleDTO testCycleDTO = new TestCycleDTO();
        testCycleDTO.setParentCycleId(cycleId);
        List<TestCycleDTO> list = cycleMapper.select(testCycleDTO);
        if (!CollectionUtils.isEmpty(list)) {
            throw new CommonException("folder.not.bottom");
        }
    }

    private TestCaseAttachmentDTO cycleAttachVoToDTO(TestCaseDTO testCaseDTO, TestCycleCaseAttachmentRelVO testCycleCaseAttachmentRelVO, CustomUserDetails userDetails) {
        TestCaseAttachmentDTO testCaseAttachmentDTO = new TestCaseAttachmentDTO();
        testCaseAttachmentDTO.setCaseId(testCaseDTO.getCaseId());
        testCaseAttachmentDTO.setLastUpdatedBy(userDetails.getUserId());
        testCaseAttachmentDTO.setCreatedBy(userDetails.getUserId());
        testCaseAttachmentDTO.setFileName(testCycleCaseAttachmentRelVO.getAttachmentName());
        String url = testCycleCaseAttachmentRelVO.getUrl();
        url = url.replace("http://", "").replace("https://", "");
        int index = url.indexOf("/");
        String newUrl = url.substring(index);
        testCaseAttachmentDTO.setUrl(newUrl);
        testCaseAttachmentDTO.setProjectId(testCaseDTO.getProjectId());
        return testCaseAttachmentDTO;
    }


    private TestCycleCaseVO dtoToVo(TestCycleCaseDTO testCycleCaseDTO, TestCycleDTO testCycleDTO) {
        TestCycleCaseVO testCycleCaseVO = modelMapper.map(testCycleCaseDTO, TestCycleCaseVO.class);
        TestIssueFolderDTO testIssueFolderDTO = testIssueFolderMapper.selectByPrimaryKey(testCycleDTO.getFolderId());
        List<TestCycleCaseStepDTO> cycleCaseStepDTOS = testCycleCaseStepMapper.querListByexecuteId(testCycleCaseDTO.getExecuteId());
        testCycleCaseVO.setFolderName(testIssueFolderDTO.getName());
        if (!CollectionUtils.isEmpty(cycleCaseStepDTOS)) {
            List<TestCycleCaseStepVO> cycleCaseStepVOS = modelMapper.map(cycleCaseStepDTOS, new TypeToken<List<TestCycleCaseStepVO>>() {
            }.getType());
            testCycleCaseVO.setCycleCaseStep(cycleCaseStepVOS);
        }
        List<TestCycleCaseAttachmentRelVO> testCycleCaseAttachmentRelVOS = testCycleCaseAttachmentRelService.listByExecuteId(testCycleCaseDTO.getExecuteId());
        if (!CollectionUtils.isEmpty(testCycleCaseAttachmentRelVOS)) {
            testCycleCaseVO.setCaseAttachment(testCycleCaseAttachmentRelVOS);
        }
        return testCycleCaseVO;
    }


    @Override
    public List<TestCycleCaseDTO> listByCycleIds(List<Long> cycleIds) {
        if (cycleIds.isEmpty()) {
            return new ArrayList<>();
        } else {
            return testCycleCaseMapper.listByCycleIds(cycleIds);
        }
    }

    private TestCycleCaseVO runTestCycleCase(TestCycleCaseVO testCycleCaseVO, Long projectId) {
        Assert.notNull(projectId, "error.projectId.illegal");

        TestCycleCaseDTO testCycleCaseDTO = new TestCycleCaseDTO();
        testCycleCaseDTO.setRank(RankUtil.Operation.INSERT.getRank(testCycleCaseVO.getLastRank(), testCycleCaseVO.getNextRank()));
        testCycleCaseDTO.setProjectId(projectId);
        testCycleCaseDTO.setCycleId(testCycleCaseVO.getCycleId());
        testCycleCaseDTO.setCaseId(testCycleCaseVO.getIssueId());
        testCycleCaseDTO.setExecutionStatus(testCycleCaseVO.getExecutionStatus());
        if (testCycleCaseMapper.validateCycleCaseInCycle(testCycleCaseDTO).longValue() > 0) {
            throw new CommonException("error.cycle.case.insert.have.one.case.in.cycle");
        }
        testCycleCaseMapper.insert(testCycleCaseDTO);
        createTestCycleCaseStep(testCycleCaseDTO);
        return modelMapper.map(testCycleCaseDTO, TestCycleCaseVO.class);
    }

    public void createTestCycleCaseStep(TestCycleCaseDTO testCycleCaseDTO) {
        TestCaseStepDTO testCaseStepDTO = new TestCaseStepDTO();
        testCaseStepDTO.setIssueId(testCycleCaseDTO.getCaseId());
        List<TestCaseStepDTO> testCaseStepES = testCaseStepMapper.query(testCaseStepDTO);
        Long defaultStepStatusId = testStatusMapper.getDefaultStatus(TestStatusType.STATUS_TYPE_CASE_STEP);
        testCaseStepES.forEach(v -> {
            TestCycleCaseStepDTO testCycleCaseStepDTO = new TestCycleCaseStepDTO();
            testCycleCaseStepDTO.setStepStatus(defaultStepStatusId);
            testCycleCaseStepDTO.setStepId(v.getStepId());
            testCycleCaseStepDTO.setExecuteId(testCycleCaseDTO.getExecuteId());
            testCycleCaseStepMapper.insert(testCycleCaseStepDTO);
        });
    }

    private TestCycleCaseDTO changeStep(Long projectId, TestCycleCaseVO testCycleCaseVO) {
        TestCycleCaseDTO testCycleCaseDTO = modelMapper.map(testCycleCaseVO, TestCycleCaseDTO.class);
        testCycleCaseDTO.setProjectId(projectId);
        if (!StringUtils.isEmpty(testCycleCaseVO.getLastRank()) || !StringUtils.isEmpty(testCycleCaseVO.getNextRank())) {
            testCycleCaseDTO.setRank(RankUtil.Operation.UPDATE.getRank(testCycleCaseVO.getLastRank(), testCycleCaseVO.getNextRank()));
        }
        Assert.notNull(testCycleCaseDTO.getProjectId(), "error.projectId.illegal");
        testCycleCaseMapper.updateByPrimaryKey(testCycleCaseDTO);
        return testCycleCaseDTO;
    }

    /**
     * 获取项目下所有cycleId
     *
     * @param projectId
     * @return
     */
    private List<Long> getCycleIdByProjectId(Long projectId) {
        Long[] versionIds = testCaseService.getVersionIds(projectId);
        if (ObjectUtils.isEmpty(versionIds)) {
            return new ArrayList<>();
        }
        return selectCyclesInVersions(versionIds);
    }


    private List<Long> selectCyclesInVersions(Long[] versionIds) {
        Assert.notNull(versionIds, "error.query.cycle.In.Versions.not.null");
        versionIds = Stream.of(versionIds).filter(Objects::nonNull).toArray(Long[]::new);

        if (versionIds.length > 0) {
            return cycleMapper.selectCyclesInVersions(versionIds);
        }
        return new ArrayList<>();
    }

    private TestCycleCaseDTO baseInsert(TestCycleCaseDTO testCycleCaseDTO) {
        if (ObjectUtils.isEmpty(testCycleCaseDTO)) {
            throw new CommonException("error.insert.cycle.case.is.null");
        }
        DBValidateUtil.executeAndvalidateUpdateNum(testCycleCaseMapper::insertSelective, testCycleCaseDTO, 1, "error.insert.cycle.case");
        return testCycleCaseDTO;
    }

    private void queryAllCycleIds(Long cycleId, Set<Long> folderIds, Map<Long, List<TestCycleDTO>> folderMap) {
        folderIds.add(cycleId);
        List<TestCycleDTO> testCycleDTOS = folderMap.get(cycleId);
        if (!CollectionUtils.isEmpty(testCycleDTOS)) {
            testCycleDTOS.forEach(v -> queryAllCycleIds(v.getCycleId(), folderIds, folderMap));
        }
    }

    private  Set<Long> queryCycleIds(Long cycleId,Long planId){
        Set<Long> cycleIds = new HashSet<>();
        if (!ObjectUtils.isEmpty(planId)) {
            TestCycleDTO testCycleDTO = new TestCycleDTO();
            testCycleDTO.setPlanId(planId);
            List<TestCycleDTO> cycleDTOS = testCycleMapper.select(testCycleDTO);
            cycleDTOS.stream().forEach(e->{
                if(e.getParentCycleId()==null){
                    e.setParentCycleId(0L);
                }
            });
            Map<Long, List<TestCycleDTO>> folderMap = cycleDTOS.stream().collect(Collectors.groupingBy(TestCycleDTO::getParentCycleId));
            queryAllCycleIds(cycleId, cycleIds, folderMap);
        }
        return cycleIds;
    }

    private List<TestCycleCaseDTO> caseToCycleCase(List<TestCaseDTO> testCaseDTOS, Map<Long, TestCycleDTO> testCycleMap, Long defaultStatusId) {
        if (CollectionUtils.isEmpty(testCaseDTOS)) {
            return new ArrayList<>();
        }
        List<TestCycleCaseDTO> testCycleCaseDTOS = new ArrayList<>();
        testCaseDTOS.forEach(v -> {
            TestCycleDTO testCycleDTO = testCycleMap.get(v.getFolderId());
            if (!ObjectUtils.isEmpty(testCycleDTO)) {
                TestCycleCaseDTO testCycleCaseDTO = new TestCycleCaseDTO();
                testCycleCaseDTO.setCycleId(testCycleDTO.getCycleId());
                testCycleCaseDTO.setCaseId(v.getCaseId());
                testCycleCaseDTO.setDescription(v.getDescription());
                testCycleCaseDTO.setProjectId(v.getProjectId());
                testCycleCaseDTO.setVersionNum(v.getVersionNum());
                testCycleCaseDTO.setExecutionStatus(defaultStatusId);
                testCycleCaseDTO.setCreatedBy(testCycleDTO.getCreatedBy());
                testCycleCaseDTO.setLastUpdatedBy(testCycleDTO.getLastUpdatedBy());
                testCycleCaseDTO.setSummary(v.getSummary());
                testCycleCaseDTO.setSource("none");
                testCycleCaseDTOS.add(testCycleCaseDTO);
            }
        });
        Map<Long, List<TestCycleCaseDTO>> listMap = testCycleCaseDTOS.stream().collect(Collectors.groupingBy(TestCycleCaseDTO::getCycleId));
        return doRank(listMap);
    }

    private void bathcInsert(List<TestCycleCaseDTO> testCycleCaseDTOS) {
        if (CollectionUtils.isEmpty(testCycleCaseDTOS)) {
            return;
        }
        testCycleCaseMapper.batchInsert(testCycleCaseDTOS);
    }

    private void previousNextId(int index, List<TestCycleCaseDTO> list, TestCycleCaseInfoVO testCycleCaseInfoVO) {
        Long previousExecuteId;
        Long nextExecuteId;
        if (index - 1 < 0) {
            previousExecuteId = null;
        } else {
            previousExecuteId = list.get(index - 1).getExecuteId();
        }
        if (index + 1 >= list.size()) {
            nextExecuteId = null;
        } else {
            nextExecuteId = list.get(index + 1).getExecuteId();
        }
        testCycleCaseInfoVO.setPreviousExecuteId(previousExecuteId);
        testCycleCaseInfoVO.setNextExecuteId(nextExecuteId);
    }

    private List<TestCycleCaseDTO> doRank(Map<Long, List<TestCycleCaseDTO>> tcycleCaseMap) {
        List<TestCycleCaseDTO> testCycleCaseDTOList = new ArrayList<>();
        for (Map.Entry<Long, List<TestCycleCaseDTO>> map : tcycleCaseMap.entrySet()
        ) {
            String prevRank = RankUtil.Operation.INSERT.getRank(testCycleCaseMapper.getLastedRank(map.getKey()), null);
            if (!CollectionUtils.isEmpty(map.getValue())) {
                for (TestCycleCaseDTO testCycleCaseDTO : map.getValue()) {
                    testCycleCaseDTO.setRank(RankUtil.Operation.INSERT.getRank(prevRank, null));
                    prevRank = testCycleCaseDTO.getRank();
                    testCycleCaseDTOList.add(testCycleCaseDTO);
                }
            }
        }
        return testCycleCaseDTOList;
    }

}