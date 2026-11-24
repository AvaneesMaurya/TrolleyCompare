import { beforeEach, describe, expect, it, vi } from 'vitest'

const jsonResponse = (body, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { 'content-type': 'application/json' },
})

describe('API client', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_API_ORIGIN', '')
    vi.stubGlobal('fetch', vi.fn())
  })

  it('refreshes CSRF state and requests a static path with credentials', async () => {
    fetch
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ id: 1 }))
    const api = await import('../src/services/api')

    await api.refreshCsrfToken()
    await expect(api.requestApi('/history')).resolves.toEqual({ id: 1 })

    expect(fetch).toHaveBeenNthCalledWith(1, `${window.location.origin}/api/auth/csrf`, {
      credentials: 'include',
    })
    expect(fetch.mock.calls[1][1].credentials).toBe('include')
    expect(fetch.mock.calls[1][1].method).toBe('GET')
  })

  it('rejects invalid API origins and unsupported paths before making requests', async () => {
    vi.stubEnv('VITE_API_ORIGIN', 'javascript:alert(1)')
    await expect(import('../src/services/api')).rejects.toThrow('Invalid API origin configuration.')

    vi.stubEnv('VITE_API_ORIGIN', '')
    vi.resetModules()
    const api = await import('../src/services/api')
    await expect(api.requestApi('/admin/users/1?unexpected=true')).rejects.toThrow('Unsupported API path.')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('validates CSRF response and applies the token to state-changing requests', async () => {
    fetch
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ ok: true }))
    const api = await import('../src/services/api')

    await expect(api.requestApi('/auth/login', {
      method: 'post',
      body: JSON.stringify({ email: 'person@example.com' }),
    })).resolves.toEqual({ ok: true })

    const request = fetch.mock.calls[1][1]
    expect(request.method).toBe('POST')
    expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-value')
    expect(request.headers.get('Content-Type')).toBe('application/json')
  })

  it('reports CSRF initialization failures', async () => {
    fetch.mockResolvedValueOnce(jsonResponse({ message: 'unavailable' }, 503))
    const api = await import('../src/services/api')
    await expect(api.refreshCsrfToken()).rejects.toThrow('Could not initialize a secure session (503).')
  })

  it.each([
    [{ detail: 'Validation failed' }, 'Validation failed'],
    [{ message: 'Permission denied' }, 'Permission denied'],
    [{}, 'Request failed (409).'],
  ])('uses server error details when a request fails', async (body, expected) => {
    fetch
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse(body, 409))
    const api = await import('../src/services/api')

    await expect(api.requestApi('/auth/logout', { method: 'POST' })).rejects.toMatchObject({
      message: expected,
      status: 409,
    })
  })

  it('handles non-JSON responses and permits user-specific paths', async () => {
    fetch
      .mockResolvedValueOnce(new Response('not json', {
        status: 502,
        headers: { 'content-type': 'text/plain' },
      }))
      .mockResolvedValueOnce(jsonResponse({ user: true }))
    const api = await import('../src/services/api')

    await expect(api.requestApi('/auth/me')).rejects.toMatchObject({
      message: 'Request failed (502).',
      status: 502,
    })
    await expect(api.requestApi('/admin/users/user_2')).resolves.toEqual({ user: true })
  })

  it('sends GraphQL requests and returns the data payload', async () => {
    fetch
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ data: { compareBasket: [] } }))
    const api = await import('../src/services/api')

    await expect(api.callGraphql('query {}', { item: 'Milk' })).resolves.toEqual({ compareBasket: [] })
    const [url, options] = fetch.mock.calls[1]
    expect(url).toBe(`${window.location.origin}/graphql`)
    expect(options.method).toBe('POST')
    expect(options.headers['X-XSRF-TOKEN']).toBe('csrf-value')
    expect(JSON.parse(options.body)).toEqual({ query: 'query {}', variables: { item: 'Milk' } })
  })

  it('propagates GraphQL errors and logs the failed request', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {})
    fetch
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ errors: [{ message: 'Bad query' }] }))
    const api = await import('../src/services/api')

    await expect(api.callGraphql('invalid')).rejects.toThrow('Bad query')
    expect(log).toHaveBeenCalledOnce()

    fetch
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({}, 503))
    vi.resetModules()
    const apiAfterReset = await import('../src/services/api')
    await expect(apiAfterReset.callGraphql('query {}')).rejects.toThrow('GraphQL request failed (503).')
  })
})
