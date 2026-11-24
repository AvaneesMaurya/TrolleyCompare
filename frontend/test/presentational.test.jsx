import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import AddProductForm from '../src/components/AddProductForm'
import BasketBuilder from '../src/components/BasketBuilder'
import BasketList from '../src/components/BasketList'
import ComparisonResults from '../src/components/ComparisonResults'
import Header from '../src/components/Header'
import StoreCard from '../src/components/StoreCard'

function BasketBuilderHarness({ initialItems = [] }) {
  const [basketItems, setBasketItems] = useState(initialItems)
  return (
    <>
      <BasketBuilder basketItems={basketItems} setBasketItems={setBasketItems} onCompare={vi.fn()} />
      <output data-testid="items">{JSON.stringify(basketItems)}</output>
    </>
  )
}

describe('Header', () => {
  it('shows guest sign-in and invokes the callback', async () => {
    const user = userEvent.setup()
    const onSignIn = vi.fn()
    render(<Header onSignIn={onSignIn} />)

    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(onSignIn).toHaveBeenCalledOnce()
  })

  it('shows role-aware navigation and invokes the supplied actions', async () => {
    const user = userEvent.setup()
    const actions = Object.fromEntries(
      ['onShowBasket', 'onShowHistory', 'onShowUsers', 'onShowProfile', 'onLogout']
        .map((name) => [name, vi.fn()])
    )
    render(<Header user={{ name: 'Admin', role: 'SUPER_ADMIN' }} activeView="history" {...actions} />)

    for (const [label, action] of [
      ['Compare prices', actions.onShowBasket],
      ['History', actions.onShowHistory],
      ['Manage users', actions.onShowUsers],
      ['My account', actions.onShowProfile],
      ['Sign out', actions.onLogout]
    ]) {
      await user.click(screen.getByRole('button', { name: label }))
      expect(action).toHaveBeenCalledOnce()
    }

    expect(screen.getByText('Admin')).toBeInTheDocument()
  })

  it('hides admin navigation from ordinary users and allows missing basket action', () => {
    render(<Header user={{ name: 'Person', role: 'USER' }} />)
    expect(screen.queryByRole('button', { name: 'Manage users' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Compare prices' })).not.toBeInTheDocument()
  })
})

describe('AddProductForm', () => {
  it('validates input and adds trimmed product details', async () => {
    const user = userEvent.setup()
    const onAddItem = vi.fn()
    render(<AddProductForm onAddItem={onAddItem} />)

    await user.click(screen.getByRole('button', { name: '+ Add Item' }))
    expect(onAddItem).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Product Keyword'), '  Milk  ')
    await user.clear(screen.getByLabelText('Quantity'))
    await user.type(screen.getByLabelText('Quantity'), '0')
    await user.click(screen.getByRole('button', { name: '+ Add Item' }))
    expect(onAddItem).not.toHaveBeenCalled()

    await user.clear(screen.getByLabelText('Quantity'))
    await user.type(screen.getByLabelText('Quantity'), '2')
    await user.selectOptions(screen.getByLabelText('Unit'), 'ml')
    await user.click(screen.getByRole('button', { name: '+ Add Item' }))
    expect(onAddItem).toHaveBeenCalledWith('Milk', 2, 'ml')
    expect(screen.getByLabelText('Product Keyword')).toHaveValue('')
    expect(screen.getByLabelText('Quantity')).toHaveValue(1)
  })

  it('uses matching recent searches, otherwise common-item suggestions', async () => {
    const user = userEvent.setup()
    render(<AddProductForm onAddItem={vi.fn()} recentSearches={[
      { name: 'Milk', quantity: 2, unit: 'L' },
      { name: 'Bread', quantity: 1, unit: 'pcs' }
    ]} />)

    const keyword = screen.getByLabelText('Product Keyword')
    await user.click(keyword)
    expect(screen.getByRole('button', { name: 'Milk (2 L)' })).toBeInTheDocument()
    await user.clear(keyword)
    await user.type(keyword, 'mil')
    await user.click(screen.getByRole('button', { name: 'Milk (2 L)' }))
    expect(keyword).toHaveValue('Milk')
    expect(screen.getByLabelText('Quantity')).toHaveValue(2)
    expect(screen.getByLabelText('Unit')).toHaveValue('L')

    await user.clear(keyword)
    await user.type(keyword, 'unknown')
    expect(screen.getByRole('button', { name: 'Milk (1 L)' })).toBeInTheDocument()
  })
})

describe('BasketBuilder', () => {
  it('adds quick and custom items, validates quantities, removes, clears, and compares', async () => {
    const user = userEvent.setup()
    render(<BasketBuilderHarness initialItems={[{ productName: 'Existing', quantity: 1, unit: 'L' }]} />)

    await user.click(screen.getByRole('button', { name: /Milk/ }))
    expect(screen.getByTestId('items')).toHaveTextContent('"productName":"Milk"')

    await user.type(screen.getByLabelText('Product Keyword'), 'Rice')
    await user.clear(screen.getByLabelText('Qty'))
    await user.type(screen.getByLabelText('Qty'), '0')
    await user.click(screen.getByRole('button', { name: 'Add Item' }))
    expect(screen.getByTestId('items')).not.toHaveTextContent('"productName":"Rice"')

    await user.clear(screen.getByLabelText('Qty'))
    await user.type(screen.getByLabelText('Qty'), '2')
    await user.click(screen.getByRole('button', { name: 'Add Item' }))
    expect(screen.getByTestId('items')).toHaveTextContent('"productName":"Rice","quantity":2')

    await user.click(screen.getAllByRole('button', { name: '✕' })[0])
    await user.click(screen.getByRole('button', { name: /Compare Supermarkets/ }))
    await user.click(screen.getByRole('button', { name: 'Clear' }))
    expect(screen.getByTestId('items')).toHaveTextContent('[]')
  })
})

describe('BasketList', () => {
  it('returns nothing for empty baskets and wires list actions', async () => {
    const user = userEvent.setup()
    const onRemoveItem = vi.fn()
    const onClearBasket = vi.fn()
    const onCompare = vi.fn()
    const { container, rerender } = render(<BasketList basketItems={[]} onRemoveItem={onRemoveItem} />)
    expect(container).toBeEmptyDOMElement()

    rerender(<BasketList
      basketItems={[{ productName: 'Milk', quantity: 1, unit: 'L' }]}
      onRemoveItem={onRemoveItem}
      onClearBasket={onClearBasket}
      onCompare={onCompare}
    />)
    await user.click(screen.getByRole('button', { name: '✕' }))
    await user.click(screen.getByTitle('Clear list'))
    await user.click(screen.getByRole('button', { name: 'Compare Supermarkets' }))
    expect(onRemoveItem).toHaveBeenCalledWith(0)
    expect(onClearBasket).toHaveBeenCalledOnce()
    expect(onCompare).toHaveBeenCalledOnce()
  })
})

describe('ComparisonResults', () => {
  it('renders loading and empty states', () => {
    const { rerender } = render(<ComparisonResults loading comparisonResult={null} />)
    expect(screen.getByText(/Comparing prices/)).toBeInTheDocument()
    rerender(<ComparisonResults loading={false} comparisonResult={[]} />)
    expect(screen.getByText(/Add items to your basket/)).toBeInTheDocument()
  })

  it('renders cheapest totals, club discounts, and unavailable item values', () => {
    render(<ComparisonResults loading={false} comparisonResult={[
      {
        storeName: 'Tesco',
        totalRegularPrice: 4,
        totalClubPrice: 3.5,
        items: [
          { productName: 'Milk', storePackageQuantity: 1, storePackageUnit: 'L', calculatedRegularPrice: 4, calculatedClubPrice: 3.5 },
          { productName: 'NA', storePackageQuantity: null, storePackageUnit: 'NA', calculatedRegularPrice: null, calculatedClubPrice: null }
        ]
      },
      {
        storeName: 'Lidl',
        totalRegularPrice: 5,
        totalClubPrice: 5,
        items: [{ productName: 'Bread', storePackageQuantity: null, storePackageUnit: null, calculatedRegularPrice: 5, calculatedClubPrice: 5 }]
      }
    ]} />)

    expect(screen.getByText(/Cheapest Store/)).toBeInTheDocument()
    expect(screen.getAllByText('€3.50')).toHaveLength(3)
    expect(screen.getByText('Club')).toBeInTheDocument()
    expect(screen.getByText('❌ Not Available')).toBeInTheDocument()
    expect(screen.getAllByText('NA', { selector: 'td' })).toHaveLength(3)
  })

  it('handles stores without optional items or prices', () => {
    render(<ComparisonResults loading={false} comparisonResult={[
      { storeName: 'Aldi', totalRegularPrice: null, totalClubPrice: null }
    ]} />)
    expect(screen.getByText('N/A')).toBeInTheDocument()
  })
})

describe('StoreCard', () => {
  it('shows cheapest and regular-only prices', () => {
    const { rerender } = render(<StoreCard
      cheapestStoreName="Aldi"
      store={{
        storeName: 'Aldi',
        totalRegularPrice: 3,
        totalClubPrice: 2.5,
        items: [{ productName: 'Milk', quantity: 1, regularSubtotal: 3 }]
      }}
    />)
    expect(screen.getByText('🏆 Cheapest')).toBeInTheDocument()
    expect(screen.getByText('Club Total')).toBeInTheDocument()

    rerender(<StoreCard
      cheapestStoreName="Other"
      store={{
        storeName: 'Lidl',
        totalRegularPrice: 0,
        totalClubPrice: null,
        items: [{ productName: 'Bread', quantity: 1, regularSubtotal: null }]
      }}
    />)
    expect(screen.getByText('Store Option')).toBeInTheDocument()
    expect(screen.getByText('N/A')).toBeInTheDocument()
  })
})
