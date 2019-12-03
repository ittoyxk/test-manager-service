/* eslint-disable no-param-reassign */
import {
  observable, action, computed, toJS,
} from 'mobx';
import { find } from 'lodash';
import { getPlanTree } from '@/api/TestPlanApi';
// import { getIssueTree } from '@/api/IssueManageApi';

class TestPlanTreeStore {
  @observable testPlanStatus = 'todo';

  @action setTestPlanStatus = (testPlanStatus) => {
    this.testPlanStatus = testPlanStatus;
  }

  @observable treeData = {
    rootIds: [],
    treeFolder: [],
  }

  @observable selectedKeys = [];

  @observable currentCycle = {};

  @observable treeLoading = false;

  treeRef = null;

  @action clearStore = () => {
    this.currentCycle = {};
    this.treeData = {
      rootIds: [],
      treeFolder: [],
    };
    this.treeRef = null;
  }

  @computed get getTreeData() {
    return toJS(this.treeData);
  }

  @computed get getSelectedKeys() {
    return toJS(this.selectedKeys);
  }

  @computed get getCurrentCycle() {
    return this.currentCycle;
  }

  getId(id = this.currentCycle.id) {
    if (!id) {
      return [id, ''];
    }
    if (typeof id === 'number') {
      return [id, ''];
    } else if (id.split('-').length === 2) {
      const [planId, folderId] = id.split('-');
      return [planId, folderId];
    } else {
      return [id, ''];
    }
  }

  @computed get getCurrentPlanId() {
    const [planId, folderId] = this.getId();
    return planId;
  }

  @computed get getPreCycle() {
    return toJS(this.preCycle);
  }

  @action setSelectedKeys(selectedKeys) {
    this.selectedKeys = selectedKeys;
  }

  async loadIssueTree(defaultSelectId) {
    this.setTreeLoading(true);
    const treeData = await getPlanTree(this.testPlanStatus);
    this.setTreeData(treeData, defaultSelectId);
    this.setTreeLoading(false);
  }

  @action setTreeData(treeData, defaultSelectId) {
    const { rootIds, treeFolder } = treeData;
    const planIds = (rootIds && rootIds.slice(0, 5).map(id => `${id}-plan`)) || [];
    // 选中之前选中的
    let selectedId = this.currentCycle ? this.currentCycle.id : undefined;
    if (!this.currentCycle.id && rootIds.length > 0) {
      selectedId = defaultSelectId ? Number(defaultSelectId) : rootIds[0];
    }
    this.treeData = {
      rootIds: planIds,
      treeFolder: (treeFolder && treeFolder.map((folder) => {
        const {
          id, planId, issueFolderVO, expanded, children, ...other
        } = folder;
        return {
          id: planId ? `${planId}-${id}` : id,
          children: children ? children.map(child => `${planId || id}-${child}`) : [],
          data: issueFolderVO,
          isExpanded: expanded,
          selected: folder.id === selectedId,
          ...other,
        };
      })) || [],
    };
    if (selectedId) {
      this.setCurrentCycle(find(this.treeData.treeFolder, { id: selectedId }) || {});
    }
  }

  @action setCurrentCycle(currentCycle) {
    this.currentCycle = currentCycle;
  }

  @action setCurrentCycleById(id) {
    const data = find(this.treeData.treeFolder, { id });
    if (data) {
      this.setCurrentCycle(data);
    }
  }

  @action setTreeLoading = (treeLoading) => {
    this.treeLoading = treeLoading;
  }

  @action setTreeRef = (treeRef) => {
    this.treeRef = treeRef;
  }
}

export default TestPlanTreeStore;
