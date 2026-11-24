import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { requestApi } from '../src/services/api'
import AuthPage from '../src/components/AuthPage'
import ComparisonHistory from '../src/components/ComparisonHistory'
import UserManagement from '../src/components/UserManagement'
import UserProfile from '../src/components/UserProfile'

vi.mock('../src/services/api', () => ({ requestApi: vi.fn() }))

beforeEach(() => {
  vi.resetAllMocks()
  requestApi.mockResolvedValue([])
})

describe('AuthPage', () => {
  it('continues as a guest and signs in successfully', async () => {
    const user = userEvent.setup()
    const onContinueAsGuest = vi.fn()
    const onLogin = vi.fn()
    requestApi.mockResolvedValue({ id: 5, name: 'Person' })
    render(<AuthPage onLogin={onLogin} onContinueAsGuest={onContinueAsGuest} />)

    await user.click(screen.getByRole('button', { name: 'Continue without signing in' }))
    expect(onContinueAsGuest).toHaveBeenCalledOnce()

    await user.type(screen.getByLabelText('Email'), 'person@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getAllByRole('button', { name: 'Sign in' }).at(-1))

    await waitFor(() => expect(onLogin).toHaveBeenCalledWith({ id: 5, name: 'Person' }))
    expect(requestApi).toHaveBeenCalledWith('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'person@example.com', password: 'password123' }),
    })
  })

  it('registers an account, clears its password, and returns to login mode', async () => {
    const user = userEvent.setup()
    requestApi.mockResolvedValueOnce({})
    render(<AuthPage onLogin={vi.fn()} onContinueAsGuest={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: 'Create an account' }))

    await user.type(screen.getByLabelText('Name'), 'New Person')
    await user.type(screen.getByLabelText('Email'), 'new@example.com')
    const password = screen.getByLabelText('Password')
    await user.type(password, 'password123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Account created')
    expect(requestApi).toHaveBeenCalledWith('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name: 'New Person', email: 'new@example.com', password: 'password123' }),
    })
    expect(password).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('shows login and registration errors and resets messages when switching modes', async () => {
    const user = userEvent.setup()
    requestApi.mockRejectedValueOnce(new Error('Invalid credentials'))
      .mockRejectedValueOnce(new Error('Email already exists'))
    render(<AuthPage onLogin={vi.fn()} onContinueAsGuest={vi.fn()} />)

    await user.type(screen.getByLabelText('Email'), 'person@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials')

    await user.click(screen.getByRole('button', { name: 'Create an account' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await user.type(screen.getByLabelText('Name'), 'Person')
    await user.clear(screen.getByLabelText('Email'))
    await user.type(screen.getByLabelText('Email'), 'person@example.com')
    await user.clear(screen.getByLabelText('Password'))
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Email already exists')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continue without signing in' })).toBeInTheDocument()
  })
})

