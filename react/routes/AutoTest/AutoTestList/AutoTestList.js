import React from 'react';
import { Link } from 'react-router-dom';
import { Choerodon } from '@choerodon/boot';
import {
  Page, Header, Content, Breadcrumb, stores,
} from '@choerodon/boot';
import moment from 'moment';
import {
  Icon, Button, Table, Select, Menu, Dropdown, Switch, Steps,
} from 'choerodon-ui';
import { Modal } from 'choerodon-ui/pro';

import TimeAgo from 'timeago-react';
import { FormattedMessage } from 'react-intl';
import _ from 'lodash';
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/base16-dark.css';
import { User } from '../../../components';
import {
  PODSTATUS, TESTRESULT, PodStatus, TestResult,
} from './AutoTestTags';
import { ContainerLog } from './components';
import { getProjectName, humanizeDuration, TestExecuteLink } from '../../../common/utils';
import CreateAutoTest from '../CreateAutoTest';
import './AutoTestList.less';
import { SelectVariable, ModifyConfig, ConfirmInfo } from '../CreateAutoTest/components';
import CreateAutoTestStore from '../stores/CreateAutoTestStore';

const { Option } = Select;
const { SubMenu, Item: MenuItem } = Menu;
const AutoTestList = ({
  loading,
  appList,
  selectLoading,
  autoRefresh,
  currentApp,
  historyList,
  envList,
  pagination,
  toCreateAutoTest,
  onRefreshClick,
  onItemClick,
  onAppChange,
  onFilterChange,
  onTableChange,
  onSaveLogRef,
  onAutoRefreshChange,
}) => {
  const getMenu = record => (
    <Menu onClick={({ item, key, keyPath }) => { onItemClick(record, { item, key, keyPath }); }} style={{ margin: '10px 0 0 28px' }}>
      <MenuItem key="log" disabled={record.testAppInstanceVO.podStatus === 0 || (record.testAppInstanceVO.podStatus !== 1 && !record.testAppInstanceVO.logId)}>
        查看日志
      </MenuItem>
      <MenuItem key="retry">
        重新执行
      </MenuItem>
      {record.moreCycle ? (
        <SubMenu title="测试循环">
          {
            record.cycleDTOS.map(cycle => (
              <MenuItem>
                <Link to={TestExecuteLink(cycle.cycleId)}>{cycle.cycleName}</Link>
              </MenuItem>
            ))
          }
        </SubMenu>
      ) : (
        <MenuItem key="cycle" disabled={!record.cycleIds}>
          {record.cycleIds ? <Link to={TestExecuteLink(record.cycleIds)}>测试循环</Link> : '测试循环'}
        </MenuItem>
      )}
      <MenuItem key="report" disabled={!record.resultId}>
        测试报告
      </MenuItem>
    </Menu>
  );

  const appOptions = appList.map(app => <Option value={app.id}>{app.name}</Option>);
  const ENVS = envList.map(env => ({ text: env.name, value: env.id.toString() }));
  const columns = [{
    title: '运行状态',
    dataIndex: 'podStatus',
    key: 'podStatus',
    filters: PODSTATUS,
    render: (status, record) => {
      const { testAppInstanceVO } = record;
      const { podStatus } = testAppInstanceVO || {};
      return PodStatus(podStatus);
    },
  },
  {
    title: '',
    dataIndex: 'action',
    key: 'action',
    render: (action, record) => (
      <div style={{ display: 'flex' }}>
        <div className="c7ntest-flex-space" />
        <Dropdown overlay={getMenu(record)} trigger={['click']}>
          <Button shape="circle" icon="more_vert" style={{ marginRight: -5 }} />
        </Dropdown>
      </div>
    ),
  },
  {
    title: '环境',
    dataIndex: 'envId',
    key: 'envId',
    filters: ENVS,
    render: (env, record) => {
      const { testAppInstanceVO } = record;
      const { envId } = testAppInstanceVO || {};
      const target = _.find(envList, { id: envId });
      return <span>{target && target.name}</span>;
    },
  },
  {
    title: '执行方',
    dataIndex: 'createUser',
    key: 'createUser',
    render: createUser => <User user={createUser} />,
  }, 
  {
    title: '测试框架',
    dataIndex: 'framework',
    key: 'framework',
    filters: [],
  }, 
  {
    title: '应用版本',
    dataIndex: 'version',
    key: 'version',
    filters: [],
    render: (version, record) => {
      const { testAppInstanceVO } = record;
      const { appVersionName } = testAppInstanceVO || {};
      return <span>{appVersionName}</span>;
    },
  }, 
  {
    title: '时长',
    dataIndex: 'during',
    key: 'during',
    render: (during, record) => {
      const { creationDate, lastUpdateDate } = record;
      const diff = moment(lastUpdateDate).diff(moment(creationDate));
      return creationDate && lastUpdateDate
        ? humanizeDuration(diff)
        : null;
    },
  }, 
  {
    title: '执行时间',
    dataIndex: 'creationDate',
    key: 'creationDate',
    render: creationDate => (
      <TimeAgo
        datetime={creationDate}
        locale={Choerodon.getMessage('zh_CN', 'en')}
      />
    ),
  }, 
  {
    title: '测试结果',
    dataIndex: 'testStatus',
    key: 'testStatus',
    filters: TESTRESULT,
    render: testStatus => TestResult(testStatus),
  }];

  const ModalContent = ({ modal }) => (
    <div>
      <SelectVariable />
    </div>
  );

  const key1 = Modal.key();
  function openModal() {
    Modal.open({
      key: key1,
      title: 'Basic',
      drawer: true,
      children: <ModalContent />,
      okProps: { children: '保存' },
    });
  }

  return (
    <Page className="c7ntest-AutoTestList">
      <Header title={<FormattedMessage id="autotestlist_title" />}>
        <Button onClick={toCreateAutoTest/* openModal */}>
          <Icon type="playlist_add icon" />
          <span>添加测试</span>
        </Button>
      </Header>
      <Breadcrumb />
      <Content>
        <Select
          label="选择应用"
          style={{ width: 512, marginBottom: 20 }}
          filter
          value={currentApp}
          loading={selectLoading}
          onChange={onAppChange}
          onFilterChange={onFilterChange}
          getPopupContainer={trigger => trigger.parentNode}
        >
          {appOptions}
        </Select>
        <Table filterBarPlaceholder="过滤表" loading={loading} columns={columns} dataSource={historyList} pagination={pagination} onChange={onTableChange} />
        <ContainerLog
          ref={onSaveLogRef('ContainerLog')}
        />
        <CreateAutoTest />
      </Content>
    </Page>
  );
};


export default AutoTestList;
