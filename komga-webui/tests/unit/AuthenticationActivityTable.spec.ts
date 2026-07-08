import AuthenticationActivityTable from '@/components/AuthenticationActivityTable.vue'

describe('AuthenticationActivityTable', () => {
  const methods = (AuthenticationActivityTable as any).options.methods

  it('clears the table immediately and reloads data after a successful clear on the first page', async () => {
    const loadData = jest.fn().mockResolvedValue(undefined)
    const clearAuthenticationActivity = jest.fn().mockResolvedValue(undefined)
    const context = {
      $komgaUsers: {
        clearAuthenticationActivity,
      },
      items: [{email: 'user@example.org'}],
      totalItems: 1,
      options: {page: 1, sortBy: [], sortDesc: [], itemsPerPage: 20},
      loadData,
    }

    await methods.doClearHistory.call(context)

    expect(clearAuthenticationActivity).toHaveBeenCalledTimes(1)
    expect(context.items).toEqual([])
    expect(context.totalItems).toBe(0)
    expect(context.options.page).toBe(1)
    expect(loadData).toHaveBeenCalledTimes(1)
  })

  it('resets to the first page after a successful clear on later pages', async () => {
    const loadData = jest.fn().mockResolvedValue(undefined)
    const clearAuthenticationActivity = jest.fn().mockResolvedValue(undefined)
    const context = {
      $komgaUsers: {
        clearAuthenticationActivity,
      },
      items: [{email: 'user@example.org'}],
      totalItems: 1,
      options: {page: 3, sortBy: [], sortDesc: [], itemsPerPage: 20},
      loadData,
    }

    await methods.doClearHistory.call(context)

    expect(clearAuthenticationActivity).toHaveBeenCalledTimes(1)
    expect(context.items).toEqual([])
    expect(context.totalItems).toBe(0)
    expect(context.options.page).toBe(1)
    expect(loadData).not.toHaveBeenCalled()
  })
})
