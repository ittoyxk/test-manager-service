package io.choerodon.test.manager.app.service.impl;

import java.util.*;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import com.alibaba.fastjson.JSON;

import io.choerodon.agile.api.vo.IssueStatusDTO;
import io.choerodon.agile.api.vo.LookupValueDTO;
import io.choerodon.agile.api.vo.ProductVersionDTO;
import io.choerodon.agile.api.vo.UserDTO;
import io.choerodon.base.domain.Sort;
import io.choerodon.base.domain.PageRequest;
import io.choerodon.test.manager.api.vo.ExcelLookupCaseVO;
import io.choerodon.test.manager.api.vo.TestCaseStepVO;
import io.choerodon.test.manager.api.vo.TestIssueFolderVO;
import io.choerodon.test.manager.api.vo.TestIssueFolderRelVO;
import io.choerodon.test.manager.app.service.TestCaseService;
import io.choerodon.test.manager.app.service.UserService;
import io.choerodon.test.manager.infra.dto.TestIssueFolderDTO;
import io.choerodon.test.manager.infra.mapper.TestIssueFolderMapper;
import io.choerodon.test.manager.infra.util.ExcelUtil;
import io.choerodon.test.manager.infra.util.SpringUtil;

/**
 * Created by zongw.lee@gmail.com on 15/10/2018
 */
public class TestCaseExcelExportServiceImpl extends AbstarctExcelExportServiceImpl<TestIssueFolderVO, TestIssueFolderRelVO> {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestIssueFolderMapper testIssueFolderMapper;

    @Autowired
    private ModelMapper modelMapper;

    private static final int MAXROWS = 1048575;

    private int lookEnd;
    private int productEnd;
    private int statusEnd;
    private int folderEnd;
    private int userEnd;

    private Map<Long, ProductVersionDTO> versionInfo;

    public TestCaseExcelExportServiceImpl() {
        SpringUtil.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
    }

    private ExcelLookupCaseVO excelLookupCaseVO;

    private static final String PRIORITIES = "priorities";
    private static final String STATUS = "status";
    private static final String FOLDERS = "folders";
    private static final String USERS = "users";

    private enum CaseHeader {
        COLUMN1("文件夹*"), COLUMN2("用例概要*"), COLUMN3("用例编号"), COLUMN4("优先级*"), COLUMN5("用例描述"),
        COLUMN6("经办人"), COLUMN7("状态"), COLUMN8("测试步骤"), COLUMN9("测试数据"), COLUMN10("预期结果"),
        COLUMN11("文件夹ID(系统自动生成)"), COLUMN12("优先级valueCode(系统自动生成)*"), COLUMN13("经办人ID(系统自动生成)");
        private String chinese;

        CaseHeader(String chinese) {
            this.chinese = chinese;
        }

        public String getValue() {
            return chinese;
        }
    }

    @Override
    public int populateVersionHeader(Sheet sheet, String projectName, TestIssueFolderVO folder, CellStyle rowStyle) {
        if (sheet.getWorkbook().getNumberOfSheets() == 1) {
            versionInfo = testCaseService.getVersionInfo(folder.getProjectId());
            return 0;
        }
        Row row1 = ExcelUtil.createRow(sheet, 0, rowStyle);
        ExcelUtil.createCell(row1, 0, ExcelUtil.CellType.TEXT, "项目：" + projectName);
        String versionName = "版本";
        String versionJson = "";
        if (folder.getVersionId() != null) {
            versionName = versionName + versionInfo.get(folder.getVersionId()).getName();
            versionJson = JSON.toJSONString(versionInfo.get(folder.getVersionId()));
            sheet.getWorkbook().setSheetName(sheet.getWorkbook().getSheetIndex(sheet),
                    WorkbookUtil.createSafeSheetName(versionName.replace('-', '_').replace(' ', '_'), '_'));
        }
        ExcelUtil.createCell(row1, 1, ExcelUtil.CellType.TEXT, versionName);
        ExcelUtil.createCell(row1, 14, ExcelUtil.CellType.TEXT, versionJson);
        return 2;
    }

