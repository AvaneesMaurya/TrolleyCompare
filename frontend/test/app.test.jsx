import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../src/App'
import { callGraphql, refreshCsrfToken, requestApi } from '../src/services/api'

vi.mock('../src/services/api', () => ({
  callGraphql: vi.fn(),
  COMPARE_BASKET_QUERY: 'compare basket query',
  refreshCsrfToken: vi.fn(),
  requestApi: vi.fn(),
}))

const unauthorized = () => Object.assign(new Error('Unauthorized'), { status: 401 })
const comparison = [{
  storeName: 'Tesco',
  totalRegularPrice: 1.2,
  totalClubPrice: 1.1,
  items: [{
    productName: 'Milk',
    storePackageQuantity: 1,
    storePackageUnit: 'L',
    calculatedRegularPrice: 1.2,
    calculatedClubPrice: 1.1,
  }],
}]

beforeEach(() => {
  vi.resetAllMocks()
  refreshCsrfToken.mockResolvedValue(undefined)
  requestApi.mockImplementation((path) => {
    if (path === '/auth/me') return Promise.reject(unauthorized())
    return Promise.resolve([])
  })
})

describe('App', () => {
  it('loads as guest, compares a basket, shows pending state, and clears results', async () => {
    const user = userEvent.setup()
    let resolveComparison
    callGraphql.mockImplementation(() => new Promise((resolve) => { resolveComparison = resolve }))
    render(<App />)
    await screen.findByText('Welcome back')

    await user.click(screen.getByRole('button', { name: 'Continue without signing in' }))
    await user.type(screen.getByLabelText('Product Keyword'), 'Milk')
    await user.click(screen.getByRole('button', { name: '+ Add Item' }))
    await user.click(screen.getByRole('button', { name: 'Compare Supermarkets' }))

    expect(await screen.findByText(/Comparing prices/)).toBeInTheDocument()
    expect(callGraphql).toHaveBeenCalledWith('compare basket query', {
      items: [{ name: 'Milk', quantity: 1, unit: 'L' }],
    })
    resolveComparison({ compareBasket: comparison })
    expect(await screen.findByText('Supermarket Comparisons')).toBeInTheDocument()
    expect(screen.getAllByText('€1.10')).not.toHaveLength(0)

    await user.click(screen.getByTitle('Clear list'))
    expect(screen.getByText(/Add items to your basket/)).toBeInTheDocument()
  })

  it('shows session initialization failures and comparison errors', async () => {
    const user = userEvent.setup()
    requestApi.mockRejectedValueOnce(new Error('Session service unavailable'))
    render(<App />)
    expect(await screen.findByText('Session service unavailable')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Continue without signing in' }))

    await user.type(screen.getByLabelText('Product Keyword'), 'Bread')
    await user.click(screen.getByRole('button', { name: '+ Add Item' }))
    callGraphql.mockRejectedValueOnce(new Error('Comparison failed'))
    await user.click(screen.getByRole('button', { name: 'Compare Supermarkets' }))

    expect(await screen.findByText('Comparison failed')).toBeInTheDocument()
    expect(screen.getByText(/Add items to your basket/)).toBeInTheDocument()
  })

  it('logs in, loads recent searches, saves a comparison, and logs out', async () => {
    const user = userEvent.setup()
    const signedIn = { id: 8, name: 'Admin', email: 'admin@example.com', role: 'SUPER_ADMIN' }
    requestApi.mockImplementation((path, options = {}) => {
      if (path === '/auth/me' && options.method === 'PUT') return Promise.resolve(signedIn)
      if (path === '/auth/me') return Promise.reject(unauthorized())
      if (path === '/auth/login') return Promise.resolve(signedIn)
      if (path === '/history/searches') return Promise.resolve([{ name: 'Milk', quantity: 1, unit: 'L' }])
      if (path === '/history' && options.method === 'POST') return Promise.resolve({ results: comparison })
      if (path === '/auth/logout') return Promise.resolve()
      return Promise.resolve([])
    })
    render(<App />)
    await screen.findByText('Welcome back')
    await user.type(screen.getByLabelText('Email'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getAllByRole('button', { name: 'Sign in' }).at(-1))

    expect(await screen.findByText('Admin')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Product Keyword'), 'Milk')
    await user.click(screen.getByRole('button', { name: '+ Add Item' }))
    await user.click(screen.getByRole('button', { name: 'Compare Supermarkets' }))
    expect(await screen.findByText('Supermarket Comparisons')).toBeInTheDocument()
    expect(requestApi).toHaveBeenCalledWith('/history', expect.objectContaining({ method: 'POST' }))
    expect(screen.getByRole('button', { name: 'Manage users' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'My account' }))
    expect(await screen.findByRole('heading', { name: 'My account' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'History' }))
    await screen.findByText(/No saved comparisons yet/)
    await user.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(await screen.findByText('Welcome back')).toBeInTheDocument()
    expect(requestApi).toHaveBeenCalledWith('/auth/logout', { method: 'POST' })
  })

  it('shows saved history, restores an entry, and routes admins to user management', async () => {
    const user = userEvent.setup()
    const signedIn = { id: 1, name: 'Admin', email: 'admin@example.com', role: 'SUPER_ADMIN' }
    const entry = {
      id: 2,
      createdAt: '2026-01-01T10:00:00Z',
      items: [{ name: 'Milk', quantity: 1, unit: 'L' }],
      results: comparison,
    }
    requestApi.mockImplementation((path) => {
      if (path === '/auth/me') return Promise.resolve(signedIn)
      if (path === '/history/searches') return Promise.resolve([])
      if (path === '/history') return Promise.resolve([entry])
      if (path === '/admin/users') return Promise.resolve([])
      return Promise.resolve()
    })
    render(<App />)
    await screen.findByText('Admin')

    await user.click(screen.getByRole('button', { name: 'History' }))
    await user.click(await screen.findByRole('button', { name: 'View results' }))
    expect(await screen.findByText('Supermarket Comparisons')).toBeInTheDocument()
    expect(screen.getByText('Milk (1 L)')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Manage users' }))
    expect(await screen.findByRole('heading', { name: 'User management' })).toBeInTheDocument()
    await waitFor(() => expect(requestApi).toHaveBeenCalledWith('/admin/users'))
  })
})
