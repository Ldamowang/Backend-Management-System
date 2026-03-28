export interface TreeNode {
  id: number
  parentId: number
  children?: TreeNode[]
  sortOrder?: number
  [key: string]: string | number | boolean | TreeNode[] | undefined
}

/** 列表转树形结构 */
export function listToTree<T extends TreeNode>(list: T[], parentId = 0): T[] {
  return list
    .filter(item => item.parentId === parentId)
    .map(item => ({
      ...item,
      children: listToTree(list, item.id)
    }))
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
}

/** 树形结构转列表 */
export function treeToList<T extends TreeNode>(tree: T[]): T[] {
  const result: T[] = []
  const walk = (nodes: T[]) => {
    nodes.forEach(node => {
      result.push(node)
      if (node.children?.length) {
        walk(node.children as T[])
      }
    })
  }
  walk(tree)
  return result
}

/** 在树中查找节点 */
export function findNode<T extends TreeNode>(
  tree: T[],
  predicate: (node: T) => boolean
): T | undefined {
  for (const node of tree) {
    if (predicate(node)) return node
    if (node.children?.length) {
      const found = findNode(node.children as T[], predicate)
      if (found) return found
    }
  }
  return undefined
}