    @Override
    public int populateHeader(Sheet sheet, int rowNum, TestIssueFolderVO folder, CellStyle rowStyle) {
        Assert.notNull(folder, "error.cycle.are.not.exist");
        Assert.notNull(sheet, "error.sheet.are.be.null");
        Row row = ExcelUtil.createRow(sheet, rowNum, rowStyle);
        int i = 0;
        //准备mapping范围
        if (sheet.getWorkbook().getNumberOfSheets() == 1) {
            log.debug("开始准备lookup sheet页数据...");
            prepareLookupData(folder);
            setLookupData(sheet, rowNum, rowStyle);
        } else {
            setAllNameMapping(sheet, folder);
            for (CaseHeader value : CaseHeader.values()) {
                ExcelUtil.createCell(row, i++, ExcelUtil.CellType.TEXT, value.getValue());
            }
        }
        return rowNum + 1;
    }

    @Override
    public int populateBody(Sheet sheet, int column, List<TestIssueFolderRelVO> folderRelDTOS, Queue<CellStyle> rowStyles) {
        if (sheet.getWorkbook().getNumberOfSheets() != 1) {
            //设置下拉框的值
            setDataValidationByFormula(sheet, PRIORITIES, 3, 3);
            setDataValidationByFormula(sheet, STATUS, 6, 6);
            setDataValidationByFormula(sheet, USERS, 5, 5);

            for (TestIssueFolderRelVO folderRel : folderRelDTOS) {
                CellStyle style;
                if (ObjectUtils.isEmpty(rowStyles)) {
                    style = null;
                } else {
                    style = rowStyles.poll();
                    rowStyles.offer(style);
                }
                column = populateCase(sheet, column, folderRel, style);
            }
        }
        sheet.setColumnHidden(14, true);
        //如果是模板默认加四百行lookup公式
        if (folderRelDTOS.size() == 1 && folderRelDTOS.get(0).getIssueInfosVO().getIssueId() == null) {
            sheet.setColumnHidden(2, true);
            sheet.setColumnHidden(6, true);
            column += addLookupFormula(sheet, column, rowStyles);
        }
        setDataValidationByFormula(sheet, sheet.getSheetName() + FOLDERS, 0, 0);
        return column;
    }

    public int addLookupFormula(Sheet sheet, int column, Queue<CellStyle> rowStyles) {
        int addDataSize = (403 - column) > 0 ? (403 - column) : 0;
        for (int i = 0; i < addDataSize; i++) {
            CellStyle style;
            if (ObjectUtils.isEmpty(rowStyles)) {
                style = null;
            } else {
                style = rowStyles.poll();
                rowStyles.offer(style);
            }

            Row row = ExcelUtil.createRow(sheet, column++, style);

            ExcelUtil.createCell(row, 10, ExcelUtil.CellType.TEXT, "").setCellFormula(
                    getLookupString("A" + (row.getRowNum() + 1), statusEnd + 2, folderEnd, 2));

            ExcelUtil.createCell(row, 11, ExcelUtil.CellType.TEXT, "").setCellFormula(
                    getLookupString("D" + (row.getRowNum() + 1), 2, lookEnd, 2));

            ExcelUtil.createCell(row, 12, ExcelUtil.CellType.TEXT, "").setCellFormula(
                    getLookupString("F" + (row.getRowNum() + 1), folderEnd + 2, userEnd, 2));
        }
        return column;
    }