describe('UserProfile', () => {
  it('submits the profile, updates the parent, and clears the password', async () => {
    const user = userEvent.setup()
    const onProfileUpdated = vi.fn()
    const updatedUser = { id: 1, name: 'Updated', email: 'updated@example.com', role: 'USER' }
    requestApi.mockResolvedValue(updatedUser)
    render(<UserProfile user={{ id: 1, name: 'Person', email: 'person@example.com', role: 'USER' }} onProfileUpdated={onProfileUpdated} />)

    await user.clear(screen.getByLabelText('Name'))
    await user.type(screen.getByLabelText('Name'), 'Updated')
    await user.type(screen.getByLabelText(/New password/), 'newpassword123')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByRole('status')).toHaveTextContent('profile has been updated')
    expect(onProfileUpdated).toHaveBeenCalledWith(updatedUser)
    expect(requestApi).toHaveBeenCalledWith('/auth/me', {
      method: 'PUT',
      body: JSON.stringify({ name: 'Updated', email: 'person@example.com', password: 'newpassword123' }),
    })
    expect(screen.getByLabelText(/New password/)).toHaveValue('')
    expect(screen.getByText('User', { selector: 'span' })).toBeInTheDocument()
  })

  it('submits a null password when left blank and reports failures', async () => {
    const user = userEvent.setup()
    requestApi.mockRejectedValueOnce(new Error('Email is taken'))
    render(<UserProfile user={{ id: 1, name: 'Admin', email: 'a@example.com', role: 'SUPER_ADMIN' }} onProfileUpdated={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email is taken')
    expect(requestApi).toHaveBeenCalledWith('/auth/me', {
      method: 'PUT',
      body: JSON.stringify({ name: 'Admin', email: 'a@example.com', password: null }),
    })
    expect(screen.getByText('Super admin')).toBeInTheDocument()
  })
})

describe('ComparisonHistory', () => {
  it('shows loading before resolving records and returns a selected comparison', async () => {
    const user = userEvent.setup()
    let resolveHistory
    requestApi.mockImplementation(() => new Promise((resolve) => { resolveHistory = resolve }))
    const onViewComparison = vi.fn()
    const { unmount } = render(<ComparisonHistory onViewComparison={onViewComparison} />)
    expect(screen.getByText(/Loading comparison history/)).toBeInTheDocument()
    await waitFor(() => expect(resolveHistory).toBeTypeOf('function'))

    const entry = {
      id: 1,
      createdAt: '2026-01-01T10:00:00Z',
      items: [{ name: 'Milk', quantity: 1, unit: 'L' }],
      results: [{ storeName: 'Tesco', totalRegularPrice: 1.2, totalClubPrice: 1.1 }],
    }
    resolveHistory([entry])
    await user.click(await screen.findByRole('button', { name: 'View results' }))
    expect(onViewComparison).toHaveBeenCalledWith(entry)
    expect(screen.getByText(/Milk \(1 L\)/)).toBeInTheDocument()
    unmount()
  })

  it('renders empty and error states', async () => {
    requestApi.mockResolvedValueOnce([])
    const firstRender = render(<ComparisonHistory onViewComparison={vi.fn()} />)
    expect(await screen.findByText(/No saved comparisons yet/)).toBeInTheDocument()

    firstRender.unmount()
    requestApi.mockRejectedValueOnce(new Error('History unavailable'))
    render(<ComparisonHistory onViewComparison={vi.fn()} />)
    expect(await screen.findByRole('alert')).toHaveTextContent('History unavailable')
  })
})

describe('UserManagement', () => {
  it('shows an empty state and fetch errors', async () => {
    requestApi.mockResolvedValueOnce([])
    const firstRender = render(<UserManagement currentUserId={1} />)
    expect(await screen.findByText('No user accounts found.')).toBeInTheDocument()

    firstRender.unmount()
    requestApi.mockRejectedValueOnce(new Error('Access denied'))
    render(<UserManagement currentUserId={1} />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Access denied')
  })

  it('creates users and reloads the table', async () => {
    const user = userEvent.setup()
    requestApi.mockResolvedValueOnce([])
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce([{ id: 2, name: 'New Person', email: 'new@example.com', role: 'USER', enabled: true }])
    render(<UserManagement currentUserId={1} />)
    await screen.findByText('No user accounts found.')

    await user.type(screen.getByLabelText('Name'), 'New Person')
    await user.type(screen.getByLabelText('Email'), 'new@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Create user' }))

    expect(await screen.findByText('New Person')).toBeInTheDocument()
    expect(requestApi).toHaveBeenNthCalledWith(2, '/admin/users', {
      method: 'POST',
      body: JSON.stringify({ name: 'New Person', email: 'new@example.com', password: 'password123' }),
    })
    expect(screen.getByText('Active')).toBeInTheDocument()
  })

  it('updates, cancels, and deletes users while protecting current and admin accounts', async () => {
    const user = userEvent.setup()
    const otherUser = { id: 2, name: 'Other', email: 'other@example.com', role: 'USER', enabled: false }
    const admin = { id: 3, name: 'Admin', email: 'admin@example.com', role: 'SUPER_ADMIN', enabled: true }
    requestApi.mockResolvedValueOnce([
      { id: 1, name: 'Current', email: 'current@example.com', role: 'USER', enabled: true },
      otherUser,
      admin,
    ])
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce([otherUser, admin])
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce([])
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<UserManagement currentUserId={1} />)
    await screen.findByText('Other')

    expect(screen.getByText('Current account protected')).toBeInTheDocument()
    expect(within(screen.getByText('Admin').closest('tr')).queryByRole('button', { name: 'Delete' }))
      .not.toBeInTheDocument()
    expect(screen.getByText('Disabled')).toBeInTheDocument()

    await user.click(within(screen.getByText('Other').closest('tr')).getByRole('button', { name: 'Edit' }))
    expect(screen.getByRole('heading', { name: 'Edit user' })).toBeInTheDocument()
    expect(screen.getByLabelText('Role')).toHaveValue('USER')
    await user.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.getByRole('heading', { name: 'Create user' })).toBeInTheDocument()

    await user.click(within(screen.getByText('Other').closest('tr')).getByRole('button', { name: 'Edit' }))
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByText('Other')).toBeInTheDocument()
    expect(requestApi).toHaveBeenNthCalledWith(2, '/admin/users/2', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({ name: 'Other', email: 'other@example.com', password: null, enabled: false, role: 'USER' }),
    }))

    confirm.mockReturnValueOnce(false)
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    expect(requestApi).toHaveBeenCalledTimes(3)

    confirm.mockReturnValueOnce(true)
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    await waitFor(() => expect(requestApi).toHaveBeenNthCalledWith(4, '/admin/users/2', { method: 'DELETE' }))
    expect(await screen.findByText('No user accounts found.')).toBeInTheDocument()
  })

  it('displays create and update request failures', async () => {
    const user = userEvent.setup()
    requestApi.mockResolvedValueOnce([])
      .mockRejectedValueOnce(new Error('Cannot create user'))
    render(<UserManagement currentUserId={1} />)
    await screen.findByText('No user accounts found.')
    await user.type(screen.getByLabelText('Name'), 'Person')
    await user.type(screen.getByLabelText('Email'), 'person@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Create user' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot create user')
  })
})
