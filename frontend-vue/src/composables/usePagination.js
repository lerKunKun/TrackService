import { reactive } from 'vue'

export function usePagination(fetchFn, defaultPageSize = 20) {
  const pagination = reactive({
    current: 1,
    pageSize: defaultPageSize,
    total: 0,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`
  })

  const handleTableChange = (pag) => {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    fetchFn()
  }

  const handleSearch = () => {
    pagination.current = 1
    fetchFn()
  }

  return { pagination, handleTableChange, handleSearch }
}