    private int populateCase(Sheet sheet, int columnNum, TestIssueFolderRelVO folderRel, CellStyle rowStyles) {
        Row row = ExcelUtil.createRow(sheet, columnNum, rowStyles);
        Optional.ofNullable(folderRel.getFolderName()).ifPresent(v -> ExcelUtil.createCell(row, 0, ExcelUtil.CellType.TEXT, v));
        if (!ObjectUtils.isEmpty(folderRel.getIssueInfosVO())) {
            Optional.ofNullable(folderRel.getIssueInfosVO().getIssueNum()).ifPresent(v -> ExcelUtil.createCell(row, 2, ExcelUtil.CellType.TEXT, v));
            Optional.ofNullable(folderRel.getIssueInfosVO().getSummary()).ifPresent(v -> ExcelUtil.createCell(row, 1, ExcelUtil.CellType.TEXT, v));
            if (!ObjectUtils.isEmpty(folderRel.getIssueInfosVO().getPriorityVO())) {
                Optional.ofNullable(folderRel.getIssueInfosVO().getPriorityVO().getName()).ifPresent(v -> ExcelUtil.createCell(row, 3, ExcelUtil.CellType.TEXT, v));
            }
            //接口修改后，改成描述
            Optional.ofNullable(ExcelUtil.getColumnWithoutRichText(folderRel.getIssueInfosVO().getDescription())).ifPresent(v -> ExcelUtil.createCell(row, 4, ExcelUtil.CellType.TEXT, v));
            Optional.ofNullable(folderRel.getIssueInfosVO().getAssigneeName()).ifPresent(v -> ExcelUtil.createCell(row, 5, ExcelUtil.CellType.TEXT, v));
            Optional.ofNullable(folderRel.getIssueInfosVO().getStatusName()).ifPresent(v -> ExcelUtil.createCell(row, 6, ExcelUtil.CellType.TEXT, v));
        }
        ExcelUtil.createCell(row, 10, ExcelUtil.CellType.TEXT, "").setCellFormula(
                getLookupString("A" + (row.getRowNum() + 1), statusEnd + 2, folderEnd, 2));

        ExcelUtil.createCell(row, 11, ExcelUtil.CellType.TEXT, "").setCellFormula(
                getLookupString("D" + (row.getRowNum() + 1), 2, lookEnd, 2));

        ExcelUtil.createCell(row, 12, ExcelUtil.CellType.TEXT, "").setCellFormula(
                getLookupString("F" + (row.getRowNum() + 1), folderEnd + 2, userEnd, 2));

        Optional.ofNullable(folderRel.getErrorInfo()).ifPresent(v -> ExcelUtil.createCell(row, 13, ExcelUtil.CellType.TEXT, v));

        return columnNum + populateCycleCaseStep(sheet, columnNum, folderRel.getTestCaseStepVOS(), rowStyles) + 1;
    }

    private int populateCycleCaseStep(Sheet sheet, int column, List<TestCaseStepVO> caseSteps, CellStyle rowStyles) {
        if (caseSteps == null || caseSteps.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < caseSteps.size(); i++) {
            Row row;
            if (i == 0) {
                row = sheet.getRow(column);
            } else {
                row = ExcelUtil.createRow(sheet, column, rowStyles);
            }
            doPopulateCaseStep(row, caseSteps.get(i));
            column++;
        }

        return caseSteps.size() - 1;
    }

    private void doPopulateCaseStep(Row row, TestCaseStepVO caseStep) {
        Optional.ofNullable(caseStep.getTestStep()).ifPresent(v -> ExcelUtil.createCell(row, 7, ExcelUtil.CellType.TEXT, v));
        Optional.ofNullable(caseStep.getTestData()).ifPresent(v -> ExcelUtil.createCell(row, 8, ExcelUtil.CellType.TEXT, v));
        Optional.ofNullable(caseStep.getExpectedResult()).ifPresent(v -> ExcelUtil.createCell(row, 9, ExcelUtil.CellType.TEXT, v));
    }

    private void prepareLookupData(TestIssueFolderVO folder) {
        Long projectId = folder.getProjectId();

        List<LookupValueDTO> lookupValueDTOS = testCaseService.queryLookupValueByCode(projectId, "priority").getLookupValues();

        PageRequest pageRequest = new PageRequest(1, 999999999, Sort.Direction.ASC, "componentId");

        List<UserDTO> userDTOS = userService.list(pageRequest, projectId, null, null).getBody().getList();

        userDTOS.forEach(v -> v.setLoginName(v.getLoginName() + v.getRealName()));

        List<ProductVersionDTO> productVersionDTOS = new ArrayList<>(versionInfo.values());

        TestIssueFolderDTO testIssueFolderDTO = new TestIssueFolderDTO();
        testIssueFolderDTO.setProjectId(projectId);
        testIssueFolderDTO.setVersionId(folder.getVersionId());

        List<TestIssueFolderVO> testIssueFolderVOS = modelMapper.map(testIssueFolderMapper.select(testIssueFolderDTO),
                new TypeToken<List<TestIssueFolderVO>>() {
        }.getType());
        List<IssueStatusDTO> issueStatusDTOS = testCaseService.listStatusByProjectId(projectId);

        //加1的原因是每一个新数据开始时都会有一个header
        //从2开始，所以第一个end是size+1
        lookEnd = lookupValueDTOS.size() + 1;
        productEnd = lookEnd + productVersionDTOS.size() + 1;
        statusEnd = productEnd + issueStatusDTOS.size() + 1;
        folderEnd = statusEnd + testIssueFolderVOS.size() + 1;
        userEnd = folderEnd + userDTOS.size() + 1;

        this.excelLookupCaseVO = new ExcelLookupCaseVO(lookupValueDTOS, userDTOS, productVersionDTOS, testIssueFolderVOS, issueStatusDTOS);
    }

