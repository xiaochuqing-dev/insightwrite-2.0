import { ref } from 'vue'

export function usePagedResource(loader, options = {}) {
  const items = ref([])
  const page = ref(options.initialPage || 1)
  const total = ref(0)
  const totalPages = ref(0)
  const isLoading = ref(true)
  const errorMsg = ref('')
  const fallbackError = options.fallbackError || '加载失败'

  async function load() {
    isLoading.value = true
    errorMsg.value = ''
    try {
      const data = await loader(page.value)
      items.value = data.items || []
      total.value = data.total || 0
      totalPages.value = data.pages || 0
    } catch (e) {
      errorMsg.value = e?.response?.data?.error || fallbackError
    } finally {
      isLoading.value = false
    }
  }

  function goPage(nextPage) {
    page.value = nextPage
    return load()
  }

  function resetPage() {
    page.value = 1
  }

  return {
    items,
    page,
    total,
    totalPages,
    isLoading,
    errorMsg,
    load,
    goPage,
    resetPage
  }
}
