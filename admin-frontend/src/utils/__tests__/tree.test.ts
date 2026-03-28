import { describe, it, expect } from 'vitest'
import { listToTree, treeToList, findNode, type TreeNode } from '../tree'

const flatMenus: TreeNode[] = [
  { id: 1, parentId: 0, menuName: '系统管理', sortOrder: 2 },
  { id: 2, parentId: 0, menuName: '首页', sortOrder: 1 },
  { id: 3, parentId: 1, menuName: '用户管理', sortOrder: 1 },
  { id: 4, parentId: 1, menuName: '角色管理', sortOrder: 2 },
  { id: 5, parentId: 3, menuName: '添加用户', sortOrder: 1 }
]

describe('tree utils', () => {
  describe('listToTree', () => {
    it('将平铺列表转换为树形结构', () => {
      const tree = listToTree(flatMenus)

      expect(tree).toHaveLength(2)
      // sortOrder: 首页(1) 在 系统管理(2) 前面
      expect(tree[0].menuName).toBe('首页')
      expect(tree[1].menuName).toBe('系统管理')
    })

    it('正确嵌套子节点', () => {
      const tree = listToTree(flatMenus)
      const sysMenu = tree.find(n => n.menuName === '系统管理')!

      expect(sysMenu.children).toHaveLength(2)
      expect(sysMenu.children![0].menuName).toBe('用户管理')
      expect(sysMenu.children![1].menuName).toBe('角色管理')
    })

    it('支持三级嵌套', () => {
      const tree = listToTree(flatMenus)
      const userMenu = tree
        .find(n => n.menuName === '系统管理')!
        .children!.find(n => n.menuName === '用户管理')!

      expect(userMenu.children).toHaveLength(1)
      expect(userMenu.children![0].menuName).toBe('添加用户')
    })

    it('空列表返回空数组', () => {
      expect(listToTree([])).toEqual([])
    })

    it('支持自定义 parentId', () => {
      const items: TreeNode[] = [
        { id: 10, parentId: 5, menuName: 'A' },
        { id: 11, parentId: 5, menuName: 'B' }
      ]
      const tree = listToTree(items, 5)
      expect(tree).toHaveLength(2)
    })
  })

  describe('treeToList', () => {
    it('将树形结构展平为列表', () => {
      const tree = listToTree(flatMenus)
      const list = treeToList(tree)

      // 所有节点都应存在
      expect(list).toHaveLength(5)
    })

    it('空树返回空数组', () => {
      expect(treeToList([])).toEqual([])
    })

    it('保持 DFS 遍历顺序', () => {
      const tree = listToTree(flatMenus)
      const list = treeToList(tree)
      const names = list.map(n => n.menuName)

      // 首页先（sortOrder=1），然后系统管理及其子节点
      expect(names[0]).toBe('首页')
      expect(names[1]).toBe('系统管理')
      expect(names[2]).toBe('用户管理')
      expect(names[3]).toBe('添加用户')
      expect(names[4]).toBe('角色管理')
    })
  })

  describe('findNode', () => {
    it('找到根节点', () => {
      const tree = listToTree(flatMenus)
      const node = findNode(tree, n => n.id === 2)

      expect(node).toBeDefined()
      expect(node!.menuName).toBe('首页')
    })

    it('找到深层嵌套节点', () => {
      const tree = listToTree(flatMenus)
      const node = findNode(tree, n => n.id === 5)

      expect(node).toBeDefined()
      expect(node!.menuName).toBe('添加用户')
    })

    it('未找到返回 undefined', () => {
      const tree = listToTree(flatMenus)
      const node = findNode(tree, n => n.id === 999)

      expect(node).toBeUndefined()
    })

    it('空树返回 undefined', () => {
      expect(findNode([] as TreeNode[], n => n.id === 1)).toBeUndefined()
    })
  })
})