    /**
     * @param sheet
     * @param typeName 给需要lookup数据起的总名称
     * @param start    需要mapping的开始行数
     * @param end      需要mapping的结尾行数
     */
    private void initNameMapping(Sheet sheet, String typeName, String cloumn, int start, int end) {
        Name name = sheet.getWorkbook().createName();
        name.setNameName(typeName);
        name.setRefersToFormula("Sheet0!$" + cloumn + "$" + start + ":$" + cloumn + "$" + end);
    }

    /**
     * 生成下拉框
     *
     * @param sheet
     * @param formulaString 下拉框的唯一名
     * @param firstCol      下拉框是哪些列
     * @param lastCol
     */
    private void setDataValidationByFormula(Sheet sheet, String formulaString, int firstCol, int lastCol) {
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) sheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint) dvHelper
                .createFormulaListConstraint(formulaString);
        CellRangeAddressList addressList = new CellRangeAddressList(3, MAXROWS, firstCol, lastCol);
        XSSFDataValidation validation = (XSSFDataValidation) dvHelper.createValidation(dvConstraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);

        sheet.addValidationData(validation);
    }

    private void setLookupData(Sheet sheet, int column, CellStyle rowStyle) {
        List<LookupValueDTO> lookupValueDTOS = excelLookupCaseVO.getLookupValueDTOS();

        List<UserDTO> userDTOS = excelLookupCaseVO.getUserDTOS();

        List<ProductVersionDTO> productVersionDTOS = excelLookupCaseVO.getProductVersionDTOS();

        List<TestIssueFolderVO> testIssueFolderVOS = excelLookupCaseVO.getTestIssueFolderVOS();

        List<IssueStatusDTO> issueStatusDTOS = excelLookupCaseVO.getIssueStatusDTOS();

        column += populateLookupHeader(sheet, column, rowStyle, "优先级");
        for (LookupValueDTO v : lookupValueDTOS) {
            column += populateLookupValue(sheet, column, v, rowStyle);
        }

        column += populateLookupHeader(sheet, column, rowStyle, "版本");
        for (ProductVersionDTO v : productVersionDTOS) {
            column += populateVersion(sheet, column, v, rowStyle);
        }

        column += populateLookupHeader(sheet, column, rowStyle, "状态");
        for (IssueStatusDTO v : issueStatusDTOS) {
            column += populateIssueStatus(sheet, column, v, rowStyle);
        }

        column += populateLookupHeader(sheet, column, rowStyle, "文件夹");
        for (TestIssueFolderVO v : testIssueFolderVOS) {
            column += populateFolder(sheet, column, v, rowStyle);
        }

        column += populateLookupHeader(sheet, column, rowStyle, "经办人");
        for (UserDTO v : userDTOS) {
            column += populateUser(sheet, column, v, rowStyle);
        }
    }

    private int populateLookupHeader(Sheet sheet, int columnNum, CellStyle style, String name) {
        Row headRow = ExcelUtil.createRow(sheet, columnNum, style);
        ExcelUtil.createCell(headRow, 0, ExcelUtil.CellType.TEXT, name);
        return 1;
    }

    private int populateLookupValue(Sheet sheet, int columnNum, LookupValueDTO lookupValueDTO, CellStyle style) {
        Row row = ExcelUtil.createRow(sheet, columnNum, style);
        Optional.ofNullable(lookupValueDTO.getValueCode()).ifPresent(v -> ExcelUtil.createCell(row, 1, ExcelUtil.CellType.TEXT, v));
        Optional.ofNullable(lookupValueDTO.getName()).ifPresent(v -> ExcelUtil.createCell(row, 0, ExcelUtil.CellType.TEXT, v));
        return 1;
    }

    private int populateVersion(Sheet sheet, int columnNum, ProductVersionDTO productVersionDTO, CellStyle style) {
        Row row = ExcelUtil.createRow(sheet, columnNum, style);
        Optional.ofNullable(productVersionDTO.getName()).ifPresent(v -> ExcelUtil.createCell(row, 0, ExcelUtil.CellType.TEXT, v));
        ExcelUtil.createCell(row, 1, ExcelUtil.CellType.TEXT, JSON.toJSONString(productVersionDTO));
        return 1;
    }

    private int populateIssueStatus(Sheet sheet, int columnNum, IssueStatusDTO issueStatusDTO, CellStyle style) {
        Row row = ExcelUtil.createRow(sheet, columnNum, style);
        Optional.ofNullable(issueStatusDTO.getName()).ifPresent(v -> ExcelUtil.createCell(row, 0, ExcelUtil.CellType.TEXT, v));
        return 1;
    }

    private int populateFolder(Sheet sheet, int columnNum, TestIssueFolderVO testIssueFolderVO, CellStyle style) {
        Row row = ExcelUtil.createRow(sheet, columnNum, style);
        ExcelUtil.createCell(row, 1, ExcelUtil.CellType.NUMBER, testIssueFolderVO.getFolderId());
        Optional.ofNullable(testIssueFolderVO.getName()).ifPresent(v -> ExcelUtil.createCell(row, 0, ExcelUtil.CellType.TEXT, v));
        return 1;
    }

    private int populateUser(Sheet sheet, int columnNum, UserDTO userDTO, CellStyle style) {
        Row row = ExcelUtil.createRow(sheet, columnNum, style);
        ExcelUtil.createCell(row, 1, ExcelUtil.CellType.NUMBER, userDTO.getId());
        Optional.ofNullable(userDTO.getLoginName()).ifPresent(v -> ExcelUtil.createCell(row, 0, ExcelUtil.CellType.TEXT, v));
        return 1;
    }

    /**
     * @param param             需要匹配的目标区域
     * @param startRow          源匹配开始行(需要匹配到后的值行也一样)
     * @param endRow            源匹配结束行(需要匹配到后的值行也一样)
     * @param destinatNumClounm 需要匹配到后的值返回列
     * @return String 匹配表达式
     */
    private String getLookupString(String param, int startRow, int endRow, int destinatNumClounm) {
        String vlookup = "VLOOKUP(" + param + ",Sheet0!$A$" + startRow + ":$B$" + endRow
                + "," + destinatNumClounm + ",FALSE)";
        return "IF(ISNA(" + vlookup + "),\"\"," + vlookup + ")";
    }

    private void setAllNameMapping(Sheet sheet, TestIssueFolderVO folder) {
        //确保folder中设置了正确versionId
        //设置映射
        if (sheet.getWorkbook().getNumberOfSheets() == 2) {
            initNameMapping(sheet, PRIORITIES, "A", 2, lookEnd);
            initNameMapping(sheet, STATUS, "A", productEnd + 2, statusEnd);
            initNameMapping(sheet, USERS, "A", folderEnd + 2, userEnd);
        }
        int start = 0;
        boolean flag = false;
        int properFolderSize = 0;
        if (folder.getVersionId() != null) {
            for (TestIssueFolderVO folderDTO : excelLookupCaseVO.getTestIssueFolderVOS()) {
                if (folderDTO.getVersionId().equals(folder.getVersionId())) {
                    properFolderSize++;
                    flag = true;
                } else if (!flag) {
                    start++;
                }
            }
        } else {
            properFolderSize = folderEnd - statusEnd - 1;
        }
        initNameMapping(sheet, sheet.getSheetName() + FOLDERS, "A", statusEnd + 2 + start, statusEnd + 1 + start + properFolderSize);
    }
}