import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

vi.mock('../src/App.jsx', () => ({
  default: () => <div>Mounted application</div>,
}))

describe('application entry point', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.resetModules()
  })

  it('mounts the application in the root element', async () => {
    document.body.innerHTML = '<div id="root"></div>'
    await import('../src/main.jsx')
    expect(await screen.findByText('Mounted application')).toBeInTheDocument()
  })
})
