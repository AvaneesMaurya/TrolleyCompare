const GRAPHQL_ENDPOINT = 'http://localhost:8080/graphql';

export const COMPARE_BASKET_QUERY = `
  query CompareBasket($items: [ItemInput!]!) {
    compareBasket(items: $items) {
      storeName
      totalRegularPrice
      totalClubPrice
      items {
        productName
        storePackageQuantity
        storePackageUnit
        calculatedRegularPrice
        calculatedClubPrice
      }
    }
  }
`;

export async function callGraphql(query, variables = {}) {
  try {
    const response = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables })
    });

    const json = await response.json();
    if (json.errors) {
      throw new Error(json.errors[0].message);
    }
    return json.data;
  } catch (err) {
    console.error("GraphQL Error:", err);
    throw err;
  }
}