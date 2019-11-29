package io.choerodon.test.manager.infra.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.annotations.Param;

import io.choerodon.agile.api.vo.SearchDTO;
import io.choerodon.mybatis.common.Mapper;
import io.choerodon.test.manager.api.vo.ExecutionStatusVO;
import io.choerodon.test.manager.infra.dto.TestCycleCaseDTO;

/**
 * Created by 842767365@qq.com on 6/11/18.
 */
public interface TestCycleCaseMapper extends Mapper<TestCycleCaseDTO> {
    List<TestCycleCaseDTO> queryWithAttachAndDefect(@Param("dto") TestCycleCaseDTO testCycleCaseDTO, @Param("page") int page, @Param("pageSize") int pageSize);

    /**
     * 查询为父cycle的所有子阶段的信息
     *
     * @param testCycleCaseDTOS
     * @param page
     * @param pageSize
     * @return
     */
    List<TestCycleCaseDTO> queryByFatherCycleWithAttachAndDefect(@Param("dtos") List<TestCycleCaseDTO> testCycleCaseDTOS, @Param("page") int page, @Param("pageSize") int pageSize, @Param("sort") String sort);

    List<TestCycleCaseDTO> queryByFatherCycleWithAttachAndDefect_oracle(@Param("dtos") List<TestCycleCaseDTO> testCycleCaseDTOS, @Param("page") int page, @Param("pageSize") int pageSize);

    List<TestCycleCaseDTO> queryWithAttachAndDefect_oracle(@Param("dto") TestCycleCaseDTO testCycleCaseDTO, @Param("page") int page, @Param("pageSize") int pageSize);

    Long queryWithAttachAndDefect_count(@Param("dto") TestCycleCaseDTO testCycleCaseDTO);

    List<TestCycleCaseDTO> filter(Map map);

    List<TestCycleCaseDTO> queryByIssue(@Param("issueId") Long issueId);


    /**
     * 查询issues下的cycleCase
     *
     * @param issueId
     * @return
     */
    List<TestCycleCaseDTO> queryInIssues(@Param("ids") Long[] issueId);

    List<TestCycleCaseDTO> queryCaseAllInfoInCyclesOrVersions(@Param("cycleIds") Long[] cycleIds, @Param("versionIds") Long[] versionIds);

    List<TestCycleCaseDTO> queryCycleCaseForReporter(@Param("ids") Long[] issueIds);

    Long countCaseNotRun(@Param("ids") Long[] cycleIds);

    /**
     * 统计cycleIds下所有已经启动的Issue的数量
     *
     * @param cycleIds
     * @return
     */
    Long countCaseNotPlain(@Param("ids") Long[] cycleIds);

    /**
     * 获取cycleIds下所有的测试实例数量
     *
     * @param cycleIds
     * @return
     */
    Long countCaseSum(@Param("ids") Long[] cycleIds);

    /**
     * 获取cycle（除了temp类型）下是否存的同issue的测试用例
     *
     * @param testCycleCase
     * @return
     */
    Long validateCycleCaseInCycle(TestCycleCaseDTO testCycleCase);

    /**
     * 获取cycle下最后一个case的rank
     *
     * @param cycleId
     * @return
     */
    String getLastedRank(@Param("cycleId") Long cycleId);

    String getLastedRank_oracle(@Param("cycleId") Long cycleId);

    int batchInsertTestCycleCases(List<TestCycleCaseDTO> testCycleCaseDTOS);

    void updateAuditFields(@Param("executeIds") Long[] executeId, @Param("userId") Long userId, @Param("date") Date date);

    /**
     * 查询执行详情
     * @param executeId
     * @return
     */
    TestCycleCaseDTO queryByCaseId(@Param("executeId") Long executeId);

    /**
     * 查询状态总览
     */
    List<ExecutionStatusVO> queryExecutionStatus(@Param("planId")Long planId);

    /**
     * 查询文件下的执行
     */

    List<TestCycleCaseDTO> queryFolderCycleCase(@Param("planId") Long planId, @Param("folderIds") Set<Long> folderIds, @Param("searchDTO") SearchDTO searchDTO);

    void fixCycleCase();

    void fixSource();

    List<TestCycleCaseDTO> listByCycleIds(@Param("cycleIds") List<Long> cycleId);

}
